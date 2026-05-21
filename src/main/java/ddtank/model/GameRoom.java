package ddtank.model;

import ddtank.packet.GSPacketIn;
import ddtank.network.RoomMgr;
import ddtank.network.TurnMgr;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

public class GameRoom {
    private final int roomId;
    private String roomName;
    private GamePlayer roomOwner;
    private final ConcurrentHashMap<Integer, GamePlayer> slots = new ConcurrentHashMap<>();
    private final CopyOnWriteArraySet<Long> readyPlayers = new CopyOnWriteArraySet<>();
    private boolean isPlaying = false;
    private TurnMgr turnMgr;

    public GameRoom(int roomId, String roomName, GamePlayer roomOwner) {
        this.roomId = roomId;
        this.roomName = roomName;
        this.roomOwner = roomOwner;
        this.slots.put(0, roomOwner);
    }

    public int getRoomId() { return roomId; }
    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }
    public GamePlayer getRoomOwner() { return roomOwner; }
    public ConcurrentHashMap<Integer, GamePlayer> getSlots() { return slots; }
    public boolean isPlaying() { return isPlaying; }

    public void setPlaying(boolean playing) {
        this.isPlaying = playing;
        if (!playing && this.turnMgr != null) {
            this.turnMgr.stopProcessor();
        }
    }

    public void startTurnEngine() {
        for (GamePlayer p : slots.values()) {
            p.resetBattleStats();
        }
        this.turnMgr = new TurnMgr(this);
        this.turnMgr.startCombat();
    }

    public TurnMgr getTurnMgr() {
        return turnMgr;
    }

    public void toggleReady(GamePlayer player) {
        Long playerId = player.getPlayerData().getId();
        if (readyPlayers.contains(playerId)) {
            readyPlayers.remove(playerId);
        } else {
            readyPlayers.add(playerId);
        }

        GSPacketIn readyNotify = new GSPacketIn((short) 91);
        readyNotify.writeInt(player.getPlayerData().getId().intValue());
        readyNotify.writeBoolean(readyPlayers.contains(playerId));
        broadcastToRoom(readyNotify);
    }

    public boolean canStartGame() {
        if (slots.size() < 2) {
            return false;
        }
        for (GamePlayer p : slots.values()) {
            if (!p.equals(roomOwner) && !readyPlayers.contains(p.getPlayerData().getId())) {
                return false;
            }
        }
        return true;
    }

    public boolean addPlayer(GamePlayer player) {
        if (slots.size() >= 8) {
            return false;
        }
        int targetSlot = -1;
        for (int i = 0; i < 8; i++) {
            if (!slots.containsKey(i)) {
                targetSlot = i;
                break;
            }
        }
        if (targetSlot != -1 && !slots.containsValue(player)) {
            slots.put(targetSlot, player);

            GSPacketIn joinNotify = new GSPacketIn((short) 95);
            joinNotify.writeBoolean(true);
            joinNotify.writeInt(player.getPlayerData().getId().intValue());
            joinNotify.writeString(player.getPlayerData().getNickname());
            joinNotify.writeInt(player.getPlayerData().getGrade());
            joinNotify.writeByte(targetSlot);

            broadcastToRoom(joinNotify);
            broadcastToRoom(createRoomMessage(player.getPlayerData().getNickname() + " entrou no Slot " + targetSlot + "."));
            return true;
        }
        return false;
    }

    public boolean switchSlot(GamePlayer player, int targetSlot) {
        if (targetSlot < 0 || targetSlot > 7 || slots.containsKey(targetSlot)) {
            return false;
        }
        int oldSlot = -1;
        for (var entry : slots.entrySet()) {
            if (entry.getValue().equals(player)) {
                oldSlot = entry.getKey();
                break;
            }
        }
        if (oldSlot != -1) {
            slots.remove(oldSlot);
            slots.put(targetSlot, player);

            GSPacketIn switchNotify = new GSPacketIn((short) 93);
            switchNotify.writeInt(player.getPlayerData().getId().intValue());
            switchNotify.writeByte(oldSlot);
            switchNotify.writeByte(targetSlot);

            broadcastToRoom(switchNotify);
            return true;
        }
        return false;
    }

    public void removePlayer(GamePlayer player) {
        readyPlayers.remove(player.getPlayerData().getId());
        slots.entrySet().removeIf(entry -> entry.getValue().equals(player));
        GSPacketIn exitNotify = new GSPacketIn((short) 96);
        exitNotify.writeInt(player.getPlayerData().getId().intValue());
        broadcastToRoom(exitNotify);

        broadcastToRoom(createRoomMessage(player.getPlayerData().getNickname() + " saiu da sala."));

        if (slots.isEmpty()) {
            RoomMgr.removeRoom(roomId);
            return;
        }

        if (roomOwner.equals(player)) {
            GamePlayer newOwner = slots.values().iterator().next();
            this.roomOwner = newOwner;

            GSPacketIn newOwnerPacket = new GSPacketIn((short) 96);
            newOwnerPacket.writeInt(-1);
            newOwnerPacket.writeInt(newOwner.getPlayerData().getId().intValue());
            broadcastToRoom(newOwnerPacket);
        }
    }

    public void broadcastToRoom(GSPacketIn packet) {
        for (GamePlayer p : slots.values()) {
            p.sendPacket(packet);
        }
    }

    private GSPacketIn createRoomMessage(String msg) {
        GSPacketIn packet = new GSPacketIn((short) 19);
        packet.writeInt(5);
        packet.writeInt(0);
        packet.writeString("Sistema");
        packet.writeString(msg);
        return packet;
    }
    // ADICIONE ESTE MÉTODO ANTES DA ÚLTIMA CHAVE DE FECHAMENTO DO SEU GAMEROOM.JAVA:
    /**
     * Transmite um pacote de dados apenas para os jogadores que estão no mesmo time que o remetente.
     * Regra: Slots 0-3 (Time Azul), Slots 4-7 (Time Vermelho).
     */
    public void broadcastToTeam(int senderSlot, GSPacketIn packet) {
        boolean isSenderBlue = senderSlot < 4;

        for (var entry : slots.entrySet()) {
            int slotId = entry.getKey();
            GamePlayer player = entry.getValue();

            boolean isPlayerBlue = slotId < 4;

            // Se o jogador estiver no mesmo time que o remetente, ele recebe o pacote
            if (isSenderBlue == isPlayerBlue) {
                player.sendPacket(packet);
            }
        }
    }
    // ADICIONE ESTES CAMPOS E MÉTODOS ANTES DA ÚLTIMA CHAVE DE FECHAMENTO DO SEU GAMEROOM.JAVA:
    private double currentWind = 0.0;

    public double getCurrentWind() {
        return currentWind;
    }

    /**
     * Define o vento atual da sala (Ex: -2.4 ou 3.1)
     */
    public void setCurrentWind(double wind) {
        // Limita matematicamente para ter apenas uma casa decimal, idêntico ao jogo clássico
        this.currentWind = Math.round(wind * 10.0) / 10.0;
    }

    // ADICIONE ESTE MÉTODO ANTES DA ÚLTIMA CHAVE DE FECHAMENTO DO SEU GAMEROOM.JAVA:
    /**
     * Calcula o impacto físico da explosão no terreno cartesiano.
     * Desloca jogadores afetados para baixo (queda) ou elimina-os se caírem no abismo.
     */
    public void checkTerrainDestruction(int bombX, int bombY, int radius) {
        for (GamePlayer player : slots.values()) {
            if (!player.isAlive()) continue;

            // Calcula a distância horizontal entre a explosão e o jogador
            int distanceX = Math.abs(player.getPosX() - bombX);

            // Se o jogador estiver dentro do raio horizontal do buraco aberto pela bomba
            if (distanceX <= radius) {
                // Simula a perda de terreno: o chão cede e o Y aumenta (DDTank adota Y para baixo)
                int newY = player.getPosY() + (radius - distanceX / 2);

                // Limite clássico do abismo do cenário (ex: Y maior que 900 significa queda livre para a morte)
                if (newY >= 900) {
                    player.takeDamage(player.getMaxHp()); // Eliminação instantânea por queda no limbo
                    System.out.println("🕳️ [Física] " + player.getPlayerData().getNickname() + " caiu no abismo e morreu!");
                } else {
                    player.setPosY(newY); // Atualiza a nova posição de pouso do jogador
                    System.out.println("🕳️ [Física] Terreno destruído! Novo Y de " + player.getPlayerData().getNickname() + ": " + newY);
                }

                // Transmite o pacote de atualização de posição forçada por queda (OpCode 92) para a sala sincronizar
                GSPacketIn fallPacket = new GSPacketIn((short) 92);
                fallPacket.writeInt(player.getPlayerData().getId().intValue());
                fallPacket.writeInt(player.getPosX());
                fallPacket.writeInt(player.getPosY());
                fallPacket.writeByte(player.getDirection());
                broadcastToRoom(fallPacket);
            }
        }
    }
    // ADICIONE ESTES COMPONENTES ANTES DA ÚLTIMA CHAVE DE FECHAMENTO DO SEU GAMEROOM.JAVA:
    private final java.util.concurrent.CopyOnWriteArrayList<GameNpc> npcs = new java.util.concurrent.CopyOnWriteArrayList<>();

    public java.util.concurrent.CopyOnWriteArrayList<GameNpc> getNpcs() {
        return npcs;
    }

    /**
     * Inicializa os monstros no mapa com base nas configurações que semeamos no MySQL
     */
    public void spawnNpcs(int count) {
        npcs.clear();
        for (int i = 1; i <= count; i++) {
            // Nasce os Bogus espalhados cartesianamente pelo cenário do mapa
            npcs.add(new GameNpc(i, "Bogu Guerreiro #" + i, 400 + (i * 80), 350));
        }
        System.out.println("🤖 [PvE AI] " + count + " monstros nasceram no mapa da Sala #" + roomId);
    }

    /**
     * Motor de Inteligência Artificial: Processa o turno de ataque de todos os monstros vivos
     */
    public void processNpcTurn() {
        if (!isPlaying) return;

        for (GameNpc npc : npcs) {
            if (!npc.isAlive()) continue;

            // 1. Localiza o jogador vivo mais próximo no mapa cartesiano (Algoritmo de Proximidade do C#)
            GamePlayer targetPlayer = null;
            double shortestDistance = Double.MAX_VALUE;

            for (GamePlayer p : slots.values()) {
                if (p.isAlive()) {
                    double dist = Math.abs(p.getPosX() - npc.getPosX());
                    if (dist < shortestDistance) {
                        shortestDistance = dist;
                        targetPlayer = p;
                    }
                }
            }

            // 2. Se encontrou um alvo válido, executa a ação de IA
            if (targetPlayer != null) {
                // Simula o monstro caminhando na direção do jogador
                int walkDirection = (targetPlayer.getPosX() > npc.getPosX()) ? 1 : -1;
                npc.setPosX(npc.getPosX() + (walkDirection * 30)); // Anda 30 pixels

                // Aplica o dano físico do monstro diretamente na saúde do jogador em memória RAM
                targetPlayer.takeDamage(npc.getDamage());
                System.out.println("🤖 [PvE AI] " + npc.getName() + " andou e atacou " + targetPlayer.getPlayerData().getNickname() + " causando -" + npc.getDamage() + " HP!");

                // Transmite os pacotes binários de animação de ataque e dano simultaneamente para a sala
                GSPacketIn npcActionPacket = new GSPacketIn((short) 99);
                npcActionPacket.writeInt(targetPlayer.getPlayerData().getId().intValue());
                npcActionPacket.writeInt(targetPlayer.getCurrentHp());
                npcActionPacket.writeBoolean(targetPlayer.isAlive());
                broadcastToRoom(npcActionPacket);
            }
        }
    }


}
