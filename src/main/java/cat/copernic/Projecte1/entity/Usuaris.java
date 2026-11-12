package cat.copernic.Projecte1.entity;

import jakarta.persistence.Column;
import cat.copernic.Projecte1.entity.Enum.rol;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

@Entity
public class Usuaris {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Dades bàsiques i accés
    private String nom;
    @Column(nullable = false, unique = true)
    private String email;
    private String password;
    private String telefon;
    private String adreca;
    private String poblacio;
    private String nacionalitat;
    @Enumerated(EnumType.STRING)
    private rol rol;

    // Document d'identitat (DNI/Passaport)
    @Column(nullable = false, unique = true)
    private String dni;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate dataCaducitatDni;
    private String imatgedni;

    // Llicència de conduir
    private String tipusLlicencia;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate dataCaducitatLlicencia;
    private String imatgeLlicencia;

    // Dades de pagament
    private String targetaCredit;

    // Estats i perfil
    private String estatRegistre; // "PENDENT", "VALIDAT", "REBUTJAT", "BAIXA"
    private Boolean premium;

    public Usuaris() {
    }

    public Usuaris(String nom, String dni, String email, String password, String telefon, String estatRegistre, rol rol) {
        this.nom = nom;
        this.dni = dni;
        this.email = email;
        this.password = password;
        this.telefon = telefon;
        this.estatRegistre = estatRegistre;
        this.rol = rol;
    }

    // --- GETTERS Y SETTERS ---
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getTelefon() {
        return telefon;
    }

    public void setTelefon(String telefon) {
        this.telefon = telefon;
    }

    public String getAdreca() {
        return adreca;
    }

    public void setAdreca(String adreca) {
        this.adreca = adreca;
    }

    public String getNacionalitat() {
        return nacionalitat;
    }

    public void setNacionalitat(String nacionalitat) {
        this.nacionalitat = nacionalitat;
    }

    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }

    public LocalDate getDataCaducitatDni() {
        return dataCaducitatDni;
    }

    public void setDataCaducitatDni(LocalDate dataCaducitatDni) {
        this.dataCaducitatDni = dataCaducitatDni;
    }

    public String getImatgedni() {
        return imatgedni;
    }

    public void setImatgedni(String imatgedni) {
        this.imatgedni = imatgedni;
    }

    public String getTipusLlicencia() {
        return tipusLlicencia;
    }

    public void setTipusLlicencia(String tipusLlicencia) {
        this.tipusLlicencia = tipusLlicencia;
    }

    public LocalDate getDataCaducitatLlicencia() {
        return dataCaducitatLlicencia;
    }

    public void setDataCaducitatLlicencia(LocalDate dataCaducitatLlicencia) {
        this.dataCaducitatLlicencia = dataCaducitatLlicencia;
    }

    public String getImatgeLlicencia() {
        return imatgeLlicencia;
    }

    public void setImatgeLlicencia(String imatgeLlicencia) {
        this.imatgeLlicencia = imatgeLlicencia;
    }

    public String getTargetaCredit() {
        return targetaCredit;
    }

    public void setTargetaCredit(String targetaCredit) {
        this.targetaCredit = targetaCredit;
    }

    public String getEstatRegistre() {
        return estatRegistre;
    }

    public void setEstatRegistre(String estatRegistre) {
        this.estatRegistre = estatRegistre;
    }

    public Boolean getPremium() {
        return premium;
    }

    public void setPremium(Boolean premium) {
        this.premium = premium;
    }

    public String getPoblacio() {
        return poblacio;
    }

    public void setPoblacio(String poblacio) {
        this.poblacio = poblacio;
    }

    public rol getRol() {
        return rol;
    }

    public void setRol(rol rol) {
        this.rol = rol;
    }

    /**
     * Obté la reputació del client a partir del seu estat Premium.
     *
     * @return {@link ReputacioClient#PREMIUM} si és client premium,
     * {@link ReputacioClient#NORMAL} en cas contrari.
     */
    public cat.copernic.Projecte1.entity.Enum.ReputacioClient getReputacio() {
        return Boolean.TRUE.equals(this.premium)
                ? cat.copernic.Projecte1.entity.Enum.ReputacioClient.PREMIUM
                : cat.copernic.Projecte1.entity.Enum.ReputacioClient.NORMAL;
    }

    /**
     * Estableix la reputació del client i sincronitza el camp {@code premium}.
     *
     * @param reputacio Nivell de reputació a assignar.
     */
    public void setReputacio(cat.copernic.Projecte1.entity.Enum.ReputacioClient reputacio) {
        this.premium = (reputacio == cat.copernic.Projecte1.entity.Enum.ReputacioClient.PREMIUM);
    }
}
