package org.redex.backend.controller.scheduledService;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.redex.backend.algorithm.evolutivo.Evolutivo;
import org.redex.backend.model.envios.Paquete;
import org.redex.backend.model.envios.PaqueteEstadoEnum;
import org.redex.backend.model.envios.PaqueteRuta;
import org.redex.backend.model.envios.RutaEstadoEnum;
import org.redex.backend.model.envios.VueloAgendado;
import org.redex.backend.model.envios.VueloAgendadoEstadoEnum;
import org.redex.backend.model.general.Persona;
import org.redex.backend.model.rrhh.Oficina;
import org.redex.backend.repository.OficinasRepository;
import org.redex.backend.repository.PaqueteRutaRepository;
import org.redex.backend.repository.PaquetesRepository;
import org.redex.backend.repository.PersonaRepository;
import org.redex.backend.repository.PlanVueloRepository;
import org.redex.backend.repository.VuelosAgendadosRepository;
import org.redex.backend.zelper.email.MailClient;
import org.redex.backend.zelper.email.MailEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;

@Service
@Transactional(readOnly = true)
public class ScheduledServiceServiceImp implements ScheduledServiceService {

    @Autowired
    PlanVueloRepository planVueloRepository;

    @Autowired
    VuelosAgendadosRepository vuelosRepository;

    @Autowired
    PaquetesRepository paquetesRepository;

    @Autowired
    OficinasRepository oficinasRepository;

    @Autowired
    PaqueteRutaRepository paqueteRutasRepository;
    
    @Autowired
    PersonaRepository personaRepository;
    
    @Autowired
    MailClient mailClient;

    @Transactional
    @Override
    public void salidaVuelos() {

        List<VueloAgendado> vuelos = vuelosRepository.findAll();
        
        List<VueloAgendado> vuelosCreados = vuelos.stream()
                .filter(vuelo -> vuelo.getEstado().equals(VueloAgendadoEstadoEnum.CREADO)).collect(Collectors.toList());
        LocalDateTime actualTime = ZonedDateTime.now(ZoneId.of("UTC")).toLocalDateTime();
        LocalDateTime previousTime = actualTime.minusMinutes(3);
        List<VueloAgendado> vuelosRecientes = vuelosCreados.stream()
                .filter(vuelo -> (vuelo.getFechaInicio().isBefore(actualTime)))
                .collect(Collectors.toList());
        
        Map<String, Oficina> oficinas = oficinasRepository.findAll()
                .stream()
                .collect(Collectors.toMap(oficina -> oficina.getCodigo(), oficina -> oficina));
        if (!vuelosRecientes.isEmpty()){
            for (VueloAgendado vA : vuelosRecientes) {
            
                vA.setEstado(VueloAgendadoEstadoEnum.ACTIVO);
                Oficina o = oficinas.get(vA.getOficinaOrigen().getCodigo());
                List<PaqueteRuta> pR = paqueteRutasRepository.findAllByVueloAgendado(vA);

                //System.out.println(pR.size());

                for (PaqueteRuta p : pR) {
                    
                    p.setEstado(RutaEstadoEnum.ACTIVO);
                    Paquete px = p.getPaquete();
                    px.setEstado(PaqueteEstadoEnum.EN_VUELO);
                    o.setCapacidadActual(o.getCapacidadActual() - 1);
                    paqueteRutasRepository.save(p);
                    paquetesRepository.save(px);
                    Persona personaOrigen = personaRepository.getOne(px.getPersonaOrigen().getId());
                    Persona personaDestino = personaRepository.getOne(px.getPersonaOrigen().getId());
                    System.out.println(vA.getVuelo().getOficinaOrigen().getPais().getNombre());
                    
                    if(personaOrigen.getEmail() != null && px.getNotiRegistro()){
                        Context miVuelo = new Context();
                        miVuelo.setVariable("miVuelo",p.getVueloAgendado());
//                        miVuelo.setVariable("fechaInicio",vA.getFechaInicio());
//                        miVuelo.setVariable("fechaFin",vA.getFechaFin());
                        mailClient.prepareAndSend(personaOrigen.getEmail(), MailEnum.NOTIFICACION_ENVIO,miVuelo);
                    }

                    if(personaDestino.getEmail() != null && px.getNotiRegistro()){
                        Context miVuelo = new Context();
                        miVuelo.setVariable("miVuelo",p.getVueloAgendado());
//                        miVuelo.setVariable("fechaInicio",vA.getFechaInicio());
//                        miVuelo.setVariable("fechaFin",vA.getFechaFin());
                        mailClient.prepareAndSend(personaDestino.getEmail(), MailEnum.NOTIFICACION_ENVIO, miVuelo);
                    }
                
                }
                vuelosRepository.save(vA);
                oficinasRepository.save(o);

            }
        } else {
            System.out.println("no hay vuelos");
        }
        

    }

    @Transactional
    @Override
    public void llegadaVuelos() {

        List<VueloAgendado> vuelos = vuelosRepository.findAll();
        
        List<VueloAgendado> vuelosCreados = vuelos.stream()
                .filter(vuelo -> vuelo.getEstado().equals(VueloAgendadoEstadoEnum.ACTIVO)).collect(Collectors.toList());
        LocalDateTime actualTime = ZonedDateTime.now(ZoneId.of("UTC")).toLocalDateTime();
        System.out.println(actualTime);
        LocalDateTime previousTime = actualTime.minusMinutes(3);
        List<VueloAgendado> vuelosRecientes = vuelosCreados.stream()
                .filter(vuelo -> (vuelo.getFechaFin().isBefore(actualTime)))
                .collect(Collectors.toList());
        
        Map<String, Oficina> oficinas = oficinasRepository.findAll()
                .stream()
                .collect(Collectors.toMap(oficina -> oficina.getCodigo(), oficina -> oficina));
        if (!vuelosRecientes.isEmpty()){
            for (VueloAgendado vA : vuelosRecientes) {
                System.out.println(vA.getCodigo());
                System.out.println(vA.getFechaFin());
                vA.setEstado(VueloAgendadoEstadoEnum.FINALIZADO);
                Oficina o = oficinas.get(vA.getVuelo().getOficinaDestino().getCodigo());
                
                List<PaqueteRuta> pR = paqueteRutasRepository.findAllByVueloAgendado(vA);
                if(!pR.isEmpty()){
                    for (PaqueteRuta p : pR) {
                        p.setEstado(RutaEstadoEnum.FINALIZADO);
                        paqueteRutasRepository.save(p);
                        Paquete px = p.getPaquete();
                        List<PaqueteRuta> pR1 = paqueteRutasRepository.findAllByPaquete(px);
                        int termino = 0;
                        for (PaqueteRuta pAux : pR1) {
                            if (pAux.getEstado().equals(RutaEstadoEnum.FINALIZADO)) {
                                termino = 1;
                            }else{
                                termino = 0;
                                break;
                            }
                        }
                        if (termino == 1) {
                            px.setEstado(PaqueteEstadoEnum.ENTREGADO);
                            if(px.getPersonaOrigen().getEmail()!= null && px.getNotiLlegada()){
                                Context miPaquete = new Context();
                                miPaquete.setVariable("miPaquete", px);
                                miPaquete.setVariable("fechaFin", vA.getFechaFinString());
                                mailClient.prepareAndSend(px.getPersonaOrigen().getEmail(), MailEnum.NOTIFICACION_LLEGADA_REMITENTE, miPaquete);
                            }
                            if(px.getPersonaDestino().getEmail() != null && px.getNotiLlegada()){
                                Context miPaquete = new Context();
                                miPaquete.setVariable("miPaquete", px);
                                miPaquete.setVariable("fechaFin", vA.getFechaFinString());
                                mailClient.prepareAndSend(px.getPersonaDestino().getEmail(), MailEnum.NOTIFICACION_LLEGADA_DESTINATARIO, miPaquete);
                            }
                        } else {
                            o.setCapacidadActual(o.getCapacidadActual() + 1);
                            px.setEstado(PaqueteEstadoEnum.EN_ALMACEN);
                            oficinasRepository.save(o);
                        }
                        paquetesRepository.save(px);
                    }
                    vuelosRepository.save(vA);
                    
                } else {
                    System.out.println("no hay paquetes");
                }
            }
        } else {
            System.out.println("no hay vuelos");
        }
        
    }
    
    @Transactional
    @Override
    public void generarRutas(){
        List<Paquete> paquetes = paquetesRepository.findAll();
        List<Paquete> paquetesAlmacen =  paquetes.stream()
                .filter(paquete -> (paquete.getEstado().equals(PaqueteEstadoEnum.EN_ALMACEN)))
                .collect(Collectors.toList());
        
        List<Oficina> oficinas = oficinasRepository.findAll();
        if (!paquetesAlmacen.isEmpty()){
            for (Paquete p : paquetesAlmacen){
                List<PaqueteRuta> pR1 = paqueteRutasRepository.findAllByPaquete(p);
                int termino = 0;
                PaqueteRuta eActual = null;
                if (!pR1.isEmpty()){
                    for (PaqueteRuta pAux : pR1) {
                        if (!pAux.getEstado().equals(RutaEstadoEnum.FINALIZADO)) {
                            eActual = pAux;
                            break;
                        } else {
                            termino++;
                        }
                    }
                }
                
                
                if (eActual == null){
                   Oficina oActual = p.getOficinaOrigen();
                } else{
                   Oficina oActual = eActual.getVueloAgendado().getOficinaOrigen();
                   
                }
                LocalDateTime diferencia;
                if(p.esIntercontinental()){
                   diferencia = p.getFechaIngreso().plusDays(2);
                } else {
                   diferencia = p.getFechaIngreso().plusDays(1); 
                }
                List<VueloAgendado> vuelos = vuelosRepository.findAll();
       
                LocalDateTime actualTime = ZonedDateTime.now(ZoneId.of("UTC")).toLocalDateTime();
                List<VueloAgendado> vuelosRecientes = vuelosRepository.findAllAlgoritmo(actualTime, diferencia);
                List<VueloAgendado> vuelosLlegan = vuelosRepository.findAllLlegan(actualTime, diferencia);
                List<VueloAgendado> vuelosParten = vuelosRepository.findAllParten(actualTime, diferencia);
                Evolutivo e = new Evolutivo();
                try{
                    List<VueloAgendado> vuelosPaquete = 
                        e.run(p, new ArrayList<VueloAgendado>(), vuelosRecientes, vuelosParten, vuelosLlegan, oficinas);
                
                    if (!pR1.isEmpty()){
                        for(int i = termino;i<pR1.size();i++){
                            paqueteRutasRepository.delete(pR1.get(i));
                        }
                    }
                    int newOrden;
                    if (!pR1.isEmpty()){
                        newOrden = termino;
                    }else {
                        newOrden = 0;
                    }
                    for (VueloAgendado va : vuelosPaquete){
                        PaqueteRuta px = new PaqueteRuta();
                        px.setPaquete(p);
                        px.setVueloAgendado(va);
                        px.setOrden(newOrden);
                        px.setEstado(RutaEstadoEnum.ACTIVO);
                        paqueteRutasRepository.save(px);
                        newOrden++;
                    }
                } catch (Exception ex){
                    System.out.println("NO se ha encontrado ruta");
                }
                
                
            }
        }
        
    }
}