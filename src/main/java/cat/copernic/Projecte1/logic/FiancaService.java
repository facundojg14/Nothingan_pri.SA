package cat.copernic.Projecte1.logic;

import cat.copernic.Projecte1.entity.Enum.EstatReserva;
import cat.copernic.Projecte1.entity.Enum.EstatVehicle;
import cat.copernic.Projecte1.entity.Enum.ReputacioClient;
import cat.copernic.Projecte1.entity.Reserva;
import cat.copernic.Projecte1.entity.Usuaris;
import cat.copernic.Projecte1.entity.Vehicle;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Servei encarregat de la gestió integral de les fiances:
 * càlcul segons reputació del client, registre del pagament inicial,
 * retenció per desperfectes o retard i liquidació final en el retorn del vehicle.
 */
@Service
public class FiancaService {

    private static final Logger logger = LoggerFactory.getLogger(FiancaService.class);

    /**
     * Factor multiplicador aplicat als clients amb reputació PREMIUM (25% de descompte).
     */
    private static final BigDecimal FACTOR_DESCOMPTE_PREMIUM = new BigDecimal("0.75");

    /**
     * Calcula l'import de la fiança que ha d'abonar un client en funció del vehicle
     * i de la seva reputació ({@code NORMAL} o {@code PREMIUM}).
     *
     * @param vehicle Vehicle objecte de la reserva.
     * @param client  Client que realitza la reserva.
     * @return Import de la fiança calculat amb 2 decimals.
     * @throws IllegalArgumentException Si el vehicle, el client o la fiança estàndard són nuls o negatius.
     */
    public BigDecimal calcularFiancaInicial(Vehicle vehicle, Usuaris client) {
        if (vehicle == null) {
            throw new IllegalArgumentException("El vehicle no pot ser nul per calcular la fiança.");
        }
        if (client == null) {
            throw new IllegalArgumentException("El client no pot ser nul per calcular la fiança.");
        }
        if (vehicle.getFiancaEstandar() == null || vehicle.getFiancaEstandar().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("La fiança estàndard del vehicle no pot ser nul·la ni negativa.");
        }

        BigDecimal fiancaEstandar = vehicle.getFiancaEstandar();
        ReputacioClient reputacio = client.getReputacio();

        BigDecimal fiancaCalculada;
        if (reputacio == ReputacioClient.PREMIUM) {
            fiancaCalculada = fiancaEstandar.multiply(FACTOR_DESCOMPTE_PREMIUM);
            logger.info("Client PREMIUM ({}): aplicat descompte del 25% sobre fiança estàndard de {} €.",
                    client.getEmail(), fiancaEstandar);
        } else {
            fiancaCalculada = fiancaEstandar;
            logger.info("Client NORMAL ({}): aplicada fiança estàndard íntegra de {} €.",
                    client.getEmail(), fiancaEstandar);
        }

        return fiancaCalculada.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Registra el pagament per avançat d'una reserva calculant la fiança aplicable
     * i fixant l'import total cobrat ({@code preuLloguer + importFianca}) perquè quedi
     * immutabilitzat davant futurs canvis de tarifa o reputació.
     *
     * @param reserva Reserva a inicialitzar i cobrar.
     * @throws IllegalArgumentException Si la reserva, el vehicle, el client o el preu de lloguer no són vàlids.
     */
    public void processarPagamentInicialReserva(Reserva reserva) {
        if (reserva == null) {
            throw new IllegalArgumentException("La reserva no pot ser nul·la.");
        }
        if (reserva.getPreuLloguer() == null || reserva.getPreuLloguer().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("L'import del lloguer no pot ser nul ni negatiu.");
        }

        BigDecimal fiancaCobrada = calcularFiancaInicial(reserva.getVehicle(), reserva.getUsuari());
        BigDecimal importTotal = reserva.getPreuLloguer()
                .add(fiancaCobrada)
                .setScale(2, RoundingMode.HALF_UP);

        reserva.setImportFianca(fiancaCobrada);
        reserva.setCostFinal(importTotal);
        reserva.setFiancaRetinguda(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        reserva.setFiancaRetornada(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        reserva.setCarrecExtraPendent(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));

        logger.info("Pagament inicial registrat per a la reserva {}: Lloguer = {} €, Fiança cobrada = {} €, Total = {} €.",
                reserva.getCodiReserva(), reserva.getPreuLloguer(), fiancaCobrada, importTotal);
    }

    /**
     * Processa el retorn del vehicle i calcula la liquidació de la fiança seguint les regles de negoci:
     * <ol>
     *   <li>Si hi ha desperfectes imputables al client, es reté el 100% de la fiança ({@code fiancaRetornada = 0}).</li>
     *   <li>Si no hi ha desperfectes imputables, es calcula el retard respecte a {@code dataFi} i es descompta
     *       {@code horesRetras * vehicle.getPreuHora()}.</li>
     *   <li>Si la penalització supera la fiança pagada, es retorna 0 € i es registra la diferència a {@code carrecExtraPendent}.</li>
     * </ol>
     *
     * @param reserva                Reserva que es finalitza.
     * @param dataHoraRetornReal     Data i hora efectiva en què es retorna el vehicle.
     * @param desperfectesImputables {@code true} si hi ha danys per mal ús o imprudència del client; {@code false} altrament.
     * @return Import final de la fiança a retornar al client.
     * @throws IllegalArgumentException Si algun paràmetre obligatori és nul o incoherent.
     */
    public BigDecimal processarRetornVehicle(Reserva reserva, LocalDateTime dataHoraRetornReal, boolean desperfectesImputables) {
        validarDadesRetorn(reserva, dataHoraRetornReal);

        BigDecimal fiancaPagada = reserva.getImportFianca().setScale(2, RoundingMode.HALF_UP);
        Vehicle vehicle = reserva.getVehicle();

        reserva.setDataHoraRetorn(dataHoraRetornReal);
        reserva.setDesperfectesImputables(desperfectesImputables);

        BigDecimal fiancaARetornar;
        BigDecimal fiancaRetinguda;
        BigDecimal carrecExtra = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        if (desperfectesImputables) {
            // Regla 1: Desperfectes imputables -> Retenció del 100% de la fiança
            fiancaARetornar = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            fiancaRetinguda = fiancaPagada;

            logger.warn("Reserva {}: Desperfectes imputables detectats. Es reté el 100% de la fiança ({} €).",
                    reserva.getCodiReserva(), fiancaRetinguda);
        } else {
            // Regla 2: Sense desperfectes imputables -> Càlcul de penalització per retard
            long horesRetras = calcularHoresRetras(reserva.getDataFi(), dataHoraRetornReal);
            BigDecimal penalitzacioRetras = vehicle.getPreuHora()
                    .multiply(BigDecimal.valueOf(horesRetras))
                    .setScale(2, RoundingMode.HALF_UP);

            if (penalitzacioRetras.compareTo(fiancaPagada) > 0) {
                // El retard supera la fiança pagada -> Retorn 0 € i registre del càrrec extra pendent
                fiancaARetornar = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                fiancaRetinguda = fiancaPagada;
                carrecExtra = penalitzacioRetras.subtract(fiancaPagada).setScale(2, RoundingMode.HALF_UP);

                logger.warn("Reserva {}: Retard de {} hores (penalització {} €) supera la fiança pagada ({} €). Càrrec extra pendent: {} €.",
                        reserva.getCodiReserva(), horesRetras, penalitzacioRetras, fiancaPagada, carrecExtra);
            } else {
                fiancaARetornar = fiancaPagada.subtract(penalitzacioRetras).setScale(2, RoundingMode.HALF_UP);
                fiancaRetinguda = penalitzacioRetras;

                logger.info("Reserva {}: Retard de {} hores. Penalització: {} €. Fiança a retornar: {} €.",
                        reserva.getCodiReserva(), horesRetras, penalitzacioRetras, fiancaARetornar);
            }
        }

        // Actualització de l'estat econòmic i operatiu de la reserva i del vehicle
        reserva.setFiancaRetornada(fiancaARetornar);
        reserva.setFiancaRetinguda(fiancaRetinguda);
        reserva.setCarrecExtraPendent(carrecExtra);
        reserva.setCostFinal(reserva.getPreuLloguer().add(fiancaRetinguda).add(carrecExtra).setScale(2, RoundingMode.HALF_UP));
        reserva.setEstat(EstatReserva.Retornada);

        vehicle.setEstatVehicle(EstatVehicle.Disponible);

        return fiancaARetornar;
    }

    /**
     * Calcula el nombre d'hores completes o fraccions d'hora de retard entre la data prevista
     * de finalització i la data real de retorn. Qualsevol minut de retard computa com una hora addicional.
     *
     * @param dataFiPrevista Data i hora prevista de fi de reserva.
     * @param dataRetornReal Data i hora real de devolució.
     * @return Nombre d'hores de retard (0 si es retorna a temps o abans d'hora).
     */
    public long calcularHoresRetras(LocalDateTime dataFiPrevista, LocalDateTime dataRetornReal) {
        if (!dataRetornReal.isAfter(dataFiPrevista)) {
            return 0L;
        }
        long minutsRetras = Duration.between(dataFiPrevista, dataRetornReal).toMinutes();
        return (long) Math.ceil(minutsRetras / 60.0);
    }

    /**
     * Valida que totes les dades necessàries per processar el retorn del vehicle siguin presents i vàlides.
     */
    private void validarDadesRetorn(Reserva reserva, LocalDateTime dataHoraRetornReal) {
        if (reserva == null) {
            throw new IllegalArgumentException("La reserva no pot ser nul·la per processar el retorn.");
        }
        if (dataHoraRetornReal == null) {
            throw new IllegalArgumentException("La data i hora de retorn real no pot ser nul·la.");
        }
        if (reserva.getDataFi() == null) {
            throw new IllegalArgumentException("La reserva no té definida una data de finalització prevista.");
        }
        if (reserva.getDataInici() != null && dataHoraRetornReal.isBefore(reserva.getDataInici())) {
            throw new IllegalArgumentException("La data de retorn real no pot ser anterior a la data d'inici de la reserva.");
        }
        if (reserva.getImportFianca() == null || reserva.getImportFianca().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("L'import de la fiança pagada a la reserva no és vàlid.");
        }
        if (reserva.getPreuLloguer() == null || reserva.getPreuLloguer().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El preu de lloguer de la reserva no és vàlid.");
        }
        if (reserva.getVehicle() == null) {
            throw new IllegalArgumentException("La reserva no té cap vehicle associat.");
        }
        if (reserva.getVehicle().getPreuHora() == null || reserva.getVehicle().getPreuHora().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El preu per hora del vehicle no pot ser nul ni negatiu.");
        }
    }
}
