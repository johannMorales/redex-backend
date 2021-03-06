package org.redex.backend.controller.oficinas;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import static java.lang.Character.isDigit;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.redex.backend.repository.ArchivosRepository;
import org.redex.backend.repository.OficinasRepository;
import org.redex.backend.repository.PaisesRepository;
import org.redex.backend.zelper.exception.ResourceNotFoundException;
import org.redex.backend.zelper.response.CargaDatosResponse;
import org.redex.backend.model.general.EstadoEnum;
import org.redex.backend.model.general.Pais;
import org.redex.backend.model.rrhh.Colaborador;
import org.redex.backend.model.rrhh.Oficina;
import org.redex.backend.zelper.crimsontable.CrimsonTableRequest;
import org.redex.backend.zelper.exception.AppException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional(readOnly = true)
public class OficinasServiceImp implements OficinasService {

    @Autowired
    OficinasRepository oficinasRepository;

    @Autowired
    ArchivosRepository archivosRepository;

    @Autowired
    PaisesRepository paisesRepository;

    @Override
    public void cambiarJefe(Oficina oficina, Colaborador colaborador) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void agregarColaborador(Colaborador colaborador) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    @Transactional
    public CargaDatosResponse carga(MultipartFile file) {

        Integer cantidadRegistros = 0;
        Integer cantidadErrores = 0;
        List<String> errores = new ArrayList<>();

        Set<String> paisesConOficina = oficinasRepository.findAll()
                .stream()
                .map(oficina -> oficina.getPais().getCodigo())
                .collect(Collectors.toSet());

        Map<String, Pais> paises = paisesRepository.findAll()
                .stream()
                .collect(Collectors.toMap(pais -> pais.getCodigo(), pais -> pais));

        List<Oficina> nuevasOficinas = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), Charset.forName(StandardCharsets.UTF_8.name())))) {
            List<String> lineasList = reader.lines().collect(Collectors.toList());
            int contLinea = 1;
            for (String linea : lineasList) {
                if (!linea.isEmpty() && isDigit(linea.charAt(0))) {
                    String code = linea.substring(5, 9);
                    if (code.isEmpty()) {
                        cantidadErrores = cantidadErrores + 1;
                        errores.add("La linea " + contLinea + " no tiene pais");
                    } else {
                        Oficina oficinaNueva = leerOficina(code, paises);
                        if (!paisesConOficina.contains(oficinaNueva.getPais().getCodigo())) {
                            nuevasOficinas.add(oficinaNueva);
                        }
                    }
                }
                contLinea++;
            }
        } catch (IOException ex) {
            Logger.getLogger(OficinasServiceImp.class.getName()).log(Level.SEVERE, null, ex);
        }

        for (Oficina oficina : nuevasOficinas) {
            try {
                oficinasRepository.save(oficina);
                cantidadRegistros++;
            } catch (Exception ex) {
                cantidadErrores++;
                errores.add("Erorr de integridad de datos");
            }
        }

        return new CargaDatosResponse(cantidadErrores, cantidadRegistros, "Carga finalizada con exito", errores);
    }

    private Oficina leerOficina(String linea, Map<String, Pais> mapPaises) {
        Oficina oficina = new Oficina();
        oficina.setCodigo(linea);
        oficina.setPais(mapPaises.get(linea));
        oficina.setCapacidadActual(0);
        oficina.setCapacidadMaxima(100);
        oficina.setEstado(EstadoEnum.ACTIVO);

        return oficina;
    }

    @Override
    public List<Oficina> all() {
        return oficinasRepository.findAll();
    }

    @Override
    @Transactional
    public void desactivar(Long id) {
        Oficina o = oficinasRepository.
                findById(id).orElseThrow(() -> new ResourceNotFoundException("Oficina", "id", id));

        o.setEstado(EstadoEnum.INACTIVO);
        oficinasRepository.save(o);
    }

    @Override
    @Transactional
    public void activar(Long id) {
        Oficina o = oficinasRepository.
                findById(id).orElseThrow(() -> new ResourceNotFoundException("Oficina", "id", id));

        o.setEstado(EstadoEnum.ACTIVO);
        oficinasRepository.save(o);
    }

    @Override
    public Oficina find(Long id) {
        return oficinasRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Oficina", "id", id));
    }

    @Override
    @Transactional
    public void save(Oficina oficina) {
        Oficina o = oficinasRepository.findByPais(oficina.getPais());
        if (o != null) {
            throw new AppException("El pais seleccionado ya cuenta con una oficina");
        }

        o = oficinasRepository.findByCodigo(oficina.getCodigo());

        if (o != null) {
            throw new AppException("El codigo seleccionado ya se encuentra en uso");
        }

        oficina.setCapacidadActual(0);
        oficina.setEstado(EstadoEnum.ACTIVO);
        oficinasRepository.save(oficina);
    }

    @Override
    public void update(Oficina oficina) {
        Oficina oficinaBD = oficinasRepository.findById(oficina.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Oficina", "id", oficina.getId()));

        oficinaBD.setCodigo(oficina.getCodigo());
        oficinaBD.setPais(oficina.getPais());
        oficinaBD.setCapacidadMaxima(oficina.getCapacidadMaxima());
        oficinasRepository.save(oficinaBD);
    }

    @Override
    public Page<Oficina> allByCrimson(CrimsonTableRequest request) {
        return oficinasRepository.customPaginatedSearch(request.getSearch(), PageRequest.of(request.getCurrent(), request.getPageSize()));
    }

    @Override
    public List<Oficina> search(String q) {
        return oficinasRepository.customSearch(q);
    }

}
