package org.redex.backend.controller.reportes;

import org.redex.backend.security.CurrentUser;
import org.redex.backend.security.DataSession;
import org.redex.backend.zelper.exception.MyFileNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.MalformedURLException;
import java.time.LocalDate;

@RestController
@RequestMapping("reportes")
public class ReportesController {

    @Autowired
    ReportesService service;

    @GetMapping("paquetesXvuelo")
    public ResponseEntity<Resource> paquetesXvuelo(
            @RequestParam Long idVueloAgendado,
            @CurrentUser DataSession ds
    ) {
        String archivo = service.paquetesXvuelo(idVueloAgendado, ds);
        return download(archivo);

    }

    @GetMapping("paquetesXusuario")
    public ResponseEntity<Resource> paquetesXusuario(
            @RequestParam Long idUsuario,
            @CurrentUser DataSession ds
    ) {
        String archivo = service.paquetesXusuario(idUsuario, ds);
        return download(archivo);

    }

    @GetMapping("enviosXfechas")
    public ResponseEntity<Resource> enviosXfechas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin,
            @CurrentUser DataSession ds
    ) {
        String archivo = service.enviosXfechas(inicio, fin, ds);
        return download(archivo);
    }

    @GetMapping("enviosXoficina")
    public ResponseEntity<Resource> enviosXoficina(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin,
            @CurrentUser DataSession ds
    ) {
        String archivo = service.enviosXoficina(inicio, fin, ds);
        return download(archivo);
    }

    @GetMapping("enviosFinalizados")
    public ResponseEntity<Resource> enviosFinalizados(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin,
            @CurrentUser DataSession ds
    ) {
        String archivo = service.enviosFinalizados(inicio, fin, ds);
        return download(archivo);
    }

    @GetMapping("auditoria")
    public ResponseEntity<Resource> auditoria(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin,
            @RequestParam Long idOficina,
            @CurrentUser DataSession ds

    ) {
        String archivo = service.auditoria(inicio, fin, idOficina, ds);
        return download(archivo);
    }


    private ResponseEntity<Resource> download(String archivo) {
        try {
            Resource resource = new UrlResource("file", archivo);
            try {
                System.out.println(resource.getFile().getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (!resource.exists()) {
                throw new MyFileNotFoundException("Archivo no encontrado " + archivo);
            }

            String contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

            String actualFileName = archivo.substring(archivo.lastIndexOf('/') + 1);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, actualFileName)
                    .body(resource);

        } catch (MalformedURLException ex) {
            throw new MyFileNotFoundException("Archivo no encontrado ", ex);
        }
    }


    // reporte de envios para cada oficina mensual johana manda las fechas

    // reporte de envios finalizados en rango de fecha

}
