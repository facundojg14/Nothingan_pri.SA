package cat.copernic.Projecte1.controller;

import cat.copernic.Projecte1.entity.Enum.rol;
import cat.copernic.Projecte1.entity.Usuaris;
import cat.copernic.Projecte1.entity.Vehicle;
import cat.copernic.Projecte1.logic.VehicleLogic;
import jakarta.servlet.http.HttpSession;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controlador de la part pública de la web: la pàgina principal i la cerca de
 * vehicles disponibles per dates.
 *
 * La cerca és pública (no cal haver iniciat sessió) i per això no té el
 * prefix /web/admin ni /web/agent.
 *
 * @author Equip4
 */
@Controller
public class HomeController {

    @Autowired
    private VehicleLogic vehicleLogic;

    /**
     * Mostra la pàgina principal pública.
     *
     * @return plantilla "index"
     */
    @GetMapping("/")
    public String home() {
        return "index";
    }

    /**
     * Busca els vehicles disponibles entre dues dates i calcula els dies de
     * lloguer. És el cercador de la pàgina principal.
     *
     * @param dataInici data i hora d'inici del lloguer
     * @param dataFi data i hora de finalització del lloguer
     * @param model model per passar les dades a la vista
     * @param session sessió HTTP (per saber si l'usuari ha iniciat sessió)
     * @return "index-client" si és un client, "index" en cas contrari
     */
    @GetMapping("/web/vehicles/cercar")
    public String cercarVehicles(
            @RequestParam("dataInici")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataInici,
            @RequestParam("dataFi")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataFi,
            Model model, HttpSession session) {

        Usuaris usuariLoguejat = (Usuaris) session.getAttribute("usuariLoguejat");

        model.addAttribute("dataInici", dataInici);
        model.addAttribute("dataFi", dataFi);

        if (dataInici.isBefore(LocalDateTime.now().withSecond(0).withNano(0))) {
            model.addAttribute("error", "No es pot fer una reserva amb una data d'inici passada.");
            return determinarVistaSegonsRol(usuariLoguejat, model);
        }

        if (!dataFi.isAfter(dataInici)) {
            model.addAttribute("error", "La data final ha de ser posterior a la inicial.");
            return determinarVistaSegonsRol(usuariLoguejat, model);
        }

        // Vehicles lliures en aquest interval
        List<Vehicle> vehicles = vehicleLogic.findVehiclesDisponibles(dataInici, dataFi);

        model.addAttribute("vehicles", vehicles);
        model.addAttribute("dataInici", dataInici);
        model.addAttribute("dataFi", dataFi);

        // Dies de lloguer (cada fracció de dia compta com un dia sencer)
        long diesLloguer = (long) Math.ceil(
                Duration.between(dataInici, dataFi).toMinutes() / 1440.0
        );
        model.addAttribute("diesLloguer", diesLloguer);

        return determinarVistaSegonsRol(usuariLoguejat, model);
    }

    // Mètode auxiliar per determinar quina plantilla carregar segons el rol de la sessió
    private String determinarVistaSegonsRol(Usuaris usuariLoguejat, Model model) {
        if (usuariLoguejat != null) {
            model.addAttribute("usuariLoguejat", usuariLoguejat);

            if (usuariLoguejat.getRol() == null || usuariLoguejat.getRol() == rol.CLIENT) {
                return "index-client";
            }
        }

        // Si no hi ha sessió (o és admin/agent), es mostra la pàgina pública general
        return "index";
    }
}