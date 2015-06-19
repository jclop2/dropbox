package com.fathzer.soft.jclop.dropbox;

import com.dropbox.core.DbxAppInfo;
import com.dropbox.core.DbxRequestConfig;

public class DbxConnectionData {
	private DbxRequestConfig config;
	private DbxAppInfo appInfo;

	public DbxConnectionData(DbxRequestConfig config, DbxAppInfo appInfo) {
		this.config = config;
		this.appInfo = appInfo;
	}

	public DbxRequestConfig getConfig() {
		return config;
	}

	public DbxAppInfo getAppInfo() {
		return appInfo;
	}

	public String getAppName() {
		// TODO Auto-generated method stub
		return "TODO";
	}
}
