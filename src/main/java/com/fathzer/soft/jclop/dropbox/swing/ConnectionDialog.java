package com.fathzer.soft.jclop.dropbox.swing;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dropbox.core.DbxAuthFinish;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxWebAuthNoRedirect;
import com.fathzer.jlocal.Formatter;
import com.fathzer.soft.ajlib.swing.Browser;
import com.fathzer.soft.ajlib.swing.Utils;
import com.fathzer.soft.ajlib.swing.dialog.AbstractDialog;
import com.fathzer.soft.ajlib.swing.widget.TextWidget;
import com.fathzer.soft.jclop.dropbox.DbxConnectionData;
import com.fathzer.soft.jclop.swing.AbstractURIChooserPanel;

@SuppressWarnings("serial")
public class ConnectionDialog extends AbstractDialog<DbxConnectionData, DbxAuthFinish> {
	private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionDialog.class);
	
	private boolean connectionHasStarted;
	private DbxAuthFinish pair;
	private JButton connectButton;
	private DbxWebAuthNoRedirect webAuth;

	private JLabel codeLabel;

	private TextWidget codeField;

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
			String code = getCodeField().getText();
			pair = webAuth.finish(code);
		} catch (DbxException.BadRequest e) {
			// The user didn't grant the access to Dropbox
			AbstractURIChooserPanel.showError(this, MessagePack.getString("com.fathzer.soft.jclop.dropbox.ConnectionDialog.accessNotGranted", getLocale()), getLocale()); //$NON-NLS-1$
			connectionHasStarted = false;
			getConnectButton().setEnabled(true);
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
			return Formatter.format(MessagePack.getString("com.fathzer.soft.jclop.dropbox.ConnectionDialog.error.processNotStarted", getLocale()), getConnectButton().getText()); //$NON-NLS-1$
		}
		return null;
	}

	@Override
	protected JPanel createButtonsPane() {
		JPanel panel = new JPanel();
		panel.add(getConnectButton());
		panel.add(getCodeLabel());
		panel.add(getCodeField());
		panel.add(getOkButton());
		panel.add(getCancelButton());
		connectButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				Window window = Utils.getOwnerWindow(connectButton);
			    webAuth = new DbxWebAuthNoRedirect(data.getConfig(), data.getAppInfo());
			    try {
			    	String authorizeUrl = webAuth.start();
			    	Browser.show(new URI(authorizeUrl), window, MessagePack.getString("com.fathzer.soft.jclop.dropbox.ConnectionDialog.error.unableToLaunchBrowser.title", getLocale())); //$NON-NLS-1$
			    	connectionHasStarted = true;
			    } catch (URISyntaxException e) {
					throw new RuntimeException(e);
				}
/*				
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
*/
				connectButton.setEnabled(false);
				updateOkButtonEnabled();
			}
		});
		return panel;
	}
	
	private TextWidget getCodeField() {
		if (codeField==null) {
			codeField = new TextWidget(15);
		}
		return codeField;
	}

	private JLabel getCodeLabel() {
		if (codeLabel==null) {
			codeLabel = new JLabel(MessagePack.getString("com.fathzer.soft.jclop.dropbox.ConnectionDialog.code.title", getLocale()));
		}
		return codeLabel;
	}

	private JButton getConnectButton() {
		if (connectButton==null) {
			connectButton = new JButton(MessagePack.getString("com.fathzer.soft.jclop.dropbox.ConnectionDialog.startButton", getLocale())); //$NON-NLS-1$
			connectButton.setToolTipText(MessagePack.getString("com.fathzer.soft.jclop.dropbox.ConnectionDialog.startButton.tooltip", getLocale())); //$NON-NLS-1$
		}
		return connectButton;
	}
/*	
	public Account getAccountInfo() {
		return this.accountInfo;
	}
*/
}
