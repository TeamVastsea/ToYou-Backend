package cc.vastsea.toyou.util;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.Random;

@Slf4j
public class PasswordUtil {
	private static final int DEFAULT_ITERATIONS = 10000;
	private static final String SALT_CHARS = "123456789zxcvbnmasdfghjklqwertyuiopQWERTYUIOPASDFGHJKLZXCVBNM";

	private static String getEncodeHash(String password, String salt) {
		SecretKeyFactory keyFactory;
		try {
			keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
		} catch (Throwable e) {
			log.error("Could NOT retrieve PBKDF2WithHmacSHA256 algorithm", e);
			System.exit(1);
			return null;
		}
		KeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt.getBytes(), DEFAULT_ITERATIONS, 256);
		SecretKey secret;
		try {
			secret = keyFactory.generateSecret(keySpec);
		} catch (InvalidKeySpecException e) {
			log.error("Could NOT generate secret key", e);
			return null;
		}
		byte[] rawHash = secret.getEncoded();
		byte[] hashBase64 = Base64.getEncoder().encode(rawHash);
		return new String(hashBase64);
	}

	public static String encodePassword(String password) {
		StringBuilder salt = new StringBuilder();
		Random random = new Random();
		for (int i = 0; i < 20; i++) {
			salt.append(SALT_CHARS.charAt(random.nextInt(SALT_CHARS.length())));
		}
		String hash = getEncodeHash(password, salt.toString());
		return String.format("%s$%s", salt.toString(), hash);
	}

	public static boolean checkPassword(String password, String hashedPassword) {
		String[] parts = hashedPassword.split("\\$");
		if (parts.length != 2) {
			return false;
		}
		String salt = parts[0];
		String hash = getEncodeHash(password, salt);
		return String.format("%s$%s", salt, hash).equals(hashedPassword);
	}
}
