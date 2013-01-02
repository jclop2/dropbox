package com.fathzer.soft.jclop.dropbox.swing;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import com.dropbox.client2.session.AccessTokenPair;
import com.fathzer.soft.jclop.Account;
import com.fathzer.soft.jclop.dropbox.DropboxService;
import com.fathzer.soft.jclop.swing.AbstractURIChooserPanel;

import net.astesana.ajlib.swing.Utils;

@SuppressWarnings("serial")
public class DropboxURIChooser extends AbstractURIChooserPanel {

	public DropboxURIChooser(DropboxService service) {
		super(service);
	}

	@Override
	protected Account createNewAccount() {
		ConnectionDialog connectionDialog = new ConnectionDialog(Utils.getOwnerWindow(this), ((DropboxService)getService()).getDropboxAPI(null), getLocale());
		connectionDialog.setVisible(true);
		AccessTokenPair pair = connectionDialog.getResult();
		if (pair==null) return null;
		com.dropbox.client2.DropboxAPI.Account accountInfo = connectionDialog.getAccountInfo();
		return new Account(getService(), Long.toString(accountInfo.uid), accountInfo.displayName, pair, accountInfo.quota, accountInfo.quotaNormal+accountInfo.quotaShared);
	}
	
	protected String getRemoteConnectingWording() {
		return MessagePack.getString("com.fathzer.soft.jclop.dropbox.connecting", getLocale()); //$NON-NLS-1$
	}

	@Override
	public String getTooltip(boolean save) {
		return save?MessagePack.getString("com.fathzer.soft.jclop.dropbox.save.tabTooltip", getLocale()):MessagePack.getString("com.fathzer.soft.jclop.dropbox.read.tabTooltip", getLocale()); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	public Icon getIcon() {
		return new ImageIcon(DropboxURIChooser.class.getResource("dropbox.png")); //$NON-NLS-1$
	}
}
