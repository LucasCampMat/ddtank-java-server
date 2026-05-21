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
    // ADICIONE ESTE MÉTODO ANTES DA ÚLTIMA CHAVE DE FECHAMENTO DO SEU WORLDMGR.JAVA:
    /**
     * Varre a memória RAM em busca de um jogador online que possua o Nickname enviado.
     * Retorna null caso o jogador esteja offline.
     */
    public static GamePlayer getPlayerByNickname(String nickname) {
        if (nickname == null || nickname.isEmpty()) return null;

        for (GamePlayer player : onlinePlayers.values()) {
            if (player.getPlayerData().getNickname().equalsIgnoreCase(nickname)) {
                return player;
            }
        }
        return null;
    }

    // ADICIONE ESTE MÉTODO ANTES DA ÚLTIMA CHAVE DE FECHAMENTO DO SEU WORLDMGR.JAVA:
    /**
     * Transmite um pacote de dados exclusivamente para os membros que pertencem à guilda especificada
     * e que estão ativamente online no servidor.
     */
    public static void broadcastToGuild(String guildName, GSPacketIn packet) {
        if (guildName == null || guildName.isEmpty()) return;

        for (GamePlayer player : onlinePlayers.values()) {
            String pGuild = player.getPlayerData().getGuildName();
            if (pGuild != null && pGuild.equalsIgnoreCase(guildName)) {
                player.sendPacket(packet);
            }
        }
    }

}

