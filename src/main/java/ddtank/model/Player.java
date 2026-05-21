package ddtank.model;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    private String nickname;
    private int grade = 1;
    private int gp = 0;
    private int gold = 5000;
    private int money = 500;

    public Player() {}

    public Player(String username, String password, String nickname) {
        this.username = username;
        this.password = password;
        this.nickname = nickname;
    }

    // Getters e Setters
    public Long getId() { return id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public int getGrade() { return grade; }
    public void setGrade(int grade) { this.grade = grade; }
    public int getGp() { return gp; }
    public void setGp(int gp) { this.gp = gp; }
    public int getGold() { return gold; }
    public void setGold(int gold) { this.gold = gold; }
    public int getMoney() { return money; }
    public void setMoney(int money) { this.money = money; }
}