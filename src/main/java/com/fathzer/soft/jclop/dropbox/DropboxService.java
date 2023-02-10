package com.fathzer.soft.jclop.dropbox;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.NoRouteToHostException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;

import org.slf4j.LoggerFactory;

import com.dropbox.core.BadResponseCodeException;
import com.dropbox.core.DbxAppInfo;
import com.dropbox.core.DbxAuthFinish;
import com.dropbox.core.DbxDownloader;
import com.dropbox.core.DbxException;
import com.dropbox.core.InvalidAccessTokenException;
import com.dropbox.core.NetworkIOException;
import com.dropbox.core.ProtocolException;
import com.dropbox.core.ServerException;
import com.dropbox.core.oauth.DbxCredential;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.GetMetadataErrorException;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.ListRevisionsBuilder;
import com.dropbox.core.v2.files.Metadata;
import com.dropbox.core.v2.files.UploadBuilder;
import com.dropbox.core.v2.files.UploadUploader;
import com.dropbox.core.v2.files.WriteMode;
import com.dropbox.core.v2.users.SpaceAllocation;
import com.dropbox.core.v2.users.SpaceUsage;
import com.fathzer.soft.ajlib.utilities.StringUtils;
import com.fathzer.soft.jclop.Account;
import com.fathzer.soft.jclop.Cancellable;
import com.fathzer.soft.jclop.Entry;
import com.fathzer.soft.jclop.HostErrorException;
import com.fathzer.soft.jclop.InvalidConnectionDataException;
import com.fathzer.soft.jclop.JClopException;
import com.fathzer.soft.jclop.NoSpaceRemainingException;
import com.fathzer.soft.jclop.Service;
import com.fathzer.soft.jclop.UnreachableHostException;
import com.fathzer.soft.jclop.swing.MessagePack;

public class DropboxService extends Service {
	private static final String OAUTH2_PREFIX = "OAuth2-";
	private static final String OAUTH2_EXPIRING_PREFIX = "OAuth2-refresh-";
	private static final int WAIT_DELAY = 30;
	private static final boolean SLOW_READING = Boolean.getBoolean("slowDataReading"); //$NON-NLS-1$
	
	public static final String URI_SCHEME = "dropbox";
	
	private DbxConnectionData data;
	private DbxClientV2 currentApi;
	private String currentId;

	public DropboxService(File root, DbxConnectionData data) throws IOException {
		super(root, false);
		this.data = data;
		this.currentId = null;
	}

	private DbxClientV2 getDropboxAPI(final Account account) {
		if (!account.getId().equals(currentId)) {
			currentApi = new DbxClientV2(data.getConfig(), getCredentials(account));
			currentId = account.getId();
		}
		return this.currentApi;
	}
	
	DbxCredential getCredentials(Account account) {
		Serializable connectionData = account.getConnectionData();
		if (connectionData instanceof String) {
			account.setConnectionData(Credentials.fromLongLived((String)connectionData));
		} else if (!(connectionData instanceof Credentials)) {
			throw new IllegalArgumentException();
		}
		final Credentials cred = (Credentials) account.getConnectionData();
		final String access = cred.getAccessToken()!=null ? cred.getAccessToken() : "fakeOne"; 
		return getDbxCredential(account, access, cred.getExpiresAt(), cred.getRefreshToken(), data.getAppInfo());
	}

	/** Gets the Dropbox credentials to be used with the account.
	 * This method is there just to allow test to override the returned class in order to mock the token refresh process.
	 * @param account The account to update when token is refreshed
	 * @param access The current access token
	 * @param expiresAt the expiration date of the access token 
	 * @param refresh The refresh token
	 * @param appInfo The Dropbox application information
	 * @return A DbxCredentials instance
	 */
	protected DbxCredential getDbxCredential(Account account, String access, long expiresAt, String refresh, DbxAppInfo appInfo) {
		return new ObservableDbxCredential(account, access, expiresAt, refresh, appInfo.getKey(), appInfo.getSecret());
	}
	
	@Override
	public String getScheme() {
		return URI_SCHEME;
	}
	
	@Override
	public Collection<Entry> getRemoteEntries(Account account, Cancellable task) throws JClopException {
		DbxClientV2 api = getDropboxAPI(account);
		try {
			// Refresh the quota data
			SpaceUsage spaceUsage = api.users().getSpaceUsage();
			SpaceAllocation allocation = spaceUsage.getAllocation();
			long quota;
			if (allocation.isIndividual()) {
				quota = allocation.getIndividualValue().getAllocated();
			} else {
				quota = allocation.getTeamValue().getAllocated();
			}
			account.setQuota(quota);
			account.setUsed(spaceUsage.getUsed());
			
			if (task.isCancelled()) {
				return null;
			}

			// Get the remote files list
			Collection<Entry> result = new ArrayList<Entry>();
			ListFolderResult files = api.files().listFolder("");
			while (true) {
			    for (Metadata metadata : files.getEntries()) {
					Entry jclopEntry = getRemoteEntry(account, metadata.getName());
					if (jclopEntry!=null) {
						result.add(jclopEntry);
					}
			    }
			    if (!files.getHasMore()) {
			        break;
			    }
			    files = api.files().listFolderContinue(files.getCursor());
			}
			return result;
		} catch (DbxException e) {
			throw getException(e);
		}
	}

	private JClopException getServerException(ServerException e) {
		return new HostErrorException(e);
	}

	private JClopException getException(DbxException e) throws JClopException {
		if ((e instanceof BadResponseCodeException) && (((BadResponseCodeException)e).getStatusCode()==507)) {
			return new NoSpaceRemainingException(e);
		} else if (e instanceof InvalidAccessTokenException) {
			// The connection data correspond to no valid account
			return new InvalidConnectionDataException(e);
		} else if (e instanceof ProtocolException) {
			// The server returned a request that the Dropbox API was not able to parse -> The server is crashed
			return new HostErrorException(e);
		} else if (e instanceof NetworkIOException) {
			return new UnreachableHostException(e);
		} else if (e instanceof ServerException) {
			return getServerException((ServerException) e);
		}
		Throwable cause = e.getCause();
		if ((cause instanceof UnknownHostException) || (cause instanceof NoRouteToHostException)) {
			return new UnreachableHostException(e);
		} else {
			throw new JClopException(e){
				private static final long serialVersionUID = 1L;
			};
		}
	}

	@Override
	public String getConnectionDataURIFragment(Serializable connectionData) {
		if (connectionData instanceof Credentials) {
			final Credentials cred = (Credentials)connectionData;
			// Long-lived token, that is no more emitted by Dropbox is not url encoded to keep compatibility with previous API releases.
			return cred.getRefreshToken()==null ? OAUTH2_PREFIX + cred.getAccessToken() : urlEncode(OAUTH2_EXPIRING_PREFIX + cred.getRefreshToken());
		} else {
			throw new IllegalArgumentException();
		}
	}

	@Override
	public Serializable getConnectionData(String uriFragment) {
		if (uriFragment.startsWith(OAUTH2_EXPIRING_PREFIX)) {
			return Credentials.fromRefresh(urlDecode(uriFragment.substring(OAUTH2_EXPIRING_PREFIX.length())));
		} else if (uriFragment.startsWith(OAUTH2_PREFIX)) {
			// Long-lived token from previous dropbox api
			return Credentials.fromLongLived(uriFragment.substring(OAUTH2_PREFIX.length()));
		} else {
			throw new IllegalArgumentException();
		}
	}
	
	@Override
	public boolean download(URI uri, OutputStream out, Cancellable task, Locale locale) throws IOException {
		Entry entry = getEntry(uri);
		try {
			DbxClientV2 api = getDropboxAPI(entry.getAccount());
			DbxDownloader<FileMetadata> downloader = api.files().download(getRemotePath(entry));
			if (task != null) {
				task.setPhase(getMessage(MessagePack.DOWNLOADING, locale),
						downloader.getResult().getSize() > 0 ? 100 : -1);
			}
			final InputStream dropboxStream = downloader.getInputStream();
			try {
				long totalSize = -1;
				// Transfer bytes from the file to the output file
				final byte[] buf = new byte[1024];
				long red = 0;
				for (int len = dropboxStream.read(buf);  len > 0; len = dropboxStream.read(buf)) {
					out.write(buf, 0, len);
					slowReading();
					if (task != null) {
						if (task.isCancelled()) {
							return false;
						}
						if (totalSize > 0) {
							red += len;
							int progress = (int) (red * 100 / totalSize);
							task.reportProgress(progress);
						}
					}
				}
				return true;
			} finally {
				dropboxStream.close();
			}
		} catch (DbxException e) {
			throw getException(e);
		}
	}

	private void slowReading() {
		if (SLOW_READING) {
			try {
				Thread.sleep(WAIT_DELAY);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new RuntimeException(e);
			}
		}
	}

	@Override
	public boolean upload(InputStream in, long length, URI uri,
			Cancellable task, Locale locale) throws JClopException, IOException {
		Entry entry = getEntry(uri);
		if (task != null) {
			task.setPhase(getMessage(MessagePack.UPLOADING, locale), -1); //$NON-NLS-1$
		}
		
		// This implementation uses ChunkedUploader to allow the user to cancel the upload
		// It has a major trap:
		// It seems that each chunk requires a new connection to Dropbox. On
		// some network configuration (with very slow proxy)
		// this dramatically slows down the upload. We use a chunck size
		// equals to the file size to prevent having such a problem.
		// For that reason, the task will never been informed of the upload
		// progress.
		//TODO Should be verified with API 2.0
		int chunkSize = (int) Math.min(Integer.MAX_VALUE, length);
		UploadBuilder uploaderBuilder = getDropboxAPI(entry.getAccount()).files().uploadBuilder(getRemotePath(entry));
		final String rev = getRemoteRevision(uri);
		uploaderBuilder.withMode(rev==null ? WriteMode.ADD : WriteMode.update(rev));
		try {
			final UploadUploader uploader = uploaderBuilder.start();
			try {
				if (task != null) {
					task.setCancelAction(new Runnable() {
						@Override
						public void run() {
							uploader.abort();
						}
					});
				}
				long byteSent = 0;
				byte[] buffer = new byte[chunkSize];
				OutputStream out = uploader.getOutputStream();
				while (byteSent<length) {
					int remaining = (int) Math.min(length-byteSent, chunkSize);
					int bytesRead = in.read(buffer,0,chunkSize);
					if (bytesRead != remaining) {
						uploader.abort();
						throw new IOException("Premature end of input stream");
					}
					out.write(buffer, 0, bytesRead);
					byteSent += bytesRead;
					if (task!=null && task.isCancelled()) {
						break;
					}
				}
				if (task!=null && task.isCancelled()) {
					uploader.abort();
				} else {
					out.flush();
					uploader.finish();
				}
			} finally {
				uploader.close();
			}
		} catch (DbxException e) {
			throw getException(e);
		} finally {
			if (task != null) {
				task.setCancelAction(null);
			}
		}
		return task == null || !task.isCancelled();
	}
	
	@Override
	public String getRemoteRevision(URI uri) throws JClopException {
		Entry entry = getEntry(uri);
		DbxClientV2 api = getDropboxAPI(entry.getAccount());
		try {
			String remotePath = getRemotePath(entry);
			if (remoteExists(api, remotePath)) {
				ListRevisionsBuilder builder = api.files().listRevisionsBuilder(remotePath);
				builder.withLimit(1L);
				List<FileMetadata> revisions = builder.start().getEntries();
				return revisions.get(0).getRev();
			} else {
				return null;
			}
		} catch (DbxException e) {
			throw getException(e);
		}
	}
	
	private boolean remoteExists(DbxClientV2 api, String remotePath) throws DbxException {
        try{
            api.files().getMetadata(remotePath);
            return true;
        }catch (GetMetadataErrorException e){
            if (e.errorValue.isPath() && e.errorValue.getPathValue().isNotFound()) {
                return false;
            } else {
                throw e;
            }
        }
    }
	
	@Override
	public String getMessage(String key, Locale locale) {
		try {
			String serviceKey = getClass().getPackage().getName() + key.substring(MessagePack.KEY_PREFIX.length());
			String result = com.fathzer.soft.jclop.dropbox.swing.MessagePack.getString(serviceKey, locale);
			return result;
		} catch (MissingResourceException e) {
			return MessagePack.DEFAULT.getString(key, locale);
		}
	}

	@Override
	public Entry getEntry(URI uri) {
		if (!uri.getScheme().equals(getScheme())) {
			throw new IllegalArgumentException();
		}
		String path = urlDecode(uri.getPath().substring(1));
		int index = path.indexOf('/');
		String accountName = path.substring(0, index);
		path = path.substring(index+1);
		String[] split = StringUtils.split(uri.getUserInfo(), ':');
		String accountId = urlDecode(split[0]);
		Account account = getAccount(accountId);
		if (account==null) {
			// The account is unknown
			Serializable connectionData = getConnectionData(split[1]);
			account = newAccount(accountId, accountName, connectionData);
		}
		return new Entry(account, path);
	}

	public DbxConnectionData getConnectionData() {
		return this.data;
	}
	
	protected void updateDisplayName(Account account) throws JClopException {
		try {
			account.setDisplayName(getDropboxAPI(account).users().getCurrentAccount().getName().getDisplayName());
		} catch (DbxException e) {
			throw getException(e);
		}
	}
	
	static String urlEncode(String str) {
		return URLEncoder.encode(str, StandardCharsets.UTF_8);
	}
	private static String urlDecode(String str) {
		return URLDecoder.decode(str, StandardCharsets.UTF_8);
	}

	/** Finish the authentication or re-authentication of an account.
	 * @param finish The result of the authenticate process
	 * @return an account that have correct connection data
	 */
	public Account authenticate(DbxAuthFinish finish) {
		final String id = finish.getUserId();
		if (id.equals(this.currentId)) {
			// Delete current client, it may have obsolete credentials
			this.currentApi = null;
			this.currentId = null;
		}
		Account account = getAccount(id);
		final Credentials connectionData = new Credentials(finish.getAccessToken(), finish.getExpiresAt(), finish.getRefreshToken());
		if (account==null) {
			// This is a new account
			account = newAccount(id, "?", connectionData);
		} else {
			// This is an existing account => update it
			account.setConnectionData(connectionData);
		}
		try {
			updateDisplayName(account);
		} catch (JClopException e) {
			LoggerFactory.getLogger(getClass()).warn("Unable to get account name from Dropbox", e);
		}
		return account;
	}
}
