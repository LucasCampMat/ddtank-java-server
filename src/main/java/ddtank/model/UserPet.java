package ddtank.model;

import jakarta.persistence.*;

@Entity
@Table(name = "user_pets")
public class UserPet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @Column(nullable = false)
    private int templateId; // O ID visual/tipo do pet (Ex: 1 = Broto de Flor, 2 = Pinto)

    private String petName;
    private int level = 1;
    private int exp = 0;
    private int hunger = 100; // Nível de fome (0 a 100)
    private boolean isEquipped = false; // Se este é o pet ativo na batalha

    // Bônus fixos que o pet concede ao jogador
    private int bonusAttack = 10;
    private int bonusDefense = 10;

    public UserPet() {}

    public UserPet(Player player, int templateId, String petName) {
        this.player = player;
        this.templateId = templateId;
        this.petName = petName;
    }

    // Getters e Setters
    public Long getId() { return id; }
    public Player getPlayer() { return player; }
    public void setPlayer(Player player) { this.player = player; }
    public int getTemplateId() { return templateId; }
    public void setTemplateId(int templateId) { this.templateId = templateId; }
    public String getPetName() { return petName; }
    public void setPetName(String petName) { this.petName = petName; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public int getExp() { return exp; }
    public void setExp(int exp) { this.exp = exp; }
    public int getHunger() { return hunger; }
    public void setHunger(int hunger) { this.hunger = hunger; }
    public boolean isEquipped() { return isEquipped; }
    public void setEquipped(boolean equipped) { this.isEquipped = equipped; }
    public int getBonusAttack() { return bonusAttack; }
    public void setBonusAttack(int bonusAttack) { this.bonusAttack = bonusAttack; }
    public int getBonusDefense() { return bonusDefense; }
    public void setBonusDefense(int bonusDefense) { this.bonusDefense = bonusDefense; }

    // ADICIONE ESTE MÉTODO ANTES DA ÚLTIMA CHAVE DE FECHAMENTO DO SEU USERPET.JAVA:
    /**
     * Alimenta o pet adicionando pontos de saciedade.
     * Limita o valor máximo ao teto clássico de 100%.
     */
    public void feed(int amount) {
        this.hunger += amount;
        if (this.hunger > 100) {
            this.hunger = 100;
        }
    }

}
