package cat.copernic.Projecte1.logic;

import cat.copernic.Projecte1.entity.Enum.Categoria;
import cat.copernic.Projecte1.entity.Enum.EstatReserva;
import cat.copernic.Projecte1.entity.Enum.EstatVehicle;
import cat.copernic.Projecte1.entity.Vehicle;
import cat.copernic.Projecte1.repository.ReservaRepository;
import cat.copernic.Projecte1.repository.VehicleRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Capa de lògica de negoci (Service) per a la gestió integral de vehicles.
 */
@Service
public class VehicleLogic {

    private final VehicleRepository vehicleRepo;
    private final ReservaRepository reservaRepo;
    private final ReservaLogic reservaLogic;

    // Logger per registrar les baixes de vehicles al fitxer de logs (RN04)
    private final Logger logger = LoggerFactory.getLogger(VehicleLogic.class);

    public VehicleLogic(VehicleRepository vehicleRepo, ReservaRepository reservaRepo, ReservaLogic reservaLogic) {
        this.vehicleRepo = vehicleRepo;
        this.reservaRepo = reservaRepo;
        this.reservaLogic = reservaLogic;
    }

    /**
     * Comprova si el vehicle té reserves actives, és a dir, en estat
     * Confirmada (pendent de lliurar) o Lliurada (el client té el vehicle).
     * Les reserves Anulada o Retornada ja han acabat i no impedeixen la baixa.
     *
     * @param vehicleId identificador del vehicle
     * @return true si té alguna reserva Confirmada o Lliurada
     */
    public boolean teReservesActives(Long vehicleId) {
        if (vehicleId == null) {
            return false;
        }
        return reservaRepo.existsByVehicle_IdAndEstatIn(vehicleId,
                List.of(EstatReserva.Confirmada, EstatReserva.Lliurada));
    }

    /**
     * Dona de baixa lògica un vehicle (RF41): no s'esborra de la base de
     * dades, es marca com a inactiu i amb estat Baixa.
     *
     * @param id identificador del vehicle
     * @return true si s'ha donat de baixa; false si té reserves o no existeix
     */
    @Transactional
    public boolean donarDeBaixaVehicle(Long id) {
        if (teReservesActives(id)) {
            logger.warn("No es pot donar de baixa el vehicle {}: té reserves", id);
            return false;
        }

        Vehicle vehicle = getVehicleById(id);
        if (vehicle != null) {
            vehicle.setActiu(false);
            vehicle.setEstatVehicle(EstatVehicle.Baixa);
            vehicleRepo.save(vehicle);
            logger.info("Vehicle {} donat de baixa", vehicle.getMatricula());
            return true;
        }

        return false;
    }

    /**
     * Obté tots els vehicles de la base de dades.
     */
    public List<Vehicle> findAllVehicles() {
        return vehicleRepo.findAll();
    }

    /**
     * Valida i desa un vehicle (alta o actualització) assegurant la coherència
     * de preu per dia, preu per hora i fiança estàndard.
     *
     * @param vehicle Entitat del vehicle a desar.
     * @return Identificador del vehicle desat.
     * @throws IllegalArgumentException Si alguna tarifa, matrícula o dada
     * obligatòria no és vàlida.
     */
    @Transactional
    public Long saveVehicle(Vehicle vehicle) {
        validarDadesVehicle(vehicle);

        // Si el preu per hora no s'ha informat o és 0, es calcula automàticament (preuDia / 24)
        if (vehicle.getPreuHora() == null || vehicle.getPreuHora().compareTo(BigDecimal.ZERO) <= 0) {
            BigDecimal preuHoraCalculat = vehicle.getPreuDia()
                    .divide(BigDecimal.valueOf(24), 2, RoundingMode.HALF_UP);
            vehicle.setPreuHora(preuHoraCalculat);
        }

        Vehicle ret = vehicleRepo.save(vehicle);
        return ret.getId();
    }

    /**
     * Valida les regles de negoci d'un vehicle abans de desar-lo.
     */
    private void validarDadesVehicle(Vehicle vehicle) {
        if (vehicle == null) {
            throw new IllegalArgumentException("Les dades del vehicle no poden ser nul·les.");
        }
        if (vehicle.getMatricula() == null || vehicle.getMatricula().trim().isEmpty()) {
            throw new IllegalArgumentException("La matrícula del vehicle és obligatòria.");
        }
        if (existsByMatricula(vehicle.getMatricula(), vehicle.getId())) {
            throw new IllegalArgumentException("Ja existeix un altre vehicle registrat amb la matrícula " + vehicle.getMatricula() + ".");
        }
        if (vehicle.getPreuDia() == null || vehicle.getPreuDia().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El preu per dia del vehicle ha de ser superior a 0 €.");
        }
        if (vehicle.getPreuHora() != null && vehicle.getPreuHora().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El preu per hora del vehicle no pot ser negatiu.");
        }
        if (vehicle.getFiancaEstandar() == null || vehicle.getFiancaEstandar().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("La fiança estàndard del vehicle no pot ser nul·la ni negativa.");
        }
    }

    /**
     * Elimina un vehicle de la base de dades a partir del seu ID.
     */
    @Transactional
    public void deleteVehicleById(Long id) {
        vehicleRepo.deleteById(id);
    }

    /**
     * Cerca un vehicle per la seva ID.
     */
    public Vehicle getVehicleById(Long id) {
        return vehicleRepo.findById(id).orElse(null);
    }

    /**
     * Cerca vehicles que continguin una determinada matrícula.
     */
    public List<Vehicle> findByMatricula(String matricula) {
        if (matricula == null || matricula.trim().isEmpty()) {
            return vehicleRepo.findAll();
        }
        return vehicleRepo.findByMatriculaContainingIgnoreCase(matricula.trim());
    }

    /**
     * Comprova si la matrícula ja existeix en un altre vehicle de la base de
     * dades.
     */
    public boolean existsByMatricula(String matricula, long id) {
        if (matricula == null || matricula.trim().isEmpty()) {
            return false;
        }
        Optional<Vehicle> existing = vehicleRepo.findByMatriculaIgnoreCase(matricula.trim());
        return existing.isPresent() && (id == 0 || existing.get().getId() != id);
    }

    /**
     * Filtra els vehicles per diversos criteris alhora (RF45).
     * Els criteris buits o nuls no es tenen en compte.
     *
     * @param matricula part de la matrícula
     * @param marca part de la marca
     * @param model part del model
     * @param anyFabricacio any de fabricació exacte
     * @param categoria categoria del vehicle
     * @param dataInici inici del període en què ha d'estar disponible
     * @param dataFi fi del període en què ha d'estar disponible
     * @return vehicles que compleixen tots els filtres indicats
     */
    public List<Vehicle> findByFiltres(String matricula, String marca, String model,
            Integer anyFabricacio, Categoria categoria,
            LocalDateTime dataInici, LocalDateTime dataFi) {

        List<Vehicle> base = findByMatricula(matricula);

        String marcaClean = (marca != null && !marca.trim().isEmpty()) ? marca.trim().toLowerCase() : null;
        String modelClean = (model != null && !model.trim().isEmpty()) ? model.trim().toLowerCase() : null;
        boolean validarFechas = (dataInici != null && dataFi != null && dataFi.isAfter(dataInici));

        return base.stream()
                .filter(v -> marcaClean == null || (v.getMarca() != null && v.getMarca().toLowerCase().contains(marcaClean)))
                .filter(v -> modelClean == null || (v.getModel() != null && v.getModel().toLowerCase().contains(modelClean)))
                .filter(v -> anyFabricacio == null || anyFabricacio <= 0 || anyFabricacio.equals(v.getAnyFabricacio()))
                .filter(v -> categoria == null || (v.getCategoria() != null && v.getCategoria().name().equalsIgnoreCase(categoria.name())))
                .filter(v -> !validarFechas || reservaLogic.vehicleDisponible(v.getId(), dataInici, dataFi))
                .toList();
    }

    /**
     * Busca vehicles disponibles per a les dates seleccionades.
     */
    public List<Vehicle> findVehiclesDisponibles(LocalDateTime dataInici, LocalDateTime dataFi) {
        if (dataInici == null || dataFi == null || !dataFi.isAfter(dataInici)) {
            return List.of();
        }

        return vehicleRepo.findByEstatVehicleAndActiuTrue(EstatVehicle.Disponible)
                .stream()
                .filter(v -> reservaLogic.vehicleDisponible(v.getId(), dataInici, dataFi))
                .toList();
    }

    /**
     * Comprova si el vehicle té qualsevol reserva associada.
     */
    public boolean teAlgunaReserva(Long vehicleId) {
        if (vehicleId == null) {
            return false;
        }
        return reservaRepo.existsByVehicle_Id(vehicleId);
    }

    /**
     * Eliminació física definitiva de la base de dades (només si no té
     * historial de reserves).
     */
    @Transactional
    public boolean eliminarVehicleDefinitivament(Long id) {
        if (teAlgunaReserva(id)) {
            return false;
        }

        Vehicle vehicle = getVehicleById(id);
        if (vehicle != null) {
            vehicleRepo.deleteById(id);
            return true;
        }

        return false;
    }
}