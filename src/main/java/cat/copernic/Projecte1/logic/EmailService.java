package cat.copernic.Projecte1.logic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class EmailService {

    // Logger per registrar els enviaments i errors al fitxer de logs (RN04)
    private final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    /**
     * Mètode únic per enviar notificacions d'anul·lació de reserva.
     * Serveix tant si l'anul·la un Client, com un Agent o un Admin.
     */
    public void enviarEmailAnullacio(String toEmail, String nomClient, String codiReserva, 
                                      double percentatgeRetorn, BigDecimal importRetornat, String motiu) {
        if (mailSender == null || toEmail == null || toEmail.isBlank()) {
            logger.warn("No s'ha enviat el correu de la reserva {}: falta el servidor de correu o l'email", codiReserva);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("no-reply@nothinganpri.com");
        message.setTo(toEmail);
        message.setSubject("Anul·lació de la reserva " + codiReserva + " - Nothingan Pri");

        StringBuilder cos = new StringBuilder();
        cos.append("Hola ").append(nomClient != null ? nomClient : "Client").append(",\n\n");
        cos.append("T'informem que la teva reserva amb codi ").append(codiReserva).append(" ha estat anul·lada.\n\n");

        // Si s'ha especificat un motiu (ex: anul·lació per Agent/Admin), s'inclou al correu
        if (motiu != null && !motiu.isBlank()) {
            cos.append("Motiu de l'anul·lació: ").append(motiu).append("\n\n");
        }

        // Informació de la devolució de l'import
        if (importRetornat != null && importRetornat.compareTo(BigDecimal.ZERO) > 0) {
            cos.append("S'ha establert una devolució del ").append((int) percentatgeRetorn).append("% de l'import pagat, ")
               .append("corresponent a un total de: ").append(importRetornat).append(" €.\n");
        } else {
            cos.append("Segons la política/resolució d'anul·lació, no se't retornarà cap import per aquesta reserva.\n");
        }

        cos.append("\nGràcies per confiar en Nothingan Pri.");
        message.setText(cos.toString());

        try {
            mailSender.send(message);
            logger.info("Correu d'anul·lació enviat a {} (reserva {})", toEmail, codiReserva);
        } catch (Exception e) {
            logger.error("Error enviant correu d'anul·lació a {}: {}", toEmail, e.getMessage());
        }
    }
}