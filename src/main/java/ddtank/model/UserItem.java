package ddtank.model;

import jakarta.persistence.*;

@Entity
@Table(name = "user_items")
public class UserItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Vincula o item ao ID do jogador dono dele no banco de dados
    @ManyToOne
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @Column(nullable = false)
    private int templateId; // O ID visual do item no DDTank (Ex: 1001 para Quebra-Tijolo)

    private int place; // O número do slot dentro da mochila (0, 1, 2...)
    private int count = 1; // Quantidade do item (acumulável)
    private int attackCompose = 0; // Nível de pérola/composição de Ataque
    private int defenseCompose = 0; // Nível de Defesa
    private int agilityCompose = 0; // Nível de Agilidade
    private int luckCompose = 0; // Nível de Sorte
    private int strengthenLevel = 0; // Nível de Fortalecimento (+1, +2... +12)

    public UserItem() {}

    public UserItem(Player player, int templateId, int place) {
        this.player = player;
        this.templateId = templateId;
        this.place = place;
    }

    // Getters e Setters
    public Long getId() { return id; }
    public Player getPlayer() { return player; }
    public void setPlayer(Player player) { this.player = player; }
    public int getTemplateId() { return templateId; }
    public void setTemplateId(int templateId) { this.templateId = templateId; }
    public int getPlace() { return place; }
    public void setPlace(int place) { this.place = place; }
    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }
    public int getAttackCompose() { return attackCompose; }
    public void setAttackCompose(int attackCompose) { this.attackCompose = attackCompose; }
    public int getDefenseCompose() { return defenseCompose; }
    public void setDefenseCompose(int defenseCompose) { this.defenseCompose = defenseCompose; }
    public int getAgilityCompose() { return agilityCompose; }
    public void setAgilityCompose(int agilityCompose) { this.agilityCompose = agilityCompose; }
    public int getLuckCompose() { return luckCompose; }
    public void setLuckCompose(int luckCompose) { this.luckCompose = luckCompose; }
    public int getStrengthenLevel() { return strengthenLevel; }
    public void setStrengthenLevel(int strengthenLevel) { this.strengthenLevel = strengthenLevel; }
}
