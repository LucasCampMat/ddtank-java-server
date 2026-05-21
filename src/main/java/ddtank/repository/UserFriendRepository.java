package ddtank.repository;

import ddtank.model.Player;
import ddtank.model.UserFriend;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface UserFriendRepository extends JpaRepository<UserFriend, Long> {
    // Busca todos os vínculos de amigos daquele jogador específico
    List<UserFriend> findByPlayer(Player player);
}
