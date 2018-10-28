package org.redex.backend.controller.paises;

import java.util.List;
import org.redex.backend.repository.PaisesRepository;
import org.redex.backend.model.general.Pais;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PaisesServiceImp implements PaisesService {

    @Autowired
    PaisesRepository paisesRepository;
    
    @Override
    public List<Pais> all() {
        return paisesRepository.findAll();
    }

    @Override
    public List<Pais> allByNombre(String nombre) {
        return paisesRepository.allByNombre(nombre, PageRequest.of(0, 5));
    }
    
}
