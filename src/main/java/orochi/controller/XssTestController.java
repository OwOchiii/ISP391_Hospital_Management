package orochi.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.ui.Model;

@Controller
@RequestMapping("/test")
public class XssTestController {

    @GetMapping("/xss")
    public String testXss(@RequestParam(required = false) String input, Model model) {
        // Intentionally adding unescaped input to the model
        model.addAttribute("userInput", input);
        model.addAttribute("title", "XSS Test Page");
        return "test/xss-test";
    }
}
