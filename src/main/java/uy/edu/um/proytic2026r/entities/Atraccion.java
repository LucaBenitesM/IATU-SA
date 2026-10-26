package uy.edu.um.proytic2026r.entities;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "atracciones")
public class Atraccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(length = 2000)
    private String description;

    private String department;
    private String locality;
    private Integer durationMinutes;
    private BigDecimal price;
    private Integer minimumAge;
    private String requirements;
    private Boolean allowsCancellation;
    private Integer cancellationDeadlineMinutes;
    private Integer refundPercentage;
    @ManyToOne
    @JoinColumn(name = "prestador_id")
    private Prestador prestador;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getLocality() { return locality; }
    public void setLocality(String locality) { this.locality = locality; }
    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Integer getMinimumAge() { return minimumAge; }
    public void setMinimumAge(Integer minimumAge) { this.minimumAge = minimumAge; }
    public String getRequirements() { return requirements; }
    public void setRequirements(String requirements) { this.requirements = requirements; }
    public Boolean getAllowsCancellation() { return allowsCancellation; }
    public void setAllowsCancellation(Boolean allowsCancellation) { this.allowsCancellation = allowsCancellation; }
    public Integer getCancellationDeadlineMinutes() { return cancellationDeadlineMinutes; }
    public void setCancellationDeadlineMinutes(Integer v) { this.cancellationDeadlineMinutes = v; }
    public Integer getRefundPercentage() { return refundPercentage; }
    public void setRefundPercentage(Integer refundPercentage) { this.refundPercentage = refundPercentage; }
    public Prestador getPrestador() { return prestador; }
    public void setPrestador(Prestador prestador) { this.prestador = prestador; }
}
