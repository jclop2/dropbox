package com.fathzer.soft.jclop.dropbox.swing;

import java.awt.Window;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import com.dropbox.client2.session.AccessTokenPair;
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
		ConnectionDialog connectionDialog = new ConnectionDialog(owner, ((DropboxService)getService()).getDropboxAPI(null), getLocale());
		connectionDialog.setVisible(true);
		AccessTokenPair pair = connectionDialog.getResult();
		if (pair==null) return null;
		com.dropbox.client2.DropboxAPI.Account accountInfo = connectionDialog.getAccountInfo();
		String id = Long.toString(accountInfo.uid);
		Account account = getService().getAccount(id);
		if (account==null) {
			// This is a new account
			account = getService().newAccount(id, accountInfo.displayName, pair);
		} else {
			// This is an existing account => update it
			account.setDisplayName(accountInfo.displayName);
			account.setConnectionData(pair);
		}
		account.setQuota(accountInfo.quota);
		account.setUsed(accountInfo.quotaNormal+accountInfo.quotaShared);
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
