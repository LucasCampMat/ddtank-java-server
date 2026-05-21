package ddtank.repository;

import ddtank.model.Player;
import ddtank.model.UserMail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface UserMailRepository extends JpaRepository<UserMail, Long> {
    // Busca a caixa de entrada de um jogador específico, listando do mais recente para o mais antigo
    List<UserMail> findByReceiverOrderBySentTimeDesc(Player receiver);
}
