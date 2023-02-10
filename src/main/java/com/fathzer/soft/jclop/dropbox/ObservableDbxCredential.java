package com.fathzer.soft.jclop.dropbox;

import java.util.Collection;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxHost;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.oauth.DbxCredential;
import com.dropbox.core.oauth.DbxRefreshResult;
import com.fathzer.soft.jclop.Account;

public class ObservableDbxCredential extends DbxCredential {
	protected final Account account;
	
	public ObservableDbxCredential(Account account, String accessToken, Long expiresAt, String refreshToken, String appKey, String appSecret) {
		super(accessToken, expiresAt, refreshToken, appKey, appSecret);
		this.account = account;
	}

	@Override
	public DbxRefreshResult refresh(DbxRequestConfig requestConfig, DbxHost host, Collection<String> scope) throws DbxException {
		final DbxRefreshResult refresh = getRefresh(requestConfig, host, scope);
		refreshed(refresh);
		return refresh;
	}
	
	protected DbxRefreshResult getRefresh(DbxRequestConfig requestConfig, DbxHost host, Collection<String> scope) throws DbxException {
		return super.refresh(requestConfig, host, scope);
	}
	
	private void refreshed(DbxRefreshResult refresh) {
		account.setConnectionData(new Credentials(refresh.getAccessToken(), refresh.getExpiresAt(), getRefreshToken()));
	}
}
