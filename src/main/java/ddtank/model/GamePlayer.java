package ddtank.model;

import ddtank.packet.GSPacketIn;
import io.netty.channel.ChannelHandlerContext;

public class GamePlayer {
    private final ChannelHandlerContext ctx;
    private final Player playerData;

    private int posX = 0;
    private int posY = 0;
    private int direction = 1;

    // ADICIONADO: Atributo de Ângulo do canhão de disparo (Padrão: 30 graus)
    private int angle = 30;

    private int maxHp = 1000;
    private int currentHp = 1000;
    private boolean isAlive = true;

    private double activeDamageMultiplier = 1.0;
    private int bonusShootCount = 1;

    public GamePlayer(ChannelHandlerContext ctx, Player playerData) {
        this.ctx = ctx;
        this.playerData = playerData;
    }

    public ChannelHandlerContext getCtx() { return ctx; }
    public Player getPlayerData() { return playerData; }

    public int getPosX() { return posX; }
    public void setPosX(int posX) { this.posX = posX; }

    public int getPosY() { return posY; }
    public void setPosY(int posY) { this.posY = posY; }

    public int getDirection() { return direction; }
    public void setDirection(int direction) { this.direction = direction; }

    public int getAngle() { return angle; }
    public void setAngle(int angle) { this.angle = angle; }

    public int getMaxHp() { return maxHp; }
    public int getCurrentHp() { return currentHp; }
    public boolean isAlive() { return isAlive; }

    public double getActiveDamageMultiplier() { return activeDamageMultiplier; }
    public int getBonusShootCount() { return bonusShootCount; }

    public void setPosition(int x, int y, int dir) {
        this.posX = x;
        this.posY = y;
        this.direction = dir;
    }

    public void resetBattleStats() {
        this.currentHp = this.maxHp;
        this.isAlive = true;
        this.angle = 30; // Reseta o ângulo padrão ao entrar em combate
        clearBuffs();
    }

    public void addDamageMultiplier(double multiplier) {
        this.activeDamageMultiplier *= multiplier;
    }

    public void addBonusShoots(int count) {
        this.bonusShootCount += count;
    }

    public void clearBuffs() {
        this.activeDamageMultiplier = 1.0;
        this.bonusShootCount = 1;
    }

    public void takeDamage(int amount) {
        if (!isAlive) return;

        this.currentHp -= amount;
        if (this.currentHp <= 0) {
            this.currentHp = 0;
            this.isAlive = false;
        }
    }

    public void sendPacket(GSPacketIn packet) {
        if (ctx != null && ctx.channel().isActive()) {
            ctx.writeAndFlush(packet.getBuffer());
        }
    }

    public void disconnect() {
        if (ctx != null) {
            ctx.close();
        }
    }
}
