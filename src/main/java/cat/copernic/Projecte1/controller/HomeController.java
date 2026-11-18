package cat.copernic.Projecte1.controller;

import cat.copernic.Projecte1.entity.Enum.rol;
import cat.copernic.Projecte1.entity.Usuaris;
import cat.copernic.Projecte1.entity.Vehicle;
import cat.copernic.Projecte1.logic.UsuarisLogic;
import cat.copernic.Projecte1.logic.VehicleLogic;
import jakarta.servlet.http.HttpSession;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controlador de la página principal y de la búsqueda pública
 * de vehículos disponibles según las fechas seleccionadas.
 *
 * @author Equip4
 */
@Controller
public class HomeController {

    // ==================== ATRIBUTOS ====================

    // Servicio para consultar los vehículos disponibles.
    @Autowired
    private VehicleLogic vehicleLogic;

    // Servicio para consultar los datos de los usuarios.
    @Autowired
    private UsuarisLogic usuarisLogic;

    // ==================== PÁGINA PRINCIPAL ====================

    /**
     * Muestra la página principal y comprueba si hay un usuario conectado.
     */
    @GetMapping("/")
    public String home(Model model) {
        // Comprueba si existe un usuario autenticado.
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            // Añade los datos del usuario conectado a la vista.
            model.addAttribute("usuariLoguejat", usuarisLogic.getUsuariByEmail(auth.getName()));
        }

        return "index";
    }

    // ==================== BÚSQUEDA DE VEHÍCULOS ====================

    /**
     * Busca vehículos disponibles entre las fechas seleccionadas
     * y calcula la duración del alquiler.
     */
    @GetMapping("/web/vehicles/cercar")
    public String cercarVehicles(
            @RequestParam("dataInici")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataInici,
            @RequestParam("dataFi")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataFi,
            Model model) {

        // Obtiene los datos del usuario conectado, si existe.
        Usuaris usuariLoguejat = getUsuariAutenticat();

        // Comprueba que la fecha de inicio no sea anterior al día actual.
        if (dataInici.toLocalDate().isBefore(java.time.LocalDate.now())) {
            model.addAttribute("error", "La data d'inici no pot ser anterior a avui.");
            return determinarVistaSegonsRol(usuariLoguejat, model);
        }

        // Añade las fechas seleccionadas a la vista.
        model.addAttribute("dataInici", dataInici);
        model.addAttribute("dataFi", dataFi);

        // Comprueba que la hora de inicio no haya pasado.
        if (dataInici.isBefore(LocalDateTime.now().withSecond(0).withNano(0))) {
            model.addAttribute("error", "No es pot fer una reserva amb una data d'inici passada.");
            return determinarVistaSegonsRol(usuariLoguejat, model);
        }

        // Comprueba que la fecha final sea posterior a la inicial.
        if (!dataFi.isAfter(dataInici)) {
            model.addAttribute("error", "La data final ha de ser posterior a la inicial.");
            return determinarVistaSegonsRol(usuariLoguejat, model);
        }

        // Busca los vehículos disponibles durante el intervalo seleccionado.
        List<Vehicle> vehicles = vehicleLogic.findVehiclesDisponibles(dataInici, dataFi);

        model.addAttribute("vehicles", vehicles);
        model.addAttribute("dataInici", dataInici);
        model.addAttribute("dataFi", dataFi);

        // Calcula los días de alquiler redondeando hacia arriba las fracciones de día.
        long diesLloguer = (long) Math.ceil(
                Duration.between(dataInici, dataFi).toMinutes() / 1440.0
        );
        model.addAttribute("diesLloguer", diesLloguer);

        return determinarVistaSegonsRol(usuariLoguejat, model);
    }

    // ==================== MÉTODOS AUXILIARES ====================

    /**
     * Obtiene los datos del usuario autenticado.
     * Devuelve null si no hay ninguna sesión autenticada.
     */
    private Usuaris getUsuariAutenticat() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }

        return usuarisLogic.getUsuariByEmail(auth.getName());
    }

    /**
     * Prepara los datos del usuario y determina la vista que se debe mostrar.
     */
    private String determinarVistaSegonsRol(Usuaris usuariLoguejat, Model model) {
        // Añade los datos del usuario a la vista si está conectado.
        if (usuariLoguejat != null) {
            model.addAttribute("usuariLoguejat", usuariLoguejat);
        }

        return "index";
    }
}