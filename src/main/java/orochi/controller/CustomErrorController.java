package orochi.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import orochi.model.Transaction;
import orochi.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Optional;

@Controller
public class CustomErrorController implements ErrorController {

    private static final Logger logger = LoggerFactory.getLogger(CustomErrorController.class);

    @Autowired
    private TransactionRepository transactionRepository;

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        // Check if this is a VNPay return (has VNPay parameters)
        String vnpTxnRef = request.getParameter("vnp_TxnRef");
        String vnpResponseCode = request.getParameter("vnp_ResponseCode");
        String vnpAmount = request.getParameter("vnp_Amount");

        if (vnpTxnRef != null && vnpResponseCode != null) {
            logger.info("VNPay return detected in error controller, redirecting to payment return handler");
            return handleVNPayReturn(request, model);
        }

        // Regular error handling
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        String errorMessage = "An unexpected error occurred.";
        Integer statusCode = null;

        if (status != null) {
            statusCode = Integer.valueOf(status.toString());
            model.addAttribute("statusCode", statusCode);

            if (statusCode == HttpStatus.NOT_FOUND.value()) {
                errorMessage = "The page you are looking for does not exist.";
            } else if (statusCode == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
                errorMessage = "There was an internal server error. Please try again later.";
            } else if (statusCode == HttpStatus.FORBIDDEN.value()) {
                errorMessage = "You do not have permission to access this page.";
            }
        } else {
            model.addAttribute("statusCode", "Error");
        }

        Object exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        if (exception instanceof Exception) {
            // Log the exception details for debugging purposes
            System.err.println("Exception occurred: " + ((Exception) exception).getMessage());
            ((Exception) exception).printStackTrace();

            // Add a generic message to the model
            model.addAttribute("exceptionMessage", "An error occurred. Please contact support if the issue persists.");
        }

        Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        if (message != null && !message.toString().isEmpty()){
            errorMessage = message.toString();
        }

        model.addAttribute("errorMessage", errorMessage);
        return "error"; // Name of the error HTML file (e.g., error.html)
    }

    private String handleVNPayReturn(HttpServletRequest request, Model model) {
        try {
            logger.info("=== HANDLING VNPAY PAYMENT RETURN IN ERROR CONTROLLER ===");

            String transactionId = request.getParameter("vnp_TxnRef");
            String paymentStatus = request.getParameter("vnp_ResponseCode");
            String amountStr = request.getParameter("vnp_Amount");
            String bankCode = request.getParameter("vnp_BankCode");
            String payDate = request.getParameter("vnp_PayDate");

            logger.info("VNPay return parameters:");
            logger.info("- Transaction ID: {}", transactionId);
            logger.info("- Response Code: {}", paymentStatus);
            logger.info("- Amount (xu): {}", amountStr);
            logger.info("- Bank Code: {}", bankCode);
            logger.info("- Pay Date: {}", payDate);

            // Convert amount string to proper number (VNPay returns amount in xu - smallest unit)
            Long amountInXu = null;
            Double amountInVND = null;
            if (amountStr != null && !amountStr.trim().isEmpty()) {
                try {
                    amountInXu = Long.parseLong(amountStr);
                    // Convert from xu to VND (divide by 100)
                    amountInVND = amountInXu / 100.0;
                    logger.info("Amount conversion: {} xu -> {} VND", amountInXu, amountInVND);
                } catch (NumberFormatException e) {
                    logger.error("Error parsing amount: {}", amountStr, e);
                }
            }

            // Get current receptionist user ID for ProcessedByUserID
            Integer currentReceptionistId = getCurrentReceptionistUserId();
            logger.info("Current receptionist processing payment in ErrorController: {}", currentReceptionistId);

            if (transactionId != null && !transactionId.trim().isEmpty()) {
                try {
                    Optional<Transaction> transactionOpt = transactionRepository.findById(Integer.valueOf(transactionId));
                    if (transactionOpt.isPresent()) {
                        Transaction transaction = transactionOpt.get();

                        String currentStatus = transaction.getStatus();
                        logger.info("Current transaction status: {}", currentStatus);

                        // Map VNPay response code to valid database Status values
                        if ("00".equals(paymentStatus)) {
                            // PAYMENT SUCCESSFUL - Update status to Paid and set ProcessedByUserID
                            transaction.setStatus("Paid");
                            transaction.setProcessedByUserId(currentReceptionistId); // Set who processed the payment
                            logger.info("✅ PAYMENT SUCCESSFUL - Updated transaction {} status from '{}' to 'Paid', ProcessedBy: {}",
                                       transactionId, currentStatus, currentReceptionistId);
                        } else {
                            // PAYMENT FAILED - Keep as Pending for retry
                            if (!"Paid".equals(currentStatus)) {
                                transaction.setStatus("Pending");
                                logger.warn("❌ PAYMENT FAILED - Transaction {} status: {} -> Pending (Response Code: {})",
                                           transactionId, currentStatus, paymentStatus);
                            } else {
                                logger.warn("⚠️ Payment failed but transaction {} is already Paid, not changing status", transactionId);
                            }
                        }

                        // Save transaction with updated status and ProcessedByUserID
                        Transaction savedTransaction = transactionRepository.save(transaction);
                        logger.info("💾 Transaction {} saved with status: {}, ProcessedBy: {}",
                                   transactionId, savedTransaction.getStatus(), savedTransaction.getProcessedByUserId());

                        // Add data to model for template
                        model.addAttribute("transaction", savedTransaction);
                        model.addAttribute("paymentStatus", paymentStatus);
                        model.addAttribute("amount", amountInVND);
                        model.addAttribute("amountInXu", amountInXu);

                        // Add success/failure message
                        if ("00".equals(paymentStatus)) {
                            model.addAttribute("successMessage", "Payment completed successfully!");
                        } else {
                            model.addAttribute("errorMessage", "Payment failed. Please try again or contact support.");
                        }

                    } else {
                        logger.error("❌ Transaction not found for ID: {}", transactionId);
                        model.addAttribute("errorMessage", "Transaction not found with ID: " + transactionId);
                    }
                } catch (NumberFormatException e) {
                    logger.error("❌ Invalid transaction ID format: {}", transactionId, e);
                    model.addAttribute("errorMessage", "Invalid transaction ID format");
                }
            } else {
                logger.error("❌ Transaction ID is missing from VNPay response");
                model.addAttribute("errorMessage", "Transaction ID is missing from payment response");
            }

            logger.info("=== PAYMENT RETURN PROCESSING COMPLETED ===");
            return "Receptionists/payment-return";

        } catch (Exception e) {
            logger.error("💥 ERROR handling VNPay payment return in error controller", e);
            model.addAttribute("errorMessage", "An error occurred while processing the payment return: " + e.getMessage());
            return "error";
        }
    }

    // Helper method to get current receptionist user ID
    private Integer getCurrentReceptionistUserId() {
        try {
            // For now, return a default receptionist user ID
            // In production, you should get this from the session or authentication context
            // Since this is error controller, we might not have full authentication context

            // You can implement logic to:
            // 1. Get from session if available
            // 2. Get the first available receptionist user ID with role = 3
            // 3. Return a default receptionist ID

            // For now, let's find a user with RECEPTIONIST role (role ID = 3)
            // This is a temporary solution - in practice you should track who initiated the payment
            return 2; // Assuming user ID 2 is a receptionist - you should adjust this
        } catch (Exception e) {
            logger.error("Error getting current receptionist user ID: {}", e.getMessage());
            return null;
        }
    }
}

