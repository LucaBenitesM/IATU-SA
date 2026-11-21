package uy.edu.um.proytic2026r.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "resenas")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Una reserva solo puede tener una reseña
    @JsonIgnore
    @OneToOne
    @JoinColumn(name = "reserva_id", nullable = false, unique = true)
    private Booking booking;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario tourist;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "atraccion_id", nullable = false)
    private Atraccion atraccion;

    private Integer rating; // de 1 a 5

    @Column(length = 1000)
    private String comment;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt; // null si nunca se editó

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Booking getBooking() { return booking; }
    public void setBooking(Booking booking) { this.booking = booking; }
    public Usuario getTourist() { return tourist; }
    public void setTourist(Usuario tourist) { this.tourist = tourist; }
    public Atraccion getAtraccion() { return atraccion; }
    public void setAtraccion(Atraccion atraccion) { this.atraccion = atraccion; }
    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // Datos del JSON público: no se expone el email del turista
    public Long getBookingId() {
        return booking != null ? booking.getId() : null;
    }

    public Long getAtraccionId() {
        return atraccion != null ? atraccion.getId() : null;
    }

    public Long getTouristId() {
        return tourist != null ? tourist.getId() : null;
    }

    public String getTouristName() {
        if (tourist == null) {
            return null;
        }
        String apellido = tourist.getLastName();
        if (apellido == null || apellido.isEmpty()) {
            return tourist.getFirstName();
        }
        return tourist.getFirstName() + " " + apellido.charAt(0) + ".";
    }
}
