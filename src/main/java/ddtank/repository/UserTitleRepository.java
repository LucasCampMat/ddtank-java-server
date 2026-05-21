package ddtank.repository;

import ddtank.model.Player;
import ddtank.model.UserTitle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserTitleRepository extends JpaRepository<UserTitle, Long> {
    List<UserTitle> findByPlayer(Player player);
    Optional<UserTitle> findByPlayerAndTitleId(Player player, int titleId);
}
