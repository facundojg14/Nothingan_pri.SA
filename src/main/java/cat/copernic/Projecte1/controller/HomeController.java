package cat.copernic.Projecte1.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controlador de la pàgina principal pública.
 *
 * @author Equip4
 */
@Controller
public class HomeController {

    // Muestra la página principal pública.
    @GetMapping("/")
    public String home() {
        return "index";
    }
}