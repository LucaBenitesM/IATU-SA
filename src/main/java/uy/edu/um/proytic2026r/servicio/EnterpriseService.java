package uy.edu.um.proytic2026r.servicio;

import uy.edu.um.proytic2026r.entities.Enterprise;
import uy.edu.um.proytic2026r.repositorio.EnterpriseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class EnterpriseService {

    @Autowired
    private EnterpriseRepository enterpriseRepository;

    public Enterprise saveEnterprise(Enterprise enterprise) {
        return enterpriseRepository.save(enterprise);
    }

    public List<Enterprise> getAllEnterprises() {
        return enterpriseRepository.findAll();
    }

    public Optional<Enterprise> getEnterpriseById(Long id) {
        return enterpriseRepository.findById(id);
    }
}