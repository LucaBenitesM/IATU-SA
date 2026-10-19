package uy.edu.um.proytic2026r.servicio;

import uy.edu.um.proytic2026r.entities.Atraccion;
import uy.edu.um.proytic2026r.repositorio.AtraccionRepositorio;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AtraccionServicio {

    private final AtraccionRepositorio atraccionrepositorio;

    public AtraccionServicio(AtraccionRepositorio atraccionrepositorio) {
        this.atraccionrepositorio = atraccionrepositorio;
    }

    public Atraccion crearAtraccion(Atraccion atraccion) {
        return atraccionrepositorio.save(atraccion);
    }

    public List<Atraccion> listarAtracciones() {
        return atraccionrepositorio.findAll();
    }

    public Optional<Atraccion> obtenerAtraccionPorId(Long id) {
        return atraccionrepositorio.findById(id);
    }
}
