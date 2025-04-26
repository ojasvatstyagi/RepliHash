package it.unitn.ds1.node;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;

public final class HashUtil {
    private static final String HASH_ALGO = "MD5";

    public static int hash(String key) {
        try {
            MessageDigest md = MessageDigest.getInstance(HASH_ALGO);
            byte[] digest = md.digest(key.getBytes(StandardCharsets.UTF_8));
            return new BigInteger(1, Arrays.copyOf(digest, 4)).intValue(); // 32-bit hash
        } catch (Exception e) {
            throw new RuntimeException("Hashing failed", e);
        }
    }

    public static int hash(int key) {
        return hash(String.valueOf(key));
    }
}
