package ddtank.controller;

import ddtank.network.RoomMgr;
import ddtank.network.WorldMgr;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DashboardController {

    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    public String getDashboard() {
        int onlineCount = WorldMgr.getOnlineCount();
        int roomCount = RoomMgr.getAllRooms().size();

        return "<!DOCTYPE html>\n" +
                "<html lang=\"pt-BR\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>DDTank Java Server - Painel de Controle</title>\n" +
                "    <style>\n" +
                "        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #1a1a2e; color: #fff; margin: 0; padding: 20px; display: flex; justify-content: center; align-items: center; height: 100vh; }\n" +
                "        .container { background: #161623; padding: 40px; border-radius: 15px; box-shadow: 0 15px 35px rgba(0,0,0,0.5); text-align: center; border: 1px solid rgba(255,255,255,0.1); width: 100%; max-width: 500px; }\n" +
                "        h1 { color: #00adb5; margin-bottom: 30px; font-size: 28px; text-transform: uppercase; letter-spacing: 2px; }\n" +
                "        .card-group { display: flex; justify-content: space-between; margin-bottom: 30px; gap: 20px; }\n" +
                "        .card { background: #222831; padding: 20px; border-radius: 10px; flex: 1; border: 1px solid #393e46; transition: transform 0.3s; }\n" +
                "        .card:hover { transform: translateY(-5px); border-color: #00adb5; }\n" +
                "        .card h2 { margin: 0; font-size: 14px; color: #eeeeee; text-transform: uppercase; opacity: 0.7; }\n" +
                "        .card p { margin: 10px 0 0 0; font-size: 36px; font-weight: bold; color: #00adb5; }\n" +
                "        .status { display: inline-block; padding: 8px 20px; background: #00adb5; color: #1a1a2e; font-weight: bold; border-radius: 20px; text-transform: uppercase; font-size: 12px; letter-spacing: 1px; box-shadow: 0 0 15px rgba(0,173,181,0.4); }\n" +
                "        .footer { margin-top: 30px; font-size: 11px; color: #393e46; }\n" +
                "    </style>\n" +
                "    <script>\n" +
                "        // Recarrega a página automaticamente a cada 5 segundos para atualizar o contador\n" +
                "        setInterval(function() { location.reload(); }, 5000);\n" +
                "    </script>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div class=\"container\">\n" +
                "        <h1>DDTank Java Engine</h1>\n" +
                "        <div class=\"card-group\">\n" +
                "            <div class=\"card\">\n" +
                "                <h2>Jogadores Online</h2>\n" +
                "                <p>" + onlineCount + "</p>\n" +
                "            </div>\n" +
                "            <div class=\"card\">\n" +
                "                <h2>Salas Ativas</h2>\n" +
                "                <p>" + roomCount + "</p>\n" +
                "            </div>\n" +
                "        </div>\n" +
                "        <div class=\"status\">Servidor Online Core v1.0</div>\n" +
                "        <div class=\"footer\">Desenvolvido em Java Moderno & Netty Framework</div>\n" +
                "    </div>\n" +
                "</body>\n" +
                "</html>";
    }
}
