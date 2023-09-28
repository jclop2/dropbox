package com.fathzer.soft.jclop.dropbox.swing;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;
import java.util.Locale;

import javax.swing.JPanel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dropbox.core.BadRequestException;
import com.dropbox.core.DbxAuthFinish;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxWebAuth;
import com.dropbox.core.TokenAccessType;
import com.fathzer.jlocal.Formatter;
import com.fathzer.soft.ajlib.swing.Browser;
import com.fathzer.soft.ajlib.swing.Utils;
import com.fathzer.soft.ajlib.swing.dialog.AbstractDialog;
import com.fathzer.soft.jclop.dropbox.DbxConnectionData;
import com.fathzer.soft.jclop.swing.AbstractURIChooserPanel;

@SuppressWarnings("serial")
public class ConnectionDialog extends AbstractDialog<DbxConnectionData, DbxAuthFinish> {
	private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionDialog.class);
	
	private boolean connectionHasStarted;
	private DbxAuthFinish pair;
	private DbxWebAuth webAuth;
	private ConnectionButtonsPanel cButtons;

	public ConnectionDialog(Window owner, DbxConnectionData appInfo, Locale locale) {
		super(owner, MessagePack.getString("com.fathzer.soft.jclop.dropbox.ConnectionDialog.title", locale), appInfo); //$NON-NLS-1$
		this.connectionHasStarted = false;
		this.setLocale(locale);
	}

	@Override
	protected JPanel createCenterPane() {
		return new ConnectionPanel(getOkButton().getText(), data.getAppName(), getLocale());
	}

	@Override
	protected DbxAuthFinish buildResult() {
		return pair;
	}
	
	@Override
	protected void confirm() {
		try {
			String code = getConnectionButtonsPanel().getCodeField().getText();
			pair = webAuth.finishFromCode(code);
		} catch (BadRequestException e) {
			// The user didn't grant the access to Dropbox
			final String message = MessagePack.getString("com.fathzer.soft.jclop.dropbox.ConnectionDialog.accessNotGranted", getLocale()); //$NON-NLS-1$
			AbstractURIChooserPanel.showError(this, Formatter.format(message, data.getAppName()), getLocale());
			getConnectionButtonsPanel().getConnectButton().setEnabled(true);
			updateOkButtonEnabled();
			return;
		} catch (DbxException e) {
			LOGGER.warn("Error while linking with Dropbox account", e);
			AbstractURIChooserPanel.showError(this, MessagePack.getString("com.fathzer.soft.jclop.dropbox.ConnectionDialog.unexpectedError", getLocale()), getLocale()); //$NON-NLS-1$
		}
		super.confirm();
	}

	@Override
	protected String getOkDisabledCause() {
		if (!this.connectionHasStarted) {
			return Formatter.format(MessagePack.getString("com.fathzer.soft.jclop.dropbox.ConnectionDialog.error.processNotStarted", getLocale()), getConnectionButtonsPanel().getConnectButton().getText()); //$NON-NLS-1$
		}
		return null;
	}

	@Override
	protected JPanel createButtonsPane() {
		getConnectionButtonsPanel().addButtons(getOkButton(), getCancelButton());
		cButtons.getConnectButton().addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				Window window = Utils.getOwnerWindow(cButtons);
			    webAuth = new DbxWebAuth(data.getConfig(), data.getAppInfo());
			    DbxWebAuth.Request authRequest = DbxWebAuth.newRequestBuilder()
			    		.withNoRedirect()
			    		.withTokenAccessType(TokenAccessType.OFFLINE)
			            .build();
		        String authorizeUrl = webAuth.authorize(authRequest);
		    	Browser.show(URI.create(authorizeUrl), window, MessagePack.getString("com.fathzer.soft.jclop.dropbox.ConnectionDialog.error.unableToLaunchBrowser.title", getLocale())); //$NON-NLS-1$
		    	connectionHasStarted = true;
			    cButtons.getConnectButton().setEnabled(false);
				updateOkButtonEnabled();
			}
		});
		return cButtons;
	}
	
	private ConnectionButtonsPanel getConnectionButtonsPanel() {
		if (cButtons==null) {
			cButtons = new ConnectionButtonsPanel(getLocale());
		}
		return cButtons;
	}
	
}
