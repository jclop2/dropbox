package com.fathzer.soft.jclop.dropbox;

import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;

import com.dropbox.core.DbxAppInfo;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.http.StandardHttpRequestor;
import com.dropbox.core.http.StandardHttpRequestor.Config;

/** The data related to Dropbox connection of the app.
 * <br>It typically contains application credentials, or proxy setting.
 * <br>It is totally independent of user account.
 */
public class DbxConnectionData {
	private DbxRequestConfig config;
	private DbxAppInfo appInfo;
	private String appName;

	/** Constructor.
	 * <br>Use this constructor if you want to have fine control over the DbxRequestConfig.
	 * In other cases, use the builder which is simpler and hides Dropbox API.
	 * @see Builder 
	 * @param appName The application name
	 * @param config The Dropbox request configuration
	 * @param appInfo The authentication information of the application
	 */
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
	
	/** A DbxConnectionData Builder.
	 */
	public static class Builder {
		private final String appName;
		private final String appKey;
		private final String appSecret;
		private String proxyHost;
		private int proxyPort;
		private String proxyUser;
		private String proxyPwd;
		
		public Builder(String appName, String appKey, String appSecret) {
			super();
			this.appName = appName;
			this.appKey = appKey;
			this.appSecret = appSecret;
		}

		public Builder withProxy(String proxyHost, int proxyPort) {
			this.proxyHost = proxyHost;
			this.proxyPort = proxyPort;
			if (proxyHost!=null && proxyPort<=0) {
				throw new IllegalArgumentException();
			}
			return this;
		}

		public Builder withProxy(String proxyHost, int proxyPort, String proxyUser, String proxyPwd) {
			this.withProxy(proxyHost, proxyPort);
			this.proxyUser = proxyUser;
			this.proxyPwd = proxyPwd;
			return this;
		}

		public DbxConnectionData build() {
			return new DbxConnectionData(appName, buildConfig(), new DbxAppInfo(appKey, appSecret));
		}
		
		private DbxRequestConfig buildConfig() {
			Config.Builder builder = Config.builder();
			if (proxyHost!=null) {
				builder.withProxy(new Proxy(Proxy.Type.HTTP,new InetSocketAddress(proxyHost, proxyPort)));
				if (proxyUser != null) {
					Authenticator.setDefault(new Authenticator() {
						@Override
						protected PasswordAuthentication getPasswordAuthentication() {
							return new PasswordAuthentication(proxyUser, proxyPwd.toCharArray());
						}
					});
				}
			}
			DbxRequestConfig.Builder rbuilder = DbxRequestConfig.newBuilder(appName);
			rbuilder.withHttpRequestor(new StandardHttpRequestor(builder.build()));
			return rbuilder.build();
		}
	}
}
