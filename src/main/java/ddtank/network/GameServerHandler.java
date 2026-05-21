package ddtank.network;

import ddtank.config.SpringContext;
import ddtank.model.GamePlayer;
import ddtank.model.GameRoom;
import ddtank.model.Player;
import ddtank.model.UserItem;
import ddtank.packet.GSPacketIn;
import ddtank.repository.PlayerRepository;
import ddtank.repository.UserItemRepository;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public class GameServerHandler extends SimpleChannelInboundHandler<ByteBuf> {

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        System.out.println("[DDTank Netty] Conexão estabelecida com o cliente: " + ctx.channel().remoteAddress());

        ByteBuf handshake = Unpooled.buffer();
        handshake.writeShort(0x77aa);
        handshake.writeShort(0);
        handshake.writeShort(1);

        ctx.writeAndFlush(handshake);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        GamePlayer player = WorldMgr.getPlayer(ctx.channel().id());
        if (player != null) {
            for (GameRoom room : RoomMgr.getAllRooms()) {
                if (room.getSlots().containsValue(player)) {
                    room.removePlayer(player);
                    break;
                }
            }
        }
        WorldMgr.removePlayer(ctx.channel().id());
        super.channelInactive(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
        if (msg.readableBytes() < 6) {
            return;
        }

        msg.markReaderIndex();
        short header = msg.readShort();
        short length = msg.readShort();

        if (msg.readableBytes() < length) {
            msg.resetReaderIndex();
            return;
        }

        short opCode = msg.readShort();

        int dataLength = length - 2;
        byte[] encryptedData = new byte[dataLength];
        msg.readBytes(encryptedData);

        byte[] decryptedData = DDTankCrypto.decrypt(encryptedData, dataLength);

        ByteBuf dataBuffer = Unpooled.copiedBuffer(decryptedData);
        GSPacketIn packet = new GSPacketIn(opCode, dataBuffer);

        System.out.println("[DDTank Netty] Pacote descriptografado recebido. OpCode: " + packet.getCode());

        handlePacket(ctx, packet);
    }

    private void handlePacket(ChannelHandlerContext ctx, GSPacketIn packet) {
        short opCode = packet.getCode();

        switch (opCode) {
            case 1: // LOGIN_REQ
                try {
                    int version = packet.readInt();
                    int clientType = packet.readInt();
                    String username = packet.readString();
                    String password = packet.readString();

                    System.out.println("[DDTank Socket] Tentativa de login recebida para o usuário: " + username);

                    PlayerRepository playerRepository = SpringContext.getBean(PlayerRepository.class);
                    Optional<Player> playerOpt = playerRepository.findByUsername(username);

                    if (playerOpt.isPresent() && playerOpt.get().getPassword().equals(password)) {
                        Player p = playerOpt.get();
                        System.out.println("[DDTank Socket] Login bem-sucedido no MySQL para: " + p.getNickname());

                        GamePlayer gamePlayer = new GamePlayer(ctx, p);
                        WorldMgr.addPlayer(ctx.channel().id(), gamePlayer);

                        GSPacketIn response = new GSPacketIn((short) 1);
                        response.writeBoolean(true);
                        response.writeInt(p.getId().intValue());
                        response.writeString(p.getNickname());
                        gamePlayer.sendPacket(response);

                        sendUserBag(gamePlayer, p);

                    } else {
                        System.out.println("[DDTank Socket] Falha de login para o usuário: " + username);
                        GSPacketIn response = new GSPacketIn((short) 1);
                        response.writeBoolean(false);
                        response.writeString("Usuário ou senha incorretos.");
                        ctx.writeAndFlush(response.getBuffer());
                    }
                } catch (Exception e) {
                    System.err.println("Erro ao processar pacote de login: " + e.getMessage());
                }
                break;

            case 19: // SYS_CHAT
                try {
                    GamePlayer sender = WorldMgr.getPlayer(ctx.channel().id());
                    if (sender != null) {
                        int chatType = packet.readInt();
                        String targetName = packet.readString();
                        String messageContent = packet.readString();

                        GSPacketIn chatResponse = new GSPacketIn((short) 19);
                        chatResponse.writeInt(chatType);
                        chatResponse.writeInt(sender.getPlayerData().getId().intValue());
                        chatResponse.writeString(sender.getPlayerData().getNickname());
                        chatResponse.writeString(messageContent);

                        GameRoom currentRoom = null;
                        int senderSlot = -1;

                        for (GameRoom room : RoomMgr.getAllRooms()) {
                            if (room.getSlots().containsValue(sender)) {
                                currentRoom = room;
                                for (var entry : room.getSlots().entrySet()) {
                                    if (entry.getValue().equals(sender)) {
                                        senderSlot = entry.getKey();
                                        break;
                                    }
                                }
                                break;
                            }
                        }

                        if (chatType == 1 && currentRoom != null && senderSlot != -1) {
                            currentRoom.broadcastToTeam(senderSlot, chatResponse);
                            System.out.println("💬 [Chat Time] " + sender.getPlayerData().getNickname() + " (Slot " + senderSlot + ") enviou tática protegida.");
                        } else if (currentRoom != null) {
                            currentRoom.broadcastToRoom(chatResponse);
                        } else {
                            WorldMgr.broadcastPacket(chatResponse);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Erro ao processar chat filtrado: " + e.getMessage());
                }
                break;

            case 44: // SYS_SHOP_BUY
                try {
                    GamePlayer buyer = WorldMgr.getPlayer(ctx.channel().id());
                    if (buyer != null) {
                        int templateId = packet.readInt();
                        int currencyType = packet.readInt();
                        int price = 500;

                        Player p = buyer.getPlayerData();
                        boolean paymentSuccess = (currencyType == 0) ? p.decreaseGold(price) : p.decreaseMoney(price);

                        GSPacketIn shopResponse = new GSPacketIn((short) 44);

                        if (paymentSuccess) {
                            PlayerRepository playerRepository = SpringContext.getBean(PlayerRepository.class);
                            playerRepository.save(p);

                            UserItemRepository itemRepository = SpringContext.getBean(UserItemRepository.class);
                            int nextSlot = itemRepository.findByPlayer(p).size();

                            UserItem newItem = new UserItem(p, templateId, nextSlot);
                            itemRepository.save(newItem);

                            shopResponse.writeBoolean(true);
                            buyer.sendPacket(shopResponse);

                            sendUserBag(buyer, p);
                        } else {
                            shopResponse.writeBoolean(false);
                            shopResponse.writeString("Saldo insuficiente.");
                            buyer.sendPacket(shopResponse);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Erro ao processar compra: " + e.getMessage());
                }
                break;

            case 76: // SYS_ITEM_STRENGTHEN (Opcode convertido do emulador alvo)
                try {
                    GamePlayer smithPlayer = WorldMgr.getPlayer(ctx.channel().id());
                    if (smithPlayer != null) {
                        int weaponBagPlace = packet.readInt(); // O slot onde a arma está guardada (ex: slot 0)

                        UserItemRepository itemRepository = SpringContext.getBean(UserItemRepository.class);
                        List<UserItem> items = itemRepository.findByPlayer(smithPlayer.getPlayerData());

                        UserItem targetWeapon = null;
                        for (UserItem item : items) {
                            if (item.getPlace() == weaponBagPlace) {
                                targetWeapon = item;
                                break;
                            }
                        }

                        GSPacketIn strengthenResponse = new GSPacketIn((short) 76);

                        if (targetWeapon != null && targetWeapon.getStrengthenLevel() < 12) {
                            int currentLevel = targetWeapon.getStrengthenLevel();

                            // Fórmula matemática simplificada de probabilidade do DDTank antigo
                            int successRate = Math.max(100 - (currentLevel * 8), 5); // Taxa diminui conforme o nível sobe
                            int roll = new Random().nextInt(100); // Gera um número de 0 a 99

                            if (roll < successRate) {
                                // SUCESSO: Sobe o nível de refino da arma e salva no MySQL
                                targetWeapon.setStrengthenLevel(currentLevel + 1);
                                itemRepository.save(targetWeapon);

                                System.out.println("🔨 [Ferreiro] Sucesso! " + smithPlayer.getPlayerData().getNickname() +
                                        " refinou Item " + targetWeapon.getTemplateId() + " para +" + (currentLevel + 1));

                                strengthenResponse.writeBoolean(true); // Confirmação de Sucesso
                                strengthenResponse.writeInt(targetWeapon.getStrengthenLevel());
                                smithPlayer.sendPacket(strengthenResponse);
                            } else {
                                // FALHA: Mantém o nível ou quebra de acordo com o padrão clássico
                                System.out.println("🔨 [Ferreiro] Falha! " + smithPlayer.getPlayerData().getNickname() +
                                        " falhou ao refinar para +" + (currentLevel + 1));

                                strengthenResponse.writeBoolean(false); // Confirmação de Falha
                                strengthenResponse.writeString("O fortalecimento falhou. Suas pedras foram consumidas!");
                                smithPlayer.sendPacket(strengthenResponse);
                            }

                            // Dispara atualização da mochila para renderizar os novos atributos na tela do jogador
                            sendUserBag(smithPlayer, smithPlayer.getPlayerData());
                        } else {
                            strengthenResponse.writeBoolean(false);
                            strengthenResponse.writeString("Item inválido ou já atingiu o nível máximo (+12).");
                            smithPlayer.sendPacket(strengthenResponse);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Erro ao processar fortalecimento no ferreiro: " + e.getMessage());
                }
                break;

            case 91: // GAME_START / ROOM_READY
                try {
                    GamePlayer actionPlayer = WorldMgr.getPlayer(ctx.channel().id());
                    if (actionPlayer != null) {
                        GameRoom playerRoom = null;
                        for (GameRoom room : RoomMgr.getAllRooms()) {
                            if (room.getSlots().containsValue(actionPlayer)) {
                                playerRoom = room;
                                break;
                            }
                        }
                        if (playerRoom != null) {
                            if (playerRoom.getRoomOwner().equals(actionPlayer)) {
                                if (playerRoom.canStartGame()) {
                                    playerRoom.setPlaying(true);

                                    GSPacketIn startPacket = new GSPacketIn((short) 91);
                                    startPacket.writeBoolean(true);
                                    startPacket.writeInt(1);
                                    playerRoom.broadcastToRoom(startPacket);
                                    System.out.println("⚔️ [Combate] Sala #" + playerRoom.getRoomId() + " iniciou a partida PvP!");

                                    playerRoom.startTurnEngine();

                                } else {
                                    GSPacketIn errorPacket = new GSPacketIn((short) 19);
                                    errorPacket.writeInt(5);
                                    errorPacket.writeInt(0);
                                    errorPacket.writeString("Sistema");
                                    errorPacket.writeString("Não foi possível iniciar. Todos os jogadores devem estar 'Pronto'.");
                                    actionPlayer.sendPacket(errorPacket);
                                }
                            } else {
                                playerRoom.toggleReady(actionPlayer);
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Erro ao processar evento de Início/Pronto: " + e.getMessage());
                }
                break;

            case 92: // GAME_ROOM_MOVE / PLAYER_MOVE
                try {
                    GamePlayer movingPlayer = WorldMgr.getPlayer(ctx.channel().id());
                    if (movingPlayer != null) {
                        int newX = packet.readInt();
                        int newY = packet.readInt();
                        int direction = packet.readByte();

                        movingPlayer.setPosition(newX, newY, direction);

                        for (GameRoom room : RoomMgr.getAllRooms()) {
                            if (room.getSlots().containsValue(movingPlayer)) {
                                GSPacketIn moveBroadcast = new GSPacketIn((short) 92);
                                moveBroadcast.writeInt(movingPlayer.getPlayerData().getId().intValue());
                                moveBroadcast.writeInt(newX);
                                moveBroadcast.writeInt(newY);
                                moveBroadcast.writeByte(direction);

                                room.broadcastToRoom(moveBroadcast);
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Erro ao processar movimento do jogador: " + e.getMessage());
                }
                break;

            case 93: // GAME_ROOM_SWITCH_TEAM
                try {
                    GamePlayer player = WorldMgr.getPlayer(ctx.channel().id());
                    if (player != null) {
                        int desiredSlot = packet.readByte();
                        for (GameRoom room : RoomMgr.getAllRooms()) {
                            if (room.getSlots().containsValue(player)) {
                                room.switchSlot(player, desiredSlot);
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Erro ao processar time: " + e.getMessage());
                }
                break;

            case 94: // GAME_ROOM_CREATE
                try {
                    GamePlayer creator = WorldMgr.getPlayer(ctx.channel().id());
                    if (creator != null) {
                        int roomType = packet.readInt();
                        int maxPlayers = packet.readByte();
                        String roomName = packet.readString();

                        GameRoom newRoom = RoomMgr.createRoom(roomName, creator);

                        GSPacketIn roomResponse = new GSPacketIn((short) 94);
                        roomResponse.writeBoolean(true);
                        roomResponse.writeInt(newRoom.getRoomId());
                        roomResponse.writeString(newRoom.getRoomName());

                        creator.sendPacket(roomResponse);
                    }
                } catch (Exception e) {
                    System.err.println("Erro ao criar sala: " + e.getMessage());
                }
                break;

            case 95: // GAME_ROOM_JOIN
                try {
                    GamePlayer player = WorldMgr.getPlayer(ctx.channel().id());
                    if (player != null) {
                        int roomId = packet.readInt();
                        GameRoom targetRoom = RoomMgr.getRoom(roomId);

                        GSPacketIn joinResponse = new GSPacketIn((short) 95);

                        if (targetRoom != null && !targetRoom.isPlaying()) {
                            boolean success = targetRoom.addPlayer(player);
                            if (!success) {
                                joinResponse.writeBoolean(false);
                                joinResponse.writeString("A sala está lotada!");
                                player.sendPacket(joinResponse);
                            } else {
                                System.out.println("⛺ [RoomMgr] " + player.getPlayerData().getNickname() + " entrou com sucesso na sala #" + roomId);
                            }
                        } else {
                            joinResponse.writeBoolean(false);
                            joinResponse.writeString("A sala não está mais disponível.");
                            player.sendPacket(joinResponse);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Erro ao entrar na sala: " + e.getMessage());
                }
                break;

            case 96: // GAME_ROOM_EXIT
                try {
                    GamePlayer player = WorldMgr.getPlayer(ctx.channel().id());
                    if (player != null) {
                        for (GameRoom room : RoomMgr.getAllRooms()) {
                            if (room.getSlots().containsValue(player)) {
                                room.removePlayer(player);
                                GSPacketIn exitConfirm = new GSPacketIn((short) 96);
                                exitConfirm.writeBoolean(true);
                                player.sendPacket(exitConfirm);
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Erro ao processar saída da sala: " + e.getMessage());
                }
                break;

            case 97: // GAME_ROOM_SHOOT / PLAYER_SHOOT
                try {
                    GamePlayer shootingPlayer = WorldMgr.getPlayer(ctx.channel().id());
                    if (shootingPlayer != null) {
                        int angle = packet.readInt();
                        int force = packet.readInt();

                        System.out.println("🚀 [Combate] " + shootingPlayer.getPlayerData().getNickname() +
                                " disparou! Ângulo: " + angle + " | Força: " + force);

                        for (GameRoom room : RoomMgr.getAllRooms()) {
                            if (room.getSlots().containsValue(shootingPlayer)) {
                                GSPacketIn shootBroadcast = new GSPacketIn((short) 97);
                                shootBroadcast.writeInt(shootingPlayer.getPlayerData().getId().intValue());
                                shootBroadcast.writeInt(angle);
                                shootBroadcast.writeInt(force);
                                shootBroadcast.writeInt(shootingPlayer.getBonusShootCount());
                                room.broadcastToRoom(shootBroadcast);

                                shootingPlayer.clearBuffs();

                                if (room.getTurnMgr() != null) {
                                    room.getTurnMgr().nextTurn();
                                }
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Erro ao processar disparo do jogador: " + e.getMessage());
                }
                break;

            case 99: // GAME_ROOM_DAMAGE / TAKE_DAMAGE
                try {
                    GamePlayer actionPlayer = WorldMgr.getPlayer(ctx.channel().id());
                    if (actionPlayer != null) {
                        int targetId = packet.readInt();
                        int damageValue = packet.readInt();

                        for (GameRoom room : RoomMgr.getAllRooms()) {
                            if (room.getSlots().containsValue(actionPlayer)) {
                                for (GamePlayer target : room.getSlots().values()) {
                                    if (target.getPlayerData().getId().intValue() == targetId) {
                                        int adjustedDamage = (int) (damageValue * actionPlayer.getActiveDamageMultiplier());
                                        target.takeDamage(adjustedDamage);

                                        System.out.println("💥 [Combate] " + target.getPlayerData().getNickname() +
                                                " sofreu -" + adjustedDamage + " HP (Modificado).");

                                        GSPacketIn damageBroadcast = new GSPacketIn((short) 99);
                                        damageBroadcast.writeInt(targetId);
                                        damageBroadcast.writeInt(target.getCurrentHp());
                                        damageBroadcast.writeBoolean(target.isAlive());
                                        room.broadcastToRoom(damageBroadcast);

                                        if (room.getTurnMgr() != null) {
                                            room.getTurnMgr().nextTurn();
                                        }
                                        break;
                                    }
                                }
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Erro ao processar dano: " + e.getMessage());
                }
                break;

            case 101: // GAME_ROOM_USE_PROP / USE_PROP
                try {
                    GamePlayer user = WorldMgr.getPlayer(ctx.channel().id());
                    if (user != null) {
                        int propId = packet.readInt();

                        switch (propId) {
                            case 10001:
                                user.addDamageMultiplier(1.5);
                                System.out.println("🧪 [Buff] " + user.getPlayerData().getNickname() + " ativou +50% de Dano.");
                                break;
                            case 10002:
                                user.addBonusShoots(1);
                                System.out.println("🧪 [Buff] " + user.getPlayerData().getNickname() + " ativou Tiro Duplo (+1 Projétil).");
                                break;
                            default:
                                System.out.println("🧪 [Buff] Item consumível não catalogado usado: " + propId);
                                break;
                        }

                        for (GameRoom room : RoomMgr.getAllRooms()) {
                            if (room.getSlots().containsValue(user)) {
                                GSPacketIn propBroadcast = new GSPacketIn((short) 101);
                                propBroadcast.writeInt(user.getPlayerData().getId().intValue());
                                propBroadcast.writeInt(propId);
                                room.broadcastToRoom(propBroadcast);
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Erro ao processar ativação de consumível de combate: " + e.getMessage());
                }
                break;

            default:
                System.out.println("[DDTank Lógica] OpCode sem tratamento atual: " + opCode);
                break;
        }
    }

    private void sendUserBag(GamePlayer gamePlayer, Player p) {
        UserItemRepository itemRepository = SpringContext.getBean(UserItemRepository.class);
        List<UserItem> items = itemRepository.findByPlayer(p);

        GSPacketIn bagPacket = new GSPacketIn((short) 41);
        bagPacket.writeInt(p.getId().intValue());
        bagPacket.writeInt(items.size());

        for (UserItem item : items) {
            bagPacket.writeInt(item.getTemplateId());
            bagPacket.writeInt(item.getPlace());
            bagPacket.writeInt(item.getCount());
            bagPacket.writeInt(item.getStrengthenLevel());
            bagPacket.writeInt(item.getAttackCompose());
            bagPacket.writeInt(item.getDefenseCompose());
        }
        gamePlayer.sendPacket(bagPacket);
        System.out.println("[DDTank Bag] Mochila updated para " + p.getNickname());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        System.err.println("[DDTank Netty] Sessão encerrada: " + cause.getMessage());
        ctx.close();
    }
}
