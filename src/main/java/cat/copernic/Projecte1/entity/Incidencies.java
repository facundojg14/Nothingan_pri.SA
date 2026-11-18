/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cat.copernic.Projecte1.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Entitat JPA que representa una incidència registrada durant el lloguer o la devolució d'un vehicle.
 * Emmagatzema els detalls de l'incident, l'import associat, la resolució i la reserva vinculada.
 *
 * @author Grup 4
 * @version 1.0
 */
@Entity
public class Incidencies {

    /** Identificador únic de la incidència. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    /** Data en què es registra la incidència (format yyyy-MM-dd). */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate Data_Hora;

    /** Quilometratge del vehicle en el moment de registrar la incidència. */
    private int Quilometratge;

    /** Descripció detallada dels desperfectes o fets ocorreguts. */
    private String Descripcio;

    /** Estat o acció de resolució aplicada a la incidència. */
    private String Resolucio;

    /** Tipus o categoria de la incidència. */
    private Enum Tipus;

    /** Import econòmic derivat de la incidència (si escau). */
    private Double Importe;

    /** Ruta o nom del fitxer d'imatge que documenta la incidència. */
    private String Foto_incidencia;

    /** Reserva a la qual pertany aquesta incidència. */
    @ManyToOne
    @JoinColumn(name = "reserva_id")
    private Reserva reserva;

    /**
     * Constructor per defecte requerit per JPA.
     */
    public Incidencies() {
    }

    /**
     * Constructor complet per inicialitzar una incidència incloent-hi el seu identificador.
     *
     * @param id Identificador primari de la incidència.
     * @param Data_Hora Data del registre de la incidència.
     * @param Quilometratge Quilometratge del vehicle en el moment de l'incident.
     * @param Descripcio Descripció detallada de la incidència.
     * @param Resolucio Resolució o gestió aplicada.
     * @param Tipus Tipus d'incidència.
     * @param Importe Cost econòmic associat.
     * @param Foto_incidencia Ruta o URL de la fotografia de la incidència.
     * @param reserva Reserva associada a la incidència.
     */
    public Incidencies(long id, LocalDate Data_Hora, int Quilometratge, String Descripcio, String Resolucio, Enum Tipus, Double Importe, String Foto_incidencia, Reserva reserva) {
        this.id = id;
        this.Data_Hora = Data_Hora;
        this.Quilometratge = Quilometratge;
        this.Descripcio = Descripcio;
        this.Resolucio = Resolucio;
        this.Tipus = Tipus;
        this.Importe = Importe;
        this.Foto_incidencia = Foto_incidencia;
        this.reserva = reserva;
    }

    /**
     * Constructor per crear una nova incidència abans de persistir-la a la base de dades.
     *
     * @param Data_Hora Data del registre de la incidència.
     * @param Quilometratge Quilometratge del vehicle en el moment de l'incident.
     * @param Descripcio Descripció detallada de la incidència.
     * @param Resolucio Resolució o gestió aplicada.
     * @param Tipus Tipus d'incidència.
     * @param Importe Cost econòmic associat.
     * @param Foto_incidencia Ruta o URL de la fotografia de la incidència.
     * @param reserva Reserva associada a la incidència.
     */
    public Incidencies(LocalDate Data_Hora, int Quilometratge, String Descripcio, String Resolucio, Enum Tipus, Double Importe, String Foto_incidencia, Reserva reserva) {
        this.Data_Hora = Data_Hora;
        this.Quilometratge = Quilometratge;
        this.Descripcio = Descripcio;
        this.Resolucio = Resolucio;
        this.Tipus = Tipus;
        this.Importe = Importe;
        this.Foto_incidencia = Foto_incidencia;
        this.reserva = reserva;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public LocalDate getData_Hora() {
        return Data_Hora;
    }

    public void setData_Hora(LocalDate Data_Hora) {
        this.Data_Hora = Data_Hora;
    }

    public int getQuilometratge() {
        return Quilometratge;
    }

    public void setQuilometratge(int Quilometratge) {
        this.Quilometratge = Quilometratge;
    }

    public String getDescripcio() {
        return Descripcio;
    }

    public void setDescripcio(String Descripcio) {
        this.Descripcio = Descripcio;
    }

    public String getResolucio() {
        return Resolucio;
    }

    public void setResolucio(String Resolucio) {
        this.Resolucio = Resolucio;
    }

    public Enum getTipus() {
        return Tipus;
    }

    public void setTipus(Enum Tipus) {
        this.Tipus = Tipus;
    }

    public Double getImporte() {
        return Importe;
    }

    public void setImporte(Double Importe) {
        this.Importe = Importe;
    }

    public String getFoto_incidencia() {
        return Foto_incidencia;
    }

    public void setFoto_incidencia(String Foto_incidencia) {
        this.Foto_incidencia = Foto_incidencia;
    }

    public Reserva getReserva() {
        return reserva;
    }

    public void setReserva(Reserva reserva) {
        this.reserva = reserva;
    }
}