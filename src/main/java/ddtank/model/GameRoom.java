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

}
