package ddtank.controller;

import ddtank.model.Player;
import ddtank.model.ShopItem;
import ddtank.model.UserItem;
import ddtank.repository.PlayerRepository;
import ddtank.repository.UserItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping({"/config", "/config/request"})
public class RoadServerController {

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private UserItemRepository userItemRepository; // Injeta o novo repositório de itens

    @GetMapping(value = "/serverlist.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public String getServerList() {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<Result value=\"true\" message=\"Success\">\n" +
                "    <Item ID=\"1\" \n" +
                "          Name=\"Servidor Java MySQL\" \n" +
                "          IP=\"127.0.0.1\" \n" +
                "          Port=\"9200\" \n" +
                "          State=\"2\" \n" +
                "          MustLevel=\"1\" \n" +
                "          LowestLevel=\"1\" \n" +
                "          OnlineTotal=\"10\" \n" +
                "          MaxCount=\"1000\" \n" +
                "          RuntimeState=\"1\" \n" +
                "          ZoneIndex=\"1\" />\n" +
                "</Result>";
    }

    @GetMapping(value = "/customprop.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public String getCustomProperties() {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<Result value=\"true\" message=\"Success\">\n" +
                "    <Item Key=\"BigExp\" Value=\"1\" />\n" +
                "    <Item Key=\"Anticheat\" Value=\"false\" />\n" +
                "    <Item Key=\"ServerVersion\" Value=\"3000\" />\n" +
                "</Result>";
    }

    @PostMapping(value = "/login.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public String loginUser(@RequestParam String username, @RequestParam String password) {
        Optional<Player> playerOpt = playerRepository.findByUsername(username);

        if (playerOpt.isPresent() && playerOpt.get().getPassword().equals(password)) {
            Player p = playerOpt.get();
            return String.format(
                    "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                            "<Result value=\"true\" message=\"Login com sucesso!\">\n" +
                            "    <UserInfo PlayerID=\"%d\" NickName=\"%s\" Grade=\"%d\" Gold=\"%d\" Money=\"%d\" GP=\"%d\" Sex=\"true\" />\n" +
                            "</Result>",
                    p.getId(), p.getNickname(), p.getGrade(), p.getGold(), p.getMoney(), p.getGp()
            );
        }
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<Result value=\"false\" message=\"Usuario ou senha incorretos.\" />";
    }

    @GetMapping(value = "/shopgoods.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public List<ShopItem> getShopGoods() {
        return Arrays.asList(
                new ShopItem(1001, "Quebra-Tijolo Legitimo", 500),
                new ShopItem(1002, "Canhao Arco-Iris Forte", 750)
        );
    }

    // Atalho HTTP para criar contas de teste pelo navegador com ITENS INICIAIS
    @GetMapping("/create-account")
    public String createAccount(@RequestParam String user, @RequestParam String pass, @RequestParam String nick) {
        if (playerRepository.findByUsername(user).isPresent()) {
            return "Erro: O usuario ja existe no banco!";
        }

        // 1. Salva o jogador no MySQL
        Player newPlayer = new Player(user, pass, nick);
        Player savedPlayer = playerRepository.save(newPlayer);

        // 2. Cria a arma inicial (TemplateID 1001) no slot 0 da mochila
        UserItem starterWeapon = new UserItem(savedPlayer, 1001, 0);
        starterWeapon.setStrengthenLevel(0); // Começa sem fortalecimento
        userItemRepository.save(starterWeapon);

        return "Conta registrada no MySQL com sucesso! Personagem '" + nick + "' recebeu um Quebra-Tijolo inicial na mochila.";
    }
}
