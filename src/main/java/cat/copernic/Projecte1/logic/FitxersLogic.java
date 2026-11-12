package cat.copernic.Projecte1.logic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Lògica per guardar els fitxers que pugen els usuaris (imatge del DNI, de la
 * llicència, fotos del lliurament...).
 *
 * Els fitxers es guarden a la carpeta "uploads" de l'arrel del projecte i es
 * poden veure a l'adreça /uploads/nom_del_fitxer (veure WebConfig).
 *
 * @author Equip4
 */
@Service
public class FitxersLogic {

    // Carpeta on es guarden els fitxers (es crea sola si no existeix)
    public static final String CARPETA_UPLOADS = "uploads";

    private final Logger logger = LoggerFactory.getLogger(FitxersLogic.class);

    /**
     * Guarda un fitxer pujat des d'un formulari.
     *
     * @param fitxer fitxer rebut del formulari (pot ser null o buit)
     * @param prefix text que s'afegeix al principi del nom (ex: "dni_12345678A")
     * @return el nom del fitxer guardat, o null si no s'ha pujat cap fitxer
     * @throws IllegalArgumentException si el fitxer no és una imatge o un PDF, o
     * si no s'ha pogut guardar
     */
    public String guardarFitxer(MultipartFile fitxer, String prefix) {
        if (fitxer == null || fitxer.isEmpty()) {
            return null;
        }

        String nomOriginal = fitxer.getOriginalFilename() != null ? fitxer.getOriginalFilename().toLowerCase() : "";

        // Només acceptem imatges JPG/PNG i documents PDF
        if (!nomOriginal.endsWith(".jpg") && !nomOriginal.endsWith(".jpeg")
                && !nomOriginal.endsWith(".png") && !nomOriginal.endsWith(".pdf")) {
            throw new IllegalArgumentException("Només es poden pujar fitxers JPG, PNG o PDF.");
        }

        String extensio = nomOriginal.substring(nomOriginal.lastIndexOf('.'));
        // Nom únic: prefix + hora actual en mil·lisegons + extensió
        String nomFitxer = prefix.replaceAll("[^A-Za-z0-9_-]", "") + "_" + System.currentTimeMillis() + extensio;

        try {
            Path carpeta = Paths.get(CARPETA_UPLOADS);
            Files.createDirectories(carpeta);
            Files.copy(fitxer.getInputStream(), carpeta.resolve(nomFitxer), StandardCopyOption.REPLACE_EXISTING);
            logger.info("Fitxer guardat: {}", nomFitxer);
            return nomFitxer;
        } catch (IOException e) {
            logger.error("No s'ha pogut guardar el fitxer {}", nomOriginal, e);
            throw new IllegalArgumentException("No s'ha pogut guardar el fitxer. Torna-ho a provar.");
        }
    }
}
