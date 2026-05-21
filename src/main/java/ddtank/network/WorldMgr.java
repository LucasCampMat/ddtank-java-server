package ddtank.network;

import ddtank.model.GamePlayer;
import ddtank.packet.GSPacketIn;
import io.netty.channel.ChannelId;
import java.util.concurrent.ConcurrentHashMap;

public class WorldMgr {
    // Tabela thread-safe que associa o ID único da conexão do Netty com o objeto do jogador online
    private static final ConcurrentHashMap<ChannelId, GamePlayer> onlinePlayers = new ConcurrentHashMap<>();

    public static void addPlayer(ChannelId id, GamePlayer player) {
        onlinePlayers.put(id, player);
        System.out.println("👥 [WorldMgr] Jogador '" + player.getPlayerData().getNickname() + "' adicionado ao mapa mundial. Total online: " + onlinePlayers.size());
    }

    public static GamePlayer getPlayer(ChannelId id) {
        return onlinePlayers.get(id);
    }

    public static void removePlayer(ChannelId id) {
        GamePlayer player = onlinePlayers.remove(id);
        if (player != null) {
            System.out.println("👥 [WorldMgr] Jogador '" + player.getPlayerData().getNickname() + "' desconectou. Total online: " + onlinePlayers.size());
        }
    }

    public static int getOnlineCount() {
        return onlinePlayers.size();
    }

    /**
     * Transmite um pacote de dados para absolutamente todos os jogadores online no servidor.
     */
    public static void broadcastPacket(GSPacketIn packet) {
        for (GamePlayer player : onlinePlayers.values()) {
            try {
                player.sendPacket(packet);
            } catch (Exception e) {
                System.err.println("Falha ao transmitir pacote para o jogador " + player.getPlayerData().getNickname() + ": " + e.getMessage());
            }
        }
    }
}

