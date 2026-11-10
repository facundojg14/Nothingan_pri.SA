package cat.copernic.Projecte1.logic;

import cat.copernic.Projecte1.entity.Enum.EstatReserva;
import cat.copernic.Projecte1.entity.Enum.EstatVehicle;
import cat.copernic.Projecte1.entity.Reserva;
import cat.copernic.Projecte1.entity.Usuaris;
import cat.copernic.Projecte1.entity.Vehicle;
import cat.copernic.Projecte1.repository.ReservaRepository;
import cat.copernic.Projecte1.repository.UsuarisRepository;
import cat.copernic.Projecte1.repository.VehicleRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*
 * Lógica para gestionar las reservas.
 * Comprueba las condiciones, calcula los precios y realiza las operaciones.
 */
@Service
public class ReservaLogic {

    private final ReservaRepository reservaRepo;
    private final VehicleRepository vehicleRepo;
    private final UsuarisRepository usuarisRepo;
    
    @Value("${app.reserves.dies-antelacio-devolucio:2}")
    private int diesAntelacioDevolucio;
    
    @Autowired
    private EmailService emailService;

    public ReservaLogic(
            ReservaRepository reservaRepo,
            VehicleRepository vehicleRepo,
            UsuarisRepository usuarisRepo) {
        this.reservaRepo = reservaRepo;
        this.vehicleRepo = vehicleRepo;
        this.usuarisRepo = usuarisRepo;
    }

    public List<Reserva> findAllReservas() {
        return reservaRepo.findAll();
    }

    public Reserva getReservaById(Integer id) {
        return reservaRepo.findById(id).orElse(null);
    }

    public List<Reserva> getReservesPerUsuari(Long id) {
        if (id == null) {
            return List.of();
        }
        return reservaRepo.findByUsuariId(id);
    }

    public boolean existsById(Integer id) {
        return reservaRepo.existsById(id);
    }

    public boolean vehicleDisponible(
            Long vehicleId, LocalDateTime dataInici, LocalDateTime dataFi) {

        if (vehicleId == null || vehicleId <= 0
                || dataInici == null || dataFi == null
                || !dataFi.isAfter(dataInici)) {
            return false;
        }

        Vehicle vehicle = vehicleRepo.findById(vehicleId).orElse(null);

        if (vehicle == null
                || !Boolean.TRUE.equals(vehicle.getActiu())
                || vehicle.getEstatVehicle() != EstatVehicle.Disponible) {
            return false;
        }

        return reservaRepo.findReservesSolapades(
                vehicleId, dataInici, dataFi, EstatReserva.Anulada).isEmpty();
    }

    @Transactional
    public Integer saveReserva(Reserva reserva) {
        if (reserva == null) {
            throw new IllegalArgumentException("La reserva no pot ser nul·la.");
        }

        if (reserva.getId() != null) {
            throw new IllegalArgumentException("No es poden modificar reserves existents.");
        }

        if (reserva.getDataInici() == null
                || reserva.getDataFi() == null
                || !reserva.getDataFi().isAfter(reserva.getDataInici())) {
            throw new IllegalArgumentException("La data final ha de ser posterior a la inicial.");
        }

        if (reserva.getVehicle() == null
                || reserva.getUsuari() == null
                || reserva.getVehicle().getId() <= 0
                || reserva.getUsuari().getId() == null) {
            throw new IllegalArgumentException("Cal seleccionar un vehicle i un usuari vàlids.");
        }

        Vehicle vehicle = vehicleRepo.findById(reserva.getVehicle().getId()).orElse(null);
        Usuaris usuari = usuarisRepo.findById(reserva.getUsuari().getId()).orElse(null);

        if (vehicle == null || usuari == null) {
            throw new IllegalArgumentException("El vehicle o l'usuari seleccionat no existeix.");
        }

        if (!Boolean.TRUE.equals(vehicle.getActiu())
                || vehicle.getEstatVehicle() != EstatVehicle.Disponible) {
            throw new IllegalArgumentException("El vehicle no està disponible per reservar.");
        }

        if (!"VALIDAT".equalsIgnoreCase(usuari.getEstatRegistre())) {
            throw new IllegalArgumentException("L'usuari ha d'estar validat per fer una reserva.");
        }

        if (vehicle.getPreuDia() == null
                || vehicle.getPreuDia().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El vehicle no té un preu diari vàlid.");
        }

        if (reserva.getImportFianca() == null
                || reserva.getImportFianca().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("La fiança ha de ser igual o superior a zero.");
        }

        Duration durada = Duration.between(reserva.getDataInici(), reserva.getDataFi());
        long dies = durada.toDays();
        if (!durada.minusDays(dies).isZero()) {
            dies++;
        }

        BigDecimal preuTotal = vehicle.getPreuDia()
                .multiply(BigDecimal.valueOf(dies))
                .setScale(2, RoundingMode.HALF_UP);

        reserva.setPreuLloguer(preuTotal);

        BigDecimal fianca = reserva.getImportFianca();
        if (Boolean.TRUE.equals(usuari.getPremium())) {
            fianca = fianca.multiply(new BigDecimal("0.90"));
        }
        reserva.setImportFianca(fianca.setScale(2, RoundingMode.HALF_UP));

        if (reserva.getCodiReserva() == null || reserva.getCodiReserva().isBlank()) {
            String codi;
            do {
                codi = "RES-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            } while (reservaRepo.existsByCodiReserva(codi));
            reserva.setCodiReserva(codi);
        } else if (reservaRepo.existsByCodiReserva(reserva.getCodiReserva())) {
            throw new IllegalArgumentException("Aquest codi de reserva ja existeix.");
        }

        // Evita reservas que coincidan en fechas para el mismo vehículo.
        List<Reserva> reserves
                = reservaRepo.findByVehicle_Id(vehicle.getId());

        for (Reserva existent : reserves) {
            if (existent.getEstat() == EstatReserva.Anulada) {
                continue;
            }
            boolean solapades = reserva.getDataInici().isBefore(existent.getDataFi())
                    && reserva.getDataFi().isAfter(existent.getDataInici());
            if (solapades) {
                throw new IllegalArgumentException("El vehicle ja està reservat en aquestes dates.");
            }
        }

        reserva.setVehicle(vehicle);
        reserva.setUsuari(usuari);
        reserva.setDataReserva(LocalDateTime.now());
        reserva.setEstat(EstatReserva.Confirmada);

        return reservaRepo.save(reserva).getId();
    }

    @Transactional
    public boolean anularReserva(Integer id) {
        Reserva reserva = getReservaById(id);
        if (reserva == null
                || reserva.getEstat() == EstatReserva.Lliurada
                || reserva.getEstat() == EstatReserva.Anulada) {
            return false;
        }

        reserva.setEstat(EstatReserva.Anulada);
        reservaRepo.save(reserva);
        return true;
    }

    public List<Reserva> obtenirInformeReserves(Integer clientId) {
        return reservaRepo.findReservesPerInforme(clientId);
    }
    
     @Transactional
    public BigDecimal anularReservaPerClient(Integer idReserva, Long idUsuariLoguejat) {
        Reserva reserva = getReservaById(idReserva);

        if (reserva == null || !reserva.getUsuari().getId().equals(idUsuariLoguejat)) {
            throw new IllegalArgumentException("La reserva no existeix o no pertany a aquest usuari.");
        }

        if (reserva.getEstat() == EstatReserva.Lliurada || reserva.getEstat() == EstatReserva.Anulada) {
            throw new IllegalArgumentException("Aquesta reserva no es pot anul·lar.");
        }

        // 1. Canviar l'estat de la reserva i alliberar el vehicle
        reserva.setEstat(EstatReserva.Anulada);

        // 2. Comprovar antelació per al retorn de diners
        LocalDate avui = LocalDate.now();
        LocalDate dataIniciReserva = reserva.getDataInici().toLocalDate();

        BigDecimal importRetornat = BigDecimal.ZERO;

        // Si queden >= diesAntelacioDevolucio entre avui i la data d'inici, es retorna l'import complet (lloguer + fiança)
        if (!avui.plusDays(diesAntelacioDevolucio).isAfter(dataIniciReserva)) {
            BigDecimal totalLloguer = reserva.getPreuLloguer() != null ? reserva.getPreuLloguer() : BigDecimal.ZERO;
            BigDecimal fianca = reserva.getImportFianca() != null ? reserva.getImportFianca() : BigDecimal.ZERO;
            importRetornat = totalLloguer.add(fianca);
        }

        // Guardar canvis
        reservaRepo.save(reserva);

// 3. Enviar correu electrònic d'anul·lació (passant els 6 paràmetres)
        double percentatgeRetorn = (importRetornat != null && importRetornat.compareTo(BigDecimal.ZERO) > 0) ? 100.0 : 0.0;

        emailService.enviarEmailAnullacio(
                reserva.getUsuari().getEmail(),
                reserva.getUsuari().getNom(),
                reserva.getCodiReserva(),
                percentatgeRetorn, // % de retorn (100 o 0)
                importRetornat,    // Import calculat en BigDecimal
                null               // Motiu buit quan ho fa el client
        );

        return importRetornat;
    }

        
        // Cerca i filtratge de reserves del client per codi, estat o data.
        public List<Reserva> filtrarReservesClient(Long usuariId, String codi, String estat, String data) {
            List<Reserva> reserves = getReservesPerUsuari(usuariId);

            return reserves.stream()
                    // Filtre per Codi (parcial)
            .filter(r -> codi == null || codi.isBlank() || 
                    (r.getCodiReserva() != null && r.getCodiReserva().toLowerCase().contains(codi.trim().toLowerCase())))
            
            // Filtre per Estat (compara com a text sense importar majúscules/minúscules)
            .filter(r -> estat == null || estat.isBlank() || 
                    (r.getEstat() != null && r.getEstat().name().equalsIgnoreCase(estat.trim())))
            
            // Filtre per Data (coincidència exacta de la data d'inici YYYY-MM-DD)
            .filter(r -> data == null || data.isBlank() || 
                    (r.getDataInici() != null && r.getDataInici().toLocalDate().toString().equals(data.trim())))
            
            .toList();
        }
}