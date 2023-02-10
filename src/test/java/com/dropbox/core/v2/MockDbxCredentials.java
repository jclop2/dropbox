package com.dropbox.core.v2;

import java.lang.reflect.Field;
import java.util.Collection;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxHost;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.oauth.DbxCredential;
import com.dropbox.core.oauth.DbxRefreshResult;
import com.fathzer.soft.jclop.Account;
import com.fathzer.soft.jclop.dropbox.ObservableDbxCredential;

public class MockDbxCredentials extends ObservableDbxCredential {
	private static final Field accTokField; 
	private static final Field expiresField; 
	static {
		try {
			accTokField = DbxCredential.class.getDeclaredField("accessToken");
			accTokField.setAccessible(true);	
			expiresField = DbxCredential.class.getDeclaredField("expiresAt");
			expiresField.setAccessible(true);	
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public MockDbxCredentials(Account account, String accessToken, Long expiresAt, String refreshToken, String appKey, String appSecret) {
		super(account, accessToken, expiresAt, refreshToken, appKey, appSecret);
	}

	@Override
	protected DbxRefreshResult getRefresh(DbxRequestConfig requestConfig, DbxHost host, Collection<String> scope) throws DbxException {
		final String accessToken = "a fresh token";
		final Long expiresAt = System.currentTimeMillis() + 4L*3600; 
		try {
			accTokField.set(this, accessToken);
			expiresField.set(this, expiresAt);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return new DbxRefreshResult(accessToken, expiresAt);
	}
}