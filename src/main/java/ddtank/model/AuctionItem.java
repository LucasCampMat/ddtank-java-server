package ddtank.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "auction_house")
public class AuctionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "seller_id", nullable = false)
    private Player seller;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "user_item_id", nullable = false)
    private UserItem item; // O item físico que está preso no leilão

    private int priceGold = 0;
    private int priceMoney = 0;
    private LocalDateTime expiryTime = LocalDateTime.now().plusDays(2); // Oferta dura 48h

    public AuctionItem() {}

    public AuctionItem(Player seller, UserItem item, int priceGold, int priceMoney) {
        this.seller = seller;
        this.item = item;
        this.priceGold = priceGold;
        this.priceMoney = priceMoney;
    }

    // Getters e Setters
    public Long getId() { return id; }
    public Player getSeller() { return seller; }
    public UserItem getItem() { return item; }
    public int getPriceGold() { return priceGold; }
    public int getPriceMoney() { return priceMoney; }
    public LocalDateTime getExpiryTime() { return expiryTime; }
}
