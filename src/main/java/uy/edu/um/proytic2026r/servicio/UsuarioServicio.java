package uy.edu.um.proytic2026r.servicio;

import uy.edu.um.proytic2026r.entities.Usuario;
import uy.edu.um.proytic2026r.repositorio.UsuarioRepositorio;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UsuarioServicio {

    private final UsuarioRepositorio usuarioRepositorio;

    public UsuarioServicio(UsuarioRepositorio usuarioRepositorio) {
        this.usuarioRepositorio = usuarioRepositorio;
    }

    public Usuario crearUsuario(Usuario user) {
        if ("ADMIN".equals(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No se puede crear un administrador");
        }
        if (usuarioRepositorio.existsByEmail(user.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El mail ya está registrado!");
        }
        return usuarioRepositorio.save(user);
    }
    public Usuario login(String email, String password, String role) {
        Usuario u = usuarioRepositorio.findByEmail(email).orElse(null);
        if (u == null || password == null || !password.equals(u.getPassword())
                || role == null || !role.equals(u.getRole())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email, contraseña o rol incorrectos");
        }
        return u;
    }
}
