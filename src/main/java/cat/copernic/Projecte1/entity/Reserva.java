package cat.copernic.Projecte1.entity;

import cat.copernic.Projecte1.entity.Enum.EstatReserva;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;

/*
 * Entidad que representa una reserva.
 * Guarda sus fechas, importes, estado y los datos del vehículo y usuario asociados.
 */
@Entity
public class Reserva {

    // Identificador único de la reserva.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Código único que identifica la reserva.
    @Column(name = "codi_reserva", nullable = false, unique = true)
    private String codiReserva;

    // Fecha en la que se crea la reserva.
    @Column(nullable = false)
    private LocalDateTime dataReserva;

    // Fechas y horas de inicio y finalización del alquiler.
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Column(nullable = false)
    private LocalDateTime dataInici;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Column(nullable = false)
    private LocalDateTime dataFi;

    // Importes económicos relacionados con la reserva.
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal preuLloguer;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal importFianca;

    @Column(precision = 10, scale = 2)
    private BigDecimal fiancaRetinguda;

    @Column(precision = 10, scale = 2)
    private BigDecimal costIncidencia;

    @Column(precision = 10, scale = 2)
    private BigDecimal fiancaRetornada;

    @Column(precision = 10, scale = 2)
    private BigDecimal costFinal;

    // Estado actual de la reserva.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstatReserva estat;

    // Observaciones adicionales de la reserva.
    @Column(length = 1000)
    private String observacions;

    // --- NUEVOS CAMPOS PARA EL LLIURAMENT DEL VEHICLE ---
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Column(name = "data_hora_lliurament")
    private LocalDateTime dataHoraLliurament;

    @Column(name = "descripcio_lliurament", length = 1000)
    private String descripcioLliurament;

    @ElementCollection
    @CollectionTable(name = "reserva_fotos_lliurament", joinColumns = @JoinColumn(name = "reserva_id"))
    @Column(name = "ruta_foto")
    private List<String> fotosLliurament = new ArrayList<>();

    // Vehículo y usuario asociados a la reserva.
    @ManyToOne
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @ManyToOne
    @JoinColumn(name = "usuari_id", nullable = false)
    private Usuaris usuari;

    // Constructor vacío necesario para JPA.
    public Reserva() {
    }

    // Getters y Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCodiReserva() {
        return codiReserva;
    }

    public void setCodiReserva(String codiReserva) {
        this.codiReserva = codiReserva;
    }

    public LocalDateTime getDataReserva() {
        return dataReserva;
    }

    public void setDataReserva(LocalDateTime dataReserva) {
        this.dataReserva = dataReserva;
    }

    public LocalDateTime getDataInici() {
        return dataInici;
    }

    public void setDataInici(LocalDateTime dataInici) {
        this.dataInici = dataInici;
    }

    public LocalDateTime getDataFi() {
        return dataFi;
    }

    public void setDataFi(LocalDateTime dataFi) {
        this.dataFi = dataFi;
    }

    public BigDecimal getPreuLloguer() {
        return preuLloguer;
    }

    public void setPreuLloguer(BigDecimal preuLloguer) {
        this.preuLloguer = preuLloguer;
    }

    public BigDecimal getImportFianca() {
        return importFianca;
    }

    public void setImportFianca(BigDecimal importFianca) {
        this.importFianca = importFianca;
    }

    public BigDecimal getFiancaRetinguda() {
        return fiancaRetinguda;
    }

    public void setFiancaRetinguda(BigDecimal fiancaRetinguda) {
        this.fiancaRetinguda = fiancaRetinguda;
    }

    public BigDecimal getCostIncidencia() {
        return costIncidencia;
    }

    public void setCostIncidencia(BigDecimal costIncidencia) {
        this.costIncidencia = costIncidencia;
    }

    public BigDecimal getFiancaRetornada() {
        return fiancaRetornada;
    }

    public void setFiancaRetornada(BigDecimal fiancaRetornada) {
        this.fiancaRetornada = fiancaRetornada;
    }

    public BigDecimal getCostFinal() {
        return costFinal;
    }

    public void setCostFinal(BigDecimal costFinal) {
        this.costFinal = costFinal;
    }

    public EstatReserva getEstat() {
        return estat;
    }

    public void setEstat(EstatReserva estat) {
        this.estat = estat;
    }

    public String getObservacions() {
        return observacions;
    }

    public void setObservacions(String observacions) {
        this.observacions = observacions;
    }

    public LocalDateTime getDataHoraLliurament() {
        return dataHoraLliurament;
    }

    public void setDataHoraLliurament(LocalDateTime dataHoraLliurament) {
        this.dataHoraLliurament = dataHoraLliurament;
    }

    public String getDescripcioLliurament() {
        return descripcioLliurament;
    }

    public void setDescripcioLliurament(String descripcioLliurament) {
        this.descripcioLliurament = descripcioLliurament;
    }

    public List<String> getFotosLliurament() {
        return fotosLliurament;
    }

    public void setFotosLliurament(List<String> fotosLliurament) {
        this.fotosLliurament = fotosLliurament;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
    }

    public Usuaris getUsuari() {
        return usuari;
    }

    public void setUsuari(Usuaris usuari) {
        this.usuari = usuari;
    }

    // --- CAMPS PER AL RETORN I LIQUIDACIÓ DE FIANÇA ---
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Column(name = "data_hora_retorn")
    private LocalDateTime dataHoraRetorn;

    @Column(name = "desperfectes_imputables")
    private Boolean desperfectesImputables = false;

    @Column(name = "carrec_extra_pendent", precision = 10, scale = 2)
    private BigDecimal carrecExtraPendent = BigDecimal.ZERO;

    public LocalDateTime getDataHoraRetorn() {
        return dataHoraRetorn;
    }

    public void setDataHoraRetorn(LocalDateTime dataHoraRetorn) {
        this.dataHoraRetorn = dataHoraRetorn;
    }

    public Boolean getDesperfectesImputables() {
        return desperfectesImputables;
    }

    public void setDesperfectesImputables(Boolean desperfectesImputables) {
        this.desperfectesImputables = desperfectesImputables;
    }

    public BigDecimal getCarrecExtraPendent() {
        return carrecExtraPendent;
    }

    public void setCarrecExtraPendent(BigDecimal carrecExtraPendent) {
        this.carrecExtraPendent = carrecExtraPendent;
    }
}
