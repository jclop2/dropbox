package com.fathzer.soft.jclop.dropbox;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.NoRouteToHostException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.core.DbxAccountInfo;
import com.dropbox.core.DbxClient;
import com.dropbox.core.DbxClient.Downloader;
import com.dropbox.core.DbxClient.Uploader;
import com.dropbox.core.DbxEntry;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxException.InvalidAccessToken;
import com.dropbox.core.DbxException.NetworkIO;
import com.dropbox.core.DbxException.ProtocolError;
import com.dropbox.core.DbxException.ServerError;
import com.dropbox.core.DbxOAuth1AccessToken;
import com.dropbox.core.DbxOAuth1Upgrader;
import com.dropbox.core.DbxWriteMode;
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
	private static final Logger LOGGER = LoggerFactory.getLogger(DropboxService.class);
	private static final int WAIT_DELAY = 30;
	private static final boolean SLOW_READING = Boolean.getBoolean("slowDataReading"); //$NON-NLS-1$
	
	public static final String URI_SCHEME = "dropbox";
	
	private DbxConnectionData data;
	private DbxClient currentApi;
	private String currentId;

	public DropboxService(File root, DbxConnectionData data) {
		super(root, false);
		this.data = data;
		this.currentId = null;
	}

	public DbxClient getDropboxAPI(Account account) {
		if (!account.getId().equals(currentId)) {
			Serializable connectionData = account.getConnectionData();
			if (connectionData instanceof AccessTokenPair) {
				connectionData = upgradeToken(((AccessTokenPair)connectionData).key, ((AccessTokenPair)connectionData).secret);
				account.setConnectionData(connectionData);
				LOGGER.info("OAuth1 token upgraded");
			}
			currentApi = new DbxClient(data.getConfig(), (String) connectionData);
			currentId = account.getId();
		}
		return this.currentApi;
	}

	@Override
	public String getScheme() {
		return URI_SCHEME;
	}
	
	@Override
	public Collection<Entry> getRemoteEntries(Account account, Cancellable task) throws JClopException {
		DbxClient api = getDropboxAPI(account);
		try {
			// Refresh the quota data
			DbxAccountInfo accountInfo = api.getAccountInfo();
			account.setQuota(accountInfo.quota.total);
			account.setUsed(accountInfo.quota.normal+accountInfo.quota.shared);
			
			if (task.isCancelled()) {
				return null;
			}
			// Get the remote files list
			//FIXME The following line will hang if content has more than 2500 entries
			List<DbxEntry> contents = api.getMetadataWithChildren("/").children; //$NON-NLS-1$
			Collection<Entry> result = new ArrayList<Entry>();
			for (DbxEntry entry : contents) {
				Entry jclopEntry = getRemoteEntry(account, entry.name);
				if (jclopEntry!=null) {
					result.add(jclopEntry);
				}
			}
			return result;
		} catch (DbxException e) {
			throw getException(e);
		}
	}

	private JClopException getServerException(ServerError e) {
		return new HostErrorException(e);
	}

	private JClopException getException(DbxException e) throws InvalidConnectionDataException, UnreachableHostException {
		if (e instanceof InvalidAccessToken) {
			// The connection data correspond to no valid account
			return new InvalidConnectionDataException(e);
		} else if (e instanceof ProtocolError) {
			// The server returned a request that the Dropbox API was not able to parse -> The server is crashed
			return new HostErrorException(e);
		} else if (e instanceof NetworkIO) {
			return new UnreachableHostException(e);
		} else if (e instanceof ServerError) {
			return getServerException((ServerError) e);
		}
		Throwable cause = e.getCause();
		if ((cause instanceof UnknownHostException) || (cause instanceof NoRouteToHostException)) {
			return new UnreachableHostException(e);
		} else {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String getConnectionDataURIFragment(Serializable connectionData) {
		return OAUTH2_PREFIX + connectionData;
	}

	@Override
	public Serializable getConnectionData(String uriFragment) {
		if (uriFragment.startsWith(OAUTH2_PREFIX)) {
			return uriFragment.substring(OAUTH2_PREFIX.length());
		} else {
			String[] split = StringUtils.split(uriFragment, '-');
			return upgradeToken(split[0], split[1]);
		}
	}
	
	private String upgradeToken(String key, String secret) {
		DbxOAuth1Upgrader upgrader = new DbxOAuth1Upgrader(getConnectionData().getConfig(), getConnectionData().getAppInfo());
		try {
			return upgrader.createOAuth2AccessToken(new DbxOAuth1AccessToken(key, secret));
		} catch (DbxException e) {
			//FIXME Should be more natural to throw an IOException
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public boolean download(URI uri, OutputStream out, Cancellable task,
			Locale locale) throws JClopException, IOException {
		Entry entry = getEntry(uri);
		try {
			String path = getRemotePath(entry);
			currentApi = getDropboxAPI(entry.getAccount());
			long totalSize = -1;
			Downloader downloader = currentApi.startGetFile(path, null);
			if (task != null) {
				task.setPhase(getMessage(MessagePack.DOWNLOADING, locale),
						downloader.metadata.numBytes > 0 ? 100 : -1);
			}
			InputStream dropboxStream = downloader.body;
			try {
				// Transfer bytes from the file to the output file
				byte[] buf = new byte[1024];
				long red = 0;
				for (int len = dropboxStream.read(buf);  len > 0; len = dropboxStream.read(buf)) {
					out.write(buf, 0, len);
					if (SLOW_READING) {
						try {
							Thread.sleep(WAIT_DELAY);
						} catch (InterruptedException e) {
							throw new RuntimeException(e);
						}
					}
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

	@Override
	public boolean upload(InputStream in, long length, URI uri,
			Cancellable task, Locale locale) throws JClopException, IOException {
		Entry entry = getEntry(uri);
		if (task != null) {
			task.setPhase(getMessage(MessagePack.UPLOADING, locale), -1); //$NON-NLS-1$
		}
		
		// This implementation uses ChunkedUploader to allow the user to
		// cancel the upload
		// It has a major trap:
		// It seems that each chunk requires a new connection to Dropbox. On
		// some network configuration (with very slow proxy)
		// this dramatically slows down the upload. We use a chunck size
		// equals to the file size to prevent having such a problem.
		// For that reason, the task will never been informed of the upload
		// progress.
		//TODO Should be verified with API 1.7
		int chunkSize = (int) Math.min(Integer.MAX_VALUE, length);
		final Uploader uploader = getDropboxAPI(entry.getAccount()).startUploadFileChunked(getRemotePath(entry), DbxWriteMode.update(getRemoteRevision(uri)),
                chunkSize);

		if (task != null) {
			task.setCancelAction(new Runnable() {
				@Override
				public void run() {
					uploader.abort();
				}
			});
		}
		DbxEntry.File dbxFile = null;
		try {
			long byteSent = 0;
			byte[] buffer = new byte[chunkSize];
			OutputStream out = uploader.getBody();
			while (byteSent<length) {
				int remaining = (int) Math.min(length-byteSent, chunkSize);
				int bytesRead = in.read(buffer,0,chunkSize);
				if (bytesRead != remaining) {
					uploader.abort();
					throw new IOException("Premature end of input stream");
				}
				out.write(buffer, 0, bytesRead);
				if (task!=null && task.isCancelled()) {
					break;
				}
			}
			if (task!=null && task.isCancelled()) {
				uploader.abort();
			} else {
				out.flush();
				dbxFile = uploader.finish();
				//TODO test there is no conflict using the dfxFile's name.
			}
			// The close() is probably not usefull, but the Dropbox documentation is not very clear on that point.
			uploader.close();
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
		DbxClient api = getDropboxAPI(entry.getAccount());
		try {
			DbxEntry metadata = api.getMetadata(getRemotePath(entry));
			if (metadata==null) {
				return null;
			} else {
				return metadata.asFile().rev;
			}
		} catch (DbxException e) {
			throw getException(e);
		}
	}
	
	public String getMessage(String key, Locale locale) {
		try {
//System.out.print ("Looking for "+key);
			String serviceKey = getClass().getPackage().getName() + key.substring(MessagePack.KEY_PREFIX.length());
//System.out.print (" customizedkey is "+serviceKey);
			String result = com.fathzer.soft.jclop.dropbox.swing.MessagePack.getString(serviceKey, locale);
//System.out.println ("-> customized found");
			return result;
		} catch (MissingResourceException e) {
//System.out.println ("-> get default");
			return MessagePack.DEFAULT.getString(key, locale);
		}
	}

	@Override
	public Entry getEntry(URI uri) {
		if (!uri.getScheme().equals(getScheme())) {
			throw new IllegalArgumentException();
		}
		try {
			String path = URLDecoder.decode(uri.getPath().substring(1), UTF_8);
			int index = path.indexOf('/');
			String accountName = path.substring(0, index);
			path = path.substring(index+1);
			String[] split = StringUtils.split(uri.getUserInfo(), ':');
			String accountId = URLDecoder.decode(split[0], UTF_8);
			for (Account account : getAccounts()) {
				if (account.getId().equals(accountId)) {
					return new Entry(account, path);
				}
			}
			// The account is unknown
			Serializable connectionData = getConnectionData(split[1]);
			Account account = new Account(this, accountId, accountName, connectionData);
			return new Entry(account, path);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	public DbxConnectionData getConnectionData() {
		return this.data;
	}
}
