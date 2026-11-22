package uy.edu.um.proytic2026r.entities;

import jakarta.persistence.*;

// Tipo de equipamiento estandarizado (por ejemplo: Casco, Chaleco salvavidas). Lo administra el admin.
@Entity
@Table(name = "tipos_equipamiento")
public class EquipmentType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String name;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
