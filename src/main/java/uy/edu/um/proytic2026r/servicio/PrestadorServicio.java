package uy.edu.um.proytic2026r.servicio;

import uy.edu.um.proytic2026r.entities.Prestador;
import uy.edu.um.proytic2026r.repositorio.PrestadorRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Optional;

@Service
public class PrestadorServicio {

    private final PrestadorRepositorio prestadorRepositorio;

    public PrestadorServicio(PrestadorRepositorio prestadorRepositorio) {
        this.prestadorRepositorio = prestadorRepositorio;
    }

    public Prestador crearPrestador(Prestador prestador) {
        if (prestador.getOwner() == null || prestador.getOwner().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Falta el usuario dueño del emprendimiento");
        }
        if ("LEGAL".equals(prestador.getPersonType())
                && (isBlank(prestador.getRut()) || isBlank(prestador.getBusinessName()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Una persona jurídica necesita RUT y razón social");
        }
        prestador.setStatus("PENDING");
        return prestadorRepositorio.save(prestador);
    }
    public Prestador cambiarEstado(Long id, String status) {
        Prestador p = prestadorRepositorio.findById(id).orElse(null);
        if (p == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Prestador no encontrado");
        }
        p.setStatus(status);
        return prestadorRepositorio.save(p);
    }
    public List<Prestador> listarPrestadores() {
        return prestadorRepositorio.findAll();
    }
    private boolean isBlank(String s) { return s == null || s.isBlank(); }

    public Optional<Prestador> obtenerPrestadorPorId(Long id) {
        return prestadorRepositorio.findById(id);
    }
}
