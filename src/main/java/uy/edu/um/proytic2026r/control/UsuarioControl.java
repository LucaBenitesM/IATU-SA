package uy.edu.um.proytic2026r.control;

import uy.edu.um.proytic2026r.entities.Usuario;
import uy.edu.um.proytic2026r.servicio.UsuarioServicio;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UsuarioControl {

    private final UsuarioServicio usuarioServicio;

    public UsuarioControl(UsuarioServicio usuarioServicio) {
        this.usuarioServicio = usuarioServicio;
    }

    @PostMapping
    public ResponseEntity<Usuario> crearUsuario(@RequestBody Usuario usuario) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioServicio.crearUsuario(usuario));
    }
}
