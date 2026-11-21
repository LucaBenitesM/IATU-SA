package uy.edu.um.proytic2026r.control;

import uy.edu.um.proytic2026r.servicio.PublicApiService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

// API publica para terceros: solo lectura, sin sesion y sin datos privados.
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/public")
public class PublicApiControl {

    private final PublicApiService servicio;

    public PublicApiControl(PublicApiService servicio) {
        this.servicio = servicio;
    }

    // RF-19
    @GetMapping("/providers")
    public List<Map<String, Object>> emprendimientos() {
        return servicio.getVerifiedProviders();
    }

    // RF-22
    @GetMapping("/providers/{id}/verification")
    public Map<String, Object> verificacion(@PathVariable Long id) {
        return servicio.getVerification(id);
    }

    // RF-20: /api/public/activities?date=2026-11-25&department=Rocha&locality=La Paloma
    @GetMapping("/activities")
    public List<Map<String, Object>> actividadesDelDia(@RequestParam(required = false) String date,
                                                       @RequestParam(required = false) String department,
                                                       @RequestParam(required = false) String locality) {
        return servicio.getActivitiesByDay(date, department, locality);
    }

    // RF-23: /api/public/activities/search?q=kayak
    @GetMapping("/activities/search")
    public List<Map<String, Object>> buscar(@RequestParam(required = false) String q) {
        return servicio.search(q);
    }

    // RF-21: /api/public/top-rated?month=2026-11 (por defecto, el mes actual)
    @GetMapping("/top-rated")
    public List<Map<String, Object>> mejorValoradas(@RequestParam(required = false) String month) {
        return servicio.getTopRated(month);
    }
}
