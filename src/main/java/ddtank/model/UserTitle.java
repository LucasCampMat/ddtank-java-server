package ddtank.model;

import jakarta.persistence.*;

@Entity
@Table(name = "user_titles")
public class UserTitle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    private int titleId; // O ID visual do título (Ex: 1 = "Pioneiro", 2 = "DDTanker Lendário")
    private boolean isEquipped = false; // Se este título está selecionado para o avatar

    public UserTitle() {}

    public UserTitle(Player player, int titleId) {
        this.player = player;
        this.titleId = titleId;
    }

    // Getters e Setters
    public Long getId() { return id; }
    public Player getPlayer() { return player; }
    public void setPlayer(Player player) { this.player = player; }
    public int getTitleId() { return titleId; }
    public void setTitleId(int titleId) { this.titleId = titleId; }
    public boolean isEquipped() { return isEquipped; }
    public void setEquipped(boolean equipped) { this.isEquipped = equipped; }
}
