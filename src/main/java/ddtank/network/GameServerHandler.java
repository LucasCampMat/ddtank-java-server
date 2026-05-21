package ddtank.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class GameServerHandler extends SimpleChannelInboundHandler<ByteBuf> {

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        System.out.println("[DDTank Netty] Conexão estabelecida com o cliente: " + ctx.channel().remoteAddress());

        // Constrói e envia o pacote "Handshake" esperado pelo jogo
        ByteBuf handshake = Unpooled.buffer();
        handshake.writeShort(0x77aa); // Assinatura obrigatória DDTank
        handshake.writeShort(0);
        handshake.writeShort(1);

        ctx.writeAndFlush(handshake);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
        if (msg.readableBytes() >= 6) {
            short header = msg.readShort();
            short length = msg.readShort();
            short opCode = msg.readShort();

            System.out.println("[DDTank Netty] Comando recebido do jogo. ID (OpCode): " + opCode);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        System.err.println("[DDTank Netty] Sessão encerrada: " + cause.getMessage());
        ctx.close();
    }
}