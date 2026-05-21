package ddtank.repository;

import ddtank.model.Player;
import ddtank.model.UserItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface UserItemRepository extends JpaRepository<UserItem, Long> {
    // Busca todos os itens que pertencem a um jogador específico para carregar na mochila
    List<UserItem> findByPlayer(Player player);
}
