package uy.edu.um.proytic2026r.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "prestadores")
public class Prestador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(length = 2000)
    private String description;
    private String personType;
    private String rut;
    private String businessName;
    private String status;
    private String department;
    private String locality;
    private String phone;

    @Column(unique = true)
    private String email;
    @ManyToOne
    @JoinColumn(name = "owner_id")
    private Usuario owner;
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
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPersonType() { return personType; }
    public void setPersonType(String personType) { this.personType = personType; }
    public String getRut() { return rut; }
    public void setRut(String rut) { this.rut = rut; }
    public String getBusinessName() { return businessName; }
    public void setBusinessName(String businessName) { this.businessName = businessName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Usuario getOwner() { return owner; }
    public void setOwner(Usuario owner) { this.owner = owner; }
}
