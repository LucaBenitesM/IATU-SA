package uy.edu.um.proytic2026r.control;

import uy.edu.um.proytic2026r.entities.ActivityInstance;
import uy.edu.um.proytic2026r.servicio.ActivityInstanceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/instances")
public class ActivityInstanceControl {

    private final ActivityInstanceService activityInstanceService;

    public ActivityInstanceControl(ActivityInstanceService activityInstanceService) {
        this.activityInstanceService = activityInstanceService;
    }

    @PostMapping
    public ResponseEntity<ActivityInstance> crearInstancia(@RequestBody ActivityInstance instance) {
        return ResponseEntity.status(HttpStatus.CREATED).body(activityInstanceService.createInstance(instance));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ActivityInstance> obtenerInstancia(@PathVariable Long id) {
        return activityInstanceService.getInstanceById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Lista las instancias/horarios de una atraccion concreta: /api/instances/attraction/5
    @GetMapping("/attraction/{atraccionId}")
    public ResponseEntity<List<ActivityInstance>> listarPorAtraccion(@PathVariable Long atraccionId) {
        return ResponseEntity.ok(activityInstanceService.getInstancesByAtraccion(atraccionId));
    }
}
