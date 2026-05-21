package ddtank.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_marry")
public class UserMarry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "husband_id", nullable = false)
    private Player husband;

    @OneToOne
    @JoinColumn(name = "wife_id", nullable = false)
    private Player wife;

    private boolean isDivorced = false;
    private LocalDateTime marryDate = LocalDateTime.now();

    public UserMarry() {}

    public UserMarry(Player husband, Player wife) {
        this.husband = husband;
        this.wife = wife;
    }

    // Getters e Setters
    public Long getId() { return id; }
    public Player getHusband() { return husband; }
    public void setHusband(Player husband) { this.husband = husband; }
    public Player getWife() { return wife; }
    public void setWife(Player wife) { this.wife = wife; }
    public boolean isDivorced() { return isDivorced; }
    public void setDivorced(boolean divorced) { this.isDivorced = divorced; }
    public LocalDateTime getMarryDate() { return marryDate; }
}
