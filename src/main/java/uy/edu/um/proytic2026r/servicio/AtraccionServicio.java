package uy.edu.um.proytic2026r.servicio;

import uy.edu.um.proytic2026r.entities.Atraccion;
import uy.edu.um.proytic2026r.entities.Prestador;
import uy.edu.um.proytic2026r.repositorio.AtraccionRepositorio;
import uy.edu.um.proytic2026r.repositorio.PrestadorRepositorio;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
public class AtraccionServicio {

    private final AtraccionRepositorio atraccionrepositorio;
    private final PrestadorRepositorio prestadorRepositorio;

    public AtraccionServicio(AtraccionRepositorio atraccionrepositorio, PrestadorRepositorio prestadorRepositorio) {
        this.atraccionrepositorio = atraccionrepositorio;
        this.prestadorRepositorio = prestadorRepositorio;
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
        return atraccionrepositorio.save(a);
    }

    public List<Atraccion> listarAtracciones() {
        return atraccionrepositorio.findAll();
    }

    public Optional<Atraccion> obtenerAtraccionPorId(Long id) {
        return atraccionrepositorio.findById(id);
    }
}
