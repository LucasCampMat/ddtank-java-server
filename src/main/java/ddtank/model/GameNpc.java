package ddtank.model;

public class GameNpc {
    private final int npcId;
    private final String name;
    private int posX;
    private int posY;
    private int hp = 500;
    private int maxHp = 500;
    private int damage = 80;
    private boolean isAlive = true;

    public GameNpc(int npcId, String name, int posX, int posY) {
        this.npcId = npcId;
        this.name = name;
        this.posX = posX;
        this.posY = posY;
    }

    // Getters e Setters
    public int getNpcId() { return npcId; }
    public String getName() { return name; }
    public int getPosX() { return posX; }
    public void setPosX(int posX) { this.posX = posX; }
    public int getPosY() { return posY; }
    public void setPosY(int posY) { this.posY = posY; }
    public int getHp() { return hp; }
    public boolean isAlive() { return isAlive; }

    public void takeDamage(int amount) {
        if (!isAlive) return;
        this.hp -= amount;
        if (this.hp <= 0) {
            this.hp = 0;
            this.isAlive = false;
        }
    }
    public int getDamage() {
        return damage;
    }

}
