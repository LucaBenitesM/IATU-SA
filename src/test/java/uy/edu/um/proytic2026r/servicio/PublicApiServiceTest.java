package uy.edu.um.proytic2026r.servicio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import uy.edu.um.proytic2026r.entities.ActivityInstance;
import uy.edu.um.proytic2026r.entities.Atraccion;
import uy.edu.um.proytic2026r.entities.Prestador;
import uy.edu.um.proytic2026r.entities.Review;
import uy.edu.um.proytic2026r.repositorio.ActivityInstanceRepository;
import uy.edu.um.proytic2026r.repositorio.AtraccionRepositorio;
import uy.edu.um.proytic2026r.repositorio.PrestadorRepositorio;
import uy.edu.um.proytic2026r.repositorio.ReviewRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// API publica para terceros: solo datos de emprendimientos verificados.
class PublicApiServiceTest {

    private PrestadorRepositorio prestadores;
    private AtraccionRepositorio atracciones;
    private ActivityInstanceRepository instancias;
    private ReviewRepository resenas;
    private PublicApiService service;

    private Prestador verificado;
    private Prestador pendiente;

    @BeforeEach
    void preparar() {
        prestadores = mock(PrestadorRepositorio.class);
        atracciones = mock(AtraccionRepositorio.class);
        instancias = mock(ActivityInstanceRepository.class);
        resenas = mock(ReviewRepository.class);
        service = new PublicApiService(prestadores, atracciones, instancias, resenas);

        verificado = prestador(1L, "Estancia", "APPROVED");
        pendiente = prestador(2L, "Nuevo", "PENDING");
    }

    private Prestador prestador(Long id, String nombre, String estado) {
        Prestador p = new Prestador();
        p.setId(id);
        p.setName(nombre);
        p.setStatus(estado);
        return p;
    }

    private Atraccion atraccion(Long id, String nombre, String depto, Prestador dueno) {
        Atraccion a = new Atraccion();
        a.setId(id);
        a.setName(nombre);
        a.setDescription("Descripción de " + nombre);
        a.setDepartment(depto);
        a.setLocality("Centro");
        a.setPrestador(dueno);
        return a;
    }

    private ActivityInstance instancia(Long id, Atraccion a, LocalDateTime cuando, int lugares) {
        ActivityInstance i = new ActivityInstance();
        i.setId(id);
        i.setAtraccion(a);
        i.setDateTime(cuando);
        i.setAvailablePlaces(lugares);
        return i;
    }

    private Review resena(Atraccion a, int puntos) {
        Review r = new Review();
        r.setAtraccion(a);
        r.setRating(puntos);
        r.setCreatedAt(LocalDateTime.now());
        return r;
    }

    private int codigo(ResponseStatusException e) {
        return e.getStatusCode().value();
    }

    @Test
    void listaSoloLosEmprendimientosVerificados() {
        List<Prestador> lista = new ArrayList<Prestador>();
        lista.add(verificado);
        when(prestadores.findByStatus("APPROVED")).thenReturn(lista);

        List<Map<String, Object>> resultado = service.getVerifiedProviders();

        assertEquals(1, resultado.size());
        assertEquals("Estancia", resultado.get(0).get("name"));
    }

    @Test
    void laRespuestaNoExponeDatosPrivados() {
        List<Prestador> lista = new ArrayList<Prestador>();
        lista.add(verificado);
        when(prestadores.findByStatus("APPROVED")).thenReturn(lista);

        Map<String, Object> fila = service.getVerifiedProviders().get(0);

        assertFalse(fila.containsKey("owner"));
        assertFalse(fila.containsKey("rut"));
        assertFalse(fila.containsKey("password"));
    }

    @Test
    void verificacionDeUnEmprendimientoAprobadoYUnoPendiente() {
        when(prestadores.findById(1L)).thenReturn(Optional.of(verificado));
        when(prestadores.findById(2L)).thenReturn(Optional.of(pendiente));

        assertEquals(true, service.getVerification(1L).get("verified"));
        assertEquals(false, service.getVerification(2L).get("verified"));
    }

    @Test
    void verificacionDeUnEmprendimientoInexistenteDa404() {
        when(prestadores.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> service.getVerification(99L));

        assertEquals(404, codigo(e));
    }

    @Test
    void actividadesDelDiaSoloMuestraLasQueTienenLugaresYSonVerificadas() {
        LocalDate dia = LocalDate.now().plusDays(3);
        Atraccion ok = atraccion(1L, "Kayak", "Rocha", verificado);
        Atraccion sinLugares = atraccion(2L, "Cabalgata", "Rocha", verificado);
        Atraccion noVerificada = atraccion(3L, "Trekking", "Rocha", pendiente);
        Atraccion otroDepto = atraccion(4L, "Vinos", "Colonia", verificado);
        List<ActivityInstance> lista = new ArrayList<ActivityInstance>();
        lista.add(instancia(1L, ok, dia.atTime(10, 0), 5));
        lista.add(instancia(2L, sinLugares, dia.atTime(11, 0), 0));
        lista.add(instancia(3L, noVerificada, dia.atTime(12, 0), 5));
        lista.add(instancia(4L, otroDepto, dia.atTime(9, 0), 5));
        when(instancias.findByDateTimeBetween(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(lista);

        List<Map<String, Object>> resultado = service.getActivitiesByDay(dia.toString(), "Rocha", null);

        assertEquals(1, resultado.size());
        assertEquals("Kayak", resultado.get(0).get("name"));
    }

    @Test
    void actividadesDelDiaSeOrdenanPorHorario() {
        LocalDate dia = LocalDate.now().plusDays(1);
        Atraccion a = atraccion(1L, "Tarde", "Rocha", verificado);
        Atraccion b = atraccion(2L, "Mañana", "Rocha", verificado);
        List<ActivityInstance> lista = new ArrayList<ActivityInstance>();
        lista.add(instancia(1L, a, dia.atTime(16, 0), 3));
        lista.add(instancia(2L, b, dia.atTime(8, 0), 3));
        when(instancias.findByDateTimeBetween(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(lista);

        List<Map<String, Object>> resultado = service.getActivitiesByDay(dia.toString(), null, null);

        assertEquals("Mañana", resultado.get(0).get("name"));
        assertEquals("Tarde", resultado.get(1).get("name"));
    }

    @Test
    void actividadesDelDiaExigenUnaFechaValida() {
        assertEquals(400, codigo(assertThrows(ResponseStatusException.class,
                () -> service.getActivitiesByDay(null, null, null))));
        assertEquals(400, codigo(assertThrows(ResponseStatusException.class,
                () -> service.getActivitiesByDay("28/11/2026", null, null))));
    }

    @Test
    void topRatedDevuelveLasTresMejoresDeCadaDepartamento() {
        Atraccion a1 = atraccion(1L, "A", "Rocha", verificado);
        Atraccion a2 = atraccion(2L, "B", "Rocha", verificado);
        Atraccion a3 = atraccion(3L, "C", "Rocha", verificado);
        Atraccion a4 = atraccion(4L, "D", "Rocha", verificado);
        Atraccion otro = atraccion(5L, "E", "Colonia", verificado);
        List<Review> lista = new ArrayList<Review>();
        lista.add(resena(a1, 3));
        lista.add(resena(a2, 5));
        lista.add(resena(a3, 4));
        lista.add(resena(a4, 2));
        lista.add(resena(otro, 4));
        when(resenas.findByCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(lista);

        List<Map<String, Object>> resultado = service.getTopRated(YearMonth.now().toString());

        assertEquals(2, resultado.size());
        Map<String, Object> colonia = resultado.get(0);
        Map<String, Object> rocha = resultado.get(1);
        assertEquals("Colonia", colonia.get("department"));
        assertEquals("Rocha", rocha.get("department"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> top = (List<Map<String, Object>>) rocha.get("activities");
        assertEquals(3, top.size());
        assertEquals("B", top.get(0).get("name"));
        assertEquals("C", top.get(1).get("name"));
        assertEquals("A", top.get(2).get("name"));
    }

    @Test
    void topRatedPromediaLasResenasDeUnaMismaActividad() {
        Atraccion a = atraccion(1L, "A", "Rocha", verificado);
        List<Review> lista = new ArrayList<Review>();
        lista.add(resena(a, 5));
        lista.add(resena(a, 4));
        when(resenas.findByCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(lista);

        List<Map<String, Object>> resultado = service.getTopRated(null);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> top = (List<Map<String, Object>>) resultado.get(0).get("activities");
        assertEquals(4.5, top.get(0).get("average"));
        assertEquals(2L, top.get(0).get("reviews"));
    }

    @Test
    void topRatedIgnoraActividadesDeEmprendimientosNoVerificados() {
        Atraccion a = atraccion(1L, "A", "Rocha", pendiente);
        List<Review> lista = new ArrayList<Review>();
        lista.add(resena(a, 5));
        when(resenas.findByCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(lista);

        assertTrue(service.getTopRated(null).isEmpty());
    }

    @Test
    void topRatedExigeUnMesValido() {
        ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> service.getTopRated("noviembre"));

        assertEquals(400, codigo(e));
    }

    @Test
    void buscaPorCoincidenciaEnNombreODescripcionSinMirarMayusculas() {
        Atraccion kayak = atraccion(1L, "Kayak Santa Lucía", "Canelones", verificado);
        Atraccion vinos = atraccion(2L, "Viñedos", "Colonia", verificado);
        vinos.setDescription("Degustación junto al río con kayak opcional");
        Atraccion otra = atraccion(3L, "Cabalgata", "Tacuarembó", verificado);
        Atraccion oculta = atraccion(4L, "Kayak oculto", "Rocha", pendiente);
        List<Atraccion> todas = new ArrayList<Atraccion>();
        todas.add(kayak);
        todas.add(vinos);
        todas.add(otra);
        todas.add(oculta);
        when(atracciones.findAll()).thenReturn(todas);

        List<Map<String, Object>> resultado = service.search("KAYAK");

        assertEquals(2, resultado.size());
    }

    @Test
    void laBusquedaNecesitaAlMenosDosCaracteres() {
        assertEquals(400, codigo(assertThrows(ResponseStatusException.class, () -> service.search(null))));
        assertEquals(400, codigo(assertThrows(ResponseStatusException.class, () -> service.search("k"))));
    }
}
