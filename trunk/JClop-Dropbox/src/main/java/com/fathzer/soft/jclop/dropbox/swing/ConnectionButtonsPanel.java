package com.fathzer.soft.jclop.dropbox.swing;

import java.util.Locale;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.fathzer.soft.ajlib.swing.widget.TextWidget;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;

@SuppressWarnings("serial")
public class ConnectionButtonsPanel extends JPanel {
	private JButton connectButton;

	private JLabel codeLabel;

	private TextWidget codeField;

	public ConnectionButtonsPanel(Locale locale) {
		super();
		this.init();
	}

	private void init() {
		GridBagLayout gridBagLayout = new GridBagLayout();
		setLayout(gridBagLayout);
		GridBagConstraints gbc_connectButton = new GridBagConstraints();
		gbc_connectButton.anchor = GridBagConstraints.NORTHWEST;
		gbc_connectButton.insets = new Insets(0, 0, 0, 10);
		gbc_connectButton.gridx = 0;
		gbc_connectButton.gridy = 0;
		add(getConnectButton(), gbc_connectButton);
		GridBagConstraints gbc_codeLabel = new GridBagConstraints();
		gbc_codeLabel.anchor = GridBagConstraints.WEST;
		gbc_codeLabel.insets = new Insets(0, 0, 0, 5);
		gbc_codeLabel.gridx = 1;
		gbc_codeLabel.gridy = 0;
		add(getCodeLabel(), gbc_codeLabel);
		GridBagConstraints gbc_codeField = new GridBagConstraints();
		gbc_codeField.insets = new Insets(0, 0, 0, 10);
		gbc_codeField.anchor = GridBagConstraints.WEST;
		gbc_codeField.gridx = 2;
		gbc_codeField.gridy = 0;
		add(getCodeField(), gbc_codeField);
	}
	
	TextWidget getCodeField() {
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

	JButton getConnectButton() {
		if (connectButton==null) {
			connectButton = new JButton(MessagePack.getString("com.fathzer.soft.jclop.dropbox.ConnectionDialog.startButton", getLocale())); //$NON-NLS-1$
			connectButton.setToolTipText(MessagePack.getString("com.fathzer.soft.jclop.dropbox.ConnectionDialog.startButton.tooltip", getLocale())); //$NON-NLS-1$
		}
		return connectButton;
	}

	void addButtons(JButton okButton, JButton cancelButton) {
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 3;
		gbc.gridy = 0;
		add(okButton, gbc);
		gbc.gridx++;
		add(cancelButton, gbc);
	}
}
