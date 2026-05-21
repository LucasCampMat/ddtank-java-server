package ddtank.network;

import ddtank.model.GamePlayer;
import ddtank.model.GameRoom;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class RoomMgr {
    private static final ConcurrentHashMap<Integer, GameRoom> activeRooms = new ConcurrentHashMap<>();
    private static final AtomicInteger idGenerator = new AtomicInteger(1);

    /**
     * Cria uma nova sala e define quem é o criador/dono
     */
    public static GameRoom createRoom(String name, GamePlayer owner) {
        int roomId = idGenerator.getAndIncrement();
        GameRoom room = new GameRoom(roomId, name, owner);
        activeRooms.put(roomId, room);
        System.out.println("⛺ [RoomMgr] Sala #" + roomId + " (\"" + name + "\") criada por " + owner.getPlayerData().getNickname());
        return room;
    }

    public static GameRoom getRoom(int roomId) {
        return activeRooms.get(roomId);
    }

    public static Collection<GameRoom> getAllRooms() {
        return activeRooms.values();
    }

    /**
     * Remove a sala completamente se ela ficar vazia
     */
    public static void removeRoom(int roomId) {
        activeRooms.remove(roomId);
        System.out.println("⛺ [RoomMgr] Sala #" + roomId + " fechada por estar vazia.");
    }
}
