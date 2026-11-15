package cat.copernic.Projecte1.repository;

import cat.copernic.Projecte1.entity.Enum.EstatReserva;
import cat.copernic.Projecte1.entity.Reserva;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

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

    // Busca las reservas de un vehículo que se solapan con unas fechas (sin contar un estado, ej. Anulada).
    // Se solapan si la reserva empieza antes de la nueva dataFi y acaba después de la nueva dataInici.
    // Los parámetros van en el mismo orden que en el nombre del método.
    List<Reserva> findByVehicle_IdAndEstatNotAndDataIniciLessThanAndDataFiGreaterThan(
            long vehicleId,
            EstatReserva estat,
            LocalDateTime dataFi,
            LocalDateTime dataInici);

    // BUSQUEDA PER INFORMES: totes les reserves, ordenades per nom del client i per data (la més recent primer)
    List<Reserva> findAllByOrderByUsuari_NomAscDataIniciDesc();

    // BUSQUEDA PER INFORMES: les reserves d'un client, ordenades per data (la més recent primer)
    List<Reserva> findByUsuari_IdOrderByDataIniciDesc(Long usuariId);

    Optional<Reserva> findByCodiReserva(String codiReserva);
}
