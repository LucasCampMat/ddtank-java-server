package ddtank.packet;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.nio.charset.StandardCharsets;

public class GSPacketIn {
    private final short code;
    private final ByteBuf buffer;

    // Construtor para pacotes novos que você vai enviar para o jogador
    public GSPacketIn(short code) {
        this.code = code;
        this.buffer = Unpooled.buffer();
    }

    // Construtor para pacotes brutos que chegam do jogador
    public GSPacketIn(short code, ByteBuf dataBuf) {
        this.code = code;
        this.buffer = dataBuf;
    }

    public short getCode() {
        return code;
    }

    public ByteBuf getBuffer() {
        return buffer;
    }

    // --- MÉTODOS DE LEITURA (Para processar dados do cliente) ---
    public byte readByte() {
        return buffer.readByte();
    }

    public int readInt() {
        return buffer.readInt();
    }

    public short readShort() {
        return buffer.readShort();
    }

    public boolean readBoolean() {
        return buffer.readBoolean();
    }

    public String readString() {
        short length = buffer.readShort();
        if (length <= 0) return "";
        byte[] bytes = new byte[length];
        buffer.readBytes(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    // --- MÉTODOS DE ESCRITA (Para preparar dados de envio) ---
    public void writeByte(int value) {
        buffer.writeByte(value);
    }

    public void writeInt(int value) {
        buffer.writeInt(value);
    }

    public void writeShort(int value) {
        buffer.writeShort(value);
    }

    public void writeBoolean(boolean value) {
        buffer.writeBoolean(value);
    }

    public void writeString(String value) {
        if (value == null || value.isEmpty()) {
            buffer.writeShort(0);
            return;
        }
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        buffer.writeShort(bytes.length);
        buffer.writeBytes(bytes);
    }
}
