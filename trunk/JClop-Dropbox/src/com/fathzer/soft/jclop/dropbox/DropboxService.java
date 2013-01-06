package com.fathzer.soft.jclop.dropbox;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.NoRouteToHostException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.DropboxInputStream;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.exception.DropboxPartialFileException;
import com.dropbox.client2.exception.DropboxServerException;
import com.dropbox.client2.exception.DropboxUnlinkedException;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.WebAuthSession;
import com.fathzer.soft.jclop.Account;
import com.fathzer.soft.jclop.Cancellable;
import com.fathzer.soft.jclop.Entry;
import com.fathzer.soft.jclop.InvalidConnectionDataException;
import com.fathzer.soft.jclop.Service;
import com.fathzer.soft.jclop.UnreachableHostException;
import com.fathzer.soft.jclop.swing.MessagePack;

import net.astesana.ajlib.utilities.StringUtils;

public class DropboxService extends Service {
	private static final int WAIT_DELAY = 30;
	private static final boolean SLOW_READING = Boolean.getBoolean("slowDataReading"); //$NON-NLS-1$
	
	public static final String URI_SCHEME = "Dropbox";
	
	private DropboxAPI<? extends WebAuthSession> api;

	public DropboxService(File root, DropboxAPI<? extends WebAuthSession> api) {
		super(root);
		this.api = api;
	}

	public DropboxAPI<? extends WebAuthSession> getDropboxAPI(Account account) {
		WebAuthSession session = this.api.getSession();
		if (account==null) { 
			// We need a new fresh unlinked session
			session.unlink();
		} else {
			// We need a linked session
			AccessTokenPair pair = this.api.getSession().getAccessTokenPair();
			if (pair!=null) {
				if (pair.equals((AccessTokenPair) account.getConnectionData())) {
					// Linked with the right account
					return this.api;
				} else {
					// Not linked with the right account
					session.unlink();
				}
			}
			session.setAccessTokenPair((AccessTokenPair) account.getConnectionData());
//To test invalid connection data			session.setAccessTokenPair(new AccessTokenPair("kjhhl","jkljmkl")); //FIXME
		}
		return this.api;
	}

	@Override
	public String getScheme() {
		return URI_SCHEME;
	}
	
	@Override
	public Collection<Entry> getRemoteEntries(Account account, Cancellable task) throws UnreachableHostException, InvalidConnectionDataException {
		DropboxAPI<? extends WebAuthSession> api = getDropboxAPI(account);
		try {
			// Refresh the quota data
			com.dropbox.client2.DropboxAPI.Account accountInfo = api.accountInfo();
			account.setQuota(accountInfo.quota);
			account.setUsed(accountInfo.quotaNormal+accountInfo.quotaShared);
			
			if (task.isCancelled()) return null;
			// Get the remote files list //FIXME The following line will hang if content has more than 2500 entries
			List<com.dropbox.client2.DropboxAPI.Entry> contents = api.metadata("", 0, null, true, null).contents; //$NON-NLS-1$
			Collection<Entry> result = new ArrayList<Entry>();
			for (com.dropbox.client2.DropboxAPI.Entry entry : contents) {
				if (!entry.isDeleted) {
					Entry jclopEntry = getRemoteEntry(account, entry.fileName());
					if (jclopEntry!=null) result.add(jclopEntry);
				}
			}
			return result;
		} catch (DropboxUnlinkedException e) {
			// The connection data correspond to no valid account
			throw new InvalidConnectionDataException();
		} catch (DropboxException e) {
			Throwable cause = e.getCause();
			if ((cause instanceof UnknownHostException) || (cause instanceof NoRouteToHostException)) {
				throw new UnreachableHostException();
			} else {
				//FIXME
				e.printStackTrace();
				throw new RuntimeException(cause);
			}
		}
	}

	@Override
	public String getConnectionDataURIFragment(Serializable connectionData) {
		AccessTokenPair tokens = (AccessTokenPair) connectionData;
		return tokens.key + "-" + tokens.secret;
	}

	@Override
	public Serializable getConnectionData(String uriFragment) {
		String[] split = StringUtils.split(uriFragment, '-');
		return new AccessTokenPair(split[0], split[1]);
	}
	
	@Override
	public boolean download(Entry entry, OutputStream out, Cancellable task, Locale locale) throws IOException {
		try {
			String path = getRemotePath(entry);
			api = getDropboxAPI(entry.getAccount());
	    long totalSize = -1;
	    if (task!=null) {
	    	totalSize = api.metadata(path, 0, null, false, null).bytes;
	    	task.setPhase(getMessage(MessagePack.DOWNLOADING, locale), totalSize>0?100:-1); //$NON-NLS-1$
	    }
	    DropboxInputStream dropboxStream = api.getFileStream(path, null);
			try {
		    // Transfer bytes from the file to the output file
		    byte[] buf = new byte[1024];
		    int len;
		    long red = 0;
				while ((len = dropboxStream.read(buf)) > 0) {
					out.write(buf, 0, len);
					if (SLOW_READING) {
						try {
							Thread.sleep(WAIT_DELAY);
						} catch (InterruptedException e) {
							throw new RuntimeException(e);
						}
					}
					if (task != null) {
						if (task.isCancelled()) return false;
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
		} catch (DropboxException e) {
			throw new IOException(e);
		}
	}

	@Override
	public boolean upload(InputStream in, long length, Entry entry, Cancellable task, Locale locale) throws IOException {
		try {
	    if (task!=null) task.setPhase(getMessage(MessagePack.UPLOADING, locale), -1); //$NON-NLS-1$

			// This implementation uses ChunkedUploader to allow the user to cancel the upload
			// It has a major trap:
			// It seems that each chunk requires a new connection to Dropbox. On some network configuration (with very slow proxy)
			//   this dramatically slows down the upload. We use a chunck size equals to the file size to prevent having such a problem.
			//   For that reason, the task will never been informed of the upload progress.
	    final DropboxAPI<? extends WebAuthSession>.ChunkedUploader uploader = getDropboxAPI(entry.getAccount()).getChunkedUploader(in, length);
	    if (task!=null) {
		    task.setCancelAction(new Runnable() {
					@Override
					public void run() {
						uploader.abort();
					}
		    });
	    }
			try {
				int retryCounter = 0;
				while (!uploader.isComplete()) {
					try {
						uploader.upload();
					} catch (DropboxException e) {
						if (retryCounter > 5) throw e; // Give up after a while.
						retryCounter++;
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e1) {
						}
					}
				}
			} catch (DropboxPartialFileException e) {
				// Upload was cancelled
				return false;
	    } finally {
	    	if (task!=null) task.setCancelAction(null);
	    }
			String parentRev = task!=null && task.isCancelled()?null:getRemoteRevision(entry);
			boolean result = task==null || !task.isCancelled();
			if (result) {
				uploader.finish(getRemotePath(entry), parentRev);
			} else {
				uploader.abort();
			}
			return result;

/*
	    // Here is an implementation that do not use chunckedUploader
			// Its major problems are:
			// - It is not possible to cancel the upload before it is completed
			// - When the upload is cancelled, the Dropbox file is reverted to its previous version but it's revision is incremented
			//   This causes the synchronization process to consider the file has been modified after the upload
			if (task!=null) task.setPhase(LocalizationData.get("dropbox.uploading"), -1); //$NON-NLS-1$
			// As this implementation doesn't allow to cancel during the upload, we will remember what was the file state
			String previous = getRemoteRevision(uri);
			Dropbox.getAPI().putFileOverwrite(path, stream, length, null);
			if (task!=null && task.isCancelled()) {
				// The upload was cancelled
				if (previous==null) {
					// The file do not existed before, delete it
					Dropbox.getAPI().delete(path);
				} else {
					// Revert to the previous version
					// Unfortunately, this not really revert to the previous state as it creates a new revision on Dropbox
					Dropbox.getAPI().restore(path, previous);
				}
				return false;
			}
			return true;
/**/
		} catch (DropboxException e) {
			System.err.println ("Dropbox Exception !!!");//TODO
			throw new IOException(e);
		}
	}
	
	public String getRemoteRevision(Entry entry) throws IOException {
		DropboxAPI<? extends WebAuthSession> api = getDropboxAPI(entry.getAccount());
		try {
			com.dropbox.client2.DropboxAPI.Entry metadata = api.metadata(getRemotePath(entry), 1, null, true, null);
			if (metadata.isDeleted) return null;
			return metadata.rev;
		} catch (DropboxServerException e) {
			if (e.error==DropboxServerException._404_NOT_FOUND) {
				return null;
			} else {
				throw new IOException(e);
			}
		} catch (DropboxException e) {
			throw new IOException(e);
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
}
