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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
 * Controlador Spring MVC encarregat de la gestió de la interfaç web pels clientes.
 * Permet visualitzar el panel principal del client, consultar l'historial i els detalls
 * de les seves reserves, sol·licitar una nova reserva, anul·lar reserves existents i gestionar el seu perfil.
 * 
 * @author Carolina
 * @version 1.0
 */

@Controller
@RequestMapping("/web/usuaris/client")
public class ClientController {
    
    /** Servei amb la lògica de negoci relativa als usuaris. */
    @Autowired
    private UsuarisLogic usuarisLogic;
    
    /** Servei amb la lògica de negoci relativa a les reserves de vehicles. */
    @Autowired
    private ReservaLogic reservaLogic;
    
    /** Servei amb la lògica de negoci relativa a la gestió de vehicles. */
    @Autowired
    private VehicleLogic vehicleLogic;
    
       /**
        * Obtiene la entidad Usuaris del usuario autenticado actualmente en Spring Security.
        */
       private Usuaris getUsuariAutenticat() {
           Authentication auth = SecurityContextHolder.getContext().getAuthentication();
           if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
               return null;
           }
           String email = auth.getName(); // Spring Security guarda el username (tu email) en getName()
           return usuarisLogic.getUsuariByEmail(email); 
       }
    /**
     * Mostra la pàgina principal (home) del tauler privat del client.
     *
     * @param model Objecte {@link Model} per transmetre dades a la vista Thymeleaf.
     * @return Nom de la vista {@code "index-client"} si el client està autenticat,
     *         o redirecció a {@code "/web/usuaris/login"} en cas contrari.
     */
    @GetMapping("/home")
    public String mostrarHomeClient(Model model) {
        Usuaris usuariLoguejat = getUsuariAutenticat();

        if (usuariLoguejat == null) {
            // Si no está logueado, lo mandamos a identificarse
            return "redirect:/web/usuaris/login";
        }

        // 2. Pasamos el usuario a la plantilla para la cabecera dinámicia
        model.addAttribute("usuariLoguejat", usuariLoguejat);

        // 3. Devolvemos la plantilla que acabamos de crear (index-logged.html)
        return "index";
    }
    
    /**
     * Mostra el llistat de reserves del client autenticat amb suport per al filtratge
     * per codi, estat i data d'inici, així com l'obertura d'un modal de detalls.
     *
     * @param codi Filtre opcional pel codi identificador de la reserva.
     * @param estat Filtre opcional per l'estat de la reserva (ex. CONFIRMADA, LLIURADA).
     * @param data Filtre opcional per la data d'inici del lloguer (YYYY-MM-DD).
     * @param detall Identificador primari de la reserva per carregar els seus detalls complets al modal.
     * @param model Objecte {@link Model} per passar les dades a la plantilla.
     * @return Nom de la plantilla {@code "client-reserves"} o redirecció al login si no hi ha sessió.
     */
    @GetMapping("/reserves")
public String mostrarReservesClient(@RequestParam(value = "codi", required = false) String codi,
                                                     @RequestParam(value = "estat", required = false) String estat,
                                                     @RequestParam(value= "data", required = false) String data,
                                                     @RequestParam(value="detall", required = false) Integer detall,
                                                     @RequestParam(value="incidencies", required = false) Integer idReservaIncidencies,
                                                     Model model) {
    //Obtenir usuari autenticat desde la sessió http
    Usuaris usuariSessio = getUsuariAutenticat();
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
    
    if (detall != null) {
        Reserva seleccionada = reservaLogic.getReservaById(detall);
        if (seleccionada != null && seleccionada.getUsuari().getId().equals(usuariLoguejat.getId())) {
            model.addAttribute("reservaSeleccionada", seleccionada);
        }
    }
    
    // Modal d'incidències de la reserva (només si pertany a l'usuari i està Retornada)
    if (idReservaIncidencies != null) {
        Reserva reservaInc = reservaLogic.getReservaById(idReservaIncidencies);
        if (reservaInc != null && reservaInc.getUsuari().getId().equals(usuariLoguejat.getId())) {
            model.addAttribute("reservaIncidencies", reservaInc);
            model.addAttribute("llistaIncidencies", reservaLogic.getIncidenciesPerReserva(idReservaIncidencies));
        }
    }
    
    model.addAttribute("reserves", reserves); // enviat al html i renderitzat per th:each"r: ${reserves}
    model.addAttribute("codiFiltre", codi);
    model.addAttribute("estatFiltre", estat);
    model.addAttribute("dataFiltre", data);
    
    return "client-reserves";
}

/**
     * Prepara i mostra el formulari de confirmació per a una nova reserva de client.
     * Calcula els dies de lloguer i delega a la capa de lògica els càlculs financers (lloguer i fiança).
     *
     * @param id Identificador primari del vehicle seleccionat.
     * @param dataInici Data i hora d'inici de la reserva.
     * @param dataFi Data i hora de devolució del vehicle.
     * @param model Objecte {@link Model} per enviar la reserva preparada i el vehicle a la vista.
     * @return Nom de la vista {@code "nova-reserva-client"} o redirecció al login.
     */
    @GetMapping("/reserves/nova")
    public String mostrarNovaReservaClient(
            @RequestParam(value = "vehicleId", required = false) Long id,
            @RequestParam(value = "dataInici", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataInici,
            @RequestParam(value = "dataFi", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataFi,
            Model model) {
        
        // Validació sessió
        Usuaris usuariSessio = getUsuariAutenticat();
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
        reserva.setDataInici(dataInici);
        reserva.setDataFi(dataFi);
        reservaLogic.prepararReservaClient(reserva, vehicle, usuariLoguejat, diesLloguer);
        
        model.addAttribute("reserva", reserva);
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("diesLloguer", diesLloguer);
        model.addAttribute("usuariLoguejat", usuariLoguejat);
        
        return "nova-reserva-client";
     }
        
/**
     * Processa la sol·licitud de desat d'una nova reserva sol·licitada per un client.
     * Persisteix la reserva invocant {@link ReservaLogic#saveReserva(Reserva)} i transmet
     * l'identificador generat mitjançant flash attributes.
     *
     * @param reserva Objecte {@link Reserva} vinculat des del formulari HTML.
     * @param redirectAttributes Atributs de redirecció per enviar missatges d'èxit o error a la vista.
     * @return Redirecció a {@code "/web/usuaris/client/reserves"} si és correcte,
     *         o retorn al formulari {@code "/web/usuaris/client/reserves/nova"} si es produeix una excepció.
     */
@PostMapping("/reserves/save")
    public String guardarReservaClient(@ModelAttribute("reserva") Reserva reserva, RedirectAttributes redirectAttributes) {
        Usuaris usuariSessio = getUsuariAutenticat();
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
                    Integer idNova = reservaLogic.saveReserva(reserva);
                
                    redirectAttributes.addFlashAttribute("idNovaReserva", idNova);
                    redirectAttributes.addFlashAttribute("missatge", "Reserva creada amb èxit!");
                
                
                    return "redirect:/web/usuaris/client/reserves";
            } catch (IllegalArgumentException e) {
                redirectAttributes.addFlashAttribute("error", e.getMessage());
                return "redirect:/web/usuaris/client/reserves/nova";
            }
    }
    
    /**
     * Sol·licita l'anul·lació d'una reserva per part del client.
     * Comprova les polítiques d'antelació i retorna l'import corresponent notificant l'usuari.
     *
     * @param id Identificador únic de la reserva a anul·lar.
     * @param redirectAttributes Atributs de redirecció per mostrar el resultat del reemborsament.
     * @return Redirecció a la llista de reserves {@code "/web/usuaris/client/reserves"}.
     */
    @PostMapping("/reserves/anullar/{id}")
    public String anullarReservaClient(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        Usuaris usuariSessio = getUsuariAutenticat();
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
    
    /**
     * Mostra el perfil personal del client autenticat permetent consultar les seves dades.
     *
     * @param model Objecte {@link Model} per transmetre les dades del perfil.
     * @return Nom de la vista {@code "perfil"} o redirecció al login.
     */
    @GetMapping("/perfil")
    public String mostrarPerfilClient(Model model) {
        Usuaris usuariLoguejat = getUsuariAutenticat();
        if (usuariLoguejat == null) {
            return "redirect:/web/usuaris/login";
        }

        // Recuperem les dades actualitzades de la base de dades
        Usuaris usuariBD = usuarisLogic.getUsuariById(usuariLoguejat.getId());
        model.addAttribute("usuariLoguejat", usuariBD);
        model.addAttribute("usuari", usuariBD); // Necessari per a th:object="${usuari}" a perfil.html
        return "perfil";
    }

    /**
     * Processa l'actualització de les dades editables del perfil del client (telèfon).
     * Manté intactes la resta d'atributs sensibles de l'usuari (rol, contrasenya, estat, etc.).
     *
     * @param usuariForm Objecte {@link Usuaris} amb les dades editades enviades des del formulari.
     * @param redirectAttributes Atributs per retornar el missatge d'èxit o error.
     * @return Redirecció a la pàgina de perfil {@code "/web/usuaris/client/perfil"}.
     */
    @PostMapping("/perfil/save") //ModelAttribute vincula camps enviats per formulari a un objecte
    public String guardarPerfilClient(@ModelAttribute("usuari") Usuaris usuariForm, HttpSession session, RedirectAttributes redirectAttributes) {
        Usuaris usuariLoguejat = getUsuariAutenticat();
        if (usuariLoguejat == null) {
            return "redirect:/web/usuaris/login";
        }

        // 1. Cargar el usuario real desde la base de datos
        Usuaris usuariBD = usuarisLogic.getUsuariById(usuariLoguejat.getId());

        if (usuariBD != null) {
            // 2. Modificar ÚNICAMENTE el teléfono
            usuariBD.setTelefon(usuariForm.getTelefon());

            // 3. Guardar en la base de datos preservando contraseña, estado, rol, premium, etc.
            usuarisLogic.saveUsuari(usuariBD);

            redirectAttributes.addFlashAttribute("missatge", "Perfil actualitzat amb èxit!");
        } else {
            redirectAttributes.addFlashAttribute("error", "No s'ha pogut trobar el perfil per actualitzar.");
        }
        return "redirect:/web/usuaris/client/perfil";
    }
} 
