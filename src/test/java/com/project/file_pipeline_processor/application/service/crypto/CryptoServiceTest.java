package com.project.file_pipeline_processor.application.service.crypto;

import static org.junit.jupiter.api.Assertions.*;

import java.security.SecureRandom;

import org.junit.jupiter.api.Test;

class CryptoServiceTest {

	@Test
	void encryptDecrypt_roundTrip_preservesContent() {
		CryptoService cryptoService = new CryptoService("", "");

		byte[] original = new byte[1024 * 1024];
		new SecureRandom().nextBytes(original);

		CryptoEnvelope envelope = cryptoService.encrypt(original);
		assertNotNull(envelope);
		assertNotNull(envelope.encryptedData());
		assertTrue(envelope.encryptedData().length > 0);
		assertNotNull(envelope.encryptedAesKey());
		assertTrue(envelope.encryptedAesKey().length > 0);
		assertNotNull(envelope.iv());
		assertTrue(envelope.iv().length > 0);

		byte[] decrypted = cryptoService.decrypt(envelope);
		assertArrayEquals(original, decrypted);

		assertEquals(cryptoService.sha256Hex(original), cryptoService.sha256Hex(decrypted));
	}
}
