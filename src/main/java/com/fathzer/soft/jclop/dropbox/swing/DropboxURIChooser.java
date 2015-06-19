package com.fathzer.soft.jclop.dropbox.swing;

import java.awt.Window;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import com.dropbox.core.DbxAccountInfo;
import com.dropbox.core.DbxAuthFinish;
import com.dropbox.core.DbxClient;
import com.dropbox.core.DbxException;
import com.fathzer.soft.ajlib.swing.Utils;
import com.fathzer.soft.jclop.Account;
import com.fathzer.soft.jclop.dropbox.DropboxService;
import com.fathzer.soft.jclop.swing.AbstractURIChooserPanel;


@SuppressWarnings("serial")
public class DropboxURIChooser extends AbstractURIChooserPanel {
	private static final String TITLE;
	
	static {
		TITLE = DropboxService.URI_SCHEME.substring(0, 1).toUpperCase() + DropboxService.URI_SCHEME.substring(1);
	}
	
	public DropboxURIChooser(DropboxService service) {
		super(service);
	}

	@Override
	protected Account createNewAccount() {
		Window owner = Utils.getOwnerWindow(this);
		ConnectionDialog connectionDialog = new ConnectionDialog(owner, ((DropboxService)getService()).getConnectionData(), getLocale());
		connectionDialog.setVisible(true);
		DbxAuthFinish finish = connectionDialog.getResult();
		if (finish==null) {
			return null;
		}
		String id = finish.userId;
		Account account = getService().getAccount(id);
		if (account==null) {
			// This is a new account
			account = getService().newAccount(id, null, finish.accessToken);
		} else {
			// This is an existing account => update it
			account.setConnectionData(finish.accessToken);
		}
System.out.println ("token = "+finish.accessToken); //TODO
		DbxClient dbxAPI = ((DropboxService)getService()).getDropboxAPI(account);
		try {
			DbxAccountInfo accountInfo = dbxAPI.getAccountInfo();
			account.setDisplayName(accountInfo.displayName);
			account.setQuota(accountInfo.quota.total);
			account.setUsed(accountInfo.quota.normal+accountInfo.quota.shared);
		} catch (DbxException e) {
			throw new RuntimeException(e); //FIXME ... strange that new account is not throwing IOException
		}
		return account;
	}

	@Override
	public String getTooltip(boolean save) {
		return save?MessagePack.getString("com.fathzer.soft.jclop.dropbox.save.tabTooltip", getLocale()):MessagePack.getString("com.fathzer.soft.jclop.dropbox.read.tabTooltip", getLocale()); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	public Icon getIcon() {
		return new ImageIcon(DropboxURIChooser.class.getResource("dropbox.png")); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see com.fathzer.soft.jclop.swing.AbstractURIChooserPanel#getTitle()
	 */
	@Override
	public String getTitle() {
		return TITLE;
	}
}
