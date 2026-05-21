package ddtank.model;

import jakarta.persistence.*;

@Entity
@Table(name = "pve_info")
public class PveInfo {

    @Id
    private Long id; // ID estático da Instância/Masmorra

    @Column(nullable = false)
    private String name;

    private String description;
    private int minLevel = 1;       // Nível mínimo exigido para entrar
    private int totalStages = 1;    // Quantidade de telas/fases que a masmorra possui

    public PveInfo() {}

    public PveInfo(Long id, String name, String description, int minLevel, int totalStages) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.minLevel = minLevel;
        this.totalStages = totalStages;
    }

    // Getters e Setters
    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getMinLevel() { return minLevel; }
    public void setMinLevel(int minLevel) { this.minLevel = minLevel; }
    public int getTotalStages() { return totalStages; }
    public void setTotalStages(int totalStages) { this.totalStages = totalStages; }
}
