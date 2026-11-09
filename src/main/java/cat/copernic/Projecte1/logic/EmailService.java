package cat.copernic.Projecte1.logic;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;

@Service
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;
    
    public void enviarEmailAnullacio(String toEmail, String nomClient, String codiReserva, BigDecimal importRetornat) {
        if (mailSender == null || toEmail == null || toEmail.isBlank()) {
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("no-reply@nothinganpri.com");
        message.setTo(toEmail);
        message.setSubject("Anul·lació de la reserva " + codiReserva + " - Nothingan Pri");

        StringBuilder cos = new StringBuilder();
        cos.append("Hola ").append(nomClient).append(",\n\n");
        cos.append("T'informem que la teva reserva amb codi ").append(codiReserva).append(" ha estat anul·lada correctament.\n\n");

        if (importRetornat != null && importRetornat.compareTo(BigDecimal.ZERO) > 0) {
            cos.append("Com que l'anul·lació s'ha fet amb l'antelació requerida, se't retornarà el 100% de l'import: ")
               .append(importRetornat).append(" €.\n");
        } else {
            cos.append("Segons la nostra política d'anul·lació, al no complir el termini mínim d'antelació, no se't retornarà cap import.\n");
        }

        cos.append("\nGràcies per confiar en Nothingan Pri.");
        message.setText(cos.toString());

        try {
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Error enviant correu d'anul·lació: " + e.getMessage());
        }
    }
}
