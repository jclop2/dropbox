package com.fathzer.soft.jclop.dropbox;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CredentialsTest {

	@Test
	void test() {
		Credentials c = Credentials.fromLongLived("access");
		c = new Credentials("a",1000,"r");
		assertEquals("a",c.getAccessToken());
		assertEquals(1000, c.getExpiresAt());
		assertEquals("r",c.getRefreshToken());
		
		c = Credentials.fromLongLived("access");
		assertEquals("access",c.getAccessToken());
		assertEquals(Long.MAX_VALUE, c.getExpiresAt());
		assertNull(c.getRefreshToken());
		// Verify hash code and toString does not throw exception
		c.hashCode();
		c.toString();
		
		c = Credentials.fromRefresh("refresh");
		assertNotNull(c.getAccessToken());
		assertTrue(System.currentTimeMillis()>c.getExpiresAt());
		assertEquals("refresh", c.getRefreshToken());
		// Verify hash code and toString does not throw exception
		c.hashCode();
		c.toString();
	}

	void testIllegal() {
		assertThrows(IllegalArgumentException.class, () -> Credentials.fromLongLived(""));
		assertThrows(IllegalArgumentException.class, () -> new Credentials(null,0,null));
		assertThrows(IllegalArgumentException.class, () -> new Credentials("toto",0,null));
	}
}
