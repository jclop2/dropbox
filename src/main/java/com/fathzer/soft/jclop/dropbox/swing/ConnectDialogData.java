package com.fathzer.soft.jclop.dropbox.swing;

import com.fathzer.soft.jclop.dropbox.DbxConnectionData;

final class ConnectDialogData {
	private String appName;
	private DbxConnectionData connectionData;
	
	ConnectDialogData(String appName, DbxConnectionData connectionData) {
		super();
		this.appName = appName;
		this.connectionData = connectionData;
	}

	public String getAppName() {
		return appName;
	}

	public DbxConnectionData getConnectionData() {
		return connectionData;
	}
}