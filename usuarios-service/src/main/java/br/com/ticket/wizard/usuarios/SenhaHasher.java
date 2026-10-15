package br.com.ticket.wizard.usuarios;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

// Formato guardado: pbkdf2$<iterações>$<sal em base64>$<hash em base64>.
public final class SenhaHasher {

    private static final String ALGORITMO = "PBKDF2WithHmacSHA256";
    private static final int ITERACOES = 600_000;
    private static final int BYTES_DO_SAL = 16;
    private static final int BITS_DO_HASH = 256;

    private static final SecureRandom ALEATORIO = new SecureRandom();

    private SenhaHasher() {
    }

    public static String gerar(String senha) {
        byte[] sal = new byte[BYTES_DO_SAL];
        ALEATORIO.nextBytes(sal);
        byte[] hash = derivar(senha, sal, ITERACOES);
        return "pbkdf2$" + ITERACOES + "$" + Base64.getEncoder().encodeToString(sal)
               + "$" + Base64.getEncoder().encodeToString(hash);
    }

    public static boolean confere(String senha, String guardado) {
        String[] partes = guardado.split("\\$");
        if (partes.length != 4 || !partes[0].equals("pbkdf2")) {
            return false;
        }
        byte[] esperado = Base64.getDecoder().decode(partes[3]);
        byte[] calculado = derivar(senha, Base64.getDecoder().decode(partes[2]), Integer.parseInt(partes[1]));
        // Comparação em tempo constante, para não revelar quantos bytes batem.
        return MessageDigest.isEqual(esperado, calculado);
    }

    private static byte[] derivar(String senha, byte[] sal, int iteracoes) {
        PBEKeySpec spec = new PBEKeySpec(senha.toCharArray(), sal, iteracoes, BITS_DO_HASH);
        try {
            return SecretKeyFactory.getInstance(ALGORITMO).generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException erro) {
            throw new IllegalStateException("PBKDF2 indisponível no JDK", erro);
        } finally {
            spec.clearPassword();
        }
    }
}
