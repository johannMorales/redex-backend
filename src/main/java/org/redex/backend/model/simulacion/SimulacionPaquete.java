package org.redex.backend.model.simulacion;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "simulacion_paquete")
public class SimulacionPaquete implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "id_simulacion", nullable = false)
    Simulacion simulacion;
    
    SimulacionOficina oficinaOrigen;

    SimulacionOficina oficinaDestino;

}