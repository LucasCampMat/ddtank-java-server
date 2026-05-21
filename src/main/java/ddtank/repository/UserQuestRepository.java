package ddtank.repository;

import ddtank.model.Player;
import ddtank.model.UserQuest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserQuestRepository extends JpaRepository<UserQuest, Long> {
    List<UserQuest> findByPlayer(Player player);
    Optional<UserQuest> findByPlayerAndQuestId(Player player, Long questId);
}
