package uy.edu.um.proytic2026r.servicio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import uy.edu.um.proytic2026r.entities.EquipmentType;
import uy.edu.um.proytic2026r.entities.Usuario;
import uy.edu.um.proytic2026r.repositorio.AtraccionRepositorio;
import uy.edu.um.proytic2026r.repositorio.EquipmentTypeRepository;
import uy.edu.um.proytic2026r.repositorio.UsuarioRepositorio;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// RF-17: el administrador gestiona los tipos de equipamiento estandarizados.
class EquipmentTypeServiceTest {

    private EquipmentTypeRepository tipos;
    private AtraccionRepositorio atracciones;
    private UsuarioRepositorio usuarios;
    private EquipmentTypeService service;

    @BeforeEach
    void preparar() {
        tipos = mock(EquipmentTypeRepository.class);
        atracciones = mock(AtraccionRepositorio.class);
        usuarios = mock(UsuarioRepositorio.class);
        service = new EquipmentTypeService(tipos, atracciones, usuarios);

        Usuario admin = new Usuario();
        admin.setId(1L);
        admin.setRole("ADMIN");
        Usuario turista = new Usuario();
        turista.setId(2L);
        turista.setRole("TOURIST");
        when(usuarios.findById(1L)).thenReturn(Optional.of(admin));
        when(usuarios.findById(2L)).thenReturn(Optional.of(turista));
        when(tipos.save(any(EquipmentType.class))).thenAnswer(i -> i.getArgument(0));
        when(tipos.findByNameIgnoreCase(any(String.class))).thenReturn(Optional.empty());
    }

    private EquipmentType tipo(Long id, String nombre) {
        EquipmentType t = new EquipmentType();
        t.setId(id);
        t.setName(nombre);
        return t;
    }

    private int codigo(ResponseStatusException e) {
        return e.getStatusCode().value();
    }

    @Test
    void elAdminPuedeCrearUnTipoDeEquipamiento() {
        EquipmentType t = service.create(1L, "  Casco ");

        assertEquals("Casco", t.getName());
    }

    @Test
    void unTuristaNoPuedeCrearTipos() {
        assertEquals(403, codigo(assertThrows(ResponseStatusException.class, () -> service.create(2L, "Casco"))));
        verify(tipos, never()).save(any(EquipmentType.class));
    }

    @Test
    void elNombreEsObligatorio() {
        assertEquals(400, codigo(assertThrows(ResponseStatusException.class, () -> service.create(1L, ""))));
    }

    @Test
    void noSePermitenNombresRepetidos() {
        when(tipos.findByNameIgnoreCase("casco")).thenReturn(Optional.of(tipo(3L, "Casco")));

        assertEquals(409, codigo(assertThrows(ResponseStatusException.class, () -> service.create(1L, "casco"))));
    }

    @Test
    void sePuedeRenombrarUnTipo() {
        when(tipos.findById(3L)).thenReturn(Optional.of(tipo(3L, "Casco")));

        EquipmentType t = service.update(1L, 3L, "Casco de seguridad");

        assertEquals("Casco de seguridad", t.getName());
    }

    @Test
    void noSePuedeEliminarUnTipoUsadoComoProporcionado() {
        when(tipos.findById(3L)).thenReturn(Optional.of(tipo(3L, "Casco")));
        when(atracciones.existsByProvidedEquipment_Id(3L)).thenReturn(true);

        assertEquals(409, codigo(assertThrows(ResponseStatusException.class, () -> service.delete(1L, 3L))));
        verify(tipos, never()).delete(any(EquipmentType.class));
    }

    @Test
    void noSePuedeEliminarUnTipoUsadoComoRequerido() {
        when(tipos.findById(3L)).thenReturn(Optional.of(tipo(3L, "Casco")));
        when(atracciones.existsByRequiredEquipment_Id(3L)).thenReturn(true);

        assertEquals(409, codigo(assertThrows(ResponseStatusException.class, () -> service.delete(1L, 3L))));
    }

    @Test
    void sePuedeEliminarUnTipoSinUso() {
        EquipmentType t = tipo(3L, "Casco");
        when(tipos.findById(3L)).thenReturn(Optional.of(t));

        service.delete(1L, 3L);

        verify(tipos).delete(t);
    }
}
