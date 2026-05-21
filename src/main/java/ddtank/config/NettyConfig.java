package ddtank.config;

import ddtank.network.GameServerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Configuration
public class NettyConfig {

    private final EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    private final EventLoopGroup workerGroup = new NioEventLoopGroup();

    @PostConstruct
    public void startServer() {
        new Thread(() -> {
            try {
                ServerBootstrap b = new ServerBootstrap();
                b.group(bossGroup, workerGroup)
                        .channel(NioServerSocketChannel.class)
                        .childHandler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel ch) {
                                ch.pipeline().addLast(new GameServerHandler());
                            }
                        });

                System.out.println("[DDTank Java] Fight Server ativado na porta TCP 9200!");
                b.bind(9200).sync().channel().closeFuture().sync();
            } catch (Exception e) {
                System.err.println("Falha crítica no servidor Netty: " + e.getMessage());
            }
        }).start();
    }

    @PreDestroy
    public void stopServer() {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }
}