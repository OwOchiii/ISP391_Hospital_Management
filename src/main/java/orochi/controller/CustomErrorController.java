package orochi.controller;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;

@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
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
            model.addAttribute("exceptionMessage", ((Exception) exception).getMessage());
        }

        Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        if (message != null && !message.toString().isEmpty()){
            errorMessage = message.toString();
        }

        model.addAttribute("errorMessage", errorMessage);
        return "error"; // Name of the error HTML file (e.g., error.html)
    }
}