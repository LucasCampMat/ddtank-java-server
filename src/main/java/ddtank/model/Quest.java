package ddtank.model;

import jakarta.persistence.*;

@Entity
@Table(name = "quests")
public class Quest {

    @Id
    private Long id; // ID estático da missão

    @Column(nullable = false)
    private String title;

    private String description;
    private int requirementCount = 1; // Quantas vitórias/ações são necessárias
    private int rewardGold = 500;    // Recompensa em moedas ao completar

    public Quest() {}

    public Quest(Long id, String title, String description, int requirementCount, int rewardGold) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.requirementCount = requirementCount;
        this.rewardGold = rewardGold;
    }

    // Getters e Setters
    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public int getRequirementCount() { return requirementCount; }
    public int getRewardGold() { return rewardGold; }
}
