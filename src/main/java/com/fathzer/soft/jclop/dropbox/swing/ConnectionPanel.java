package com.fathzer.soft.jclop.dropbox.swing;

import javax.swing.JPanel;
import javax.swing.UIManager;

import java.awt.GridBagLayout;

import javax.swing.JLabel;

import java.awt.GridBagConstraints;
import java.text.MessageFormat;
import java.util.Locale;

import java.awt.Insets;

@SuppressWarnings("serial")
class ConnectionPanel extends JPanel {
	
	public static final String STATE_PROPERTY = "State"; //$NON-NLS-1$
	
	private JLabel lblNewLabel;
	private JLabel lblNewLabel1;
	private JLabel textArea;

	private String okButtonName;
	
	/**
	 * Create the panel.
	 */
	ConnectionPanel(String okButtonName, Locale locale) {
		setLocale(locale);
		this.okButtonName = okButtonName;
		GridBagLayout gridBagLayout = new GridBagLayout();
		setLayout(gridBagLayout);
		GridBagConstraints gbcLblNewLabel1 = new GridBagConstraints();
		gbcLblNewLabel1.fill = GridBagConstraints.HORIZONTAL;
		gbcLblNewLabel1.anchor = GridBagConstraints.WEST;
		gbcLblNewLabel1.insets = new Insets(0, 0, 5, 5);
		gbcLblNewLabel1.gridx = 0;
		gbcLblNewLabel1.gridy = 0;
		add(getLblNewLabel1(), gbcLblNewLabel1);
		GridBagConstraints gbcTextArea = new GridBagConstraints();
		gbcTextArea.fill = GridBagConstraints.HORIZONTAL;
		gbcTextArea.weightx = 1.0;
		gbcTextArea.insets = new Insets(0, 0, 5, 0);
		gbcTextArea.gridx = 1;
		gbcTextArea.gridy = 0;
		add(getTextArea(), gbcTextArea);
		GridBagConstraints gbcLblNewLabel = new GridBagConstraints();
		gbcLblNewLabel.gridwidth = 0;
		gbcLblNewLabel.fill = GridBagConstraints.HORIZONTAL;
		gbcLblNewLabel.weightx = 1.0;
		gbcLblNewLabel.insets = new Insets(5, 5, 5, 5);
		gbcLblNewLabel.gridx = 0;
		gbcLblNewLabel.gridy = 1;
		add(getLblNewLabel(), gbcLblNewLabel);
	}

	private JLabel getLblNewLabel1() {
		if (lblNewLabel1 == null) {
			lblNewLabel1 = new JLabel(UIManager.getIcon("OptionPane.informationIcon")); //$NON-NLS-1$
		}
		return lblNewLabel1;
	}
	
	private JLabel getLblNewLabel() {
		if (lblNewLabel == null) {
			String message = MessagePack.getString("com.fathzer.soft.jclop.dropbox.ConnectionDialog.message.content", getLocale()); //$NON-NLS-1$
			message = MessageFormat.format(message, getConnectButtonName(), okButtonName);
			lblNewLabel = new JLabel();
			lblNewLabel.setText(message);
		}
		return lblNewLabel;
	}

	String getConnectButtonName() {
		return MessagePack.getString("com.fathzer.soft.jclop.dropbox.ConnectionDialog.startButton", getLocale()); //$NON-NLS-1$
	}
	
	private JLabel getTextArea() {
		if (textArea == null) {
			textArea = new JLabel(MessagePack.getString("com.fathzer.soft.jclop.dropbox.ConnectionDialog.message.header", getLocale()));  //$NON-NLS-1$
		}
		return textArea;
	}
}
