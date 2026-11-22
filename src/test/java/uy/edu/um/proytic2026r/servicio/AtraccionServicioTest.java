package uy.edu.um.proytic2026r.servicio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import uy.edu.um.proytic2026r.entities.Atraccion;
import uy.edu.um.proytic2026r.entities.Category;
import uy.edu.um.proytic2026r.entities.EquipmentType;
import uy.edu.um.proytic2026r.entities.Prestador;
import uy.edu.um.proytic2026r.repositorio.AtraccionRepositorio;
import uy.edu.um.proytic2026r.repositorio.CategoryRepository;
import uy.edu.um.proytic2026r.repositorio.EquipmentTypeRepository;
import uy.edu.um.proytic2026r.repositorio.PrestadorRepositorio;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Publicacion de actividades: emprendimiento aprobado, categoria obligatoria y equipamiento estandarizado.
class AtraccionServicioTest {

    private AtraccionRepositorio atracciones;
    private PrestadorRepositorio prestadores;
    private CategoryRepository categorias;
    private EquipmentTypeRepository equipos;
    private AtraccionServicio service;

    @BeforeEach
    void preparar() {
        atracciones = mock(AtraccionRepositorio.class);
        prestadores = mock(PrestadorRepositorio.class);
        categorias = mock(CategoryRepository.class);
        equipos = mock(EquipmentTypeRepository.class);
        service = new AtraccionServicio(atracciones, prestadores, categorias, equipos);

        Prestador aprobado = new Prestador();
        aprobado.setId(1L);
        aprobado.setStatus("APPROVED");
        Prestador pendiente = new Prestador();
        pendiente.setId(2L);
        pendiente.setStatus("PENDING");
        when(prestadores.findById(1L)).thenReturn(Optional.of(aprobado));
        when(prestadores.findById(2L)).thenReturn(Optional.of(pendiente));
        when(categorias.findById(10L)).thenReturn(Optional.of(categoria(10L)));
        when(equipos.findById(20L)).thenReturn(Optional.of(equipo(20L)));
        when(equipos.findById(21L)).thenReturn(Optional.of(equipo(21L)));
        when(atracciones.save(any(Atraccion.class))).thenAnswer(i -> i.getArgument(0));
    }

    private Category categoria(Long id) {
        Category c = new Category();
        c.setId(id);
        c.setName("Cabalgatas");
        return c;
    }

    private EquipmentType equipo(Long id) {
        EquipmentType e = new EquipmentType();
        e.setId(id);
        e.setName("Equipo " + id);
        return e;
    }

    private Set<EquipmentType> conjunto(Long... ids) {
        Set<EquipmentType> s = new LinkedHashSet<EquipmentType>();
        for (Long id : ids) {
            EquipmentType e = new EquipmentType();
            e.setId(id);
            s.add(e);
        }
        return s;
    }

    private Atraccion actividad(Long prestadorId, Long categoriaId) {
        Atraccion a = new Atraccion();
        a.setName("Cabalgata");
        a.setPrice(new BigDecimal("1000"));
        a.setAllowsCancellation(false);
        Prestador p = new Prestador();
        p.setId(prestadorId);
        a.setPrestador(p);
        if (categoriaId != null) {
            a.setCategory(categoria(categoriaId));
        }
        return a;
    }

    private int codigo(ResponseStatusException e) {
        return e.getStatusCode().value();
    }

    @Test
    void publicaUnaActividadConCategoriaYEquipamiento() {
        Atraccion a = actividad(1L, 10L);
        a.setProvidedEquipment(conjunto(20L));
        a.setRequiredEquipment(conjunto(21L));

        Atraccion creada = service.crearAtraccion(a);

        assertEquals(10L, creada.getCategory().getId());
        assertEquals(1, creada.getProvidedEquipment().size());
        assertEquals(1, creada.getRequiredEquipment().size());
        verify(atracciones).save(any(Atraccion.class));
    }

    @Test
    void laCategoriaEsObligatoria() {
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.crearAtraccion(actividad(1L, null)));

        assertEquals(400, codigo(e));
        verify(atracciones, never()).save(any(Atraccion.class));
    }

    @Test
    void laCategoriaTieneQueExistir() {
        when(categorias.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.crearAtraccion(actividad(1L, 99L)));

        assertEquals(404, codigo(e));
    }

    @Test
    void elEquipamientoTieneQueExistir() {
        when(equipos.findById(88L)).thenReturn(Optional.empty());
        Atraccion a = actividad(1L, 10L);
        a.setRequiredEquipment(conjunto(88L));

        assertEquals(404, codigo(assertThrows(ResponseStatusException.class, () -> service.crearAtraccion(a))));
    }

    @Test
    void unEquipamientoNoPuedeSerProporcionadoYRequeridoALaVez() {
        Atraccion a = actividad(1L, 10L);
        a.setProvidedEquipment(conjunto(20L));
        a.setRequiredEquipment(conjunto(20L));

        ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> service.crearAtraccion(a));

        assertEquals(400, codigo(e));
    }

    @Test
    void elEquipamientoEsOpcional() {
        Atraccion creada = service.crearAtraccion(actividad(1L, 10L));

        assertEquals(0, creada.getProvidedEquipment().size());
        assertEquals(0, creada.getRequiredEquipment().size());
    }

    @Test
    void unEmprendimientoNoAprobadoNoPuedePublicar() {
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.crearAtraccion(actividad(2L, 10L)));

        assertEquals(403, codigo(e));
    }

    @Test
    void siPermiteCancelarHaceFaltaLimiteYReembolsoValido() {
        Atraccion a = actividad(1L, 10L);
        a.setAllowsCancellation(true);
        a.setCancellationDeadlineMinutes(60);
        a.setRefundPercentage(150);

        assertEquals(400, codigo(assertThrows(ResponseStatusException.class, () -> service.crearAtraccion(a))));
    }
}
