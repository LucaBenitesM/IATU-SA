package uy.edu.um.proytic2026r.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

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

    // Categoria de la actividad (RF-12) y equipamiento estandarizado (RF-03/RF-17)
    @ManyToOne
    @JoinColumn(name = "categoria_id")
    private Category category;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "atraccion_equipo_provisto",
            joinColumns = @JoinColumn(name = "atraccion_id"),
            inverseJoinColumns = @JoinColumn(name = "tipo_equipamiento_id"))
    private Set<EquipmentType> providedEquipment = new LinkedHashSet<EquipmentType>();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "atraccion_equipo_requerido",
            joinColumns = @JoinColumn(name = "atraccion_id"),
            inverseJoinColumns = @JoinColumn(name = "tipo_equipamiento_id"))
    private Set<EquipmentType> requiredEquipment = new LinkedHashSet<EquipmentType>();

    // Imagen opcional de la actividad (base64). No se envia en el JSON de la actividad:
    // se pide aparte en GET /api/attractions/{id}/image
    @JsonIgnore
    @Column(columnDefinition = "TEXT")
    private String imageData;

    @JsonIgnore
    private String imageType;

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

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public Set<EquipmentType> getProvidedEquipment() { return providedEquipment; }
    public void setProvidedEquipment(Set<EquipmentType> providedEquipment) { this.providedEquipment = providedEquipment; }
    public Set<EquipmentType> getRequiredEquipment() { return requiredEquipment; }
    public void setRequiredEquipment(Set<EquipmentType> requiredEquipment) { this.requiredEquipment = requiredEquipment; }

    @JsonIgnore
    public String getImageData() { return imageData; }
    @JsonIgnore
    public void setImageData(String imageData) { this.imageData = imageData; }
    @JsonIgnore
    public String getImageType() { return imageType; }
    @JsonIgnore
    public void setImageType(String imageType) { this.imageType = imageType; }

    // Aparece en el JSON como "hasImage" para que la pagina sepa si hay foto
    public boolean getHasImage() { return imageData != null && !imageData.isEmpty(); }
}
