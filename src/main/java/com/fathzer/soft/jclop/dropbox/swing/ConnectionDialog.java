package com.fathzer.soft.jclop.dropbox.swing;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;
import java.util.Locale;

import javax.swing.JButton;
import javax.swing.JPanel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.Account;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.exception.DropboxUnlinkedException;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.WebAuthSession;
import com.dropbox.client2.session.WebAuthSession.WebAuthInfo;
import com.fathzer.jlocal.Formatter;
import com.fathzer.soft.ajlib.swing.Browser;
import com.fathzer.soft.ajlib.swing.Utils;
import com.fathzer.soft.ajlib.swing.dialog.AbstractDialog;
import com.fathzer.soft.jclop.swing.AbstractURIChooserPanel;

@SuppressWarnings("serial")
public class ConnectionDialog extends AbstractDialog<DropboxAPI<? extends WebAuthSession>, AccessTokenPair> {
	private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionDialog.class);
	
	private boolean connectionHasStarted;
	private WebAuthInfo info;
	private AccessTokenPair pair;
	private JButton connectButton;
	private Account accountInfo;

	public ConnectionDialog(Window owner, DropboxAPI<? extends WebAuthSession> dropboxAPI, Locale locale) {
		super(owner, MessagePack.getString("com.fathzer.soft.jclop.dropbox.ConnectionDialog.title", locale), dropboxAPI); //$NON-NLS-1$
		this.connectionHasStarted = false;
		this.setLocale(locale);
	}

	@Override
	protected JPanel createCenterPane() {
		return new ConnectionPanel(getOkButton().getText(), getLocale());
	}

	@Override
	protected AccessTokenPair buildResult() {
		return pair;
	}
	
	@Override
	protected void confirm() {
		try {
			data.getSession().retrieveWebAccessToken(info.requestTokenPair);
			pair = data.getSession().getAccessTokenPair();
			accountInfo = data.accountInfo();
		} catch (DropboxUnlinkedException e) {
			// The user didn't grant the access to Dropbox
			AbstractURIChooserPanel.showError(this, MessagePack.getString("com.fathzer.soft.jclop.dropbox.ConnectionDialog.accessNotGranted", getLocale()), getLocale()); //$NON-NLS-1$
			connectionHasStarted = false;
			getConnectButton().setEnabled(true);
			updateOkButtonEnabled();
			return;
		} catch (DropboxException e) {
			LOGGER.warn("Error while linking with Dropbox account", e);
			AbstractURIChooserPanel.showError(this, MessagePack.getString("com.fathzer.soft.jclop.dropbox.ConnectionDialog.unexpectedError", getLocale()), getLocale()); //$NON-NLS-1$
		}
		super.confirm();
	}

	@Override
	protected String getOkDisabledCause() {
		if (!this.connectionHasStarted) {
			return Formatter.format(MessagePack.getString("com.fathzer.soft.jclop.dropbox.ConnectionDialog.error.processNotStarted", getLocale()), getConnectButton().getText()); //$NON-NLS-1$
		}
		return null;
	}

	@Override
	protected JPanel createButtonsPane() {
		JPanel panel = new JPanel();
		panel.add(getConnectButton());
		panel.add(getOkButton());
		panel.add(getCancelButton());
		connectButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				Window window = Utils.getOwnerWindow(connectButton);
				try {
					data.getSession().unlink();
					try {
						info = data.getSession().getAuthInfo();
					} catch (Exception e) {
						LOGGER.warn("Error while getting Authentication info", e);
						AbstractURIChooserPanel.showError(window, MessagePack.getString("com.fathzer.soft.jclop.dropbox.connectionFailed", getLocale()), getLocale()); //$NON-NLS-1$
						return;
					}
					Browser.show(new URI(info.url), window, MessagePack.getString("com.fathzer.soft.jclop.dropbox.ConnectionDialog.error.unableToLaunchBrowser.title", getLocale())); //$NON-NLS-1$
					connectionHasStarted = true;
				} catch (Exception e) {
					LOGGER.warn("Error while unlinking Dropbox session", e);
					AbstractURIChooserPanel.showError(window, MessagePack.getString("com.fathzer.soft.jclop.dropbox.ConnectionDialog.error.unableToLaunchBrowser.message", getLocale()), getLocale()); //$NON-NLS-1$
				}
				connectButton.setEnabled(false);
				updateOkButtonEnabled();
			}
		});
		return panel;
	}
	
	private JButton getConnectButton() {
		if (connectButton==null) {
			connectButton = new JButton(MessagePack.getString("com.fathzer.soft.jclop.dropbox.ConnectionDialog.startButton", getLocale())); //$NON-NLS-1$
			connectButton.setToolTipText(MessagePack.getString("com.fathzer.soft.jclop.dropbox.ConnectionDialog.startButton.tooltip", getLocale())); //$NON-NLS-1$
		}
		return connectButton;
	}
	
	public Account getAccountInfo() {
		return this.accountInfo;
	}
}