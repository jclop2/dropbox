package com.fathzer.soft.jclop.dropbox;

import com.dropbox.core.DbxAppInfo;
import com.dropbox.core.DbxRequestConfig;

public class DbxConnectionData {
	private DbxRequestConfig config;
	private DbxAppInfo appInfo;
	private String appName;

	public DbxConnectionData(String appName, DbxRequestConfig config, DbxAppInfo appInfo) {
		this.appName = appName;
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
		return appName;
	}
}
