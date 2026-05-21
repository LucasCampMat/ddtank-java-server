package ddtank.controller;

import ddtank.model.Player;
import ddtank.model.ShopItem;
import ddtank.repository.PlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/config")
public class RoadServerController {

    @Autowired
    private PlayerRepository playerRepository;

    // Retorna a lista de mundos ativos
    @GetMapping(value = "/serverlist.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public String getServerList() {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<Result value=\"true\" message=\"Success\">\n" +
                "    <Item ID=\"1\" Name=\"Servidor Java MySQL\" IP=\"127.0.0.1\" Port=\"9200\" State=\"1\" MustLevel=\"1\" LowestLevel=\"1\" OnlineTotal=\"1\" />\n" +
                "</Result>";
    }

    // Autentica o login consultando o MySQL
    @PostMapping(value = "/login.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public String loginUser(@RequestParam String username, @RequestParam String password) {
        Optional<Player> playerOpt = playerRepository.findByUsername(username);

        if (playerOpt.isPresent() && playerOpt.get().getPassword().equals(password)) {
            Player p = playerOpt.get();
            return String.format(
                    "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                            "<Result value=\"true\" message=\"Login com sucesso!\">\n" +
                            "    <UserInfo PlayerID=\"%d\" NickName=\"%s\" Grade=\"%d\" Gold=\"%d\" Money=\"%d\" GP=\"%d\" />\n" +
                            "</Result>",
                    p.getId(), p.getNickname(), p.getGrade(), p.getGold(), p.getMoney(), p.getGp()
            );
        }
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<Result value=\"false\" message=\"Usuario ou senha incorretos.\" />";
    }

    // Carrega os itens da loja convertendo os objetos Java para XML automaticamente
    @GetMapping(value = "/shopgoods.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public List<ShopItem> getShopGoods() {
        return Arrays.asList(
                new ShopItem(1001, "Quebra-Tijolo Legitimo", 500),
                new ShopItem(1002, "Canhao Arco-Iris Forte", 750)
        );
    }

    // Atalho HTTP para criar contas de teste pelo navegador
    @GetMapping("/create-account")
    public String createAccount(@RequestParam String user, @RequestParam String pass, @RequestParam String nick) {
        if (playerRepository.findByUsername(user).isPresent()) {
            return "Erro: O usuario ja existe no banco!";
        }
        Player newPlayer = new Player(user, pass, nick);
        playerRepository.save(newPlayer);
        return "Conta registrada no MySQL com sucesso para o personagem: " + nick;
    }
}