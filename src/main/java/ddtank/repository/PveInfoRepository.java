package ddtank.repository;

import ddtank.model.PveInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PveInfoRepository extends JpaRepository<PveInfo, Long> {
}
