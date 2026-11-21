package uy.edu.um.proytic2026r.config;

import uy.edu.um.proytic2026r.entities.ActivityInstance;
import uy.edu.um.proytic2026r.entities.Atraccion;
import uy.edu.um.proytic2026r.entities.Booking;
import uy.edu.um.proytic2026r.entities.Prestador;
import uy.edu.um.proytic2026r.entities.Usuario;
import uy.edu.um.proytic2026r.repositorio.ActivityInstanceRepository;
import uy.edu.um.proytic2026r.repositorio.AtraccionRepositorio;
import uy.edu.um.proytic2026r.repositorio.BookingRepository;
import uy.edu.um.proytic2026r.repositorio.PrestadorRepositorio;
import uy.edu.um.proytic2026r.repositorio.UsuarioRepositorio;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

// Carga datos de ejemplo al arrancar, pero SOLO si la base de datos no tiene ningun usuario.
// Asi quien abre el proyecto con una base vacia encuentra un administrador, un emprendedor
// aprobado, atracciones con horarios y un turista para probar. En una base con datos no hace nada.
// Se puede apagar con app.datos-iniciales=false en application.properties.
@Component
public class DatosIniciales implements CommandLineRunner {

    private final UsuarioRepositorio usuarios;
    private final PrestadorRepositorio prestadores;
    private final AtraccionRepositorio atracciones;
    private final ActivityInstanceRepository instancias;
    private final BookingRepository reservas;

    @Value("${app.datos-iniciales:true}")
    private boolean activo;

    public DatosIniciales(UsuarioRepositorio usuarios, PrestadorRepositorio prestadores,
                          AtraccionRepositorio atracciones, ActivityInstanceRepository instancias,
                          BookingRepository reservas) {
        this.usuarios = usuarios;
        this.prestadores = prestadores;
        this.atracciones = atracciones;
        this.instancias = instancias;
        this.reservas = reservas;
    }

    @Override
    public void run(String... args) {
        if (!activo || usuarios.count() > 0) {
            return;
        }

        usuarios.save(usuario("Admin", "IATU", "admin@iatu.uy", "admin123", "ADMIN"));
        Usuario turista = usuarios.save(usuario("Ana", "Pérez", "ana@iatu.uy", "ana123", "TOURIST"));
        Usuario duenio = usuarios.save(usuario("Martín", "Techera", "martin@iatu.uy", "martin123", "PROVIDER"));

        Prestador valleEden = new Prestador();
        valleEden.setName("Valle Edén Aventuras");
        valleEden.setDescription("Actividades de campo y turismo rural en las sierras de Tacuarembó.");
        valleEden.setPersonType("PHYSICAL");
        valleEden.setStatus("APPROVED");
        valleEden.setDepartment("Tacuarembó");
        valleEden.setLocality("Valle Edén");
        valleEden.setPhone("099123456");
        valleEden.setEmail("contacto@valleeden.uy");
        valleEden.setOwner(duenio);
        valleEden = prestadores.save(valleEden);

        Prestador costa = new Prestador();
        costa.setName("Costa Viva S.R.L.");
        costa.setDescription("Paseos y deportes de playa en la costa de Rocha.");
        costa.setPersonType("LEGAL");
        costa.setRut("216543210018");
        costa.setBusinessName("Costa Viva Servicios Turísticos S.R.L.");
        costa.setStatus("APPROVED");
        costa.setDepartment("Rocha");
        costa.setLocality("La Paloma");
        costa.setPhone("094765432");
        costa.setEmail("hola@costaviva.uy");
        costa.setOwner(duenio);
        costa = prestadores.save(costa);

        Atraccion cabalgata = atraccion(valleEden, "Cabalgata por las sierras del Valle Edén",
                "Recorrido a caballo de dos horas por senderos serranos, con guía y parada para mate junto al arroyo.",
                "Tacuarembó", "Valle Edén", 120, "2700", 10, "Calzado cerrado y ropa cómoda.", true, 1440, 50);
        Atraccion trekking = atraccion(valleEden, "Trekking al Valle Edén con almuerzo serrano",
                "Caminata guiada de dificultad media con almuerzo típico incluido.",
                "Tacuarembó", "Villa Edén", 240, "2000", 12, "Buen estado físico y calzado de trekking.", true, 720, 50);
        Atraccion vinedo = atraccion(valleEden, "Visita a viñedo con degustación",
                "Recorrida por el viñedo, bodega y degustación de tres vinos.",
                "Tacuarembó", "Villa Edén", 90, "1800", 18, "Solo mayores de 18 años.", false, null, null);
        Atraccion kayak = atraccion(costa, "Kayak al atardecer en La Paloma",
                "Paseo en kayak por la costa al atardecer, con equipo e instructor.",
                "Rocha", "La Paloma", 120, "1500", 12, "Saber nadar.", true, 360, 100);
        Atraccion avistaje = atraccion(costa, "Avistaje de lobos marinos",
                "Salida en lancha hasta la isla de Lobos para ver la colonia.",
                "Rocha", "La Paloma", 150, "2200", 6, null, true, 1440, 80);

        Atraccion[] todas = new Atraccion[] {cabalgata, trekking, vinedo, kayak, avistaje};
        LocalDate hoy = LocalDate.now();
        for (Atraccion a : todas) {
            for (int dia = 2; dia <= 6; dia += 2) {
                instancias.save(instancia(a, hoy.plusDays(dia).atTime(LocalTime.of(10, 0)), 10, 10));
            }
        }

        // Una actividad que ya pasó, con reserva confirmada de la turista: sirve para probar las reseñas
        ActivityInstance pasada = instancias.save(instancia(cabalgata, hoy.minusDays(3).atTime(LocalTime.of(10, 0)), 10, 8));
        Booking reserva = new Booking();
        reserva.setTourist(turista);
        reserva.setActivityInstance(pasada);
        reserva.setParticipantsCount(2);
        reserva.setTotalPrice(cabalgata.getPrice().multiply(BigDecimal.valueOf(2)));
        reserva.setBookingDate(LocalDateTime.now().minusDays(7));
        reserva.setStatus(Booking.BookingStatus.CONFIRMED);
        reservas.save(reserva);

        System.out.println("Datos iniciales cargados: admin@iatu.uy / admin123, ana@iatu.uy / ana123, martin@iatu.uy / martin123");
    }

    private Usuario usuario(String nombre, String apellido, String email, String clave, String rol) {
        Usuario u = new Usuario();
        u.setFirstName(nombre);
        u.setLastName(apellido);
        u.setEmail(email);
        u.setPassword(clave);
        u.setRole(rol);
        return u;
    }

    private Atraccion atraccion(Prestador prestador, String nombre, String descripcion, String departamento,
                                String localidad, int minutos, String precio, Integer edadMinima,
                                String requisitos, boolean cancela, Integer plazoCancelacion, Integer reembolso) {
        Atraccion a = new Atraccion();
        a.setPrestador(prestador);
        a.setName(nombre);
        a.setDescription(descripcion);
        a.setDepartment(departamento);
        a.setLocality(localidad);
        a.setDurationMinutes(minutos);
        a.setPrice(new BigDecimal(precio));
        a.setMinimumAge(edadMinima);
        a.setRequirements(requisitos);
        a.setAllowsCancellation(cancela);
        a.setCancellationDeadlineMinutes(plazoCancelacion);
        a.setRefundPercentage(reembolso);
        return atracciones.save(a);
    }

    private ActivityInstance instancia(Atraccion a, LocalDateTime fecha, int capacidad, int libres) {
        ActivityInstance i = new ActivityInstance();
        i.setAtraccion(a);
        i.setDateTime(fecha);
        i.setMaxCapacity(capacidad);
        i.setAvailablePlaces(libres);
        i.setMinutesBeforeBookingDeadline(60);
        return i;
    }
}
