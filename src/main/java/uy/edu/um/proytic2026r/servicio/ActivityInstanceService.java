package uy.edu.um.proytic2026r.servicio;

import uy.edu.um.proytic2026r.entities.ActivityInstance;
import uy.edu.um.proytic2026r.repositorio.ActivityInstanceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ActivityInstanceService {

    @Autowired
    private ActivityInstanceRepository activityInstanceRepository;

    public ActivityInstance createInstance(ActivityInstance instance) {
        // Al crear la instancia, el cupo disponible arranca siendo igual a la capacidad máxima
        if (instance.getAvailablePlaces() == null) {
            instance.setAvailablePlaces(instance.getMaxCapacity());
        }
        return activityInstanceRepository.save(instance);
    }

    public List<ActivityInstance> getInstancesByAtraccion(Long atraccionId) {
        return activityInstanceRepository.findByAtraccionId(atraccionId);
    }

    // Fechas futuras con lugares libres de todas las atracciones (para el filtro por fecha)
    public List<Map<String, Object>> getUpcoming() {
        List<Map<String, Object>> resultado = new ArrayList<Map<String, Object>>();
        for (ActivityInstance i : activityInstanceRepository.findByDateTimeAfter(LocalDateTime.now())) {
            if (i.getAvailablePlaces() != null && i.getAvailablePlaces() > 0) {
                Map<String, Object> fila = new HashMap<String, Object>();
                fila.put("atraccionId", i.getAtraccion().getId());
                fila.put("dateTime", i.getDateTime());
                fila.put("availablePlaces", i.getAvailablePlaces());
                resultado.add(fila);
            }
        }
        return resultado;
    }

    public Optional<ActivityInstance> getInstanceById(Long id) {
        return activityInstanceRepository.findById(id);
    }
}