package cat.copernic.Projecte1.controller;

import cat.copernic.Projecte1.entity.Reserva;
import cat.copernic.Projecte1.entity.Usuaris;
import cat.copernic.Projecte1.entity.Vehicle;
import cat.copernic.Projecte1.logic.ReservaLogic;
import cat.copernic.Projecte1.logic.UsuarisLogic;
import cat.copernic.Projecte1.logic.VehicleLogic;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
/**
 *
 * @author Carolina
 */

@Controller
@RequestMapping("/web/usuaris/client")
public class ClientController {
    
    @Autowired
    private UsuarisLogic usuarisLogic;
    
    @Autowired
    private ReservaLogic reservaLogic;
    
    @Autowired
    private VehicleLogic vehicleLogic;
    
    
    // Pàgina principal loguejat com a client
    @GetMapping("/home")
    public String mostrarHomeClient(HttpSession session, Model model) {
        Usuaris usuariLoguejat = (Usuaris) session.getAttribute("usuariLoguejat");

        if (usuariLoguejat == null) {
            // Si no está logueado, lo mandamos a identificarse
            return "redirect:/web/usuaris/login";
        }

        // 2. Pasamos el usuario a la plantilla para la cabecera dinámicia
        model.addAttribute("usuariLoguejat", usuariLoguejat);

        // 3. Devolvemos la plantilla que acabamos de crear (index-logged.html)
        return "index-client";
    }
    
    
    @GetMapping("/reserves")
public String mostrarReservesClient(@RequestParam(value = "codi", required = false) String codi,
                                                     @RequestParam(value = "estat", required = false) String estat,
                                                     @RequestParam(value= "data", required = false) String data,
                                                      HttpSession session, Model model) {
    //Obtenir usuari autenticat desde la sessió http
    Usuaris usuariSessio = (Usuaris) session.getAttribute("usuariLoguejat");
    // si no hi ha sessió, redirigir al login.
    if (usuariSessio == null) {
        return "redirect:/web/usuaris/login";
    }
    
    Usuaris usuariLoguejat = usuarisLogic.getUsuariById(usuariSessio.getId());
    if (usuariLoguejat == null) {           // Si no retorna cap valor o falla i retorna null, el if
        usuariLoguejat = usuariSessio; // evita un NullPointerException. En canvi assigna
    }                                                // els valor ja guardats en la sessió HTTP.
    
    
    model.addAttribute("usuariLoguejat", usuariLoguejat); //passar l'usuari al model per mostrar el nom en el header
    
    //List<Reserva> reserves = reservaLogic.getReservesPerUsuari(usuariLoguejat.getId());
    List<Reserva> reserves = reservaLogic.filtrarReservesClient(usuariLoguejat.getId(), codi, estat, data);
    if (reserves == null) {    // Si no en té reserves asigna una llista buida per
        reserves = List.of(); // evitar errors de renderitzat i càrrega.
    }
    model.addAttribute("reserves", reserves); // enviat al html i renderitzat per th:each"r: ${reserves}
    
    model.addAttribute("codiFiltre", codi);
    model.addAttribute("estatFiltre", estat);
    model.addAttribute("dataFiltre", data);
    
    return "client-reserves";
}


// FORMULARIO CREACIÓN RESERVA CLIENTE
    @GetMapping("/reserves/nova")
    public String mostrarNovaReservaClient(
            @RequestParam(value = "vehicleId", required = false) Long id,
            @RequestParam(value = "dataInici", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataInici,
            @RequestParam(value = "dataFi", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataFi,
            HttpSession session, Model model) {
        
        // Validació sessió
        Usuaris usuariSessio = (Usuaris) session.getAttribute("usuariLoguejat");
        if (usuariSessio == null) {
            return "redirect:/web/usuaris/login";
        }
        // Carregar usuari complet de la BD
            Usuaris usuariLoguejat = usuarisLogic.getUsuariById(usuariSessio.getId());
                if (usuariLoguejat == null) {
                    usuariLoguejat = usuariSessio;
          }
        Vehicle vehicle = null;
        if (id != null && id > 0) {
            vehicle = vehicleLogic.getVehicleById(id);
        }
        
        long diesLloguer = 1;
        if (dataInici != null && dataFi != null && dataFi.isAfter(dataInici)) {
            diesLloguer = (long) Math.ceil(Duration.between(dataInici, dataFi).toMinutes() / 1440.0);
        }
        Reserva reserva = new Reserva();
        reserva.setVehicle(vehicle != null ? vehicle : new Vehicle());
        reserva.setUsuari(usuariLoguejat);
        reserva.setDataInici(dataInici);
        reserva.setDataFi(dataFi);
        
        model.addAttribute("reserva", reserva);
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("diesLloguer", diesLloguer);
        model.addAttribute("usuariLoguejat", usuariLoguejat);
        
        return "nova-reserva-client";
     }
        

@PostMapping("/reserves/save")
    public String guardarReservaClient(@ModelAttribute("reserva") Reserva reserva, HttpSession session, RedirectAttributes redirectAttributes) {
        Usuaris usuariSessio = (Usuaris) session.getAttribute("usuariLoguejat");
            if (usuariSessio == null) {
                return "redirect:/web/usuaris/login";
            }

            // Assignem l'usuari real de la BD a la reserva abans de guardar
            Usuaris usuariBD = usuarisLogic.getUsuariById(usuariSessio.getId());
            reserva.setUsuari(usuariBD);

            if (reserva.getImportFianca() == null) { // valor predeterminat per evitar excepcions
                reserva.setImportFianca(java.math.BigDecimal.ZERO);
            }

            try {
                reservaLogic.saveReserva(reserva);
                
                List<Reserva> mevesReserves = reservaLogic.getReservesPerUsuari(usuariBD.getId());
                if (!mevesReserves.isEmpty()) {
                    // Si la llista ve ordenada per ID descendent, la darrera és la primera (get(0))
                    // Si no, agafem la darrera de la llista (mevesReserves.get(mevesReserves.size() - 1))
                    Reserva novaReserva = mevesReserves.get(0); 
                    redirectAttributes.addFlashAttribute("idNovaReserva", novaReserva.getId());
                }
                redirectAttributes.addFlashAttribute("missatge", "Reserva creada amb èxit!");
                return "redirect:/web/usuaris/client/reserves";
            } catch (IllegalArgumentException e) {
                redirectAttributes.addFlashAttribute("error", e.getMessage());
                return "redirect:/web/usuaris/client/reserves/nova";
            }
    }
    
    @PostMapping("/reserves/anullar/{id}")
    public String anullarReservaClient(@PathVariable("id") Integer id, HttpSession session, RedirectAttributes redirectAttributes) {
        Usuaris usuariSessio = (Usuaris) session.getAttribute("usuariLoguejat");
        if (usuariSessio == null) {
            return "redirect:/web/usuaris/login";
        }

        try {
            BigDecimal importRetornat = reservaLogic.anularReservaPerClient(id, usuariSessio.getId());

            if (importRetornat.compareTo(BigDecimal.ZERO) > 0) {
                redirectAttributes.addFlashAttribute("missatge", 
                    "Reserva anul·lada amb èxit. Se't retornarà un total de " + importRetornat + " € (100% de l'import). S'ha enviat un correu de confirmació.");
            } else {
                redirectAttributes.addFlashAttribute("missatge", 
                    "Reserva anul·lada amb èxit. Atenció: No se't retornarà cap import per no haver realitzat l'anul·lació amb l'antelació suficient. S'ha enviat un correu de confirmació.");
            }
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/web/usuaris/client/reserves";
    }
    
    
    @GetMapping("/perfil")
    public String mostrarPerfilClient(HttpSession session, Model model) {
        Usuaris usuariLoguejat = (Usuaris) session.getAttribute("usuariLoguejat");
        if (usuariLoguejat == null) {
            return "redirect:/web/usuaris/login";
        }

        // Recuperem les dades actualitzades de la base de dades
        Usuaris usuariBD = usuarisLogic.getUsuariById(usuariLoguejat.getId());
        model.addAttribute("usuariLoguejat", usuariBD);
        model.addAttribute("usuari", usuariBD); // Necessari per a th:object="${usuari}" a perfil.html
        return "perfil";
    }

    // GUARDAR CAMBIOS DEL PERFIL
    @PostMapping("/perfil/save") //ModelAttribute vincula camps enviats per formulari a un objecte
    public String guardarPerfilClient(@ModelAttribute("usuari") Usuaris usuariForm, HttpSession session, RedirectAttributes redirectAttributes) {
        Usuaris usuariLoguejat = (Usuaris) session.getAttribute("usuariLoguejat");
        if (usuariLoguejat == null) {
            return "redirect:/web/usuaris/login";
        }

        // 1. Cargar el usuario real desde la base de datos
        Usuaris usuariBD = usuarisLogic.getUsuariById(usuariLoguejat.getId());

        if (usuariBD != null) {
            // 2. Modificar ÚNICAMENTE el email y el teléfono
            usuariBD.setTelefon(usuariForm.getTelefon());

            // 3. Guardar en la base de datos preservando contraseña, estado, rol, premium, etc.
            usuarisLogic.saveUsuari(usuariBD);

            // 4. Actualizar el usuario en la sesión para mantener la coherencia
            session.setAttribute("usuariLoguejat", usuariBD);

            redirectAttributes.addFlashAttribute("missatge", "Perfil actualitzat amb èxit!");
        } else {
            redirectAttributes.addFlashAttribute("error", "No s'ha pogut trobar el perfil per actualitzar.");
        }
        return "redirect:/web/usuaris/client/perfil";
    }
} 
