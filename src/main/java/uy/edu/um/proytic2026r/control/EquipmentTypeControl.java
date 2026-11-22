package uy.edu.um.proytic2026r.control;

import uy.edu.um.proytic2026r.entities.EquipmentType;
import uy.edu.um.proytic2026r.servicio.EquipmentTypeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// La consulta es publica; alta, modificacion y baja exigen el id de un administrador (adminId).
@RestController
@RequestMapping("/api/equipment-types")
public class EquipmentTypeControl {

    private final EquipmentTypeService servicio;

    public EquipmentTypeControl(EquipmentTypeService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<EquipmentType> listar() {
        return servicio.list();
    }

    @PostMapping
    public ResponseEntity<EquipmentType> crear(@RequestParam Long adminId, @RequestBody Map<String, String> body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.create(adminId, body.get("name")));
    }

    @PutMapping("/{id}")
    public EquipmentType modificar(@PathVariable Long id, @RequestParam Long adminId, @RequestBody Map<String, String> body) {
        return servicio.update(adminId, id, body.get("name"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id, @RequestParam Long adminId) {
        servicio.delete(adminId, id);
        return ResponseEntity.noContent().build();
    }
}
