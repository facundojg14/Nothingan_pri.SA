package cat.copernic.Projecte1.repository;

import cat.copernic.Projecte1.entity.Enum.EstatReserva;
import cat.copernic.Projecte1.entity.Reserva;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio JPA encargado de las consultas de reservas.
 *
 * Permite buscar reservas por usuario o vehículo, comprobar su existencia
 * y consultar solapamientos y datos para los informes.
 *
 * @author Equip4
 */
public interface ReservaRepository extends JpaRepository<Reserva, Integer> {

    // ==================== CONSULTAS POR VEHÍCULO Y USUARIO ====================

    /** Busca las reservas de un vehículo. */
    List<Reserva> findByVehicle_Id(long vehicleId);

    /** Busca las reservas de un usuario. */
    List<Reserva> findByUsuariId(Long id);


    // ==================== COMPROBACIONES DE RESERVAS ====================

    /** Comprueba si el código de reserva ya existe. */
    boolean existsByCodiReserva(String codiReserva);

    /** Comprueba si existe alguna reserva asociada al vehículo. */
    boolean existsByVehicle_Id(long vehicleId);

    /** Comprueba si existe alguna reserva del vehículo excluyendo un estado concreto. */
    boolean existsByVehicle_IdAndEstatNot(long vehicleId, EstatReserva estat);

    /** Comprueba si el vehículo tiene reservas en alguno de los estados indicados. */
    boolean existsByVehicle_IdAndEstatIn(long vehicleId, List<EstatReserva> estats);


    // ==================== BÚSQUEDA DE RESERVAS SOLAPADAS ====================

    /**
     * Busca las reservas de un vehículo que coinciden con un intervalo de fechas,
     * excluyendo el estado indicado.
     *
     * Dos reservas se solapan cuando una empieza antes de que termine la otra
     * y termina después de que empiece.
     */
    List<Reserva> findByVehicle_IdAndEstatNotAndDataIniciLessThanAndDataFiGreaterThan(
            long vehicleId,
            EstatReserva estat,
            LocalDateTime dataFi,
            LocalDateTime dataInici);


    // ==================== CONSULTAS PARA INFORMES ====================

    /** Busca todas las reservas ordenadas por cliente y fecha más reciente. */
    List<Reserva> findAllByOrderByUsuari_NomAscDataIniciDesc();

    /** Busca las reservas de un cliente ordenadas por fecha más reciente. */
    List<Reserva> findByUsuari_IdOrderByDataIniciDesc(Long usuariId);


    // ==================== BÚSQUEDA POR CÓDIGO DE RESERVA ====================

    /** Busca una reserva mediante su código. */
    Optional<Reserva> findByCodiReserva(String codiReserva);
}