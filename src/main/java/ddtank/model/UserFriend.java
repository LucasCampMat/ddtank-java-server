package ddtank.model;

import jakarta.persistence.*;

@Entity
@Table(name = "user_friends")
public class UserFriend {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // O jogador que é dono da lista de amigos
    @ManyToOne
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    // O ID do amigo que foi adicionado
    @ManyToOne
    @JoinColumn(name = "friend_id", nullable = false)
    private Player friend;

    public UserFriend() {}

    public UserFriend(Player player, Player friend) {
        this.player = player;
        this.friend = friend;
    }

    public Long getId() { return id; }
    public Player getPlayer() { return player; }
    public void setPlayer(Player player) { this.player = player; }
    public Player getFriend() { return friend; }
    public void setFriend(Player friend) { this.friend = friend; }
}
