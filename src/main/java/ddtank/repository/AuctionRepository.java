package ddtank.repository;

import ddtank.model.AuctionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuctionRepository extends JpaRepository<AuctionItem, Long> {
    // Fornece métodos nativos para listar o mercado global de itens à venda
}
