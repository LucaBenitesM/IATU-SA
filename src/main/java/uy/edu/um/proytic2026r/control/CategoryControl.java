package uy.edu.um.proytic2026r.control;

import uy.edu.um.proytic2026r.entities.Category;
import uy.edu.um.proytic2026r.servicio.CategoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// La consulta es publica; alta, modificacion y baja exigen el id de un administrador (adminId).
@RestController
@RequestMapping("/api/categories")
public class CategoryControl {

    private final CategoryService servicio;

    public CategoryControl(CategoryService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<Category> listar() {
        return servicio.list();
    }

    @PostMapping
    public ResponseEntity<Category> crear(@RequestParam Long adminId, @RequestBody Map<String, String> body) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(servicio.create(adminId, body.get("name"), body.get("description")));
    }

    @PutMapping("/{id}")
    public Category modificar(@PathVariable Long id, @RequestParam Long adminId, @RequestBody Map<String, String> body) {
        return servicio.update(adminId, id, body.get("name"), body.get("description"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id, @RequestParam Long adminId) {
        servicio.delete(adminId, id);
        return ResponseEntity.noContent().build();
    }
}
