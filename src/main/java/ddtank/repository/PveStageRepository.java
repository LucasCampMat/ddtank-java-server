package ddtank.repository;

import ddtank.model.PveInfo;
import ddtank.model.PveStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PveStageRepository extends JpaRepository<PveStage, Long> {
    // Carrega todas as fases ordenadas pertencentes a uma masmorra específica
    List<PveStage> findByPveInfoOrderByStageIndexAsc(PveInfo pveInfo);
}
