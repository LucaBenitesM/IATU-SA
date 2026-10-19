package uy.edu.um.proytic2026r.servicio;

import uy.edu.um.proytic2026r.entities.Prestador;
import uy.edu.um.proytic2026r.repositorio.PrestadorRepositorio;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PrestadorServicio {

    private final PrestadorRepositorio prestadorRepositorio;

    public PrestadorServicio(PrestadorRepositorio prestadorRepositorio) {
        this.prestadorRepositorio = prestadorRepositorio;
    }

    public Prestador crearPrestador(Prestador prestador) {
        return prestadorRepositorio.save(prestador);
    }

    public List<Prestador> listarPrestadores() {
        return prestadorRepositorio.findAll();
    }

    public Optional<Prestador> obtenerPrestadorPorId(Long id) {
        return prestadorRepositorio.findById(id);
    }
}
