package org.redex.backend.controller.paquetes;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.redex.backend.model.envios.Paquete;
import org.redex.backend.security.CurrentUser;
import org.redex.backend.security.DataSession;
import org.redex.backend.zelper.crimsontable.CrimsonTableRequest;
import org.redex.backend.zelper.crimsontable.CrimsonTableResponse;
import org.redex.backend.zelper.response.CargaDatosResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pe.albatross.zelpers.miscelanea.JsonHelper;

import javax.xml.crypto.Data;
import org.redex.backend.model.envios.PaqueteRuta;
import org.redex.backend.model.general.Pais;
import org.redex.backend.repository.OficinasRepository;
import org.redex.backend.repository.PaisesRepository;

@RestController
@RequestMapping("paquetes")
public class PaquetesController {

    @Autowired
    PaquetesService service;
    
    @Autowired
    PaisesRepository paisesRepository;

    @Autowired
    OficinasRepository oficinasRepository;
    
    @GetMapping
    public ResponseEntity<?> list(CrimsonTableRequest request, @CurrentUser DataSession ds) {
        Page<Paquete> paquetes = service.crimsonList(request, ds);
        Map<String, Pais> paises = paisesRepository.findAll()
                .stream()
                .collect(Collectors.toMap(pais -> pais.getCodigo(), pais -> pais));
        Pais ps = paises.get(ds.getOficina().getCodigo());
        for(Paquete p: paquetes){
            p.setFechaIngreso(p.getFechaIngreso().minusHours(ps.getHusoHorario()));
        }
        return ResponseEntity.ok(CrimsonTableResponse.of(paquetes, new String[]{
                "id",
                "personaOrigen.id",
                "personaOrigen.nombreCompleto",
                "personaOrigen.numeroDocumentoIdentidad",
                "personaOrigen.tipoDocumentoIdentidad.id",
                "personaOrigen.tipoDocumentoIdentidad.simbolo",
                "personaDestino.id",
                "personaDestino.nombreCompleto",
                "personaDestino.numeroDocumentoIdentidad",
                "personaDestino.tipoDocumentoIdentidad.id",
                "personaDestino.tipoDocumentoIdentidad.simbolo",
                "oficinaOrigen.id",
                "oficinaOrigen.codigo",
                "oficinaOrigen.pais.id",
                "oficinaOrigen.pais.nombre",
                "oficinaDestino.id",
                "oficinaDestino.codigo",
                "oficinaDestino.pais.id",
                "oficinaDestino.pais.nombre",
                "codigoRastreo",
                "estado",
                "fechaIngresoString"
        }));
    }

    @GetMapping("/{id}")
    public ObjectNode find(@PathVariable Long id, @CurrentUser DataSession ds) {
        Paquete p = service.find(id);
        
        Map<String, Pais> paises = paisesRepository.findAll()
                .stream()
                .collect(Collectors.toMap(pais -> pais.getCodigo(), pais -> pais));
        Pais ps = paises.get(ds.getOficina().getCodigo());
        
        p.setFechaIngreso(p.getFechaIngreso().minusHours(ps.getHusoHorario()));
        List<PaqueteRuta> pr = p.getPaqueteRutas();
        for (PaqueteRuta r : pr){
            r.getVueloAgendado().setFechaInicio(r.getVueloAgendado().getFechaInicio().minusHours(ps.getHusoHorario()));
            r.getVueloAgendado().setFechaFin(r.getVueloAgendado().getFechaFin().minusHours(ps.getHusoHorario()));
        }
        p.setPaqueteRutas(pr);
        return JsonHelper.createJson(p, JsonNodeFactory.instance, new String[]{
            "id",
            "personaOrigen.id",
            "personaOrigen.nombreCompleto",
            "personaOrigen.numeroDocumentoIdentidad",
            "personaOrigen.tipoDocumentoIdentidad.id",
            "personaOrigen.tipoDocumentoIdentidad.simbolo",
            "personaDestino.id",
            "personaDestino.nombreCompleto",
            "personaDestino.numeroDocumentoIdentidad",
            "personaDestino.tipoDocumentoIdentidad.id",
            "personaDestino.tipoDocumentoIdentidad.simbolo",
            "oficinaOrigen.id",
            "oficinaOrigen.codigo",
            "oficinaOrigen.pais.id",
            "oficinaOrigen.pais.nombre",
            "oficinaDestino.id",
            "oficinaDestino.codigo",
            "oficinaDestino.pais.id",
            "oficinaDestino.pais.nombre",
            "codigoRastreo",
            "estado",
            "fechaIngresoString",
            "paqueteRutas.id",
            "paqueteRutas.estado",
            "paqueteRutas.orden",
            "paqueteRutas.vueloAgendado.id",
            "paqueteRutas.vueloAgendado.oficinaOrigen.id",
            "paqueteRutas.vueloAgendado.oficinaOrigen.codigo",
            "paqueteRutas.vueloAgendado.oficinaOrigen.pais.id",
            "paqueteRutas.vueloAgendado.oficinaOrigen.pais.nombre",
            "paqueteRutas.vueloAgendado.oficinaDestino.id",
            "paqueteRutas.vueloAgendado.oficinaDestino.codigo",
            "paqueteRutas.vueloAgendado.oficinaDestino.pais.id",
            "paqueteRutas.vueloAgendado.oficinaDestino.pais.nombre",
            "paqueteRutas.vueloAgendado.fechaInicioString",
            "paqueteRutas.vueloAgendado.fechaFinString"
        });
    }
    
    @PostMapping("/carga")
    public CargaDatosResponse carga(@RequestParam("file") MultipartFile file) {
        return service.carga(file);
    }
    
    @PostMapping("/save")
    public ResponseEntity<?> save(@RequestBody Paquete paquete, @CurrentUser DataSession ds){
        service.save(paquete, ds);
        return ResponseEntity.ok("Paquete guardado");
    }

    @GetMapping("/tracking")
    public ResponseEntity<?> rastrear(@RequestParam String trackNumber){
        ObjectNode s = service.estadoPaquete(trackNumber);
        return ResponseEntity.ok(s);
    }

}
