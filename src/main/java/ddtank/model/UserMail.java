package ddtank.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_mails")
public class UserMail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "sender_id", nullable = true) // null se for mensagem automática do sistema
    private Player sender;

    @ManyToOne
    @JoinColumn(name = "receiver_id", nullable = false)
    private Player receiver;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    private int goldAnnex = 0;   // Moedas de Gold anexadas na carta
    private int moneyAnnex = 0;  // Moedas de Money anexadas na carta
    private boolean isRead = false;
    private LocalDateTime sentTime = LocalDateTime.now();

    public UserMail() {}

    public UserMail(Player sender, Player receiver, String title, String content) {
        this.sender = sender;
        this.receiver = receiver;
        this.title = title;
        this.content = content;
    }

    // Getters e Setters
    public Long getId() { return id; }
    public Player getSender() { return sender; }
    public void setSender(Player sender) { this.sender = sender; }
    public Player getReceiver() { return receiver; }
    public void setReceiver(Player receiver) { this.receiver = receiver; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public int getGoldAnnex() { return goldAnnex; }
    public void setGoldAnnex(int goldAnnex) { this.goldAnnex = goldAnnex; }
    public int getMoneyAnnex() { return moneyAnnex; }
    public void setMoneyAnnex(int moneyAnnex) { this.moneyAnnex = moneyAnnex; }
    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { this.isRead = read; }
    public LocalDateTime getSentTime() { return sentTime; }
}
