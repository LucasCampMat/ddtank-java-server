package ddtank.model;

import jakarta.persistence.*;

@Entity
@Table(name = "user_quests", uniqueConstraints = {@UniqueConstraint(columnNames = {"player_id", "quest_id"})})
public class UserQuest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @ManyToOne
    @JoinColumn(name = "quest_id", nullable = false)
    private Quest quest;

    private int currentProgress = 0; // Progresso atual (Ex: 0 vitórias)
    private boolean isCompleted = false;

    public UserQuest() {}

    public UserQuest(Player player, Quest quest) {
        this.player = player;
        this.quest = quest;
    }

    // Getters e Setters
    public Long getId() { return id; }
    public Player getPlayer() { return player; }
    public Quest getQuest() { return quest; }
    public int getCurrentProgress() { return currentProgress; }
    public void setCurrentProgress(int currentProgress) { this.currentProgress = currentProgress; }
    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { this.isCompleted = completed; }

    /**
     * Incrementa o progresso e valida se atingiu o requisito da missão
     */
    public boolean incrementProgress() {
        if (isCompleted) return false;

        this.currentProgress++;
        if (this.currentProgress >= quest.getRequirementCount()) {
            this.isCompleted = true;
            return true; // Missão Concluída!
        }
        return false;
    }
}
