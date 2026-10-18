package uy.edu.um.proytic2026r.servicio;

import uy.edu.um.proytic2026r.entities.ActivityInstance;
import uy.edu.um.proytic2026r.repositorio.ActivityInstanceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
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

    public Optional<ActivityInstance> getInstanceById(Long id) {
        return activityInstanceRepository.findById(id);
    }
}