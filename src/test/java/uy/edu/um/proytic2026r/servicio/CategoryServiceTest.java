package uy.edu.um.proytic2026r.servicio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import uy.edu.um.proytic2026r.entities.Category;
import uy.edu.um.proytic2026r.entities.Usuario;
import uy.edu.um.proytic2026r.repositorio.AtraccionRepositorio;
import uy.edu.um.proytic2026r.repositorio.CategoryRepository;
import uy.edu.um.proytic2026r.repositorio.UsuarioRepositorio;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// RF-16: el administrador gestiona las categorias de actividades.
class CategoryServiceTest {

    private CategoryRepository categorias;
    private AtraccionRepositorio atracciones;
    private UsuarioRepositorio usuarios;
    private CategoryService service;

    @BeforeEach
    void preparar() {
        categorias = mock(CategoryRepository.class);
        atracciones = mock(AtraccionRepositorio.class);
        usuarios = mock(UsuarioRepositorio.class);
        service = new CategoryService(categorias, atracciones, usuarios);

        Usuario admin = new Usuario();
        admin.setId(1L);
        admin.setRole("ADMIN");
        Usuario turista = new Usuario();
        turista.setId(2L);
        turista.setRole("TOURIST");
        when(usuarios.findById(1L)).thenReturn(Optional.of(admin));
        when(usuarios.findById(2L)).thenReturn(Optional.of(turista));
        when(categorias.save(any(Category.class))).thenAnswer(i -> i.getArgument(0));
        when(categorias.findByNameIgnoreCase(any(String.class))).thenReturn(Optional.empty());
    }

    private Category categoria(Long id, String nombre) {
        Category c = new Category();
        c.setId(id);
        c.setName(nombre);
        return c;
    }

    private int codigo(ResponseStatusException e) {
        return e.getStatusCode().value();
    }

    @Test
    void elAdminPuedeCrearUnaCategoriaYSeLimpianLosEspacios() {
        Category c = service.create(1L, "  Cabalgatas  ", "   ");

        assertEquals("Cabalgatas", c.getName());
        assertNull(c.getDescription());
    }

    @Test
    void unTuristaNoPuedeCrearCategorias() {
        ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> service.create(2L, "Aventura", null));

        assertEquals(403, codigo(e));
        verify(categorias, never()).save(any(Category.class));
    }

    @Test
    void sinUsuarioNoSePuedeCrear() {
        assertEquals(403, codigo(assertThrows(ResponseStatusException.class, () -> service.create(null, "Aventura", null))));
        assertEquals(403, codigo(assertThrows(ResponseStatusException.class, () -> service.create(99L, "Aventura", null))));
    }

    @Test
    void elNombreEsObligatorioYTieneUnLargoMaximo() {
        assertEquals(400, codigo(assertThrows(ResponseStatusException.class, () -> service.create(1L, "   ", null))));
        assertEquals(400, codigo(assertThrows(ResponseStatusException.class, () -> service.create(1L, null, null))));
        assertEquals(400, codigo(assertThrows(ResponseStatusException.class, () -> service.create(1L, "a".repeat(61), null))));
    }

    @Test
    void noSePermitenNombresRepetidos() {
        when(categorias.findByNameIgnoreCase("cabalgatas")).thenReturn(Optional.of(categoria(5L, "Cabalgatas")));

        ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> service.create(1L, "cabalgatas", null));

        assertEquals(409, codigo(e));
    }

    @Test
    void sePuedeModificarUnaCategoriaConservandoSuNombre() {
        Category existente = categoria(5L, "Cabalgatas");
        when(categorias.findById(5L)).thenReturn(Optional.of(existente));
        when(categorias.findByNameIgnoreCase("Cabalgatas")).thenReturn(Optional.of(existente));

        Category c = service.update(1L, 5L, "Cabalgatas", "A caballo");

        assertEquals("A caballo", c.getDescription());
    }

    @Test
    void noSePuedeRenombrarConElNombreDeOtraCategoria() {
        when(categorias.findById(5L)).thenReturn(Optional.of(categoria(5L, "Cabalgatas")));
        when(categorias.findByNameIgnoreCase("Enoturismo")).thenReturn(Optional.of(categoria(6L, "Enoturismo")));

        ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> service.update(1L, 5L, "Enoturismo", null));

        assertEquals(409, codigo(e));
    }

    @Test
    void modificarUnaCategoriaInexistenteDa404() {
        when(categorias.findById(77L)).thenReturn(Optional.empty());

        assertEquals(404, codigo(assertThrows(ResponseStatusException.class, () -> service.update(1L, 77L, "X", null))));
    }

    @Test
    void sePuedeEliminarUnaCategoriaSinUso() {
        Category c = categoria(5L, "Cabalgatas");
        when(categorias.findById(5L)).thenReturn(Optional.of(c));
        when(atracciones.existsByCategory_Id(5L)).thenReturn(false);

        service.delete(1L, 5L);

        verify(categorias).delete(c);
    }

    @Test
    void noSePuedeEliminarUnaCategoriaQueTieneActividades() {
        when(categorias.findById(5L)).thenReturn(Optional.of(categoria(5L, "Cabalgatas")));
        when(atracciones.existsByCategory_Id(5L)).thenReturn(true);

        ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> service.delete(1L, 5L));

        assertEquals(409, codigo(e));
        verify(categorias, never()).delete(any(Category.class));
    }

    @Test
    void soloElAdminPuedeEliminar() {
        assertEquals(403, codigo(assertThrows(ResponseStatusException.class, () -> service.delete(2L, 5L))));
    }
}
