package uy.edu.um.proytic2026r.control;

import uy.edu.um.proytic2026r.entities.Prestador;
import uy.edu.um.proytic2026r.servicio.PrestadorServicio;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/providers")
public class PrestadorControl {

    private final PrestadorServicio prestadorServicio;

    public PrestadorControl(PrestadorServicio prestadorServicio) {
        this.prestadorServicio = prestadorServicio;
    }

    @PostMapping
    public ResponseEntity<Prestador> crearPrestador(@RequestBody Prestador prestador) {
        return ResponseEntity.status(HttpStatus.CREATED).body(prestadorServicio.crearPrestador(prestador));
    }

    @GetMapping
    public ResponseEntity<List<Prestador>> listarPrestadores() {
        return ResponseEntity.ok(prestadorServicio.listarPrestadores());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Prestador> obtenerPrestador(@PathVariable Long id) {
        return prestadorServicio.obtenerPrestadorPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    @PutMapping("/{id}/approve")
    public Prestador aprobar(@PathVariable Long id) {
        return prestadorServicio.cambiarEstado(id, "APPROVED");
    }

    @PutMapping("/{id}/deny")
    public Prestador denegar(@PathVariable Long id) {
        return prestadorServicio.cambiarEstado(id, "DENIED");
    }
}
