package cat.copernic.Projecte1.repository;

import cat.copernic.Projecte1.entity.Enum.EstatReserva;
import cat.copernic.Projecte1.entity.Reserva;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositori JPA de les reserves.
 *
 * Conté les consultes per buscar reserves per usuari o per vehicle, comprovar
 * si dues reserves es solapen en el temps i obtenir les dades de l'informe de
 * reserves per client.
 *
 * @author Equip4
 */
public interface ReservaRepository extends JpaRepository<Reserva, Integer> {

    // Comprueba si el código de reserva ya existe.
    boolean existsByCodiReserva(String codiReserva);

    // Busca las reservas de un vehículo.
    List<Reserva> findByVehicle_Id(long vehicleId);

    // Busca las reservas de un usuario.
    List<Reserva> findByUsuariId(Long id);

    // Comprueba si existe alguna reserva asociada al vehículo
    boolean existsByVehicle_Id(long vehicleId);

    // Comprueba si existe alguna reserva asociada al vehículo excluyendo un estado concreto (ej. ANULADA)
    boolean existsByVehicle_IdAndEstatNot(long vehicleId, EstatReserva estat);

    // Comprueba si el vehículo tiene alguna reserva en uno de los estados indicados (ej. Confirmada o Lliurada)
    boolean existsByVehicle_IdAndEstatIn(long vehicleId, List<EstatReserva> estats);

    // Busca las reservas de un vehículo que coinciden con unas fechas.
    @Query("SELECT r FROM Reserva r WHERE r.vehicle.id = :vehicleId "
            + "AND r.estat <> :anulada "
            + "AND r.dataInici < :dataFi "
            + "AND r.dataFi > :dataInici")
    List<Reserva> findReservesSolapades(
            @Param("vehicleId") long vehicleId,
            @Param("dataInici") LocalDateTime dataInici,
            @Param("dataFi") LocalDateTime dataFi,
            @Param("anulada") EstatReserva anulada);

    // BUSQUEDA PER INFORMES    
    @Query("SELECT r FROM Reserva r WHERE "
            + "(:clientId IS NULL OR r.usuari.id = :clientId) "
            + "ORDER BY r.usuari.nom ASC, r.dataInici DESC")
    List<Reserva> findReservesPerInforme(@Param("clientId") Integer clientId);

    Optional<Reserva> findByCodiReserva(String codiReserva);
}
