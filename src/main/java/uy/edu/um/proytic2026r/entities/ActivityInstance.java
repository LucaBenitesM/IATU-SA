package uy.edu.um.proytic2026r.entities;



import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "instancias_actividades")
public class ActivityInstance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "atraccion_id", nullable = false)
    private Atraccion atraccion;

    private LocalDateTime dateTime; // Fecha y hora de inicio de la actividad
    private Integer maxCapacity;    // Cupo máximo de personas
    private Integer availablePlaces; // Lugares aún disponibles

    // Minutos previos al inicio hasta los cuales se permite reservar (0 = hasta la hora de inicio)
    private Integer minutesBeforeBookingDeadline;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Atraccion getAtraccion() { return atraccion; }
    public void setAtraccion(Atraccion atraccion) { this.atraccion = atraccion; }
    public LocalDateTime getDateTime() { return dateTime; }
    public void setDateTime(LocalDateTime dateTime) { this.dateTime = dateTime; }
    public Integer getMaxCapacity() { return maxCapacity; }
    public void setMaxCapacity(Integer maxCapacity) { this.maxCapacity = maxCapacity; }
    public Integer getAvailablePlaces() { return availablePlaces; }
    public void setAvailablePlaces(Integer availablePlaces) { this.availablePlaces = availablePlaces; }
    public Integer getMinutesBeforeBookingDeadline() { return minutesBeforeBookingDeadline; }
    public void setMinutesBeforeBookingDeadline(Integer minutesBeforeBookingDeadline) { this.minutesBeforeBookingDeadline = minutesBeforeBookingDeadline; }
}