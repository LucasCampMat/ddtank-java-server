package ddtank.network;

import ddtank.config.SpringContext;
import ddtank.model.*;
import ddtank.packet.GSPacketIn;
import ddtank.repository.*;
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
        if (msg.readableBytes() < 6) return;

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
        GSPacketIn packet = new GSPacketIn(opCode, Unpooled.copiedBuffer(decryptedData));

        System.out.println("[DDTank Netty] Pacote processado. OpCode: " + packet.getCode());
        handlePacket(ctx, packet);
    }

    private void handlePacket(ChannelHandlerContext ctx, GSPacketIn packet) {
        short opCode = packet.getCode();

        switch (opCode) {
            case 1:
                handleLogin(ctx, packet);
                break;
            case 19:
                handleChat(ctx, packet);
                break;
            case 44:
                handleShopBuy(ctx, packet);
                break;
            case 76:
                handleStrengthen(ctx, packet);
                break;
            case 78:
                handleCompose(ctx, packet);
                break;
            case 79:
                handleFusion(ctx, packet);
                break;
            case 90:
                handleAngleOrSkip(ctx, packet);
                break;
            case 91:
                handleGameStart(ctx, packet);
                break;
            case 92:
                handlePlayerMove(ctx, packet);
                break;
            case 93:
                handleSwitchTeam(ctx, packet);
                break;
            case 94:
                handleRoomCreate(ctx, packet);
                break;
            case 95:
                handleRoomJoin(ctx, packet);
                break;
            case 96:
                handleRoomExit(ctx, packet);
                break;
            case 97:
                handlePlayerShoot(ctx, packet);
                break;
            case 99:
                handleTakeDamage(ctx, packet);
                break;
            case 101:
                handleUseProp(ctx, packet);
                break;
            case 102: handleUseUtil(ctx, packet); break;
            case 115:
                handleMailBox(ctx, packet);
                break;
            case 105: handleQuitMatch(ctx, packet); break;
            case 160:
                handleFriendList(ctx, packet);
                break;
            case 180:
                handleUserTitle(ctx, packet);
                break;
            case 210:
                handleEquipPet(ctx, packet);
                break;
            case 211:
                handlePetFeed(ctx, packet);
                break;
            case 212: handlePetSkillUpgrade(ctx, packet); break;
            case 120:
                handleAuction(ctx, packet);
                break;
            case 220:
                handleMarry(ctx, packet);
                break;
            case 221:
                handleDivorce(ctx, packet);
                break;
            case 74:
                handleStoneUpgrade(ctx, packet);
                break;
            default:
                System.out.println("[DDTank Lógica] OpCode sem tratamento: " + opCode);
                break;
        }
    }

    // --- MÉTODOS AUXILIARES ISOLADOS PARA MANTER O CÓDIGO LIMPO ---

    private void handlePetSkillUpgrade(ChannelHandlerContext ctx, GSPacketIn packet) {
        try {
            GamePlayer gamer = WorldMgr.getPlayer(ctx.channel().id());
            if (gamer == null) return;

            int targetSkillId = packet.readInt(); // O ID do talento/skill que a interface quer liberar (Ex: 5001 = Ataque Crítico)

            UserPetRepository petRepo = SpringContext.getBean(UserPetRepository.class);
            List<UserPet> userPets = petRepo.findByPlayer(gamer.getPlayerData());

            // 1. Localiza qual é o mascote que está ativamente equipado na conta
            UserPet activePet = userPets.stream().filter(UserPet::isEquipped).findFirst().orElse(null);

            GSPacketIn skillResponse = new GSPacketIn((short) 212);

            if (activePet != null) {
                int petLevel = activePet.getLevel();
                boolean canUnlock = false;

                // Regra de Negócio clássica convertida: bônus e skills exigem níveis mínimos do Pet
                if (targetSkillId == 5001 && petLevel >= 10) { // Skill 5001 exige Nível 10
                    activePet.setBonusAttack(activePet.getBonusAttack() + 20); // Incrementa o bônus permanente
                    canUnlock = true;
                } else if (targetSkillId == 5002 && petLevel >= 20) { // Skill 5002 exige Nível 20
                    activePet.setBonusDefense(activePet.getBonusDefense() + 20);
                    canUnlock = true;
                }

                if (canUnlock) {
                    // 2. Persiste os novos bônus de atributos do mascote diretamente no MySQL via JPA
                    petRepo.save(activePet);

                    System.out.println("🐾 [Pets] " + gamer.getPlayerData().getNickname() + " desbloqueou a Skill #" + targetSkillId + " para o pet " + activePet.getPetName());

                    // 3. Responde ao cliente autorizando a renderização da skill no painel do mascote
                    skillResponse.writeBoolean(true);
                    skillResponse.writeInt(targetSkillId);
                    gamer.sendPacket(skillResponse);
                } else {
                    skillResponse.writeBoolean(false);
                    skillResponse.writeString("Seu mascote não possui o nível necessário para desbloquear este talento.");
                    gamer.sendPacket(skillResponse);
                }
            } else {
                skillResponse.writeBoolean(false);
                skillResponse.writeString("Você precisa equipar um mascote para acessar a árvore de habilidades.");
                gamer.sendPacket(skillResponse);
            }
        } catch (Exception e) {
            System.err.println("Erro ao processar upgrade de habilidades de mascote: " + e.getMessage());
        }
    }


    private void handleQuitMatch(ChannelHandlerContext ctx, GSPacketIn packet) {
        try {
            GamePlayer quitter = WorldMgr.getPlayer(ctx.channel().id());
            if (quitter == null) return;

            Player p = quitter.getPlayerData();
            GameRoom room = findRoomByPlayer(quitter);

            GSPacketIn penaltyResponse = new GSPacketIn((short) 105);

            if (room != null && room.isPlaying()) {
                PlayerRepository playerRepo = SpringContext.getBean(PlayerRepository.class);

                // 1. Aplica a penalidade matemática convertida do C# alvo: perde 50 de GP (Experiência)
                int currentGp = p.getGp();
                int penaltyValue = 50;

                // Impede que o GP fique negativo no banco
                p.setGp(Math.max(0, currentGp - penaltyValue));

                // Recalcula o nível (Grade) caso a perda de GP rebaixe o jogador
                int newGrade = ddtank.network.LevelMgr.getLevelByGP(p.getGp());
                p.setGrade(newGrade);

                // Salva a punição de forma estável no MySQL via JPA
                playerRepo.save(p);

                System.out.println("❌ [Penalidade] " + p.getNickname() + " abandonou a partida em andamento. Perdeu " + penaltyValue + " GP.");

                // 2. Remove o jogador das estruturas de combate da sala de forma limpa para não travar o TurnMgr
                room.removePlayer(quitter);

                // 3. Responde ao desertor com a confirmação da penalidade e força o fechamento da tela de jogo dele
                penaltyResponse.writeBoolean(true);
                penaltyResponse.writeInt(p.getGp());
                penaltyResponse.writeInt(p.getGrade());
                quitter.sendPacket(penaltyResponse);

                // 4. Avisa os membros restantes da sala via mensagem de sistema sobre o abandono
                GSPacketIn alert = new GSPacketIn((short) 19);
                alert.writeInt(5); alert.writeInt(0); alert.writeString("Sistema");
                alert.writeString(p.getNickname() + " abandonou o combate e foi penalizado com a perda de 50 GP!");
                room.broadcastToRoom(alert);

                // Força o motor de turnos a passar a vez imediatamente para desatravancar o jogo
                if (room.getTurnMgr() != null) {
                    room.getTurnMgr().nextTurn();
                }
            }
        } catch (Exception e) {
            System.err.println("Erro ao processar penalidade de abandono de jogo: " + e.getMessage());
        }
    }


    private void handleUseUtil(ChannelHandlerContext ctx, GSPacketIn packet) {
        try {
            GamePlayer user = WorldMgr.getPlayer(ctx.channel().id());
            if (user == null) return;

            int utilId = packet.readInt(); // ID do item utilitário (Ex: 20001 = Teleporte Avião, 20002 = Escudo)

            GameRoom room = findRoomByPlayer(user);
            GSPacketIn utilBc = new GSPacketIn((short) 102);

            if (room != null && room.isPlaying()) {
                switch (utilId) {
                    case 20001: // CASO CLÁSSICO: Avião de Teletransporte do DDTank
                        // Lê as novas coordenadas X e Y para onde o jogador jogou a linha de voo
                        int targetX = packet.readInt();
                        int targetY = packet.readInt();

                        // Atualiza a posição cartesiana do jogador de forma imediata na memória RAM
                        user.setPosition(targetX, targetY, user.getDirection());
                        System.out.println("✈️ [Combate] " + user.getPlayerData().getNickname() + " usou Avião para X: " + targetX + " | Y: " + targetY);

                        // Prepara o broadcast com as novas coordenadas de pouso para atualizar as telas
                        utilBc.writeInt(user.getPlayerData().getId().intValue());
                        utilBc.writeInt(utilId);
                        utilBc.writeInt(targetX);
                        utilBc.writeInt(targetY);
                        room.broadcastToRoom(utilBc);
                        break;

                    case 20002: // CASO CLÁSSICO: Escudo de Mitigação de Dano
                        // Aplica uma blindagem multiplicadora inversa de 0.5 (Mitiga 50% do próximo dano sofrido)
                        user.addDamageMultiplier(0.5);
                        System.out.println("🛡️ [Combate] " + user.getPlayerData().getNickname() + " ativou Escudo Defensivo.");

                        // Avisa os clientes para renderizarem a bolha protetora azul ao redor do personagem
                        utilBc.writeInt(user.getPlayerData().getId().intValue());
                        utilBc.writeInt(utilId);
                        room.broadcastToRoom(utilBc);
                        break;

                    default:
                        System.out.println("✈️ [Combate] Item utilitário não listado acionado: " + utilId);
                        break;
                }
            }
        } catch (Exception e) {
            System.err.println("Erro ao processar item utilitário de batalha: " + e.getMessage());
        }
    }


    private void handlePetFeed(io.netty.channel.ChannelHandlerContext ctx, ddtank.packet.GSPacketIn packet) {
        try {
            GamePlayer gamer = WorldMgr.getPlayer(ctx.channel().id());
            if (gamer == null) return;

            int foodBagPlace = packet.readInt(); // O slot da mochila onde a ração está guardada

            UserPetRepository petRepo = SpringContext.getBean(UserPetRepository.class);
            UserItemRepository itemRepo = SpringContext.getBean(UserItemRepository.class);

            List<UserPet> userPets = petRepo.findByPlayer(gamer.getPlayerData());
            List<UserItem> userBag = itemRepo.findByPlayer(gamer.getPlayerData());

            // 1. Localiza qual é o pet que está ativamente equipado e precisa de comida
            UserPet equippedPet = userPets.stream().filter(UserPet::isEquipped).findFirst().orElse(null);

            // 2. Localiza o item de ração no slot enviado pelo cliente (Simulando TemplateID 3001 como comida de pet)
            UserItem foodItem = userBag.stream().filter(i -> i.getPlace() == foodBagPlace).findFirst().orElse(null);

            GSPacketIn petFeedResponse = new GSPacketIn((short) 211);

            if (equippedPet != null && foodItem != null && foodItem.getCount() > 0) {
                // 3. Consome 1 unidade do item de ração do banco de dados
                if (foodItem.getCount() == 1) {
                    itemRepo.delete(foodItem);
                } else {
                    foodItem.setCount(foodItem.getCount() - 1);
                    itemRepo.save(foodItem);
                }

                // 4. Alimenta o pet adicionando 25 pontos de saciedade na memória RAM e persiste no MySQL
                equippedPet.feed(25);
                petRepo.save(equippedPet);

                System.out.println("🐾 [Pets] " + gamer.getPlayerData().getNickname() + " alimentou o pet " + equippedPet.getPetName() + ". Nova Fome: " + equippedPet.getHunger());

                // 5. Envia resposta de sucesso absoluto com o novo valor de fome para atualizar o medidor visual do Flash
                petFeedResponse.writeBoolean(true);
                petFeedResponse.writeInt(equippedPet.getHunger());
                gamer.sendPacket(petFeedResponse);

                // 6. Sincroniza a mochila atualizada para o jogador ver a quantidade de ração diminuir
                sendUserBag(gamer, gamer.getPlayerData());
            } else {
                petFeedResponse.writeBoolean(false);
                petFeedResponse.writeString("Não foi possível alimentar. Certifique-se de ter um pet equipado e ração no inventário.");
                gamer.sendPacket(petFeedResponse);
            }
        } catch (Exception e) {
            System.err.println("Erro ao processar alimentação de mascote: " + e.getMessage());
        }
    }


    private void handleEquipPet(io.netty.channel.ChannelHandlerContext ctx, ddtank.packet.GSPacketIn packet) {
        try {
            GamePlayer gamer = WorldMgr.getPlayer(ctx.channel().id());
            if (gamer == null) return;

            int targetPetId = packet.readInt(); // ID do Pet enviado pela interface do jogo

            UserPetRepository petRepo = SpringContext.getBean(UserPetRepository.class);
            List<UserPet> userPets = petRepo.findByPlayer(gamer.getPlayerData());

            GSPacketIn petResponse = new GSPacketIn((short) 210);

            // Procura o pet correspondente na coleção do jogador no MySQL
            UserPet selectedPet = userPets.stream()
                    .filter(p -> p.getId().intValue() == targetPetId)
                    .findFirst().orElse(null);

            if (selectedPet != null) {
                // 1. Desequipa todos os pets antigos do jogador para manter apenas um ativo
                for (UserPet p : userPets) {
                    if (p.isEquipped()) {
                        p.setEquipped(false);
                        petRepo.save(p);
                    }
                }

                // 2. Ativa e equipa o pet selecionado persistindo no MySQL
                selectedPet.setEquipped(true);
                petRepo.save(selectedPet);

                System.out.println("🐾 [Pets] " + gamer.getPlayerData().getNickname() + " equipou o mascote: " + selectedPet.getPetName());

                // 3. Responde com sucesso absoluto para o cliente renderizar o pet ao lado do personagem
                petResponse.writeBoolean(true);
                petResponse.writeInt(targetPetId);
                gamer.sendPacket(petResponse);
            } else {
                petResponse.writeBoolean(false);
                petResponse.writeString("Mascote inválido ou não encontrado na sua coleção.");
                gamer.sendPacket(petResponse);
            }
        } catch (Exception e) {
            System.err.println("Erro ao processar ativação de mascote: " + e.getMessage());
        }
    }

    private void handleUserTitle(io.netty.channel.ChannelHandlerContext ctx, ddtank.packet.GSPacketIn packet) {
        try {
            GamePlayer gamer = WorldMgr.getPlayer(ctx.channel().id());
            if (gamer == null) return;

            int targetTitleId = packet.readInt(); // O ID do título que o jogador quer equipar (ex: 2)

            UserTitleRepository titleRepo = SpringContext.getBean(UserTitleRepository.class);
            List<UserTitle> userTitles = titleRepo.findByPlayer(gamer.getPlayerData());

            GSPacketIn titleResponse = new GSPacketIn((short) 180);

            // Localiza se o jogador realmente possui o título solicitado em sua coleção
            UserTitle selectedTitle = userTitles.stream()
                    .filter(t -> t.getTitleId() == targetTitleId)
                    .findFirst().orElse(null);

            if (selectedTitle != null) {
                // 1. Remove o equipamento visual de todos os outros títulos antigos dele para evitar duplicidade
                for (UserTitle t : userTitles) {
                    if (t.isEquipped()) {
                        t.setEquipped(false);
                        titleRepo.save(t);
                    }
                }

                // 2. Equipará o novo título selecionado persistindo a flag no MySQL
                selectedTitle.setEquipped(true);
                titleRepo.save(selectedTitle);

                System.out.println("🏅 [Título] " + gamer.getPlayerData().getNickname() + " equipou a insígnia #" + targetTitleId);

                // 3. Responde ao cliente autorizando a renderização da insígnia sobre a cabeça do avatar
                titleResponse.writeBoolean(true);
                titleResponse.writeInt(targetTitleId);
                gamer.sendPacket(titleResponse);

                // 4. Se ele estiver dentro de uma sala de jogo, retransmite a atualização para os parceiros verem o novo visual
                GameRoom room = findRoomByPlayer(gamer);
                if (room != null) {
                    room.broadcastToRoom(titleResponse);
                }
            } else {
                titleResponse.writeBoolean(false);
                titleResponse.writeString("Você ainda não desbloqueou esta conquista de título!");
                gamer.sendPacket(titleResponse);
            }
        } catch (Exception e) {
            System.err.println("Erro ao processar alteração de título honorífico: " + e.getMessage());
        }
    }


    private void handleStoneUpgrade(io.netty.channel.ChannelHandlerContext ctx, ddtank.packet.GSPacketIn packet) {
        try {
            GamePlayer smithPlayer = WorldMgr.getPlayer(ctx.channel().id());
            if (smithPlayer == null) return;

            // O cliente envia as posições das 4 pedras de atributo idênticas a serem fundidas
            int s1 = packet.readInt();
            int s2 = packet.readInt();
            int s3 = packet.readInt();
            int s4 = packet.readInt();

            UserItemRepository itemRepo = SpringContext.getBean(UserItemRepository.class);
            List<UserItem> bag = itemRepo.findByPlayer(smithPlayer.getPlayerData());

            UserItem item1 = bag.stream().filter(i -> i.getPlace() == s1).findFirst().orElse(null);
            UserItem item2 = bag.stream().filter(i -> i.getPlace() == s2).findFirst().orElse(null);
            UserItem item3 = bag.stream().filter(i -> i.getPlace() == s3).findFirst().orElse(null);
            UserItem item4 = bag.stream().filter(i -> i.getPlace() == s4).findFirst().orElse(null);

            GSPacketIn upgradeResponse = new GSPacketIn((short) 74);

            // Garante a existência física das 4 pedras originais na mochila do MySQL
            if (item1 != null && item2 != null && item3 != null && item4 != null) {
                // Captura o TemplateID original (ex: ID da Pedra de Ataque Nv 1)
                int currentTemplateId = item1.getTemplateId();

                // 1. Consome (apaga) as quatro pedras antigas do MySQL
                itemRepo.delete(item1);
                itemRepo.delete(item2);
                itemRepo.delete(item3);
                itemRepo.delete(item4);

                // 2. Cria a nova pedra de nível superior incrementando o ID estrutural (Regra padrão DDTank)
                int upgradedTemplateId = currentTemplateId + 1;

                // Recalcula o espaço para não encavalar os slots
                int freeSlot = itemRepo.findByPlayer(smithPlayer.getPlayerData()).size();
                UserItem advancedStone = new UserItem(smithPlayer.getPlayerData(), upgradedTemplateId, freeSlot);
                itemRepo.save(advancedStone);

                System.out.println("🔨 [Ferreiro] Upgrade concluído! " + smithPlayer.getPlayerData().getNickname() +
                        " gerou a Pedra Nível Superior: " + upgradedTemplateId);

                // 3. Retorna confirmação de sucesso absoluto para acionar os efeitos do Flash
                upgradeResponse.writeBoolean(true);
                upgradeResponse.writeInt(upgradedTemplateId);
                smithPlayer.sendPacket(upgradeResponse);

                // 4. Força a sincronização visual da mochila
                sendUserBag(smithPlayer, smithPlayer.getPlayerData());
            } else {
                upgradeResponse.writeBoolean(false);
                upgradeResponse.writeString("Você não possui as 4 pedras necessárias no seu inventário.");
                smithPlayer.sendPacket(upgradeResponse);
            }
        } catch (Exception e) {
            System.err.println("Erro ao processar upgrade de pedras no ferreiro: " + e.getMessage());
        }
    }


    private void handleDivorce(io.netty.channel.ChannelHandlerContext ctx, ddtank.packet.GSPacketIn packet) {
        try {
            GamePlayer player = WorldMgr.getPlayer(ctx.channel().id());
            if (player == null) return;

            Player p = player.getPlayerData();
            UserMarryRepository marryRepo = SpringContext.getBean(UserMarryRepository.class);
            PlayerRepository playerRepo = SpringContext.getBean(PlayerRepository.class);

            GSPacketIn divorceResponse = new GSPacketIn((short) 221);

            // 1. Busca se o jogador realmente possui uma união matrimonial ativa no MySQL
            java.util.Optional<UserMarry> marriageOpt = marryRepo.findActiveMarriage(p);

            if (marriageOpt.isPresent()) {
                UserMarry marriage = marriageOpt.get();

                // 2. Cobra a taxa de divórcio (Desconta 300 moedas de Money de quem solicita)
                if (p.decreaseMoney(300)) {
                    // Atualiza o saldo do solicitante no banco
                    playerRepo.save(p);

                    // 3. Altera a propriedade de casamento para divorciado e persiste no MySQL
                    marriage.setDivorced(true);
                    marryRepo.save(marriage);

                    System.out.println("💔 [Capela] Separação concluída para a união #" + marriage.getId());

                    // 4. Responde ao solicitante confirmando o divórcio
                    divorceResponse.writeBoolean(true);
                    divorceResponse.writeString("Você se divorciou com sucesso e a aliança foi quebrada.");
                    player.sendPacket(divorceResponse);

                    // 5. Se o ex-cônjuge estiver online na memória RAM, notifica o cliente dele também em tempo real
                    Player exSpouse = marriage.getHusband().equals(p) ? marriage.getWife() : marriage.getHusband();
                    GamePlayer onlineEx = WorldMgr.getPlayerByNickname(exSpouse.getNickname());
                    if (onlineEx != null) {
                        GSPacketIn spouseNotify = new GSPacketIn((short) 221);
                        spouseNotify.writeBoolean(true);
                        spouseNotify.writeString("Seu casamento foi dissolvido na Capela.");
                        onlineEx.sendPacket(spouseNotify);
                    }
                } else {
                    divorceResponse.writeBoolean(false);
                    divorceResponse.writeString("Money insuficiente para pagar a taxa de divórcio (Custo: 300 Money).");
                    player.sendPacket(divorceResponse);
                }
            } else {
                divorceResponse.writeBoolean(false);
                divorceResponse.writeString("Você não está em um casamento ativo no momento.");
                player.sendPacket(divorceResponse);
            }
        } catch (Exception e) {
            System.err.println("Erro ao processar divórcio: " + e.getMessage());
        }
    }


    private void handleMarry(io.netty.channel.ChannelHandlerContext ctx, ddtank.packet.GSPacketIn packet) {
        try {
            GamePlayer suitor = WorldMgr.getPlayer(ctx.channel().id());
            if (suitor == null) return;

            String partnerNickname = packet.readString(); // Nome do parceiro para propor casamento
            int ringSlot = packet.readInt();              // Slot onde a aliança está equipada

            Player pSuitor = suitor.getPlayerData();
            UserMarryRepository marryRepo = SpringContext.getBean(UserMarryRepository.class);
            PlayerRepository playerRepo = SpringContext.getBean(PlayerRepository.class);

            GSPacketIn marryResponse = new GSPacketIn((short) 220);

            // 1. Valida se quem está propondo já não é casado
            if (marryRepo.findActiveMarriage(pSuitor).isPresent()) {
                marryResponse.writeBoolean(false);
                marryResponse.writeString("Você já está casado!");
                suitor.sendPacket(marryResponse);
                return;
            }

            // 2. Busca o parceiro escolhido na memória RAM global (precisa estar online na capela)
            GamePlayer partner = WorldMgr.getPlayerByNickname(partnerNickname);

            if (partner != null) {
                Player pPartner = partner.getPlayerData();

                // Valida se o parceiro também não é casado
                if (marryRepo.findActiveMarriage(pPartner).isPresent()) {
                    marryResponse.writeBoolean(false);
                    marryResponse.writeString("O alvo já está casado com outra pessoa!");
                    suitor.sendPacket(marryResponse);
                    return;
                }

                // 3. Simula a taxa da cerimônia (Desconta 500 moedas de Money de quem propõe)
                if (pSuitor.decreaseMoney(500)) {
                    playerRepo.save(pSuitor);

                    // 4. Cria o laço estável na tabela 'user_marry' do MySQL
                    UserMarry marriage = new UserMarry(pSuitor, pPartner);
                    marryRepo.save(marriage);

                    System.out.println("💍 [Capela] Casamento realizado! " + pSuitor.getNickname() + " se casou com " + pPartner.getNickname());

                    // 5. Notifica o casal com sucesso absoluto
                    marryResponse.writeBoolean(true);
                    marryResponse.writeString("Casamento realizado com sucesso na Capela!");
                    suitor.sendPacket(marryResponse);
                    partner.sendPacket(marryResponse);
                } else {
                    marryResponse.writeBoolean(false);
                    marryResponse.writeString("Money insuficiente para alugar a capela (Custo: 500 Money).");
                    suitor.sendPacket(marryResponse);
                }
            } else {
                marryResponse.writeBoolean(false);
                marryResponse.writeString("O parceiro escolhido encontra-se offline.");
                suitor.sendPacket(marryResponse);
            }
        } catch (Exception e) {
            System.err.println("Erro ao processar cerimônia de casamento: " + e.getMessage());
        }
    }


    private void handleLogin(ChannelHandlerContext ctx, GSPacketIn packet) {
        try {
            packet.readInt();
            packet.readInt(); // Versão e Tipo do Cliente
            String username = packet.readString();
            String password = packet.readString();

            PlayerRepository pRepo = SpringContext.getBean(PlayerRepository.class);
            Optional<Player> playerOpt = pRepo.findByUsername(username);

            if (playerOpt.isPresent() && playerOpt.get().getPassword().equals(password)) {
                Player p = playerOpt.get();
                GamePlayer gamePlayer = new GamePlayer(ctx, p);
                WorldMgr.addPlayer(ctx.channel().id(), gamePlayer);

                GSPacketIn response = new GSPacketIn((short) 1);
                response.writeBoolean(true);
                response.writeInt(p.getId().intValue());
                response.writeString(p.getNickname());
                gamePlayer.sendPacket(response);

                sendUserBag(gamePlayer, p);
            } else {
                GSPacketIn response = new GSPacketIn((short) 1);
                response.writeBoolean(false);
                response.writeString("Usuário ou senha incorretos.");
                ctx.writeAndFlush(response.getBuffer());
            }
        } catch (Exception e) {
            System.err.println("Erro no Login: " + e.getMessage());
        }
    }

    private void handleChat(ChannelHandlerContext ctx, GSPacketIn packet) {
        try {
            GamePlayer sender = WorldMgr.getPlayer(ctx.channel().id());
            if (sender == null) return;

            int chatType = packet.readInt(); // 0 = Geral, 1 = Time, 2 = Sussurro, 3 = Guilda
            String targetName = packet.readString();
            String content = packet.readString();

            GSPacketIn chatResponse = new GSPacketIn((short) 19);
            chatResponse.writeInt(chatType);
            chatResponse.writeInt(sender.getPlayerData().getId().intValue());
            chatResponse.writeString(sender.getPlayerData().getNickname());
            chatResponse.writeString(content);

            // 1. FLUXO DE EXECUÇÃO DE SUSSURRO PRIVADO (chatType == 2)
            if (chatType == 2) {
                GamePlayer receiver = WorldMgr.getPlayerByNickname(targetName);
                if (receiver != null) {
                    receiver.sendPacket(chatResponse);
                    sender.sendPacket(chatResponse);
                } else {
                    sendSysMessage(sender, "O jogador '" + targetName + "' encontra-se offline.");
                }
                return;
            }

            // 2. FLUXO DE EXECUÇÃO DE CHAT DE GUILDA / SOCIEDADE (chatType == 3)
            if (chatType == 3) {
                String guildName = sender.getPlayerData().getGuildName();
                if (guildName != null && !guildName.isEmpty()) {
                    WorldMgr.broadcastToGuild(guildName, chatResponse);
                    System.out.println("💬 [Chat Guilda] " + sender.getPlayerData().getNickname() + " enviou mensagem para a guilda: " + guildName);
                } else {
                    sendSysMessage(sender, "Você não pertence a nenhuma guilda no momento!");
                }
                return;
            }

            // 3. FLUXO DE CHAT REGULAR DE SALA OU LOBBY GLOBAL (Geral / Time)
            GameRoom room = findRoomByPlayer(sender);
            if (chatType == 1 && room != null) { // Chat Time
                int slot = findSlotByPlayer(room, sender);
                if (slot != -1) room.broadcastToTeam(slot, chatResponse);
            } else if (room != null) {
                room.broadcastToRoom(chatResponse);
            } else {
                WorldMgr.broadcastPacket(chatResponse);
            }
        } catch (Exception e) {
            System.err.println("Erro no roteamento avançado do Chat: " + e.getMessage());
        }
    }


    private void handleShopBuy(ChannelHandlerContext ctx, GSPacketIn packet) {
        try {
            GamePlayer buyer = WorldMgr.getPlayer(ctx.channel().id());
            if (buyer == null) return;

            int templateId = packet.readInt();
            int currencyType = packet.readInt();
            int price = 500;
            Player p = buyer.getPlayerData();

            boolean success = (currencyType == 0) ? p.decreaseGold(price) : p.decreaseMoney(price);
            GSPacketIn shopResponse = new GSPacketIn((short) 44);

            if (success) {
                SpringContext.getBean(PlayerRepository.class).save(p);
                UserItemRepository iRepo = SpringContext.getBean(UserItemRepository.class);
                UserItem newItem = new UserItem(p, templateId, iRepo.findByPlayer(p).size());
                iRepo.save(newItem);

                shopResponse.writeBoolean(true);
                buyer.sendPacket(shopResponse);
                sendUserBag(buyer, p);
            } else {
                shopResponse.writeBoolean(false);
                shopResponse.writeString("Saldo insuficiente.");
                buyer.sendPacket(shopResponse);
            }
        } catch (Exception e) {
            System.err.println("Erro na Loja: " + e.getMessage());
        }
    }

    private void handleStrengthen(ChannelHandlerContext ctx, GSPacketIn packet) {
        try {
            GamePlayer smithPlayer = WorldMgr.getPlayer(ctx.channel().id());
            if (smithPlayer == null) return;

            int place = packet.readInt();
            UserItemRepository iRepo = SpringContext.getBean(UserItemRepository.class);
            List<UserItem> items = iRepo.findByPlayer(smithPlayer.getPlayerData());

            UserItem weapon = items.stream().filter(i -> i.getPlace() == place).findFirst().orElse(null);
            GSPacketIn response = new GSPacketIn((short) 76);

            if (weapon != null && weapon.getStrengthenLevel() < 12) {
                int lv = weapon.getStrengthenLevel();
                if (new Random().nextInt(100) < Math.max(100 - (lv * 8), 5)) {
                    weapon.setStrengthenLevel(lv + 1);
                    iRepo.save(weapon);
                    response.writeBoolean(true);
                    response.writeInt(weapon.getStrengthenLevel());
                } else {
                    response.writeBoolean(false);
                    response.writeString("O fortalecimento falhou.");
                }
                smithPlayer.sendPacket(response);
                sendUserBag(smithPlayer, smithPlayer.getPlayerData());
            }
        } catch (Exception e) {
            System.err.println("Erro no Ferreiro: " + e.getMessage());
        }
    }

    private void handleCompose(ChannelHandlerContext ctx, GSPacketIn packet) {
        try {
            GamePlayer smithPlayer = WorldMgr.getPlayer(ctx.channel().id());
            if (smithPlayer == null) return;

            int place = packet.readInt();
            int type = packet.readByte();
            int power = packet.readInt();

            UserItemRepository iRepo = SpringContext.getBean(UserItemRepository.class);
            UserItem item = iRepo.findByPlayer(smithPlayer.getPlayerData()).stream().filter(i -> i.getPlace() == place).findFirst().orElse(null);
            GSPacketIn response = new GSPacketIn((short) 78);

            if (item != null && new Random().nextInt(100) < 60) {
                switch (type) {
                    case 1:
                        item.setAttackCompose(item.getAttackCompose() + power);
                        break;
                    case 2:
                        item.setDefenseCompose(item.getDefenseCompose() + power);
                        break;
                    case 3:
                        item.setAgilityCompose(item.getAgilityCompose() + power);
                        break;
                    case 4:
                        item.setLuckCompose(item.getLuckCompose() + power);
                        break;
                }
                iRepo.save(item);
                response.writeBoolean(true);
            } else {
                response.writeBoolean(false);
                response.writeString("A fusão falhou!");
            }
            smithPlayer.sendPacket(response);
            sendUserBag(smithPlayer, smithPlayer.getPlayerData());
        } catch (Exception e) {
            System.err.println("Erro na Composição: " + e.getMessage());
        }
    }

    private void handleFusion(ChannelHandlerContext ctx, GSPacketIn packet) {
        try {
            GamePlayer smithPlayer = WorldMgr.getPlayer(ctx.channel().id());
            if (smithPlayer == null) return;

            int s1 = packet.readInt();
            int s2 = packet.readInt();
            int s3 = packet.readInt();
            int s4 = packet.readInt();
            UserItemRepository iRepo = SpringContext.getBean(UserItemRepository.class);
            List<UserItem> bag = iRepo.findByPlayer(smithPlayer.getPlayerData());

            UserItem i1 = bag.stream().filter(i -> i.getPlace() == s1).findFirst().orElse(null);
            UserItem i2 = bag.stream().filter(i -> i.getPlace() == s2).findFirst().orElse(null);
            UserItem i3 = bag.stream().filter(i -> i.getPlace() == s3).findFirst().orElse(null);
            UserItem i4 = bag.stream().filter(i -> i.getPlace() == s4).findFirst().orElse(null);

            GSPacketIn response = new GSPacketIn((short) 79);

            if (i1 != null && i2 != null && i3 != null && i4 != null) {
                iRepo.delete(i1);
                iRepo.delete(i2);
                iRepo.delete(i3);
                iRepo.delete(i4);
                int rewardId = new Random().nextBoolean() ? 1001 : 1002;
                iRepo.save(new UserItem(smithPlayer.getPlayerData(), rewardId, iRepo.findByPlayer(smithPlayer.getPlayerData()).size()));

                response.writeBoolean(true);
                response.writeInt(rewardId);
                smithPlayer.sendPacket(response);
                sendUserBag(smithPlayer, smithPlayer.getPlayerData());
            } else {
                response.writeBoolean(false);
                response.writeString("Itens inválidos.");
                smithPlayer.sendPacket(response);
            }
        } catch (Exception e) {
            System.err.println("Erro na Fusão: " + e.getMessage());
        }
    }

    private void handleAngleOrSkip(ChannelHandlerContext ctx, GSPacketIn packet) {
        try {
            GamePlayer player = WorldMgr.getPlayer(ctx.channel().id());
            if (player == null) return;

            int parameter = packet.readInt();
            GameRoom room = findRoomByPlayer(player);

            if (parameter == -9999) { // Skip Turn
                player.clearBuffs();
                if (room != null && room.getTurnMgr() != null) room.getTurnMgr().nextTurn();
            } else { // Angle Change
                player.setAngle(parameter);
                if (room != null) {
                    GSPacketIn angleBc = new GSPacketIn((short) 90);
                    angleBc.writeInt(player.getPlayerData().getId().intValue());
                    angleBc.writeInt(parameter);
                    room.broadcastToRoom(angleBc);
                }
            }
        } catch (Exception e) {
            System.err.println("Erro no Ângulo/Skip: " + e.getMessage());
        }
    }

    private void handleGameStart(ChannelHandlerContext ctx, GSPacketIn packet) {
        try {
            GamePlayer leader = WorldMgr.getPlayer(ctx.channel().id());
            GameRoom room = findRoomByPlayer(leader);

            if (room != null && room.getRoomOwner().equals(leader)) {
                if (room.canStartGame()) {
                    room.setPlaying(true);
                    GSPacketIn startPacket = new GSPacketIn((short) 91);
                    startPacket.writeBoolean(true);
                    startPacket.writeInt(1); // Mapa 1
                    room.broadcastToRoom(startPacket);
                    room.startTurnEngine();
                } else {
                    sendSysMessage(leader, "Todos os jogadores devem estar 'Pronto'.");
                }
            } else if (room != null) {
                room.toggleReady(leader);
            }
        } catch (Exception e) {
            System.err.println("Erro no Game Start: " + e.getMessage());
        }
    }

    private void handlePlayerMove(ChannelHandlerContext ctx, GSPacketIn packet) {
        try {
            GamePlayer player = WorldMgr.getPlayer(ctx.channel().id());
            if (player == null) return;

            int x = packet.readInt();
            int y = packet.readInt();
            int dir = packet.readByte();
            player.setPosition(x, y, dir);

            GameRoom room = findRoomByPlayer(player);
            if (room != null) {
                GSPacketIn moveBc = new GSPacketIn((short) 92);
                moveBc.writeInt(player.getPlayerData().getId().intValue());
                moveBc.writeInt(x);
                moveBc.writeInt(y);
                moveBc.writeByte(dir);
                room.broadcastToRoom(moveBc);
            }
        } catch (Exception e) {
            System.err.println("Erro no Movimento: " + e.getMessage());
        }
    }

    private void handleSwitchTeam(ChannelHandlerContext ctx, GSPacketIn packet) {
        try {
            GamePlayer player = WorldMgr.getPlayer(ctx.channel().id());
            int desiredSlot = packet.readByte();
            GameRoom room = findRoomByPlayer(player);
            if (room != null) room.switchSlot(player, desiredSlot);
        } catch (Exception e) {
            System.err.println("Erro na Troca de Time: " + e.getMessage());
        }
    }

    private void handleRoomCreate(ChannelHandlerContext ctx, GSPacketIn packet) {
        try {
            GamePlayer creator = WorldMgr.getPlayer(ctx.channel().id());
            if (creator == null) return;

            packet.readInt();
            packet.readByte(); // Ignora metadados simples
            String name = packet.readString();

            GameRoom newRoom = RoomMgr.createRoom(name, creator);
            GSPacketIn response = new GSPacketIn((short) 94);
            response.writeBoolean(true);
            response.writeInt(newRoom.getRoomId());
            response.writeString(newRoom.getRoomName());
            creator.sendPacket(response);
        } catch (Exception e) {
            System.err.println("Erro ao criar sala: " + e.getMessage());
        }
    }

    private void handleRoomJoin(ChannelHandlerContext ctx, GSPacketIn packet) {
        try {
            GamePlayer player = WorldMgr.getPlayer(ctx.channel().id());
            if (player == null) return;

            int roomId = packet.readInt();
            GameRoom room = RoomMgr.getRoom(roomId);
            GSPacketIn response = new GSPacketIn((short) 95);

            if (room != null && !room.isPlaying() && room.addPlayer(player)) {
                System.out.println("[Room] Jogador entrou na sala: " + roomId);
            } else {
                response.writeBoolean(false);
                response.writeString("Sala indisponível ou lotada.");
                player.sendPacket(response);
            }
        } catch (Exception e) {
            System.err.println("Erro ao entrar na sala: " + e.getMessage());
        }
    }

    private void handleRoomExit(ChannelHandlerContext ctx, GSPacketIn packet) {
        try {
            GamePlayer player = WorldMgr.getPlayer(ctx.channel().id());
            GameRoom room = findRoomByPlayer(player);
            if (room != null) {
                room.removePlayer(player);
                GSPacketIn response = new GSPacketIn((short) 96);
                response.writeBoolean(true);
                player.sendPacket(response);
            }
        } catch (Exception e) {
            System.err.println("Erro ao sair da sala: " + e.getMessage());
        }
    }

    private void handlePlayerShoot(ChannelHandlerContext ctx, GSPacketIn packet) {
        try {
            GamePlayer shooter = WorldMgr.getPlayer(ctx.channel().id());
            if (shooter == null) return;

            int angle = packet.readInt();
            int force = packet.readInt();
            GameRoom room = findRoomByPlayer(shooter);

            if (room != null) {
                GSPacketIn shootBc = new GSPacketIn((short) 97);
                shootBc.writeInt(shooter.getPlayerData().getId().intValue());
                shootBc.writeInt(angle);
                shootBc.writeInt(force);
                shootBc.writeInt(shooter.getBonusShootCount());
                room.broadcastToRoom(shootBc);

                shooter.clearBuffs();
                if (room.getTurnMgr() != null) room.getTurnMgr().nextTurn();
            }
        } catch (Exception e) {
            System.err.println("Erro ao Atirar: " + e.getMessage());
        }
    }

    private void handleTakeDamage(ChannelHandlerContext ctx, GSPacketIn packet) {
        try {
            GamePlayer attacker = WorldMgr.getPlayer(ctx.channel().id());
            int targetId = packet.readInt();
            int dmg = packet.readInt();
            GameRoom room = findRoomByPlayer(attacker);

            if (room != null) {
                GamePlayer target = room.getSlots().values().stream()
                        .filter(t -> t.getPlayerData().getId().intValue() == targetId)
                        .findFirst().orElse(null);

                if (target != null) {
                    int finalDmg = (int) (dmg * attacker.getActiveDamageMultiplier());
                    target.takeDamage(finalDmg);

                    GSPacketIn dmgBc = new GSPacketIn((short) 99);
                    dmgBc.writeInt(targetId);
                    dmgBc.writeInt(target.getCurrentHp());
                    dmgBc.writeBoolean(target.isAlive());
                    room.broadcastToRoom(dmgBc);

                    // ADICIONADO: Aciona o cálculo físico de queda e escavação baseado no ponto de impacto (Posição X/Y atual da vítima)
                    // Simula um raio padrão de explosão e deformação de relevo de 40 pixels
                    room.checkTerrainDestruction(target.getPosX(), target.getPosY(), 40);

                    if (room.getTurnMgr() != null) {
                        room.getTurnMgr().nextTurn();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Erro ao aplicar dano com física de terreno: " + e.getMessage());
        }
    }


    private void handleUseProp(ChannelHandlerContext ctx, GSPacketIn packet) {
        try {
            GamePlayer user = WorldMgr.getPlayer(ctx.channel().id());
            if (user == null) return;

            int propId = packet.readInt();
            if (propId == 10001) user.addDamageMultiplier(1.5);
            else if (propId == 10002) user.addBonusShoots(1);

            GameRoom room = findRoomByPlayer(user);
            if (room != null) {
                GSPacketIn propBc = new GSPacketIn((short) 101);
                propBc.writeInt(user.getPlayerData().getId().intValue());
                propBc.writeInt(propId);
                room.broadcastToRoom(propBc);
            }
        } catch (Exception e) {
            System.err.println("Erro ao usar consumível: " + e.getMessage());
        }
    }

    private void handleMailBox(ChannelHandlerContext ctx, GSPacketIn packet) {
        try {
            GamePlayer req = WorldMgr.getPlayer(ctx.channel().id());
            if (req == null) return;

            List<UserMail> mails = SpringContext.getBean(UserMailRepository.class).findByReceiverOrderBySentTimeDesc(req.getPlayerData());
            GSPacketIn response = new GSPacketIn((short) 115);
            response.writeInt(mails.size());

            for (UserMail m : mails) {
                response.writeInt(m.getId().intValue());
                response.writeString(m.getSender() != null ? m.getSender().getNickname() : "Sistema");
                response.writeString(m.getTitle());
                response.writeString(m.getContent());
                response.writeInt(m.getGoldAnnex());
                response.writeInt(m.getMoneyAnnex());
                response.writeBoolean(m.isRead());
            }
            req.sendPacket(response);
        } catch (Exception e) {
            System.err.println("Erro no Correio: " + e.getMessage());
        }
    }

    private void handleFriendList(ChannelHandlerContext ctx, GSPacketIn packet) {
        try {
            GamePlayer req = WorldMgr.getPlayer(ctx.channel().id());
            if (req == null) return;

            List<UserFriend> friends = SpringContext.getBean(UserFriendRepository.class).findByPlayer(req.getPlayerData());
            GSPacketIn response = new GSPacketIn((short) 160);
            response.writeInt(friends.size());

            for (UserFriend f : friends) {
                Player fd = f.getFriend();
                response.writeInt(fd.getId().intValue());
                response.writeString(fd.getNickname());
                response.writeInt(fd.getGrade());
                response.writeBoolean(WorldMgr.getPlayerByNickname(fd.getNickname()) != null);
            }
            req.sendPacket(response);
        } catch (Exception e) {
            System.err.println("Erro na Lista de Amigos: " + e.getMessage());
        }
    }

    private void handleAuction(ChannelHandlerContext ctx, GSPacketIn packet) {
        try {
            GamePlayer seller = WorldMgr.getPlayer(ctx.channel().id());
            if (seller == null) return;

            int itemBagPlace = packet.readInt(); // Slot do item na mochila que o jogador quer vender
            int priceGold = packet.readInt();    // Preço em Gold estipulado pelo vendedor
            int priceMoney = packet.readInt();   // Preço em Money estipulado pelo vendedor

            UserItemRepository itemRepo = SpringContext.getBean(UserItemRepository.class);
            AuctionRepository auctionRepo = SpringContext.getBean(AuctionRepository.class);

            // Localiza o item físico na mochila do jogador
            List<UserItem> items = itemRepo.findByPlayer(seller.getPlayerData());
            UserItem targetItem = items.stream().filter(i -> i.getPlace() == itemBagPlace).findFirst().orElse(null);

            GSPacketIn auctionResponse = new GSPacketIn((short) 120);

            if (targetItem != null) {
                // 1. Modifica o slot do item para um valor representativo (ex: -1) indicando que ele saiu da mochila e está preso no leilão
                targetItem.setPlace(-1);
                itemRepo.save(targetItem);

                // 2. Cria o registro oficial na tabela 'auction_house' do MySQL usando a entidade que você criou
                AuctionItem auctionEntry = new AuctionItem(seller.getPlayerData(), targetItem, priceGold, priceMoney);
                auctionRepo.save(auctionEntry);

                System.out.println("⚖️ [Leilão] " + seller.getPlayerData().getNickname() + " colocou o item " + targetItem.getTemplateId() + " à venda.");

                // 3. Responde ao cliente confirmando a publicação
                auctionResponse.writeBoolean(true);
                seller.sendPacket(auctionResponse);

                // 4. Sincroniza a mochila do jogador imediatamente para o item sumir da tela dele
                sendUserBag(seller, seller.getPlayerData());
            } else {
                auctionResponse.writeBoolean(false);
                auctionResponse.writeString("Item não encontrado no inventário.");
                seller.sendPacket(auctionResponse);
            }
        } catch (Exception e) {
            System.err.println("Erro ao processar publicação no Leilão: " + e.getMessage());
        }
    }


    // --- UTILITÁRIOS GLOBAIS REUTILIZÁVEIS ---

    private void sendUserBag(GamePlayer gamePlayer, Player p) {
        List<UserItem> items = SpringContext.getBean(UserItemRepository.class).findByPlayer(p);
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
    }

    private GameRoom findRoomByPlayer(GamePlayer target) {
        return RoomMgr.getAllRooms().stream().filter(r -> r.getSlots().containsValue(target)).findFirst().orElse(null);
    }

    private int findSlotByPlayer(GameRoom room, GamePlayer target) {
        return room.getSlots().entrySet().stream().filter(e -> e.getValue().equals(target)).map(java.util.Map.Entry::getKey).findFirst().orElse(-1);
    }

    private void sendSysMessage(GamePlayer p, String txt) {
        GSPacketIn err = new GSPacketIn((short) 19);
        err.writeInt(5);
        err.writeInt(0);
        err.writeString("Sistema");
        err.writeString(txt);
        p.sendPacket(err);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        System.err.println("[DDTank Netty] Sessão encerrada: " + cause.getMessage());
        ctx.close();
    }
}
