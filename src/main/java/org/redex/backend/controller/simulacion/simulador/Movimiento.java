package org.redex.backend.controller.simulacion.simulador;

import org.redex.backend.model.envios.VueloAgendado;
import org.redex.backend.model.rrhh.Oficina;

import java.time.LocalDateTime;

public class Movimiento implements Comparable<Movimiento> {

    public void log() {
        if (tipo == Tipo.ENTRADA)
            System.out.println(String.format("%s +%d paquetes", cantidad));
        else
            System.out.println(String.format("%s -%d paquetes", cantidad));

    }

    public enum Tipo {
        ENTRADA, SALIDA
    }

    Integer cantidad;

    Oficina oficina;

    LocalDateTime momento;

    VueloAgendado vueloAgendado;

    public Tipo tipo;

    public void process() {
        if (this.tipo == Tipo.SALIDA) {
            this.oficina.quitarPaquetes(cantidad);
        } else {
            this.oficina.agregarPaquetes(cantidad);
        }
        this.oficina.checkIntegrity(momento, this);
    }

    public static Movimiento fromFinVuelo(VueloAgendado va) {
        Movimiento mov = new Movimiento();
        mov.cantidad = va.getCapacidadActual();
        mov.momento = va.getFechaFin();
        mov.oficina = va.getOficinaDestino();
        mov.tipo = Tipo.ENTRADA;
        mov.vueloAgendado = va;
        return mov;
    }

    public static Movimiento fromSalidaPaquetes(VueloAgendado va) {
        Movimiento mov = new Movimiento();
        mov.cantidad = va.getCantidadSalida();
        mov.momento = va.getFechaFin();
        mov.oficina = va.getOficinaDestino();
        mov.tipo = Tipo.SALIDA;
        mov.vueloAgendado = va;
        return mov;
    }


    public static Movimiento fromInicioVuelo(VueloAgendado va) {
        Movimiento mov = new Movimiento();
        mov.cantidad = va.getCapacidadActual();
        mov.momento = va.getFechaInicio();
        mov.oficina = va.getOficinaOrigen();
        mov.tipo = Tipo.SALIDA;
        mov.vueloAgendado = va;
        return mov;
    }

    public Integer tipoValue() {
        switch (this.tipo) {
            case SALIDA:
                return 1;
            case ENTRADA:
                return 0;
        }

        return 0;
    }

    @Override
    public int compareTo(Movimiento o) {
        int momentComparison = momento.compareTo(o.momento);
        if (momentComparison == 0) {
            return this.tipoValue().compareTo(o.tipoValue());
        }
        return momentComparison;
    }

    @Override
    public String toString() {
        return String.format("%s %d [%s]", oficina.getCodigo(), cantidad, tipo.name());
    }
}
