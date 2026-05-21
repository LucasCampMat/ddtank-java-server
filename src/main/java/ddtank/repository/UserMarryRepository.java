package ddtank.repository;

import ddtank.model.Player;
import ddtank.model.UserMarry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserMarryRepository extends JpaRepository<UserMarry, Long> {

    // Busca se existe algum casamento ativo onde o jogador seja o marido ou a esposa
    @Query("SELECT m FROM UserMarry m WHERE (m.husband = :player OR m.wife = :player) AND m.isDivorced = false")
    Optional<UserMarry> findActiveMarriage(@Param("player") Player player);
}
