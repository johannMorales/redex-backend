package org.redex.backend.controller.personas;

import org.redex.backend.zelper.response.CargaDatosResponse;
import org.redex.backend.model.general.Persona;
import org.redex.backend.model.general.TipoDocumentoIdentidad;
import org.redex.backend.zelper.crimsontable.CrimsonTableRequest;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

public interface PersonasService {

    public CargaDatosResponse carga(MultipartFile file);

    public Persona find(Long id);

    public Page<Persona> allByCrimson(CrimsonTableRequest request);

    public Persona save(PersonaRegistro persona);

    public Persona findByDocumento(TipoDocumentoIdentidad tipoDocumentoIdentidad, String numeroDocumentoIdentidad);
    
    public void editar(Persona persona);
    
}
