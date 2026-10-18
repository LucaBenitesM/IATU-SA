package uy.edu.um.proytic2026r.repositorio;

import uy.edu.um.proytic2026r.entities.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepositorio extends JpaRepository<Usuario, Long> {
    boolean existsByEmail(String email);
}
