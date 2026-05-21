package ddtank.network;

public class LevelMgr {

    // Tabela estática contendo o GP acumulado necessário para atingir cada nível (Exemplo clássico DDTank v3.0)
    private static final int[] LEVEL_EXP = new int[51];

    static {
        LEVEL_EXP[1] = 0;
        LEVEL_EXP[2] = 100;
        LEVEL_EXP[3] = 300;
        LEVEL_EXP[4] = 600;
        LEVEL_EXP[5] = 1000;
        LEVEL_EXP[6] = 1500;
        LEVEL_EXP[7] = 2200;
        LEVEL_EXP[8] = 3100;
        LEVEL_EXP[9] = 4200;
        LEVEL_EXP[10] = 5500;
        // Preenche os níveis subsequentes com um multiplicador padrão progressivo
        for (int i = 11; i <= 50; i++) {
            LEVEL_EXP[i] = LEVEL_EXP[i - 1] + (i * 200);
        }
    }

    /**
     * Com base no GP atual do jogador, calcula qual deve ser o nível (Grade) real dele
     */
    public static int getLevelByGP(int gp) {
        int level = 1;
        for (int i = 1; i <= 50; i++) {
            if (gp >= LEVEL_EXP[i]) {
                level = i;
            } else {
                break;
            }
        }
        return level;
    }
}
