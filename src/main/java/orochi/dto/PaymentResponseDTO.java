package orochi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponseDTO {
    private String transactionId;
    private String status;
    private String message;
    private BigDecimal amount;
    private String paymentUrl;
    private String qrCodeUrl;
    private String receiptNumber;
}
