package com.fathzer.soft.jclop.dropbox.swing;

import java.awt.Window;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.slf4j.LoggerFactory;

import com.dropbox.core.DbxAuthFinish;
import com.fathzer.soft.ajlib.swing.Utils;
import com.fathzer.soft.jclop.Account;
import com.fathzer.soft.jclop.JClopException;
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
			try {
				((DropboxService)getService()).setDisplayName(account);
			} catch (JClopException e) {
				LoggerFactory.getLogger(getClass()).warn("Unable to get account name from Dropbox", e);
			}
		} else {
			// This is an existing account => update it
			account.setConnectionData(finish.accessToken);
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
