/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redex.backend.repository;

import org.redex.model.envios.Vuelo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 *
 * @author Oscar
 */
@Repository

public interface VuelosRepository extends JpaRepository<Vuelo, Long> {
    
}