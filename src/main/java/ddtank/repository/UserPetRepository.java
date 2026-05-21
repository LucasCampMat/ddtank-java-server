package ddtank.repository;

import ddtank.model.Player;
import ddtank.model.UserPet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface UserPetRepository extends JpaRepository<UserPet, Long> {
    List<UserPet> findByPlayer(Player player);
}
