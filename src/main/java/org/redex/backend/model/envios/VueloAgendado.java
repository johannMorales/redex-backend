package org.redex.backend.model.envios;

import org.redex.backend.model.rrhh.Oficina;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "vuelo_agendado")
public class VueloAgendado implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "id_vuelo", nullable = false)
    private Vuelo vuelo;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private VueloAgendadoEstadoEnum estado;

    @Column(nullable = false)
    private ZonedDateTime fechaInicio;

    @Column(nullable = false)
    private ZonedDateTime fechaFin;

    @Column(nullable = false)
    private Integer capacidadActual;

    @Column(nullable = false)
    private Integer capacidadMaxima;

    public VueloAgendado() {
    }

    public VueloAgendado(Long id) {
        this.id = id;
    }

    public VueloAgendado(Vuelo vuelo, ZonedDateTime fechaInicio) {
        ZonedDateTime fechaInicioLimpia = fechaInicio.truncatedTo(ChronoUnit.DAYS);
        ZonedDateTime fechaFinLimpia = vuelo.esDeUnDia() ? ZonedDateTime.from(fechaInicioLimpia) : fechaInicioLimpia.plus(1L, ChronoUnit.DAYS);

        fechaInicioLimpia = fechaInicioLimpia.with(vuelo.getHoraInicio());
        fechaFinLimpia = fechaFinLimpia.with(vuelo.getHoraFin());

        this.fechaInicio = fechaInicioLimpia;
        this.fechaFin = fechaFinLimpia;

        this.capacidadActual = 0;
        this.capacidadMaxima = 100;

        this.vuelo = vuelo;
    }

    public VueloAgendado(ZonedDateTime fechaLlegada, ZonedDateTime fechaSalida, Integer capacidadMaxima, Integer capacidadActual, Vuelo vuelo) {
        this.fechaInicio = fechaLlegada;
        this.fechaFin = fechaSalida;
        this.capacidadMaxima = capacidadMaxima;
        this.capacidadActual = capacidadActual;
        this.vuelo = vuelo;
    }

    public VueloAgendado(ZonedDateTime fechaLlegada, ZonedDateTime fechaSalida, Oficina oficinaDestino, Oficina oficinaOrigen) {
        this.fechaInicio = fechaLlegada;
        this.fechaFin = fechaSalida;
        this.capacidadMaxima = 0;
        this.capacidadActual = 500;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ZonedDateTime getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(ZonedDateTime fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public ZonedDateTime getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(ZonedDateTime fechaFin) {
        this.fechaFin = fechaFin;
    }

    public Integer getCapacidadMaxima() {
        return capacidadMaxima;
    }

    public void setCapacidadMaxima(Integer capacidadMaxima) {
        this.capacidadMaxima = capacidadMaxima;
    }

    public Integer getCapacidadActual() {
        return capacidadActual;
    }

    public void setCapacidadActual(Integer capacidadActual) {
        this.capacidadActual = capacidadActual;
    }

    public double getPorcentajeUsado() {
        return getCapacidadActual() / getCapacidadMaxima() * 100;
    }

    public Oficina getOficinaDestino() {
        return getVuelo().getOficinaDestino();
    }

    public Oficina getOficinaOrigen() {
        return getVuelo().getOficinaOrigen();
    }

    public String getCodigo() {
        return String.format("%s-%s-%s-%s",
                getOficinaOrigen().getCodigo(),
                getOficinaDestino().getCodigo(),
                DateTimeFormatter.ofPattern("dd/MM HH:mm").format(getFechaInicio()),
                DateTimeFormatter.ofPattern("dd/MM HH:mm").format(getFechaFin())
        );
    }

    @Override
    public String toString() {
        return String.format("%s %s -> %s %s",
                getOficinaOrigen().getCodigo(),
                DateTimeFormatter.ofPattern("dd/MM HH:mm").format(getFechaInicio()),
                getOficinaDestino().getCodigo(),
                DateTimeFormatter.ofPattern("dd/MM HH:mm").format(getFechaFin()));
    }

    public void agregarPaquete() {
        this.setCapacidadActual((Integer) (this.getCapacidadActual() + 1));
    }

    public boolean tieneEspacioDisponible() {
        return getCapacidadActual() < getCapacidadMaxima();
    }

    public Vuelo getVuelo() {
        return vuelo;
    }

    public void setVuelo(Vuelo vuelo) {
        this.vuelo = vuelo;
    }

    public VueloAgendadoEstadoEnum getEstado() {
        return estado;
    }

    public void setEstado(VueloAgendadoEstadoEnum estado) {
        this.estado = estado;
    }

}