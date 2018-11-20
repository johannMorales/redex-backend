package org.redex.backend.controller.simulacion;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.redex.backend.controller.simulacion.simulador.GestorPaquetes;
import org.redex.backend.controller.simulacion.simulador.GestorVuelosAgendados;
import org.redex.backend.controller.simulacion.simulador.Simulador;
import org.redex.backend.model.envios.Paquete;
import org.redex.backend.model.envios.PlanVuelo;
import org.redex.backend.model.envios.Vuelo;
import org.redex.backend.model.general.EstadoEnum;
import org.redex.backend.model.general.Pais;
import org.redex.backend.model.rrhh.Oficina;
import org.redex.backend.repository.OficinasRepository;
import org.redex.backend.repository.PaisesRepository;
import org.redex.backend.repository.PlanVueloRepository;
import org.redex.backend.repository.VuelosRepository;
import org.redex.backend.zelper.response.CargaDatosResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.lang.Character.isDigit;

@Service
@Transactional(readOnly = true)
public class SimulacionServiceImp implements SimulacionService {

    private final Logger logger = LogManager.getLogger(SimulacionServiceImp.class);

    @Autowired
    Simulador simulador;

    @Autowired
    GestorVuelosAgendados gestorVuelosAgendados;

    @Autowired
    GestorPaquetes gestorPaquetes;

    @Autowired
    PaisesRepository paisesRepository;

    @Autowired
    OficinasRepository oficinasRepository;

    @Autowired
    PlanVueloRepository planVueloRepository;

    @Autowired
    VuelosRepository vuelosRepository;

    @Override
    public CargaDatosResponse cargaPaquetes(MultipartFile file) {
        Map<String, Oficina> oficinas = simulador.getOficinas();

        Integer cantidadRegistros = 0;
        Integer cantidadErrores = 0;
        List<String> errores = new ArrayList<>();
        List<Paquete> nuevosPaquetes = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), Charset.forName(StandardCharsets.UTF_8.name())))) {
            List<String> lineasList = reader.lines().collect(Collectors.toList());
            int contLinea = 1;
            for (String linea : lineasList) {
                if (!linea.isEmpty()) {
                    List<String> separateLine = Arrays.asList(linea.split("-"));
                    if (separateLine.size() == 4) {
                        Paquete nuevoP = leePaquete(separateLine, oficinas);
                        nuevosPaquetes.add(nuevoP);
                    } else {
                        cantidadErrores = cantidadErrores + 1;
                        errores.add("La linea " + contLinea + " no tiene todos los campos");
                    }
                }
            }
            gestorPaquetes.agregarLista(nuevosPaquetes);
        } catch (IOException ex) {

        }
        return new CargaDatosResponse(cantidadErrores, cantidadRegistros, "Carga finalizada con exito", errores);
    }

    @Override
    public CargaDatosResponse cargaVuelos(MultipartFile file) {
        Integer cantidadRegistros = 0;
        Integer cantidadErrores = 0;
        List<String> errores = new ArrayList<>();

        Map<String, Oficina> oficinas = simulador.getOficinas();

        List<Vuelo> nuevosVuelos = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), Charset.forName(StandardCharsets.UTF_8.name())))) {

            List<String> lineasList = reader.lines().collect(Collectors.toList());
            int contLinea = 1;
            for (String linea : lineasList) {
                if (!linea.isEmpty() && (linea.length() == 21)) {

                    String codeOffice1 = linea.substring(0, 4);
                    String codeOffice2 = linea.substring(5, 9);
                    String horaIni = linea.substring(10, 15);
                    String horaFin = linea.substring(16);
                    Pattern p = Pattern.compile(".*([01]?[0-9]|2[0-3]):[0-5][0-9].*");
                    Matcher m1 = p.matcher(horaIni);
                    Matcher m2 = p.matcher(horaFin);
                    if (codeOffice1.matches("[A-Z]+") && codeOffice2.matches("[A-Z]+")
                            && m1.matches() && m2.matches()) {
                        Vuelo nV = leerVuelo(codeOffice1, codeOffice2,
                                horaIni, horaFin, oficinas);
                        nuevosVuelos.add(nV);
                    } else {
                        cantidadErrores = cantidadErrores + 1;
                        errores.add("La linea " + contLinea + " tiene formato incorrecto");
                    }
                } else {
                    cantidadErrores = cantidadErrores + 1;
                    errores.add("La linea " + contLinea + " tiene formato incorrecto");
                }
                contLinea++;
            }

            simulador.setVuelos(nuevosVuelos);

        } catch (IOException ex) {

        }

        return new CargaDatosResponse(cantidadErrores, cantidadRegistros, "Carga finalizada con exito", errores);
    }

    @Override
    public CargaDatosResponse cargaOficinas(MultipartFile file) {
        Integer cantidadRegistros = 0;
        Integer cantidadErrores = 0;
        List<String> errores = new ArrayList<>();

        Map<String, Oficina> oficinas = simulador.getOficinas();

        oficinas.clear();

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
                    System.out.println(linea);
                    if (code.isEmpty()) {
                        cantidadErrores = cantidadErrores + 1;
                        errores.add("La linea " + contLinea + " no tiene pais");
                    } else {
                        Oficina oficinaNueva = leerOficina(code, paises);
                        nuevasOficinas.add(oficinaNueva);
                    }
                }
                contLinea++;
            }
            simulador.setOficinas(nuevasOficinas);
        } catch (IOException ex) {
            
        }

        return new CargaDatosResponse(cantidadErrores, cantidadRegistros, "Carga finalizada con exito", errores);
    }

    @Override
    public void crear() {
        List<Oficina> oficinas = oficinasRepository.findAllByEstado(EstadoEnum.ACTIVO);
        simulador.setOficinas(oficinas);

        PlanVuelo pv = planVueloRepository.findByEstado(EstadoEnum.ACTIVO);
        simulador.setVuelos(pv.getVuelos());
    }

    private Paquete leePaquete(List<String> datos, Map<String, Oficina> oficinas) {
        Paquete p = new Paquete();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        LocalDateTime date = LocalDateTime.parse(datos.get(1).substring(0, 4) + "-"
                + datos.get(1).substring(4, 6) + "-" + datos.get(1).substring(6, 8)
                + " " + datos.get(2).substring(0, 2) + datos.get(2).substring(2), formatter);

        Pais pI = oficinas.get(datos.get(0).substring(0, 4)).getPais();
        date = date.plus(pI.getHusoHorario() * -1, ChronoUnit.HOURS);
        p.setFechaIngreso(date);
        p.setOficinaDestino(oficinas.get(datos.get(3)));
        p.setOficinaOrigen(oficinas.get(datos.get(0).substring(0, 4)));
        p.setRutaGenerada(Boolean.FALSE);
        return p;
    }

    private Oficina leerOficina(String linea, Map<String, Pais> mapPaises) {
        Oficina oficina = new Oficina();
        oficina.setCodigo(linea);
        oficina.setPais(mapPaises.get(linea));
        oficina.setCapacidadActual(0);
        oficina.setCapacidadMaxima(1000);
        oficina.setEstado(EstadoEnum.ACTIVO);
        return oficina;
    }

    private Vuelo leerVuelo(String codeOffice1, String codeOffice2,
                            String horaIni, String horaFin, Map<String, Oficina> mapOficinas) {
        Vuelo vuelo = new Vuelo();
        Pais pI = mapOficinas.get(codeOffice1).getPais();
        Pais pF = mapOficinas.get(codeOffice2).getPais();
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_TIME;
        vuelo.setOficinaOrigen(mapOficinas.get(codeOffice1));
        vuelo.setOficinaDestino(mapOficinas.get(codeOffice2));
        vuelo.setHoraInicio(LocalTime.parse(horaIni, dateTimeFormatter).plusHours(pI.getHusoHorario() * -1));
        vuelo.setHoraFin(LocalTime.parse(horaFin, dateTimeFormatter).plusHours(pF.getHusoHorario() * -1));
        vuelo.setCapacidad(300);

        return vuelo;
    }

}
