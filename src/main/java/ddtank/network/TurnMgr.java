package ddtank.network;

import ddtank.config.SpringContext;
import ddtank.model.GamePlayer;
import ddtank.model.GameRoom;
import ddtank.packet.GSPacketIn;
import ddtank.repository.PlayerRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class TurnMgr {
    private final GameRoom room;
    private final List<GamePlayer> turnOrder = new ArrayList<>();
    private int currentTurnIndex = -1;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
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

        // Valida se o jogo acabou antes de passar o próximo turno
        if (checkGameEnd()) {
            return;
        }

        if (!room.isPlaying() || turnOrder.isEmpty()) {
            stopProcessor();
            return;
        }

        // Encontra o próximo jogador vivo na fila de rodadas
        int attempts = 0;
        do {
            currentTurnIndex = (currentTurnIndex + 1) % turnOrder.size();
            attempts++;
        } while (!turnOrder.get(currentTurnIndex).isAlive() && attempts <= turnOrder.size());

        GamePlayer activePlayer = turnOrder.get(currentTurnIndex);

        // Se por algum motivo ninguém estiver vivo, encerra
        if (!activePlayer.isAlive()) {
            checkGameEnd();
            return;
        }

        System.out.println("⏱️ [TurnMgr] É a vez de: " + activePlayer.getPlayerData().getNickname());

        GSPacketIn turnPacket = new GSPacketIn((short) 98);
        turnPacket.writeInt(activePlayer.getPlayerData().getId().intValue());
        turnPacket.writeInt(15);
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
     * Distribui as premiações e encerra oficialmente o estado de batalha da sala
     */
    private void processMatchEnd(int winningTeam) {
        System.out.println("🏁 [Combate] Partida encerrada na Sala #" + room.getRoomId() + ". Time vencedor: " + (winningTeam == 0 ? "Azul" : "Vermelho"));
        stopProcessor();

        room.setPlaying(false);
        PlayerRepository playerRepository = SpringContext.getBean(PlayerRepository.class);

        // Constrói o pacote de Resultados de Fim de Jogo (OpCode 100 no C# alvo)
        GSPacketIn endPacket = new GSPacketIn((short) 100);
        endPacket.writeInt(winningTeam); // Informa qual time ganhou

        for (var entry : room.getSlots().entrySet()) {
            int slotId = entry.getKey();
            GamePlayer gp = entry.getValue();
            var p = gp.getPlayerData();

            int isWinner = ((slotId < 4 && winningTeam == 0) || (slotId >= 4 && winningTeam == 1)) ? 1 : 0;

            // Cálculo de recompensas convertidas do repositório original
            int goldReward = isWinner == 1 ? 200 : 50;
            int gpReward = isWinner == 1 ? 100 : 20;

            // Altera o saldo na memória RAM e salva de forma persistente no MySQL
            p.setGold(p.getGold() + goldReward);
            p.setGp(p.getGp() + gpReward);
            playerRepository.save(p);

            // Escreve os dados de premiação individuais de cada um dentro do pacote de rede
            endPacket.writeInt(p.getId().intValue());
            endPacket.writeInt(isWinner); // 1 = Ganhou, 0 = Perdeu
            endPacket.writeInt(goldReward);
            endPacket.writeInt(gpReward);
        }

        // Envia a tela de pontuação final para todo mundo ver os espólios
        room.broadcastToRoom(endPacket);
    }

    public void stopProcessor() {
        if (turnTimeoutTask != null) turnTimeoutTask.cancel(true);
        scheduler.shutdown();
        System.out.println("🛑 [TurnMgr] Gerenciador de turnos da Sala #" + room.getRoomId() + " desligado.");
    }
}
