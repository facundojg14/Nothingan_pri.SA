package cat.copernic.Projecte1.entity;

import cat.copernic.Projecte1.entity.Enum.Categoria;
import cat.copernic.Projecte1.entity.Enum.EstatVehicle;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Entitat que representa un vehicle de la flota de lloguer.
 */
@Entity
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "Matricula", nullable = false, unique = true, length = 15)
    private String matricula;

    @Column(name = "Marca", nullable = false, length = 50)
    private String marca;

    @Column(name = "Model", nullable = false, length = 50)
    private String model;

    @Column(name = "Any_Fabricacio")
    private int anyFabricacio;

    @Enumerated(EnumType.STRING)
    @Column(name = "Categoria", nullable = false)
    private Categoria categoria;

    @Column(name = "Preu_Dia", nullable = false, precision = 7, scale = 2)
    private BigDecimal preuDia;

    @Column(name = "Preu_Hora", nullable = false, precision = 7, scale = 2)
    private BigDecimal preuHora;

    @Column(name = "Fianca_Estandar", nullable = false, precision = 10, scale = 2)
    private BigDecimal fiancaEstandar;

    @Enumerated(EnumType.STRING)
    @Column(name = "Estat_Vehicle", nullable = false)
    private EstatVehicle estatVehicle = EstatVehicle.Disponible;

    @Column(name = "Actiu")
    private Boolean actiu = true;

    @Column(name = "Foto_vehicle", length = 255)
    private String fotoVehicle;

    public Vehicle() {
    }

    public Vehicle(long id, String matricula, String marca, String model, int anyFabricacio,
            Categoria categoria, BigDecimal preuDia, BigDecimal preuHora,
            BigDecimal fiancaEstandar, String fotoVehicle) {
        this.id = id;
        this.matricula = matricula;
        this.marca = marca;
        this.model = model;
        this.anyFabricacio = anyFabricacio;
        this.categoria = categoria;
        this.preuDia = preuDia;
        this.preuHora = (preuHora != null && preuHora.compareTo(BigDecimal.ZERO) > 0)
                ? preuHora
                : calcularPreuHoraPerDefecte(preuDia);
        this.fiancaEstandar = fiancaEstandar;
        this.fotoVehicle = fotoVehicle;
    }

    /**
     * Garanteix la coherència de tarifes i estats abans de persistir o
     * actualitzar a BD.
     */
    @PrePersist
    @PreUpdate
    private void prepararDadesAbansDeDesar() {
        if (this.matricula != null) {
            this.matricula = this.matricula.trim().toUpperCase();
        }
        if (this.preuDia != null && (this.preuHora == null || this.preuHora.compareTo(BigDecimal.ZERO) <= 0)) {
            this.preuHora = calcularPreuHoraPerDefecte(this.preuDia);
        }
        if (this.fiancaEstandar == null) {
            this.fiancaEstandar = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (this.actiu == null) {
            this.actiu = true;
        }
    }

    private BigDecimal calcularPreuHoraPerDefecte(BigDecimal preuDia) {
        if (preuDia == null || preuDia.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return preuDia.divide(BigDecimal.valueOf(24), 2, RoundingMode.HALF_UP);
    }

    // --- GETTERS Y SETTERS ---
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getMatricula() {
        return matricula;
    }

    public void setMatricula(String matricula) {
        this.matricula = matricula;
    }

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca = marca;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getAnyFabricacio() {
        return anyFabricacio;
    }

    public void setAnyFabricacio(int anyFabricacio) {
        this.anyFabricacio = anyFabricacio;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
    }

    public BigDecimal getPreuDia() {
        return preuDia;
    }

    public void setPreuDia(BigDecimal preuDia) {
        this.preuDia = preuDia;
        if (preuDia != null && (this.preuHora == null || this.preuHora.compareTo(BigDecimal.ZERO) == 0)) {
            this.preuHora = calcularPreuHoraPerDefecte(preuDia);
        }
    }

    public BigDecimal getPreuHora() {
        return preuHora;
    }

    public void setPreuHora(BigDecimal preuHora) {
        this.preuHora = preuHora;
    }

    public BigDecimal getFiancaEstandar() {
        return fiancaEstandar;
    }

    public void setFiancaEstandar(BigDecimal fiancaEstandar) {
        this.fiancaEstandar = fiancaEstandar;
    }

    public EstatVehicle getEstatVehicle() {
        return estatVehicle;
    }

    public void setEstatVehicle(EstatVehicle estatVehicle) {
        this.estatVehicle = estatVehicle;
    }

    public Boolean getActiu() {
        return actiu;
    }

    public void setActiu(Boolean actiu) {
        this.actiu = actiu;
    }

    public String getFotoVehicle() {
        return fotoVehicle;
    }

    public void setFotoVehicle(String fotoVehicle) {
        this.fotoVehicle = fotoVehicle;
    }
}
