package kr.noco.qticket.ui.home;

import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final MessageSource messageSource;

    public HomeController(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @GetMapping("/")
    public String index(Model model, Locale locale) {
        model.addAttribute("pageTitle", messageSource.getMessage("home.title", null, locale));
        return "index";
    }
}
