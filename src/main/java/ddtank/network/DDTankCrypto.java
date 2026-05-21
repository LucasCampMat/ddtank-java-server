package ddtank.network;

public class DDTankCrypto {

    // Chave padrão utilizada nas versões antigas do ecossistema DDTank 3.0
    private static final byte[] DEFAULT_KEY = { 0x6e, 0x3a, 0x56, 0x73, 0x42, 0x2b, 0x49, 0x5a };

    /**
     * Criptografa ou descriptografa um array de bytes (Algoritmo XOR)
     * No DDTank, aplicar a mesma função duas vezes reverte o estado do dado.
     */
    public static byte[] decrypt(byte[] src, int len) {
        byte[] result = new byte[len];
        for (int i = 0; i < len; i++) {
            // Aplica a operação XOR combinando a posição do byte com a chave padrão
            result[i] = (byte) ((src[i] ^ DEFAULT_KEY[i % 8]) + (i & 0xff));
        }
        return result;
    }

    public static byte[] encrypt(byte[] src, int len) {
        byte[] result = new byte[len];
        for (int i = 0; i < len; i++) {
            result[i] = (byte) ((src[i] - (i & 0xff)) ^ DEFAULT_KEY[i % 8]);
        }
        return result;
    }
}
