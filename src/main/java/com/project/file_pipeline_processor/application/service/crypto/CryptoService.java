package com.project.file_pipeline_processor.application.service.crypto;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CryptoService {

	private static final String DATA_ALGO = "AES/GCM/NoPadding";
	private static final String KEY_WRAP_ALGO = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";
	private static final int GCM_TAG_BITS = 128;
	private static final int IV_BYTES = 12;
	private static final int AES_BITS = 256;

	private final SecureRandom secureRandom = new SecureRandom();

	private final KeyPair keyPair;

	public CryptoService(
			@Value("${crypto.rsa.private-key-pkcs8-base64:}") String privateKeyBase64,
			@Value("${crypto.rsa.public-key-x509-base64:}") String publicKeyBase64
	) {
		this.keyPair = loadOrGenerateKeyPair(privateKeyBase64, publicKeyBase64);
	}

	public PublicKey publicKey() {
		return keyPair.getPublic();
	}

	public CryptoEnvelope encrypt(byte[] plaintext) {
		try {
			SecretKey aesKey = generateAesKey();
			byte[] iv = new byte[IV_BYTES];
			secureRandom.nextBytes(iv);

			Cipher aes = Cipher.getInstance(DATA_ALGO);
			aes.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
			byte[] encryptedData = aes.doFinal(plaintext);

			Cipher rsa = Cipher.getInstance(KEY_WRAP_ALGO);
			rsa.init(Cipher.ENCRYPT_MODE, keyPair.getPublic());
			byte[] encryptedAesKey = rsa.doFinal(aesKey.getEncoded());

			return new CryptoEnvelope("RSA-OAEP+AES-256-GCM", encryptedData, encryptedAesKey, iv);
		} catch (GeneralSecurityException ex) {
			throw new IllegalStateException("Error encrypting data", ex);
		}
	}

	public byte[] decrypt(CryptoEnvelope envelope) {
		try {
			Cipher rsa = Cipher.getInstance(KEY_WRAP_ALGO);
			rsa.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
			byte[] aesKeyBytes = rsa.doFinal(envelope.encryptedAesKey());

			SecretKey aesKey = new javax.crypto.spec.SecretKeySpec(aesKeyBytes, "AES");
			Cipher aes = Cipher.getInstance(DATA_ALGO);
			aes.init(Cipher.DECRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_BITS, envelope.iv()));
			return aes.doFinal(envelope.encryptedData());
		} catch (GeneralSecurityException ex) {
			throw new IllegalStateException("Error decrypting data", ex);
		}
	}

	public String sha256Hex(byte[] data) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(data);
			return toHex(hash);
		} catch (GeneralSecurityException ex) {
			throw new IllegalStateException("SHA-256 not available", ex);
		}
	}

	private SecretKey generateAesKey() throws GeneralSecurityException {
		KeyGenerator keyGen = KeyGenerator.getInstance("AES");
		keyGen.init(AES_BITS, secureRandom);
		return keyGen.generateKey();
	}

	private static KeyPair loadOrGenerateKeyPair(String privateKeyBase64, String publicKeyBase64) {
		try {
			if (privateKeyBase64 != null && !privateKeyBase64.isBlank()
					&& publicKeyBase64 != null && !publicKeyBase64.isBlank()) {
				KeyFactory keyFactory = KeyFactory.getInstance("RSA");
				byte[] privBytes = java.util.Base64.getDecoder().decode(privateKeyBase64);
				byte[] pubBytes = java.util.Base64.getDecoder().decode(publicKeyBase64);
				PrivateKey privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(privBytes));
				PublicKey publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(pubBytes));
				return new KeyPair(publicKey, privateKey);
			}

			KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
			generator.initialize(2048);
			return generator.generateKeyPair();
		} catch (GeneralSecurityException ex) {
			throw new IllegalStateException("Cannot initialize RSA keypair", ex);
		}
	}

	private static String toHex(byte[] bytes) {
		StringBuilder sb = new StringBuilder(bytes.length * 2);
		for (byte b : bytes) {
			sb.append(Character.forDigit((b >> 4) & 0xF, 16));
			sb.append(Character.forDigit(b & 0xF, 16));
		}
		return sb.toString();
	}
}
