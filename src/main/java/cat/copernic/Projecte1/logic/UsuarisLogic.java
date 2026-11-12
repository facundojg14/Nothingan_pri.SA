package cat.copernic.Projecte1.logic;

import cat.copernic.Projecte1.entity.Enum.rol;
import cat.copernic.Projecte1.entity.Usuaris;
import cat.copernic.Projecte1.repository.ReservaRepository;
import cat.copernic.Projecte1.repository.UsuarisRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Lògica de negoci dels usuaris (clients, agents i administradors).
 *
 * @author Equip4
 */
@Service
public class UsuarisLogic {

    @Autowired
    private UsuarisRepository usuariRepo;

    // Per comprovar si un usuari té reserves abans d'esborrar-lo
    @Autowired
    private ReservaRepository reservaRepo;

    // Logger per registrar registres i validacions al fitxer de logs (RN04)
    private final Logger logger = LoggerFactory.getLogger(UsuarisLogic.class);

    // Encriptador de contrasenyes (definit a PasswordEncoderConfig)
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    // --- OPERACIONES BÁSICAS CRUD ---
    public List<Usuaris> findAllUsuaris() {
        return usuariRepo.findAll();
    }

    /**
     * Crea o actualitza un usuari. Si la contrasenya encara no està
     * encriptada, l'encripta amb BCrypt abans de guardar-la.
     *
     * @param usuari usuari a guardar
     * @return id de l'usuari guardat
     */
    public Long saveUsuari(Usuaris usuari) {
        validarCamposUnicos(usuari);
        xifrarContrasenya(usuari);
        if (usuari.getEstatRegistre() == null || usuari.getEstatRegistre().isBlank()) {
            usuari.setEstatRegistre("PENDENT");
        }
        if (usuari.getRol() == null) {
            usuari.setRol(rol.CLIENT); // Asignas el valor del Enum
        }
        if (usuari.getPremium() == null) {
            usuari.setPremium(false);
        }
        Usuaris ret = usuariRepo.save(usuari);
        return ret.getId();
    }

    /**
     * Esborra un usuari (client o agent).
     *
     * No es pot esborrar si té reserves associades, perquè la base de dades
     * no ho permet (clau forana) i es perdrien les dades de les reserves.
     *
     * @param id id de l'usuari a esborrar
     * @throws IllegalArgumentException si l'usuari no existeix o té reserves
     */
    public void deleteUsuariById(Long id) {
        if (!usuariRepo.existsById(id)) {
            throw new IllegalArgumentException("No s'ha trobat l'usuari.");
        }

        if (!reservaRepo.findByUsuariId(id).isEmpty()) {
            throw new IllegalArgumentException("No es pot esborrar l'usuari perquè té reserves associades.");
        }

        usuariRepo.deleteById(id);
        logger.info("Usuari {} esborrat", id);
    }

    /**
     * Encripta la contrasenya de l'usuari amb BCrypt si encara no ho està.
     * Les contrasenyes de BCrypt sempre comencen per "$2a$", "$2b$" o "$2y$".
     *
     * @param usuari usuari amb la contrasenya a encriptar
     */
    public void xifrarContrasenya(Usuaris usuari) {
        String password = usuari.getPassword();
        if (password != null && !password.isBlank() && !password.startsWith("$2")) {
            usuari.setPassword(passwordEncoder.encode(password));
        }
    }

    public Usuaris getUsuariById(Long id) {
        return usuariRepo.findById(id).orElse(null);
    }

    public boolean existsById(Long id) {
        return usuariRepo.existsById(id);
    }

    // --- LÓGICA DE NEGOCIO (REQUISITOS FUNCIONALES) ---
    /**
     * RF81: Registrar client des de la web pública amb validacions.
     */
    public Long registrarClientWeb(Usuaris client) throws IllegalArgumentException {

        validarCamposUnicos(client);

        // 1. Validar camps obligatoris
        if (client.getEmail() == null || client.getEmail().isBlank()
                || client.getNom() == null || client.getNom().isBlank()
                || client.getDni() == null || client.getDni().isBlank()) {
            throw new IllegalArgumentException("Tots els camps obligatoris s'han d'omplir.");
        }

        // 3. Validar data de caducitat del DNI
        if (client.getDataCaducitatDni() != null && client.getDataCaducitatDni().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("El document d'identificació (DNI/NIE) està caducat.");
        }

        // 4. Validar data de caducitat de la llicència
        if (client.getDataCaducitatLlicencia() != null && client.getDataCaducitatLlicencia().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("La llicència de conduir està caducada.");
        }

        // 5. La contrasenya és obligatòria i es guarda encriptada (RN52)
        if (client.getPassword() == null || client.getPassword().isBlank()) {
            throw new IllegalArgumentException("La contrasenya és obligatòria.");
        }
        xifrarContrasenya(client);

        // Estat inicial i rol per defecte per al registre web (RF81)
        client.setEstatRegistre("PENDENT");
        client.setRol(rol.CLIENT);
        client.setPremium(false);

        Usuaris nouClient = usuariRepo.save(client);
        logger.info("Nou registre pendent de validar: {}", nouClient.getEmail());
        return nouClient.getId();
    }

    /**
     * Autenticació d'usuaris controlant els estats PENDENT i REBUTJAT.
     *
     * Nota: el login real el fa Spring Security (CustomUserDetailsService +
     * SecurityConfig). Aquest mètode es manté perquè el fa servir el test
     * UsuarisLogicTest.
     */
    public Usuaris autenticarIValidarEstat(String email, String password) {
        Optional<Usuaris> usuariOpt = usuariRepo.findByEmail(email);

        if (usuariOpt.isEmpty() || !usuariOpt.get().getPassword().equals(password)) {
            throw new IllegalArgumentException("El correu o la contrasenya són incorrectes.");
        }

        Usuaris usuari = usuariOpt.get();

        if ("PENDENT".equalsIgnoreCase(usuari.getEstatRegistre())) {
            throw new IllegalStateException("El teu registre encara està pendent de revisió per un administrador.");
        }

        if ("REBUTJAT".equalsIgnoreCase(usuari.getEstatRegistre())) {
            throw new IllegalStateException("La teva sol·licitud de registre ha estat rebutjada.");
        }

        return usuari;
    }

    /**
     * RF82: Llistar registres pendents de validació.
     */
    public List<Usuaris> obtenirRegistresPendents() {
        return usuariRepo.findByEstatRegistre("PENDENT");
    }

    /**
     * Valida o rebutja la sol·licitud de registre d'un client (RF82).
     *
     * @param idUsuari identificador de l'usuari pendent
     * @param nouEstat nou estat: "VALIDAT" o "REBUTJAT"
     * @throws IllegalArgumentException si l'usuari no existeix
     */
    public void canviarEstatRegistre(Long idUsuari, String nouEstat) {
        Usuaris usuari = usuariRepo.findById(idUsuari)
                .orElseThrow(() -> new IllegalArgumentException("Usuari no trobat"));

        usuari.setEstatRegistre(nouEstat); // "VALIDAT" o "REBUTJAT"
        usuariRepo.save(usuari);
        logger.info("Registre de {} canviat a {}", usuari.getEmail(), nouEstat);
    }

    /**
     * Actualitzar dades del client per l'agent/admin des del formulari
     * d'edició.
     */
    public void actualitzarClientPerAgent(Usuaris usuariEditat) {
        validarCamposUnicos(usuariEditat);
        Usuaris usuariExistent = usuariRepo.findById(usuariEditat.getId())
                .orElseThrow(() -> new IllegalArgumentException("Usuari no trobat amb ID: " + usuariEditat.getId()));

        // Mantener la contraseña existente si no se proporciona una nueva
        if (usuariEditat.getPassword() != null && !usuariEditat.getPassword().isBlank()) {
            usuariExistent.setPassword(usuariEditat.getPassword());
            xifrarContrasenya(usuariExistent);
        }

        // Actualització de dades personals
        usuariExistent.setNom(usuariEditat.getNom());
        usuariExistent.setEmail(usuariEditat.getEmail());
        usuariExistent.setTelefon(usuariEditat.getTelefon());
        usuariExistent.setAdreca(usuariEditat.getAdreca());
        usuariExistent.setPoblacio(usuariEditat.getPoblacio());
        usuariExistent.setNacionalitat(usuariEditat.getNacionalitat());
        usuariExistent.setDni(usuariEditat.getDni());
        usuariExistent.setDataCaducitatDni(usuariEditat.getDataCaducitatDni());
        usuariExistent.setTipusLlicencia(usuariEditat.getTipusLlicencia());
        usuariExistent.setDataCaducitatLlicencia(usuariEditat.getDataCaducitatLlicencia());
        usuariExistent.setTargetaCredit(usuariEditat.getTargetaCredit());

        // Imàtges
        if (usuariEditat.getImatgedni() != null) {
            usuariExistent.setImatgedni(usuariEditat.getImatgedni());
        }
        if (usuariEditat.getImatgeLlicencia() != null) {
            usuariExistent.setImatgeLlicencia(usuariEditat.getImatgeLlicencia());
        }

        // Estat, Rol i Premium
        if (usuariEditat.getRol() != null) {
            usuariExistent.setRol(usuariEditat.getRol());
        }
        usuariExistent.setEstatRegistre(usuariEditat.getEstatRegistre());
        usuariExistent.setPremium(usuariEditat.getPremium() != null ? usuariEditat.getPremium() : false);

        usuariRepo.save(usuariExistent);
    }

    // Mètode per filtrar usuaris per DNI de forma Parcial
    public List<Usuaris> findByEmail(String email) {
        return usuariRepo.findByEmailContainingIgnoreCase(email);
    }

    public List<Usuaris> findByDni(String dni) {
        return usuariRepo.findByDniContainingIgnoreCase(dni);
    }

    /**
     * Filtra els clients per diversos criteris alhora (RF26).
     * Els criteris buits no es tenen en compte.
     *
     * @param email part de l'email
     * @param nacionalitat part de la nacionalitat
     * @param poblacio part de la població
     * @param dni part del DNI
     * @param nom part del nom
     * @param telefon part del telèfon
     * @return usuaris que compleixen tots els criteris indicats
     */
    public List<Usuaris> filtrarClients(String email, String nacionalitat, String poblacio, String dni, String nom, String telefon) {
        return usuariRepo.findClientByFiltreMultiple(
                cleanParam(email),
                cleanParam(nacionalitat),
                cleanParam(poblacio),
                cleanParam(dni),
                cleanParam(nom),
                cleanParam(telefon)
        );
    }
// Método auxiliar para convertir cadenas vacías con espacios ("   ") a null

    private String cleanParam(String param) {
        return (param != null && !param.trim().isEmpty()) ? param.trim() : null;
    }

    /**
     * Valida les dades d'un usuari abans de guardar-lo (RN20).
     *
     * Comprova que el nom no estigui buit, que el DNI tingui 8 números i una
     * lletra, que el telèfon tingui 9 dígits i que l'email, el DNI i el
     * telèfon no els tingui ja un altre usuari.
     *
     * @param usuari usuari a validar (si té id, és una modificació i no es
     * compara amb ell mateix)
     * @throws IllegalArgumentException amb el missatge de l'error si alguna
     * dada no és vàlida o ja existeix
     */
    public void validarCamposUnicos(Usuaris usuari) throws IllegalArgumentException {

        // 1. Validar si l'Email ja existeix
        //primero comprueba que lo que mete el usuario en email no este vacio ni en blanco, después hace la misma comprobación en la bbdd,
        if (usuari.getEmail() != null && !usuari.getEmail().isBlank()) {
            //dentro del if envia una petición a la bbdd comprobanco si el email ya existe
            Optional<Usuaris> existent = usuariRepo.findByEmail(usuari.getEmail());
            // si encuentra un usuario con ese mimso mail devuelve solo ese usuario

            //si el mail ya existe verifica por la id para saber si es de otro usuario y salta el error dato ya existe 
            //si dice que nadie esta con ese email ya esta todo verificado y permite guardar 
            if (existent.isPresent() && (usuari.getId() == null || !existent.get().getId().equals(usuari.getId()))) {
                //si el email ya existe salta el siguiente error
                throw new IllegalArgumentException("El correu electrònic '" + usuari.getEmail() + "' ja està registrat.");
            }
            // si todas las comprobaciones estan guays actualiza o crea un usuario 
        }

        // 2. Validar si el DNI ja existeix
        if (usuari.getDni() != null && !usuari.getDni().isBlank()) {
            if (usuari.getDni().length() != 9) {
                throw new IllegalArgumentException("El dni '" + usuari.getDni() + "' ha de tenir 9 digits.");
            }

            for (int i = 0; i < 8; i++) {
                if (!Character.isDigit(usuari.getDni().charAt(i))) {
                    throw new IllegalArgumentException("Els primers 8 caràcters del DNI han de ser números.");
                }
            }

            if (Character.isDigit(usuari.getDni().charAt(8))) {
                throw new IllegalArgumentException("L'últim caràcter del DNI ha de ser una lletra, no un número.");
            }

            Optional<Usuaris> existent = usuariRepo.findByDni(usuari.getDni());
            if (existent.isPresent() && (usuari.getId() == null || !existent.get().getId().equals(usuari.getId()))) {
                throw new IllegalArgumentException("El DNI '" + usuari.getDni() + "' ja està registrat.");
            }
        }

        if (usuari == null || usuari.getNom() == null || usuari.getNom().isBlank()) {
            throw new IllegalArgumentException("El nom es obligatori");
        }

        // ja con la mida comprueba si es de 9 digitos osea logica != 9 (si no es de 9 digitos salta error)
        if (usuari.getTelefon().length() != 9) {
            throw new IllegalArgumentException("El número de telèfon ha de tenir exactament 9 dígits.");
        }

        for (int i = 0; i < usuari.getTelefon().length(); i++) {
            if (!Character.isDigit(usuari.getTelefon().charAt(i))) {
                throw new IllegalArgumentException("El telèfon només pot contenir números.");
            }
        }

        //dentro del if envia una petición a la bbdd comprobanco si el numero ya existe
        Optional<Usuaris> existente = usuariRepo.findByTelefon(usuari.getTelefon());
        // si encuentra un usuario con ese mismo telefono devuelve solo ese usuario

        //si el telefono ya existe verifica por la id para saber si es de otro usuario y salta el error dato ya existe 
        //si dice que nadie esta con ese telefono ya esta todo verificado y permite guardar 
        if (existente.isPresent() && (usuari.getId() == null || !existente.get().getId().equals(usuari.getId()))) {
            //si el telefono ya existe salta el siguiente error
            throw new IllegalArgumentException("El número de telèfon '" + usuari.getTelefon() + "' ja està registrat.");
        }
        // si todas las comprobaciones estan guays actualiza o crea un usuario 
    }
}
