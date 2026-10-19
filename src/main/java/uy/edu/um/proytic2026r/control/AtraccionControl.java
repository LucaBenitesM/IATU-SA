package uy.edu.um.proytic2026r.control;

import uy.edu.um.proytic2026r.entities.Atraccion;
import uy.edu.um.proytic2026r.servicio.AtraccionServicio;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/attractions")
public class AtraccionControl {

    private final AtraccionServicio atraccionServicio;

    public AtraccionControl(AtraccionServicio atraccionServicio) {
        this.atraccionServicio = atraccionServicio;
    }

    @PostMapping
    public ResponseEntity<Atraccion> crearAtraccion(@RequestBody Atraccion attraction) {
        return ResponseEntity.status(HttpStatus.CREATED).body(atraccionServicio.crearAtraccion(attraction));
    }

    @GetMapping
    public ResponseEntity<List<Atraccion>> listarAtracciones() {
        return ResponseEntity.ok(atraccionServicio.listarAtracciones());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Atraccion> obtenerAtraccion(@PathVariable Long id) {
        return atraccionServicio.obtenerAtraccionPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
