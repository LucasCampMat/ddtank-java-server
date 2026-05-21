package ddtank.model;

import jakarta.persistence.*;

@Entity
@Table(name = "pve_stages")
public class PveStage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "pve_id", nullable = false)
    private PveInfo pveInfo; // Vincula ao ID da masmorra dona deste estágio

    private int stageIndex;   // Qual é o número da tela (Fase 1, Fase 2...)
    private int mapId;        // O ID do cenário gráfico a ser renderizado
    private int npcCount = 0; // Quantidade de monstros iniciais que nascem na tela

    public PveStage() {}

    public PveStage(PveInfo pveInfo, int stageIndex, int mapId, int npcCount) {
        this.pveInfo = pveInfo;
        this.stageIndex = stageIndex;
        this.mapId = mapId;
        this.npcCount = npcCount;
    }

    // Getters e Setters
    public Long getId() { return id; }
    public PveInfo getPveInfo() { return pveInfo; }
    public void setPveInfo(PveInfo pveInfo) { this.pveInfo = pveInfo; }
    public int getStageIndex() { return stageIndex; }
    public void setStageIndex(int stageIndex) { this.stageIndex = stageIndex; }
    public int getMapId() { return mapId; }
    public void setMapId(int mapId) { this.mapId = mapId; }
    public int getNpcCount() { return npcCount; }
    public void setNpcCount(int npcCount) { this.npcCount = npcCount; }
}
