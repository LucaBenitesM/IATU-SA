package uy.edu.um.proytic2026r.servicio;

import uy.edu.um.proytic2026r.entities.EquipmentType;
import uy.edu.um.proytic2026r.entities.Usuario;
import uy.edu.um.proytic2026r.repositorio.AtraccionRepositorio;
import uy.edu.um.proytic2026r.repositorio.EquipmentTypeRepository;
import uy.edu.um.proytic2026r.repositorio.UsuarioRepositorio;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

// RF-17: administracion de los tipos de equipamiento estandarizados.
@Service
public class EquipmentTypeService {

    private final EquipmentTypeRepository tipos;
    private final AtraccionRepositorio atracciones;
    private final UsuarioRepositorio usuarios;

    public EquipmentTypeService(EquipmentTypeRepository tipos, AtraccionRepositorio atracciones, UsuarioRepositorio usuarios) {
        this.tipos = tipos;
        this.atracciones = atracciones;
        this.usuarios = usuarios;
    }

    public List<EquipmentType> list() {
        return tipos.findAllByOrderByNameAsc();
    }

    public EquipmentType create(Long adminId, String name) {
        exigirAdmin(adminId);
        String limpio = validarNombre(name);
        if (tipos.findByNameIgnoreCase(limpio).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un tipo de equipamiento con ese nombre.");
        }
        EquipmentType t = new EquipmentType();
        t.setName(limpio);
        return tipos.save(t);
    }

    public EquipmentType update(Long adminId, Long id, String name) {
        exigirAdmin(adminId);
        EquipmentType t = tipos.findById(id).orElse(null);
        if (t == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El tipo de equipamiento no existe.");
        }
        String limpio = validarNombre(name);
        Optional<EquipmentType> otro = tipos.findByNameIgnoreCase(limpio);
        if (otro.isPresent() && !otro.get().getId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un tipo de equipamiento con ese nombre.");
        }
        t.setName(limpio);
        return tipos.save(t);
    }

    public void delete(Long adminId, Long id) {
        exigirAdmin(adminId);
        EquipmentType t = tipos.findById(id).orElse(null);
        if (t == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El tipo de equipamiento no existe.");
        }
        if (atracciones.existsByProvidedEquipment_Id(id) || atracciones.existsByRequiredEquipment_Id(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede eliminar: hay actividades que usan este equipamiento.");
        }
        tipos.delete(t);
    }

    private void exigirAdmin(Long adminId) {
        Usuario u = adminId == null ? null : usuarios.findById(adminId).orElse(null);
        if (u == null || !"ADMIN".equals(u.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo los administradores pueden hacer esto.");
        }
    }

    private String validarNombre(String name) {
        String limpio = name == null ? "" : name.trim();
        if (limpio.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre es obligatorio.");
        }
        if (limpio.length() > 60) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre no puede superar los 60 caracteres.");
        }
        return limpio;
    }
}
