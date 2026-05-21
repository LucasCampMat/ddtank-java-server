package ddtank.network;

import ddtank.config.SpringContext;
import ddtank.model.GamePlayer;
import ddtank.model.GameRoom;
import ddtank.model.Quest;
import ddtank.model.UserQuest;
import ddtank.packet.GSPacketIn;
import ddtank.repository.PlayerRepository;
import ddtank.repository.QuestRepository;
import ddtank.repository.UserQuestRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class TurnMgr {
    private final GameRoom room;
    private final List<GamePlayer> turnOrder = new ArrayList<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private int currentTurnIndex = -1;
    private ScheduledFuture<?> turnTimeoutTask;

    public TurnMgr(GameRoom room) {
        this.room = room;
        this.turnOrder.addAll(room.getSlots().values());
    }

    public void startCombat() {
        if (turnOrder.isEmpty()) return;
        System.out.println("⚔️ [TurnMgr] Ciclo de turnos iniciado para a Sala #" + room.getRoomId());
        nextTurn();
    }

    public synchronized void nextTurn() {
        if (turnTimeoutTask != null && !turnTimeoutTask.isDone()) {
            turnTimeoutTask.cancel(true);
        }

        if (checkGameEnd()) {
            return;
        }

        if (!room.isPlaying() || turnOrder.isEmpty()) {
            stopProcessor();
            return;
        }

        // Gera um vento aleatório entre -5.0 e +5.0 a cada nova rodada de combate
        double randomWind = (new Random().nextInt(101) - 50) / 10.0;
        room.setCurrentWind(randomWind);
        System.out.println("💨 [Clima] O vento da Sala #" + room.getRoomId() + " mudou para: " + room.getCurrentWind());

        // Avança o índice da fila em formato circular
        int attempts = 0;
        do {
            currentTurnIndex = (currentTurnIndex + 1) % turnOrder.size();
            attempts++;
        } while (!turnOrder.get(currentTurnIndex).isAlive() && attempts <= turnOrder.size());

        if (currentTurnIndex == 0 && room.isPlaying()) {
            System.out.println("🤖 [TurnMgr] Rodada dos jogadores finalizada. Ativando Turno da Inteligência Artificial (NPCs)...");
            room.processNpcTurn();
        }

        GamePlayer activePlayer = turnOrder.get(currentTurnIndex);

        if (!activePlayer.isAlive()) {
            checkGameEnd();
            return;
        }

        System.out.println("⏱️ [TurnMgr] É a vez de: " + activePlayer.getPlayerData().getNickname());

        // Atualizamos o pacote de Notificação de Turno (OpCode 98) para injetar o vento atualizado
        GSPacketIn turnPacket = new GSPacketIn((short) 98);
        turnPacket.writeInt(activePlayer.getPlayerData().getId().intValue());
        turnPacket.writeInt(15); // 15 segundos

        // Envia o vento atual multiplicado por 10 (padrão binário do flash ler inteiros como decimais de precisão)
        turnPacket.writeInt((int) (room.getCurrentWind() * 10));

        room.broadcastToRoom(turnPacket);

        turnTimeoutTask = scheduler.schedule(() -> {
            System.out.println("⏳ [TurnMgr] Tempo esgotado para " + activePlayer.getPlayerData().getNickname() + ". Passando turno...");
            nextTurn();
        }, 15, TimeUnit.SECONDS);
    }

    /**
     * Varre os slots para verificar se algum time foi totalmente eliminado
     */
    private boolean checkGameEnd() {
        boolean blueTeamAlive = false;
        boolean redTeamAlive = false;

        // Regra clássica do DDTank: slots de 0 a 3 são Time Azul, slots de 4 a 7 são Time Vermelho
        for (var entry : room.getSlots().entrySet()) {
            int slotId = entry.getKey();
            GamePlayer p = entry.getValue();

            if (p.isAlive()) {
                if (slotId < 4) {
                    blueTeamAlive = true;
                } else {
                    redTeamAlive = true;
                }
            }
        }

        // Se um dos lados não tiver mais ninguém vivo, a partida acabou!
        if (!blueTeamAlive || !redTeamAlive) {
            int winningTeam = blueTeamAlive ? 0 : 1; // 0 = Azul venceu, 1 = Vermelho venceu
            processMatchEnd(winningTeam);
            return true;
        }

        return false;
    }

    /**
     * Distribui as premiações e encerra oficialmente o estado de batalha da sala, checando por Level UPs e Quests
     */
    private void processMatchEnd(int winningTeam) {
        System.out.println("🏁 [Combate] Partida encerrada na Sala #" + room.getRoomId() + ". Time vencedor: " + (winningTeam == 0 ? "Azul" : "Vermelho"));
        stopProcessor();

        room.setPlaying(false);
        PlayerRepository playerRepository = SpringContext.getBean(PlayerRepository.class);
        UserQuestRepository uqRepository = SpringContext.getBean(UserQuestRepository.class);
        QuestRepository qRepository = SpringContext.getBean(QuestRepository.class);

        GSPacketIn endPacket = new GSPacketIn((short) 100);
        endPacket.writeInt(winningTeam);

        for (var entry : room.getSlots().entrySet()) {
            int slotId = entry.getKey();
            GamePlayer gp = entry.getValue();
            var p = gp.getPlayerData();

            int isWinner = ((slotId < 4 && winningTeam == 0) || (slotId >= 4 && winningTeam == 1)) ? 1 : 0;

            int goldReward = isWinner == 1 ? 200 : 50;
            int gpReward = isWinner == 1 ? 100 : 20;

            // Altera saldos na memória RAM
            p.setGold(p.getGold() + goldReward);

            // Executa a adição de GP avaliando se houve alteração de nível (Grade)
            boolean leveledUp = p.addGp(gpReward);

            // Persiste de forma segura os novos valores calculados no MySQL
            playerRepository.save(p);

            // INTEGRADO: Lógica de Processamento de Missões para o Vencedor
            if (isWinner == 1) {
                try {
                    Optional<UserQuest> uqOpt = uqRepository.findByPlayerAndQuestId(p, 1L);
                    UserQuest userQuest;

                    if (uqOpt.isPresent()) {
                        userQuest = uqOpt.get();
                    } else {
                        // Cria a missão inicial dinamicamente se ela não existir no banco
                        Quest staticQuest = qRepository.findById(1L).orElseGet(() ->
                                qRepository.save(new Quest(1L, "Primeira Vitória", "Vença uma partida PvP", 1, 500))
                        );
                        userQuest = new UserQuest(p, staticQuest);
                    }

                    if (!userQuest.isCompleted()) {
                        boolean completedNow = userQuest.incrementProgress();
                        uqRepository.save(userQuest);

                        if (completedNow) {
                            // Concede bônus extra em Gold por completar a missão
                            p.setGold(p.getGold() + userQuest.getQuest().getRewardGold());
                            playerRepository.save(p);
                            System.out.println("🎁 [Quest] " + p.getNickname() + " completou a missão '" + userQuest.getQuest().getTitle() + "'!");

                            // Envia notificação visual de Missão Concluída (OpCode 176)
                            GSPacketIn questPacket = new GSPacketIn((short) 176);
                            questPacket.writeInt(1);
                            questPacket.writeInt(1);
                            gp.sendPacket(questPacket);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Erro ao processar progresso de missões de combate: " + e.getMessage());
                }
            }

            // Escreve dados normais de premiação
            endPacket.writeInt(p.getId().intValue());
            endPacket.writeInt(isWinner);
            endPacket.writeInt(goldReward);
            endPacket.writeInt(gpReward);

            // Se o jogador subiu de nível, dispara o pacote de efeito visual de Level UP (OpCode 5)
            if (leveledUp) {
                GSPacketIn levelUpPacket = new GSPacketIn((short) 5);
                levelUpPacket.writeInt(p.getId().intValue());
                levelUpPacket.writeInt(p.getGrade());
                room.broadcastToRoom(levelUpPacket);
            }
        }

        room.broadcastToRoom(endPacket);
    }

    public void stopProcessor() {
        if (turnTimeoutTask != null) turnTimeoutTask.cancel(true);
        scheduler.shutdown();
        System.out.println("🛑 [TurnMgr] Gerenciador de turnos da Sala #" + room.getRoomId() + " desligado.");
    }
}

