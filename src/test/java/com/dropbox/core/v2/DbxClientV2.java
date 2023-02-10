package com.dropbox.core.v2;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxHost;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.DbxRequestUtil;
import com.dropbox.core.DbxWrappedException;
import com.dropbox.core.http.HttpRequestor;
import com.dropbox.core.oauth.DbxCredential;
import com.dropbox.core.oauth.DbxOAuthException;
import com.dropbox.core.oauth.DbxRefreshResult;
import com.dropbox.core.stone.StoneSerializer;
import com.dropbox.core.v2.common.PathRoot;

import java.util.List;

/**
 * A Dropbox client mock based on the original Dropbox source code
 */
public class DbxClientV2 extends DbxClientV2Base {
	// Added to allow mock
	public static RCPMock rcp = new RCPMock() {
		@Override
		public <ArgT, ResT, ErrT> ResT rpcStyle(String host, String path, ArgT arg, boolean noAuth,
				StoneSerializer<ArgT> argSerializer, StoneSerializer<ResT> responseSerializer,
				StoneSerializer<ErrT> errorSerializer, DbxUserRawClientV2 client)
				throws DbxWrappedException, DbxException {
			throw new DbxException("No mock defined");
		}
	};

	// Some unused constructors has been removed
     /**
     *
     *
     * Create a client that uses {@link com.dropbox.core.oauth.DbxCredential} instead of raw
     * access token. The credential object include access token as well as refresh token,
     * expiration time, app key and app secret. Using credential enables dropbox client to support
     * short live token feature.
     *
     * @param requestConfig Default attributes to use for each request
     * @param credential The credential object containing all the information for authentication.
     */
    public DbxClientV2(DbxRequestConfig requestConfig, DbxCredential credential) {
        this(requestConfig, credential, DbxHost.DEFAULT, null, null);
    }

    private DbxClientV2(
        DbxRequestConfig requestConfig,
        DbxCredential credential,
        DbxHost host,
        String userId,
        PathRoot pathRoot) {
        super(new DbxUserRawClientV2(requestConfig, credential, host, userId, pathRoot));
    }

    /**
     * For internal use only.
     *
     * <p> Used by other clients to provide functionality like DbxTeamClientV2.asMember(..)
     *
     * @param client Raw v2 client ot use for issuing requests
     */
    DbxClientV2(DbxRawClientV2 client) {
        super(client);
    }

    /**
     * Returns a new {@link DbxClientV2} that performs requests against Dropbox API
     * user endpoints relative to a namespace without including the namespace as
     * part of the path variable for every request.
     * (<a href="https://www.dropbox.com/developers/reference/namespace-guide#pathrootmodes">https://www.dropbox.com/developers/reference/namespace-guide#pathrootmodes</a>).
     *
     * <p> This method performs no validation of the namespace ID. </p>
     *
     * @param pathRoot  the path root for this client, never {@code null}.
     *
     * @return Dropbox client that issues requests with Dropbox-API-Path-Root header.
     *
     * @throws IllegalArgumentException  If {@code pathRoot} is {@code null}
     */
    public DbxClientV2 withPathRoot(PathRoot pathRoot) {
        if (pathRoot == null) {
            throw new IllegalArgumentException("'pathRoot' should not be null");
        }
        return new DbxClientV2(_client.withPathRoot(pathRoot));
    }

    /**
     *
     *
     * Refresh the access token inside {@link DbxCredential}. It has the same behavior as
     * {@link DbxCredential#refresh(DbxRequestConfig)}.
     * @return The result contains new short-live access token and expiration time.
     * @throws DbxOAuthException If refresh failed because of invalid parameter or invalid refresh
     * token.
     * @throws DbxException If refresh failed before of general problems like network issue.
     */
    public DbxRefreshResult refreshAccessToken() throws DbxException {
    	System.out.println("client refresh method was called");
        return this._client.refreshAccessToken();
    }

    /**
     * {@link DbxRawClientV2} raw client that adds user OAuth2 auth headers to all requests.
     */
    // Class was private ... but we have to pass it to the RCP mock => public
    public static final class DbxUserRawClientV2 extends DbxRawClientV2 {
    	// Was private .. but is used to verify credential is refreshed
        public final DbxCredential credential;

        DbxUserRawClientV2(DbxRequestConfig requestConfig, DbxCredential credential, DbxHost host,
                           String userId, PathRoot pathRoot) {
            super(requestConfig, host, userId, pathRoot);
            if (credential == null) throw new NullPointerException("credential");
            this.credential = credential;
        }

        @Override
        public DbxRefreshResult refreshAccessToken() throws DbxException {
            credential.refresh(this.getRequestConfig());
            return new DbxRefreshResult(credential.getAccessToken(), (credential.getExpiresAt() - System.currentTimeMillis())/1000);
        }

        @Override
        public boolean canRefreshAccessToken() {
            return credential.getRefreshToken() != null;
        }

        @Override
        public boolean needsRefreshAccessToken() {
            return canRefreshAccessToken() && credential.aboutToExpire();
        }

        @Override
        protected void addAuthHeaders(List<HttpRequestor.Header> headers) {
            DbxRequestUtil.removeAuthHeader(headers);
            DbxRequestUtil.addAuthHeader(headers, credential.getAccessToken());
        }

        @Override
        protected DbxRawClientV2 withPathRoot(PathRoot pathRoot) {
            return new DbxUserRawClientV2(
                this.getRequestConfig(),
                this.credential,
                this.getHost(),
                this.getUserId(),
                pathRoot
            );
        }
        
        @Override
        public <ArgT, ResT, ErrT> ResT rpcStyle(String host, String path, ArgT arg, boolean noAuth,
        		StoneSerializer<ArgT> argSerializer, StoneSerializer<ResT> responseSerializer,
        		StoneSerializer<ErrT> errorSerializer) throws DbxWrappedException, DbxException {
        	// Changed to prevent calling Dropbox
            if (needsRefreshAccessToken()) {
            	refreshAccessToken();
            }
        	return rcp.rpcStyle(host, path, arg, noAuth, argSerializer, responseSerializer, errorSerializer, this);
        }
    }
}
