package org.redex.backend.controller.simulacion.simulador;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.redex.backend.algorithm.evolutivo.Evolutivo;
import org.redex.backend.algorithm.gestor.AlgoritmoMovimiento;
import org.redex.backend.controller.simulacion.Ventana;
import org.redex.backend.controller.simulacionaccion.SimulacionAccionWrapper;
import org.redex.backend.model.AppConstants;
import org.redex.backend.model.envios.Paquete;
import org.redex.backend.model.envios.Vuelo;
import org.redex.backend.model.envios.VueloAgendado;
import org.redex.backend.model.rrhh.Oficina;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.*;

@Component
public class Simulador {
    private static Logger logger = LogManager.getLogger(Simulador.class);

    @Autowired
    GestorVuelosAgendados gestorVuelosAgendados;

    @Autowired
    GestorPaquetes gestorPaquetes;

    private Map<String, Oficina> oficinas;

    private List<Oficina> oficinasList;

    private LocalDateTime fechaActual;

    private boolean termino;

    public String getFielName() {
        return fielName;
    }

    public void setFielName(String fielName) {
        this.fielName = fielName;
    }

    private String fielName;

    Simulador() {
        this.oficinas = new HashMap<>();
        this.oficinasList = new ArrayList<>();
        this.termino = false;

        this.fielName = AppConstants.TMP_DIR + "/simulation-" + System.currentTimeMillis() + ".log";
        try {
            FileWriter fw = new FileWriter(this.fielName, false);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter out = new PrintWriter(bw);
            out.println();
            out.close();
        } catch (IOException e) {
            //exception handling left as an exercise for the reader
        }
    }

    public List<SimulacionAccionWrapper> procesarVentana(Ventana ventana) {
        this.gestorVuelosAgendados.crearVuelosAgendadosNecesarios(ventana);
        List<SimulacionAccionWrapper> acciones = this.acciones(ventana);
        return acciones;
    }

    private List<SimulacionAccionWrapper> acciones(Ventana ventana) {
        List<SimulacionAccionWrapper> acciones = new ArrayList<>();

        try {
            FileWriter fw = new FileWriter(this.fielName, true);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter out = new PrintWriter(bw);


            List<VueloAgendado> vuelosAgendados = gestorVuelosAgendados.allPartenEnVentana(ventana);


            List<Paquete> paquetes = gestorPaquetes.allEntranVentana(ventana);
            this.gestorPaquetes.limpiarHasta(ventana.getFin());

            Integer paquetesProcesados = 0;
            Integer paquetesTotales = 0;

            for (Paquete paquete : paquetes) {
                try {
                    paquetesTotales++;
                    procesarPaquete(paquete, out);
                    paquete.getOficinaOrigen().agregarPaquete();
                    paquete.getOficinaOrigen().checkIntegrity(paquete.getFechaIngreso(), paquete);
                    acciones.add(SimulacionAccionWrapper.of(paquete));
                    paquetesProcesados++;
                } catch (AvoidableException ex) {
                    continue;
                } catch (DeadSimulationException dex) {
                    acciones.add(SimulacionAccionWrapper.of(paquete));
                    paquetesProcesados++;
                    this.termino = true;
                    break;
                }
            }

            if (!this.termino) {
                this.simular(ventana.getFin());
            }

            logger.info("Paquetes procesados: {}", paquetesProcesados);
            logger.info("Paquetes totales: {}", paquetesTotales);

            for (VueloAgendado vuelosAgendado : vuelosAgendados) {
                if (this.termino) {
                    if (vuelosAgendado.getFechaInicio().isAfter(fechaActual)) {
                        continue;
                    }
                }
                SimulacionAccionWrapper saw = SimulacionAccionWrapper.of(vuelosAgendado);
                acciones.add(saw);
            }

            Collections.sort(acciones);

            out.close();
        } catch (IOException e) {
            //exception handling left as an exercise for the reader
        }

        return acciones;
    }

    private void procesarPaquete(Paquete paquete, PrintWriter pw) throws AvoidableException, DeadSimulationException {
        simular(paquete.getFechaIngreso());
        LocalDateTime fechaInicio = paquete.getFechaIngreso();
        LocalDateTime fechaFin = paquete.getFechaMaximaEntrega();

        Ventana ventanaPaquete = Ventana.of(fechaInicio, fechaFin);

        List<VueloAgendado> vuelosCumplen = gestorVuelosAgendados.allAlgoritmo(ventanaPaquete, paquete.getOficinaDestino());

        pw.printf("%s\n", paquete);
        Evolutivo e = new Evolutivo();

        List<VueloAgendado> ruta = e.run(paquete, vuelosCumplen, gestorVuelosAgendados.getMovsPositivos(), gestorVuelosAgendados.getMovsNegativos(), oficinasList);

        int cont = 0;

        for (VueloAgendado item : ruta) {

            cont++;
            if (cont == ruta.size()) {
                item.setCantidadSalida(item.getCantidadSalida() + 1);
            }
            item.setCapacidadActual(item.getCapacidadActual() + 1);
            pw.printf("%s\n", item);
        }
        pw.printf("\n");
    }

    private void simular(LocalDateTime fechaLimite) {
        if (fechaActual != null && (fechaLimite.isBefore(this.fechaActual) || fechaLimite.equals(this.fechaActual))) {
            return;
        }

        List<AlgoritmoMovimiento> movimientos = gestorVuelosAgendados.allMovimientoAlgoritmo(Ventana.of(fechaActual, fechaLimite));

        Collections.sort(movimientos);

        for (AlgoritmoMovimiento movimiento : movimientos) {
            movimiento.process();
        }

        gestorVuelosAgendados.limpiarHasta(fechaLimite);
        gestorVuelosAgendados.reiniciarMemVariaciones();

        for (Oficina oficina : oficinasList) {
            if (!(oficina.getCapacidadMaxima() >= oficina.getCapacidadActual())) {
                logger.error("{} capacidad mayor que maxima", oficina);
            }

            if (!(oficina.getCapacidadActual() >= 0)) {
                logger.error("{} capacidad actual menor que 0", oficina);
            }
        }

        this.fechaActual = fechaLimite;
    }


    public List<Vuelo> getVuelos() {
        return this.gestorVuelosAgendados.getVuelos();
    }

    public void eliminar() {
        this.termino = false;
        this.gestorPaquetes.inicializar();
        this.gestorVuelosAgendados.inicializar();
        this.oficinas = new HashMap<>();
        this.oficinasList = new ArrayList<>();
    }

    public void resetear() {
        this.termino = false;
        this.gestorVuelosAgendados.reiniciar();
        this.gestorPaquetes.inicializar();
    }

    public void setVuelos(List<Vuelo> vuelos) {
        this.gestorVuelosAgendados.setVuelos(vuelos);
        logger.info("{} vuelos agregados", vuelos.size());
    }

    public void setOficinas(List<Oficina> oficinas) {
        this.oficinas = new HashMap<>();
        this.oficinasList = oficinas;

        for (Oficina ofi : this.oficinasList) {
            this.oficinas.put(ofi.getCodigo(), ofi);
        }

        for (Oficina oficina : this.oficinasList) {
            this.gestorVuelosAgendados.getMovsNegativos().put(oficina, SortedSumList.create());
            this.gestorVuelosAgendados.getMovsPositivos().put(oficina, SortedSumList.create());
        }

        logger.info("{} oficinas agregadas", this.oficinas.size());
    }

    public Map<String, Oficina> getOficinas() {
        return this.oficinas;
    }

    public List<Oficina> getOficinasList() {
        return this.oficinasList;
    }

    public Oficina findOficina(String almacenColapso) {
        return oficinas.get(almacenColapso);
    }

    public boolean isTermino() {
        return termino;
    }
}
