package uy.edu.um.proytic2026r.servicio;

import uy.edu.um.proytic2026r.entities.ActivityInstance;
import uy.edu.um.proytic2026r.entities.Atraccion;
import uy.edu.um.proytic2026r.entities.Prestador;
import uy.edu.um.proytic2026r.entities.Review;
import uy.edu.um.proytic2026r.repositorio.ActivityInstanceRepository;
import uy.edu.um.proytic2026r.repositorio.AtraccionRepositorio;
import uy.edu.um.proytic2026r.repositorio.PrestadorRepositorio;
import uy.edu.um.proytic2026r.repositorio.ReviewRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

// Datos para terceros (RF-19 a RF-23). Solo lectura y sin datos privados:
// no se devuelven emails de usuarios, contrasenas ni reservas.
@Service
public class PublicApiService {

    private static final String VERIFICADO = "APPROVED";

    private final PrestadorRepositorio prestadores;
    private final AtraccionRepositorio atracciones;
    private final ActivityInstanceRepository instancias;
    private final ReviewRepository resenas;

    public PublicApiService(PrestadorRepositorio prestadores, AtraccionRepositorio atracciones,
                            ActivityInstanceRepository instancias, ReviewRepository resenas) {
        this.prestadores = prestadores;
        this.atracciones = atracciones;
        this.instancias = instancias;
        this.resenas = resenas;
    }

    // RF-19: emprendimientos registrados y verificados
    public List<Map<String, Object>> getVerifiedProviders() {
        List<Map<String, Object>> lista = new ArrayList<Map<String, Object>>();
        for (Prestador p : prestadores.findByStatus(VERIFICADO)) {
            Map<String, Object> fila = new LinkedHashMap<String, Object>();
            fila.put("id", p.getId());
            fila.put("name", p.getName());
            fila.put("description", p.getDescription());
            fila.put("personType", p.getPersonType());
            fila.put("department", p.getDepartment());
            fila.put("locality", p.getLocality());
            fila.put("phone", p.getPhone());
            fila.put("email", p.getEmail());
            lista.add(fila);
        }
        return lista;
    }

    // RF-20: actividades con lugares libres en un dia, para un departamento o localidad
    public List<Map<String, Object>> getActivitiesByDay(String fecha, String departamento, String localidad) {
        if (fecha == null || fecha.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Falta la fecha (date=AAAA-MM-DD).");
        }
        LocalDate dia;
        try {
            dia = LocalDate.parse(fecha.trim());
        } catch (DateTimeParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha debe tener el formato AAAA-MM-DD.");
        }
        List<Map<String, Object>> lista = new ArrayList<Map<String, Object>>();
        for (ActivityInstance i : instancias.findByDateTimeBetween(dia.atStartOfDay(), dia.plusDays(1).atStartOfDay())) {
            Atraccion a = i.getAtraccion();
            if (!esPublica(a) || i.getAvailablePlaces() == null || i.getAvailablePlaces() <= 0) {
                continue;
            }
            if (!coincide(a.getDepartment(), departamento) || !coincide(a.getLocality(), localidad)) {
                continue;
            }
            Map<String, Object> fila = resumen(a);
            fila.put("instanceId", i.getId());
            fila.put("dateTime", i.getDateTime());
            fila.put("availablePlaces", i.getAvailablePlaces());
            lista.add(fila);
        }
        Collections.sort(lista, new Comparator<Map<String, Object>>() {
            @Override
            public int compare(Map<String, Object> x, Map<String, Object> y) {
                return ((LocalDateTime) x.get("dateTime")).compareTo((LocalDateTime) y.get("dateTime"));
            }
        });
        return lista;
    }

    // RF-21: las 3 actividades mejor valoradas del mes en cada departamento
    public List<Map<String, Object>> getTopRated(String mes) {
        YearMonth periodo;
        try {
            periodo = (mes == null || mes.trim().isEmpty()) ? YearMonth.now() : YearMonth.parse(mes.trim());
        } catch (DateTimeParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El mes debe tener el formato AAAA-MM.");
        }
        List<Review> delMes = resenas.findByCreatedAtBetween(
                periodo.atDay(1).atStartOfDay(), periodo.plusMonths(1).atDay(1).atStartOfDay());

        Map<Long, double[]> acumulado = new HashMap<Long, double[]>();
        Map<Long, Atraccion> porId = new HashMap<Long, Atraccion>();
        for (Review r : delMes) {
            Atraccion a = r.getAtraccion();
            if (a == null || !esPublica(a)) {
                continue;
            }
            double[] suma = acumulado.get(a.getId());
            if (suma == null) {
                suma = new double[] {0, 0};
                acumulado.put(a.getId(), suma);
                porId.put(a.getId(), a);
            }
            suma[0] += r.getRating();
            suma[1] += 1;
        }

        Map<String, List<Map<String, Object>>> porDepartamento = new TreeMap<String, List<Map<String, Object>>>();
        for (Map.Entry<Long, double[]> e : acumulado.entrySet()) {
            Atraccion a = porId.get(e.getKey());
            String dep = a.getDepartment() == null ? "Sin departamento" : a.getDepartment();
            Map<String, Object> fila = resumen(a);
            fila.put("average", Double.valueOf(Math.round(e.getValue()[0] / e.getValue()[1] * 10.0) / 10.0));
            fila.put("reviews", Long.valueOf((long) e.getValue()[1]));
            List<Map<String, Object>> grupo = porDepartamento.get(dep);
            if (grupo == null) {
                grupo = new ArrayList<Map<String, Object>>();
                porDepartamento.put(dep, grupo);
            }
            grupo.add(fila);
        }

        List<Map<String, Object>> resultado = new ArrayList<Map<String, Object>>();
        for (Map.Entry<String, List<Map<String, Object>>> e : porDepartamento.entrySet()) {
            List<Map<String, Object>> grupo = e.getValue();
            Collections.sort(grupo, new Comparator<Map<String, Object>>() {
                @Override
                public int compare(Map<String, Object> x, Map<String, Object> y) {
                    int c = Double.compare((Double) y.get("average"), (Double) x.get("average"));
                    if (c != 0) {
                        return c;
                    }
                    return Long.compare((Long) y.get("reviews"), (Long) x.get("reviews"));
                }
            });
            List<Map<String, Object>> top = new ArrayList<Map<String, Object>>();
            for (int i = 0; i < grupo.size() && i < 3; i++) {
                grupo.get(i).put("rank", Integer.valueOf(i + 1));
                top.add(grupo.get(i));
            }
            Map<String, Object> fila = new LinkedHashMap<String, Object>();
            fila.put("department", e.getKey());
            fila.put("month", periodo.toString());
            fila.put("activities", top);
            resultado.add(fila);
        }
        return resultado;
    }

    // RF-22: estado de verificacion de un emprendimiento
    public Map<String, Object> getVerification(Long id) {
        Prestador p = prestadores.findById(id).orElse(null);
        if (p == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El emprendimiento no existe.");
        }
        Map<String, Object> fila = new LinkedHashMap<String, Object>();
        fila.put("id", p.getId());
        fila.put("name", p.getName());
        fila.put("status", p.getStatus());
        fila.put("verified", VERIFICADO.equals(p.getStatus()));
        return fila;
    }

    // RF-23: busqueda por coincidencia de texto en nombre y descripcion
    public List<Map<String, Object>> search(String texto) {
        if (texto == null || texto.trim().length() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La búsqueda necesita al menos 2 caracteres (q=...).");
        }
        String buscado = texto.trim().toLowerCase();
        List<Map<String, Object>> lista = new ArrayList<Map<String, Object>>();
        for (Atraccion a : atracciones.findAll()) {
            if (!esPublica(a)) {
                continue;
            }
            String nombre = a.getName() == null ? "" : a.getName().toLowerCase();
            String descripcion = a.getDescription() == null ? "" : a.getDescription().toLowerCase();
            if (nombre.contains(buscado) || descripcion.contains(buscado)) {
                lista.add(resumen(a));
            }
        }
        return lista;
    }

    // Solo se publican actividades de emprendimientos verificados
    private boolean esPublica(Atraccion a) {
        return a != null && a.getPrestador() != null && VERIFICADO.equals(a.getPrestador().getStatus());
    }

    private boolean coincide(String valor, String filtro) {
        if (filtro == null || filtro.trim().isEmpty()) {
            return true;
        }
        return valor != null && valor.trim().equalsIgnoreCase(filtro.trim());
    }

    private Map<String, Object> resumen(Atraccion a) {
        Map<String, Object> fila = new LinkedHashMap<String, Object>();
        fila.put("attractionId", a.getId());
        fila.put("name", a.getName());
        fila.put("description", a.getDescription());
        fila.put("category", a.getCategory() == null ? null : a.getCategory().getName());
        fila.put("department", a.getDepartment());
        fila.put("locality", a.getLocality());
        fila.put("price", a.getPrice());
        fila.put("durationMinutes", a.getDurationMinutes());
        fila.put("minimumAge", a.getMinimumAge());
        fila.put("providerId", a.getPrestador().getId());
        fila.put("providerName", a.getPrestador().getName());
        return fila;
    }
}
