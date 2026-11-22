package uy.edu.um.proytic2026r.servicio;

import uy.edu.um.proytic2026r.entities.Category;
import uy.edu.um.proytic2026r.entities.Usuario;
import uy.edu.um.proytic2026r.repositorio.AtraccionRepositorio;
import uy.edu.um.proytic2026r.repositorio.CategoryRepository;
import uy.edu.um.proytic2026r.repositorio.UsuarioRepositorio;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

// RF-16: administracion (alta, consulta, modificacion y baja) de categorias de actividades.
@Service
public class CategoryService {

    private final CategoryRepository categorias;
    private final AtraccionRepositorio atracciones;
    private final UsuarioRepositorio usuarios;

    public CategoryService(CategoryRepository categorias, AtraccionRepositorio atracciones, UsuarioRepositorio usuarios) {
        this.categorias = categorias;
        this.atracciones = atracciones;
        this.usuarios = usuarios;
    }

    public List<Category> list() {
        return categorias.findAllByOrderByNameAsc();
    }

    public Category create(Long adminId, String name, String description) {
        exigirAdmin(adminId);
        String limpio = validarNombre(name);
        if (categorias.findByNameIgnoreCase(limpio).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una categoría con ese nombre.");
        }
        Category c = new Category();
        c.setName(limpio);
        c.setDescription(limpiarDescripcion(description));
        return categorias.save(c);
    }

    public Category update(Long adminId, Long id, String name, String description) {
        exigirAdmin(adminId);
        Category c = categorias.findById(id).orElse(null);
        if (c == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "La categoría no existe.");
        }
        String limpio = validarNombre(name);
        Optional<Category> otra = categorias.findByNameIgnoreCase(limpio);
        if (otra.isPresent() && !otra.get().getId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una categoría con ese nombre.");
        }
        c.setName(limpio);
        c.setDescription(limpiarDescripcion(description));
        return categorias.save(c);
    }

    public void delete(Long adminId, Long id) {
        exigirAdmin(adminId);
        Category c = categorias.findById(id).orElse(null);
        if (c == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "La categoría no existe.");
        }
        if (atracciones.existsByCategory_Id(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede eliminar: hay actividades que usan esta categoría.");
        }
        categorias.delete(c);
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

    private String limpiarDescripcion(String description) {
        if (description == null || description.trim().isEmpty()) {
            return null;
        }
        String limpia = description.trim();
        return limpia.length() > 300 ? limpia.substring(0, 300) : limpia;
    }
}
