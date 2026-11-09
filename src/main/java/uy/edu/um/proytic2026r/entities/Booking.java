package uy.edu.um.proytic2026r.entities;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "reservas")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario tourist; // Turista que realiza la reserva

    @ManyToOne
    @JoinColumn(name = "activity_instance_id", nullable = false)
    private ActivityInstance activityInstance; // Instancia/horario reservado

    private Integer participantsCount; // Cantidad de lugares reservados
    private BigDecimal totalPrice;     // Precio total calculado
    private LocalDateTime bookingDate;  // Fecha/hora en que se hizo la reserva

    @Enumerated(EnumType.STRING)
    private BookingStatus status;
    private BigDecimal refundAmount;// Estado de la reserva

    public enum BookingStatus {
        PENDING_PAYMENT,
        CONFIRMED,
        CANCELLED,
        COMPLETED
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Usuario getTourist() { return tourist; }
    public void setTourist(Usuario tourist) { this.tourist = tourist; }
    public ActivityInstance getActivityInstance() { return activityInstance; }
    public void setActivityInstance(ActivityInstance activityInstance) { this.activityInstance = activityInstance; }
    public Integer getParticipantsCount() { return participantsCount; }
    public void setParticipantsCount(Integer participantsCount) { this.participantsCount = participantsCount; }
    public BigDecimal getTotalPrice() { return totalPrice; }
    public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }
    public LocalDateTime getBookingDate() { return bookingDate; }
    public void setBookingDate(LocalDateTime bookingDate) { this.bookingDate = bookingDate; }
    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }
    public BigDecimal getRefundAmount() { return refundAmount; }
    public void setRefundAmount(BigDecimal refundAmount) { this.refundAmount = refundAmount; }
}