package ddtank.config;

import ddtank.model.PveInfo;
import ddtank.model.PveStage;
import ddtank.repository.PveInfoRepository;
import ddtank.repository.PveStageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class PveDataSeed implements CommandLineRunner {

    @Autowired
    private PveInfoRepository pveInfoRepository;

    @Autowired
    private PveStageRepository pveStageRepository;

    @Override
    public void run(String... args) throws Exception {
        // Verifica se a Masmorra clássica ID 1 (Castelo Bogu) já foi cadastrada no banco
        if (pveInfoRepository.findById(1L).isEmpty()) {
            System.out.println("📦 [PvE Seed] Alimentando o MySQL com as configurações das Masmorras...");

            // 1. Cria e salva o modelo estático do Castelo Bogu (ID 1, Nível Mínimo 10, Total de 3 Fases)
            PveInfo casteloBogu = new PveInfo(1L, "Castelo Bogu", "Enfrente o exército Bogu e destrua o Rei Bogu!", 10, 3);
            pveInfoRepository.save(casteloBogu);

            // 2. Cria e salva as 3 fases/telas internas pertencentes a essa masmorra
            // Fase 1: Mapa Gráfico 501, com 5 Bogus Guerreiros iniciais
            PveStage fase1 = new PveStage(casteloBogu, 1, 501, 5);
            // Fase 2: Mapa Gráfico 502, com 6 Bogus Atiradores iniciais
            PveStage fase2 = new PveStage(casteloBogu, 2, 502, 6);
            // Fase 3: Mapa Gráfico 503 (Sala do Trono), com 1 Rei Bogu (Chefe) e 2 Guardas
            PveStage fase3 = new PveStage(casteloBogu, 3, 503, 3);

            pveStageRepository.save(fase1);
            pveStageRepository.save(fase2);
            pveStageRepository.save(fase3);

            System.out.println("✅ [PvE Seed] Castelo Bogu e seus 3 estágios inseridos com sucesso no MySQL!");
        } else {
            System.out.println("📦 [PvE Seed] Configurações de masmorras já estão atualizadas no MySQL.");
        }
    }
}
