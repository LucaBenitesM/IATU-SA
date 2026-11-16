package uy.edu.um.proytic2026r.control;

import uy.edu.um.proytic2026r.entities.Atraccion;
import uy.edu.um.proytic2026r.servicio.AtraccionServicio;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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

    // Body: { "contentType": "image/jpeg", "data": "<base64 sin prefijo>" }
    @PutMapping("/{id}/image")
    public ResponseEntity<Void> subirImagen(@PathVariable Long id, @RequestBody Map<String, String> body) {
        atraccionServicio.guardarImagen(id, body.get("contentType"), body.get("data"));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/image")
    public ResponseEntity<byte[]> obtenerImagen(@PathVariable Long id) {
        Atraccion a = atraccionServicio.obtenerAtraccionPorId(id).orElse(null);
        if (a == null || !a.getHasImage()) {
            return ResponseEntity.notFound().build();
        }
        byte[] bytes = Base64.getDecoder().decode(a.getImageData());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(a.getImageType()))
                .cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES))
                .body(bytes);
    }
}
