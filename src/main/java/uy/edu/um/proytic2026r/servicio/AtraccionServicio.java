package uy.edu.um.proytic2026r.servicio;

import uy.edu.um.proytic2026r.entities.Atraccion;
import uy.edu.um.proytic2026r.entities.Category;
import uy.edu.um.proytic2026r.entities.EquipmentType;
import uy.edu.um.proytic2026r.entities.Prestador;
import uy.edu.um.proytic2026r.repositorio.AtraccionRepositorio;
import uy.edu.um.proytic2026r.repositorio.CategoryRepository;
import uy.edu.um.proytic2026r.repositorio.EquipmentTypeRepository;
import uy.edu.um.proytic2026r.repositorio.PrestadorRepositorio;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.List;
import java.util.Optional;

@Service
public class AtraccionServicio {

    private final AtraccionRepositorio atraccionrepositorio;
    private final PrestadorRepositorio prestadorRepositorio;
    private final CategoryRepository categoryRepository;
    private final EquipmentTypeRepository equipmentTypeRepository;

    public AtraccionServicio(AtraccionRepositorio atraccionrepositorio, PrestadorRepositorio prestadorRepositorio,
                             CategoryRepository categoryRepository, EquipmentTypeRepository equipmentTypeRepository) {
        this.atraccionrepositorio = atraccionrepositorio;
        this.prestadorRepositorio = prestadorRepositorio;
        this.categoryRepository = categoryRepository;
        this.equipmentTypeRepository = equipmentTypeRepository;
    }

    public Atraccion crearAtraccion(Atraccion a) {
        if (a.getPrestador() == null || a.getPrestador().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Falta el emprendimiento");
        }
        Prestador p = prestadorRepositorio.findById(a.getPrestador().getId()).orElse(null);
        if (p == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Emprendimiento no encontrado");
        }
        if (!"APPROVED".equals(p.getStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El emprendimiento todavía no fue aprobado");
        }
        if (Boolean.TRUE.equals(a.getAllowsCancellation())) {
            Integer d = a.getCancellationDeadlineMinutes();
            Integer r = a.getRefundPercentage();
            if (d == null || d < 0 || r == null || r < 0 || r > 100) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Indicá el límite de cancelación y un reembolso entre 0 y 100");
            }
        } else {
            a.setAllowsCancellation(false);
            a.setCancellationDeadlineMinutes(null);
            a.setRefundPercentage(null);
        }
        a.setPrestador(p);
        a.setCategory(resolverCategoria(a.getCategory()));
        Set<EquipmentType> provisto = resolverEquipos(a.getProvidedEquipment());
        Set<EquipmentType> requerido = resolverEquipos(a.getRequiredEquipment());
        for (EquipmentType p1 : provisto) {
            for (EquipmentType r1 : requerido) {
                if (p1.getId().equals(r1.getId())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Un mismo equipamiento no puede ser a la vez proporcionado y requerido.");
                }
            }
        }
        a.setProvidedEquipment(provisto);
        a.setRequiredEquipment(requerido);
        return atraccionrepositorio.save(a);
    }

    // La categoria es obligatoria al publicar una actividad (RF-12)
    private Category resolverCategoria(Category recibida) {
        if (recibida == null || recibida.getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Elegí una categoría para la actividad.");
        }
        Category c = categoryRepository.findById(recibida.getId()).orElse(null);
        if (c == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "La categoría no existe.");
        }
        return c;
    }

    private Set<EquipmentType> resolverEquipos(Set<EquipmentType> recibidos) {
        Set<EquipmentType> resueltos = new LinkedHashSet<EquipmentType>();
        if (recibidos == null) {
            return resueltos;
        }
        for (EquipmentType e : recibidos) {
            EquipmentType real = e == null || e.getId() == null ? null : equipmentTypeRepository.findById(e.getId()).orElse(null);
            if (real == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El tipo de equipamiento no existe.");
            }
            resueltos.add(real);
        }
        return resueltos;
    }

    public List<Atraccion> listarAtracciones() {
        return atraccionrepositorio.findAll();
    }

    public Optional<Atraccion> obtenerAtraccionPorId(Long id) {
        return atraccionrepositorio.findById(id);
    }

    public void guardarImagen(Long id, String contentType, String data) {
        Atraccion a = atraccionrepositorio.findById(id).orElse(null);
        if (a == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Actividad no encontrada");
        }
        if (contentType == null || data == null || data.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Falta la imagen");
        }
        if (!contentType.equals("image/jpeg") && !contentType.equals("image/png") && !contentType.equals("image/webp")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Formato no permitido. Usa JPG, PNG o WEBP");
        }
        if (data.length() > 4000000) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "La imagen es demasiado grande (maximo 3 MB)");
        }
        try {
            Base64.getDecoder().decode(data);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La imagen no es valida");
        }
        a.setImageType(contentType);
        a.setImageData(data);
        atraccionrepositorio.save(a);
    }
}
