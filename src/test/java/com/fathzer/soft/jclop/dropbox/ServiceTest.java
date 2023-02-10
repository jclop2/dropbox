package com.fathzer.soft.jclop.dropbox;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.LogTrick;
import org.slf4j.spi.LocationAwareLogger;

import com.dropbox.core.DbxAppInfo;
import com.dropbox.core.DbxAuthFinish;
import com.dropbox.core.oauth.DbxCredential;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.MockDbxCredentials;
import com.fathzer.soft.jclop.Account;
import com.fathzer.soft.jclop.Entry;
import com.fathzer.soft.jclop.InvalidConnectionDataException;

public class ServiceTest {
	@Rule
	public TemporaryFolder folder = new TemporaryFolder();
	private DropboxService service;
	
	@BeforeClass
	public static void setMock() {
		DbxClientV2.rcp = new CurrentAccountRCPMock();
	}
	
	@AfterClass
	public static void removeMock() {
		DbxClientV2.rcp = null;
	}
	
	private static void setRcpDisplayName(String name) {
		((CurrentAccountRCPMock)DbxClientV2.rcp).displayName = name;
	}
	
	private DropboxService getService() throws IOException {
		if (service==null) {
			final DbxConnectionData ctx = new DbxConnectionData.Builder("Test", "TestKey", "TestSecret").build();
			service = new DropboxService(folder.getRoot(), ctx) {
				@Override
				protected DbxCredential getDbxCredential(Account account, String access, long expiresAt, String refresh, DbxAppInfo appInfo) {
					return new MockDbxCredentials(account, access, expiresAt, refresh, appInfo.getKey(), appInfo.getSecret());
				}
			};
		}
		return service;
	}
	
	@Test
	public void testTokenUpdate() throws Exception {
		setRcpDisplayName("otherAccountName");
		final DropboxService svce = getService();
		DbxAuthFinish auth = new DbxAuthFinish("expired", -1000L, "refresh", "17884", "team", "account", null);
		Account account = svce.authenticate(auth);
		assertEquals("17884",account.getId());
		assertEquals("otherAccountName",account.getDisplayName());
		// Verify token was refreshed
		assertNotEquals("expired", ((Credentials)account.getConnectionData()).getAccessToken());
		assertTrue(((Credentials)account.getConnectionData()).getExpiresAt()>System.currentTimeMillis());
	}
	
	@Test
	public void testAccountToCredentials() throws IOException {
		final DropboxService svce = getService();
		Account account = new Account(svce, "x", "name", "aLongLivedToken");
		DbxCredential credentials = svce.getCredentials(account);
		assertEquals("aLongLivedToken",credentials.getAccessToken());
		assertEquals(Long.MAX_VALUE, credentials.getExpiresAt().longValue());
		assertNull(credentials.getRefreshToken());
		
		// Verify it work when no access token is stored in the account
		account.setConnectionData(new Credentials(null, 0, "refresh"));
		credentials = svce.getCredentials(account);
		assertNotNull(credentials.getAccessToken());
		assertTrue(credentials.aboutToExpire());
		assertEquals("refresh",credentials.getRefreshToken());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testWrongAccountCredentials() throws IOException {
		final DropboxService svce = getService();
		Account account = new Account(svce, "x", "name", Long.MAX_VALUE);
		svce.getCredentials(account);
	}

	
	@Test
	public void testURL() throws Exception {
		setRcpDisplayName("otherAccountName");
		final DropboxService svce = getService();
		final URI url = URI.create("dropbox://17882:OAuth2-MyFuckingToken@cloud.jclop.fathzer.com/Jules+Cesar/Comptes");
		Entry entry = svce.getEntry(url);
		assertEquals("Comptes",entry.getDisplayName());
		assertEquals("Jules Cesar",entry.getAccount().getDisplayName());
		Credentials expectedCred = Credentials.fromLongLived("MyFuckingToken");
		assertEquals(expectedCred,entry.getAccount().getConnectionData());
		assertEquals("17882",entry.getAccount().getId());
		assertEquals(-1,entry.getAccount().getQuota());
		assertEquals(-1,entry.getAccount().getUsed());
		assertEquals(svce, entry.getAccount().getService());
		assertTrue(entry.getAccount().getLocalEntries().isEmpty());
		
		assertEquals(url, svce.getURI(entry));
		
		// Verify credentials are not affected by other URI 
		final URI otherConnectionData = URI.create("dropbox://17882:OAuth2-refresh-MyFuckingRefresh@cloud.jclop.fathzer.com/Jules+Cesar/Comptes");
		assertEquals(expectedCred, svce.getEntry(otherConnectionData).getAccount().getConnectionData());
		
		// Test with an unknown account with refresh token
		final URI refreshOtherURI = URI.create("dropbox://18882:OAuth2-refresh-MyFuckingRefresh@cloud.jclop.fathzer.com/Albert+Enstein/Research");
		entry = svce.getEntry(refreshOtherURI);
		assertEquals("Research",entry.getDisplayName());
		assertEquals("Albert Enstein",entry.getAccount().getDisplayName());
		expectedCred = Credentials.fromRefresh("MyFuckingRefresh");
		assertEquals(expectedCred,entry.getAccount().getConnectionData());
		
		// Authenticate on new account
		String refresh = "refresh";
		DbxAuthFinish auth = new DbxAuthFinish("blabla", 1000L, refresh, "17883", "team", "account", null);
		long expiresAt = auth.getExpiresAt();
		Account account = svce.authenticate(auth);
		assertEquals("17883",account.getId());
		assertEquals("otherAccountName",account.getDisplayName());
		// Verify credentials are updated
		assertEquals(new Credentials("blabla", expiresAt, refresh), account.getConnectionData());
		assertEquals(DropboxService.urlEncode("OAuth2-refresh-"+refresh), svce.getConnectionDataURIFragment(account.getConnectionData()));

		// Re-authenticate
		// refresh token is chosen to contain a char that needs url encoding. Just to verify fragment is URL safe
		setRcpDisplayName("accountName");
		refresh = "s?dxyj/kmkj-sldksq7895klm";
		auth = new DbxAuthFinish("blabla", 3600L, refresh, "17882", "team", "account", null);
		expiresAt = auth.getExpiresAt();
		account = svce.authenticate(auth);
		assertEquals("17882",account.getId());
		assertEquals("accountName",account.getDisplayName());
		// Verify credentials are updated
		assertEquals(new Credentials("blabla", expiresAt, refresh), account.getConnectionData());
		assertEquals(DropboxService.urlEncode("OAuth2-refresh-"+refresh), svce.getConnectionDataURIFragment(account.getConnectionData()));
	}
	
	@Test
	public void testDbxClientUpdatedWhenAuthenticationIsMadeAgain() throws IOException {
		final DropboxService svce = getService();
		final Account account = svce.getEntry(URI.create("dropbox://17882:OAuth2-MyFuckingToken@cloud.jclop.fathzer.com/Jules+Cesar/Comptes")).getAccount();
		final String okToken = "workingOne";
		
		final Logger logger = LoggerFactory.getLogger(svce.getClass());
		final int previousLogLevel = LogTrick.setLogLevel(logger, LocationAwareLogger.ERROR_INT);
		((CurrentAccountRCPMock)DbxClientV2.rcp).acceptedTokens = Collections.singleton(okToken);
		try {
			svce.updateDisplayName(account);
			fail("Connection should have failed");
		} catch (InvalidConnectionDataException e) {
			service.authenticate(new DbxAuthFinish(okToken, 1000L, "refresh", "17882", "team", "account", null));
			assertEquals("Jules Cesar",account.getDisplayName());
		} finally {
			LogTrick.setLogLevel(logger, previousLogLevel);
			((CurrentAccountRCPMock)DbxClientV2.rcp).acceptedTokens = null;
		}
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void wrongObject() throws IOException {
		getService().getConnectionDataURIFragment("Wrong connection data object");
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void wrongObject2() throws IOException {
		getService().getConnectionData("Wrong connection data URI fragment");
	}

	@Test(expected = IllegalArgumentException.class)
	public void wrongURL() throws IOException {
		getService().getEntry(URI.create("http://17882:OAuth2-MyFuckingToken@cloud.jclop.fathzer.com/Jules+Cesar/Comptes"));
	}
}
