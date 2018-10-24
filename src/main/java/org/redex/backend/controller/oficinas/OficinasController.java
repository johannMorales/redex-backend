package org.redex.backend.controller.oficinas;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.util.List;
import org.redex.backend.zelper.response.CargaDatosResponse;
import org.redex.backend.model.general.Archivo;
import org.redex.backend.model.rrhh.Oficina;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.albatross.zelpers.miscelanea.JsonHelper;

@RestController
@RequestMapping("/oficinas")
public class OficinasController {

    @Autowired
    OficinasService service;

    @GetMapping
    public ArrayNode list() {
        ArrayNode arr = new ArrayNode(JsonNodeFactory.instance);
        List<Oficina> oficinas = service.all();
        for (Oficina oficina : oficinas) {
            arr.add(JsonHelper.createJson(oficina, JsonNodeFactory.instance, new String[]{
                "id",
                "pais.id",
                "pais.codigo",
                "pais.nombre",
                "capacidadActual",
                "capacidadMaxima",
                "estado",
                "codigo"
            }));
        }
        return arr;
    }

    @PostMapping("/carga")
    public CargaDatosResponse carga(@RequestBody Archivo archivo) {
        return service.carga(archivo);
    }

    @PostMapping("{id}/desactivar")
    public void desactivar(@PathVariable Long id) {
         service.desactivar(id);
    }

    @PostMapping("{id}/activar")
    public void acrivar(@PathVariable Long id) {
         service.activar(id);
    }

}
