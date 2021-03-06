package org.redex.backend.controller.usuarios;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.apache.commons.codec.digest.DigestUtils;
import org.redex.backend.controller.oficinas.OficinasServiceImp;
import org.redex.backend.repository.ArchivosRepository;
import org.redex.backend.repository.ColaboradoresRepository;
import org.redex.backend.repository.OficinasRepository;
import org.redex.backend.repository.PaisesRepository;
import org.redex.backend.repository.PersonaRepository;
import org.redex.backend.repository.RolesRepository;
import org.redex.backend.repository.TipoDocumentoIdentidadRepository;
import org.redex.backend.repository.UsuariosRepository;
import org.redex.backend.zelper.response.CargaDatosResponse;
import org.redex.backend.model.general.EstadoEnum;
import org.redex.backend.model.general.Pais;
import org.redex.backend.model.general.Persona;
import org.redex.backend.model.general.TipoDocumentoIdentidad;
import org.redex.backend.model.rrhh.CargoEnum;
import org.redex.backend.model.rrhh.Colaborador;
import org.redex.backend.model.rrhh.Oficina;
import org.redex.backend.model.seguridad.Rol;
import org.redex.backend.model.seguridad.Usuario;
import org.redex.backend.zelper.crimsontable.CrimsonTableRequest;
import org.redex.backend.zelper.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional(readOnly = true)
public class UsuariosServiceImp implements UsuariosService {

    @Autowired
    OficinasRepository oficinasRepository;

    @Autowired
    ArchivosRepository archivosRepository;

    @Autowired
    PaisesRepository paisesRepository;

    @Autowired
    PersonaRepository personaRepository;

    @Autowired
    ColaboradoresRepository colaboradoresRepository;

    @Autowired
    UsuariosRepository usuariosRepository;

    @Autowired
    TipoDocumentoIdentidadRepository tpiRepository;

    @Autowired
    RolesRepository rolesRepository;

    @Override
    @Transactional
    public CargaDatosResponse carga(MultipartFile file) {
        Integer cantidadRegistros = 0;
        Integer cantidadErrores = 0;
        List<String> errores = new ArrayList<>();

        //hashmap de paises por el codigo
        Map<String, Pais> paises = paisesRepository.findAll()
                .stream()
                .collect(Collectors.toMap(pais -> pais.getCodigo(), pais -> pais));

        Map<String, Rol> roles = rolesRepository.findAll()
                .stream()
                .collect(Collectors.toMap(rol -> rol.getCodigo().name(), rol -> rol));

        Map<String, TipoDocumentoIdentidad> tpis = tpiRepository.findAll()
                .stream()
                .collect(Collectors.toMap(tpi -> tpi.getSimbolo(), tpi -> tpi));

        Map<String, Oficina> oficinas = oficinasRepository.findAll()
                .stream()
                .collect(Collectors.toMap(oficina -> oficina.getCodigo(), oficina -> oficina));

        //para guardar las oficinas que luego iran a bd
        List<Persona> nuevasPersonas = new ArrayList<>();
        List<Colaborador> nuevosColaboradores = new ArrayList<>();
        List<Usuario> nuevosUsuarios = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), Charset.forName(StandardCharsets.UTF_8.name())))) {
            List<String> lineasList = reader.lines().collect(Collectors.toList());
            int contLinea = 1;
            for (String linea : lineasList) {
                // si le vas a poner validacoines aqui deberias controlarlas
                if (!linea.isEmpty()) {

                    List<String> separateLine = Arrays.asList(linea.split(","));
                    if (separateLine.size() == 14) {
                        Persona nuevaPersona = leePersona(separateLine, paises, tpis);
                        Colaborador nuevoColaborador = leeColaborador(separateLine, nuevaPersona, oficinas);
                        Usuario nuevoUsuario = leeUsuario(separateLine, nuevoColaborador, roles);
                        nuevasPersonas.add(nuevaPersona);
                        nuevosColaboradores.add(nuevoColaborador);
                        nuevosUsuarios.add(nuevoUsuario);
                    } else {
                        cantidadErrores = cantidadErrores + 1;
                        errores.add("La linea " + contLinea + " no tiene todos los campos");
                    }

                }
            }

            for (Persona persona : nuevasPersonas) {
                personaRepository.save(persona);
            }

            for (Colaborador colaborador : nuevosColaboradores) {
                colaboradoresRepository.save(colaborador);
            }

            for (Usuario usuario : nuevosUsuarios) {
                usuariosRepository.save(usuario);
            }

        } catch (IOException ex) {
            Logger.getLogger(OficinasServiceImp.class.getName()).log(Level.SEVERE, null, ex);
        }

        return new CargaDatosResponse(cantidadErrores, cantidadRegistros, "Carga finalizada con exito", errores);
    }

    private Persona leePersona(List<String> datos, Map<String, Pais> mapPaises,
            Map<String, TipoDocumentoIdentidad> tpis) {

        Persona p = new Persona();
        p.setNombres(datos.get(0));
        p.setPaterno(datos.get(1));
        p.setMaterno(datos.get(2));
        p.setTelefono(datos.get(3));
        p.setEmail(datos.get(4));
        p.setCelular(datos.get(5));
        p.setPais(mapPaises.get(datos.get(6)));
        p.setTipoDocumentoIdentidad(tpis.get(datos.get(7)));
        p.setNumeroDocumentoIdentidad(datos.get(8));

        return p;
    }

    private Colaborador leeColaborador(List<String> datos, Persona p,
            Map<String, Oficina> oficinas) {
        Colaborador c = new Colaborador();
        c.setPersona(p);
        c.setCargo(CargoEnum.valueOf(datos.get(9)));
        c.setEstado(EstadoEnum.valueOf(datos.get(10)));
        c.setOficina(oficinas.get(datos.get(11)));
        c.setCelular(p.getCelular());
        c.setEmail(p.getEmail());
        c.setTelefono(p.getTelefono());

        return c;
    }

    private Usuario leeUsuario(List<String> datos, Colaborador c, Map<String, Rol> roles) {
        Usuario u = new Usuario();
        u.setColaborador(c);
        u.setEstado(EstadoEnum.ACTIVO);
        u.setUsername(datos.get(12));
        String password = DigestUtils.sha256Hex(datos.get(13));
        u.setPassword(password);
        System.out.println(datos.get(9));
        System.out.println(roles.get(datos.get(9)));
        u.setRol(roles.get(datos.get(9)));
        return u;
    }

    @Override
    public List<Usuario> all() {
        return usuariosRepository.findAll();
    }

    @Transactional
    @Override
    public void crearUsuario(UsuariosPayload paylaod) {

        Persona p = new Persona();

        p.setNombres(paylaod.getNombres());
        p.setPaterno(paylaod.getaPaterno());
        p.setMaterno(paylaod.getaMaterno());
        p.setTipoDocumentoIdentidad(paylaod.getTipDoc());
        p.setTelefono(paylaod.getTelefono());
        p.setEmail(paylaod.getEmail());
        p.setNumeroDocumentoIdentidad(paylaod.getDocId());
        personaRepository.save(p);

        Colaborador c = new Colaborador();

        c.setPersona(p);
        c.setOficina(paylaod.getOficina());
        c.setEstado(EstadoEnum.ACTIVO);
        colaboradoresRepository.save(c);

        Oficina o = oficinasRepository.getOne(paylaod.getOficina().getId());

        Usuario usuario = new Usuario();
        usuario.setEstado(EstadoEnum.ACTIVO);
        usuario.setColaborador(c);
        usuario.setPassword("$2a$10$08lY1Bupapwa00DK9MP4K.3t7n/d7MCtARhPd9oMaxWHCtTh6KOnS");
    usuario.setUsername(String.format("%s%s", o.getCodigo(), Long.toString(System.currentTimeMillis())));
    usuario.setRol(paylaod.getRol());
        usuariosRepository.save(usuario);
    }

    @Transactional
    public void restablecerContraseña(UsuariosPayload usuario) {

    }

    @Override
    public Page<Usuario> crimsonList(CrimsonTableRequest request) {
        return usuariosRepository.crimsonList(request.getSearch(), request.createPagination());
    }

    @Override
    @Transactional
    public void desactivar(Long id) {
        Usuario u = usuariosRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", id));
        u.setEstado(EstadoEnum.INACTIVO);
        usuariosRepository.save(u);
    }

    @Override
    @Transactional
    public void activar(Long id) {
        Usuario u = usuariosRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", id));

        u.setEstado(EstadoEnum.ACTIVO);
        usuariosRepository.save(u);
    }

    @Override
    @Transactional
    public void editar(Usuario usuario) {
        Usuario u = usuariosRepository.getOne(usuario.getId());
        Colaborador c = u.getColaborador();
        u.setRol(usuario.getRol());
        c.setOficina(usuario.getColaborador().getOficina());
        c.setEmail(usuario.getColaborador().getEmail());
        c.setTelefono(usuario.getColaborador().getTelefono());

        colaboradoresRepository.save(c);
        usuariosRepository.save(u);
    }

    @Override
    public Usuario find(Long id){
        return usuariosRepository.getOne(id);
    }

    @Override
    @Transactional
    public void actualizarPassword(Usuario usuario) {
        Usuario uBD = usuariosRepository.getOne(usuario.getId());
        uBD.setPassword(new BCryptPasswordEncoder().encode(usuario.getPassword()));
        usuariosRepository.save(uBD);
    }

}
