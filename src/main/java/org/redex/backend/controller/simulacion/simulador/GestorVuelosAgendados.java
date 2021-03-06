package org.redex.backend.controller.simulacion.simulador;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tomcat.jni.Local;
import org.redex.backend.algorithm.gestor.AlgoritmoMovimiento;
import org.redex.backend.controller.simulacion.Ventana;
import org.redex.backend.model.envios.Vuelo;
import org.redex.backend.model.envios.VueloAgendado;
import org.redex.backend.model.rrhh.Oficina;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class GestorVuelosAgendados {

    @Autowired
    Simulador simulador;

    private static final Logger logger = LogManager.getLogger(GestorVuelosAgendados.class);

    private List<Vuelo> vuelos;
    private SortedSimpleList<LocalDateTime, VueloAgendado> vuelosAgendadosPorInicio;
    private SortedSimpleList<LocalDateTime, VueloAgendado> vuelosAgendadosPorFin;
    private SortedSimpleList<LocalDateTime, AlgoritmoMovimiento> movimientos;

    private HashSet<LocalDate> diasGenerados;

    private HashMap<Oficina, SortedSumList<LocalDateTime, Integer>> movsPositivos;
    private HashMap<Oficina, SortedSumList<LocalDateTime, Integer>> movsNegativos;

    public HashMap<Oficina, SortedSumList<LocalDateTime, Integer>> getMovsPositivos(){
        return movsPositivos;
    }

    public HashMap<Oficina, SortedSumList<LocalDateTime, Integer>> getMovsNegativos(){
        return movsNegativos;
    }


    public GestorVuelosAgendados() {
        this.inicializar();
    }

    public void inicializar() {
        this.vuelosAgendadosPorInicio =  SortedSimpleList.create();
        this.vuelosAgendadosPorFin = SortedSimpleList.create();
        movimientos = SortedSimpleList.create();
        this.vuelos = new ArrayList<>();
        movsPositivos = new HashMap<>();
        movsNegativos = new HashMap<>();
        diasGenerados = new HashSet<>();
    }

    public void reiniciar() {
        this.vuelosAgendadosPorInicio =  SortedSimpleList.create();
        this.vuelosAgendadosPorFin = SortedSimpleList.create();
        movimientos = SortedSimpleList.create();
        movsPositivos = new HashMap<>();
        movsNegativos = new HashMap<>();
        diasGenerados = new HashSet<>();
    }

    public void reiniciarMemVariaciones(){
        for (Map.Entry<Oficina, SortedSumList<LocalDateTime, Integer>> entry : this.movsPositivos.entrySet()) {
            entry.getValue().reset();
        }
        for (Map.Entry<Oficina, SortedSumList<LocalDateTime, Integer>> entry : this.movsNegativos.entrySet()) {
            entry.getValue().reset();
        }
    }

    public void crearVuelosAgendadosNecesarios(Ventana ventana) {
        LocalDate inicio = ventana.getInicio().toLocalDate();
        LocalDate fin = ventana.getFin().toLocalDate().plusDays(4L);

        while (true) {
            this.crearUnDiaVuelosAgendados(inicio);
            inicio = inicio.plusDays(1L);
            if (inicio.equals(fin)) {
                break;
            }
        }
    }

    private void crearUnDiaVuelosAgendados(LocalDate dia) {
        if(diasGenerados.contains(dia)) return;

        for (Vuelo vuelo : vuelos) {
            LocalDateTime inicio = LocalDateTime.of(dia, vuelo.getHoraInicio());
            LocalDateTime fin = LocalDateTime.of(dia, vuelo.getHoraFin());

            if (vuelo.getHoraFin().isBefore(vuelo.getHoraInicio()) || vuelo.getHoraFin().equals(vuelo.getHoraInicio())) {
                fin = LocalDateTime.of(dia.plusDays(1L), vuelo.getHoraFin());
            }

            VueloAgendado sva = new VueloAgendado();
            sva.setFechaInicio(inicio);
            sva.setFechaFin(fin);
            sva.setCapacidadActual(0);
            sva.setCantidadSalida(0);
            sva.setCapacidadMaxima(vuelo.getCapacidad());
            sva.setVuelo(vuelo);

            this.agregarVueloAgendado(sva);

            AlgoritmoMovimiento movSalida = AlgoritmoMovimiento.crearSalidaPaquetes(sva);
            AlgoritmoMovimiento movPartida = AlgoritmoMovimiento.crearSalidaVuelo(sva);
            AlgoritmoMovimiento movLlegada = AlgoritmoMovimiento.crearEntradaVuelo(sva);

            this.agregarMovimientoAlgoritmo(movSalida);
            this.agregarMovimientoAlgoritmo(movPartida);
            this.agregarMovimientoAlgoritmo(movLlegada);
        }

        this.diasGenerados.add(dia);
    }

    private void agregarVueloAgendado(VueloAgendado sva) {
        this.vuelosAgendadosPorInicio.add(sva.getFechaInicio(), sva);
        this.vuelosAgendadosPorFin.add(sva.getFechaFin(), sva);
    }

    public void agregarMovimientoAlgoritmo(AlgoritmoMovimiento algoritmoMovimiento) {
       movimientos.add(algoritmoMovimiento.getMomento(), algoritmoMovimiento);
    }


   public List<AlgoritmoMovimiento> allMovimientoAlgoritmo(Ventana ventana){
        return movimientos.inWindow(ventana).stream().filter(m -> m.getVariacion() != 0).collect(Collectors.toList());
   }


    public List<VueloAgendado> allLleganEnVentana(Ventana ventana) {
        return vuelosAgendadosPorFin.inWindow(ventana);

    }

    public List<VueloAgendado> allPartenEnVentana(Ventana ventana) {
        return vuelosAgendadosPorInicio.inWindow(ventana);
    }

    public List<VueloAgendado> allAlgoritmo(Ventana ventana, Oficina oficina) {
        return vuelosAgendadosPorInicio.inWindow(ventana)
                .stream()
                .filter(va -> va.getOficinaOrigen() != oficina)
                .filter(va -> va.getCapacidadActual() < va.getCapacidadMaxima())
                .filter(va -> va.getFechaFin().isBefore(ventana.getFin()) || va.getFechaFin().equals(ventana.getFin()))
                .collect(Collectors.toList());
    }


    public void setVuelos(List<Vuelo> nuevosVuelos) {
        this.vuelos = nuevosVuelos;
    }

    public List<Vuelo> getVuelos() {
        return vuelos;
    }

    public void limpiarHasta(LocalDateTime fechaLimite) {
        this.vuelosAgendadosPorFin.deleteBeforeOrEqual(fechaLimite);
        this.vuelosAgendadosPorInicio.deleteBeforeOrEqual(fechaLimite);
        this.movimientos.deleteBeforeOrEqual(fechaLimite);
        for (Map.Entry<Oficina, SortedSumList<LocalDateTime, Integer>> entry : this.movsNegativos.entrySet()) {
            entry.getValue().removeBeforeOrEqual(fechaLimite);
        }
        for (Map.Entry<Oficina, SortedSumList<LocalDateTime, Integer>> entry : this.movsPositivos.entrySet()) {
            entry.getValue().removeBeforeOrEqual(fechaLimite);
        }
    }
}
