package com.fathzer.soft.jclop.dropbox;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
 
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.WebAuthSession;
 
/** An abstract Dropbox session compatible with <b>authenticated</b> proxy.
 * <br>You should create a subclass of it and implement the getProxyName and getProxyPassword methods.
 * <br><b>Please note</b> that this class requires apache http client 4.2.1 or more (Dropbox libraries are currently delivered with 4.0.3) and the apache commons-codec library.
 * @author Jean-Marc Astesana
 * <BR>License : GPL v3
 */
public abstract class AuthenticatedProxiedSession extends WebAuthSession {
	/** Constructor.
	 * @param appKeyPair The application key pair
	 * @param accessType The access type
	 */
	public AuthenticatedProxiedSession(AppKeyPair appKeyPair, AccessType accessType) {
		super(appKeyPair, accessType);
	}

	/** Gets the proxy user name.
	 * @return a String or null if the proxy is not authenticated
	 */
	public abstract String getProxyUserName();

	/** Gets the proxy user password.
	 * @return a String or null if the proxy is not authenticated
	 */
	public abstract String getProxyPassword();

	private HttpHost getProxy() {
		ProxyInfo proxyInfo = getProxyInfo();
		HttpHost proxy = null;
		if (proxyInfo != null && proxyInfo.host != null && !proxyInfo.host.equals("")) {
			if (proxyInfo.port < 0) {
				proxy = new HttpHost(proxyInfo.host);
			} else {
				proxy = new HttpHost(proxyInfo.host, proxyInfo.port);
			}
		}
		return proxy;
	}

	/** This method is used by Dropbox to retrieve its HttpClient. */
	@Override
	public synchronized HttpClient getHttpClient() {
		DefaultHttpClient client = (DefaultHttpClient) super.getHttpClient();
		// Added to allow connection through a authenticated proxy
		// In addition, you should add commons-codec to the classpath
		HttpHost proxy = getProxy();
		if (proxy != null && getProxyUserName()!=null) {
			client.getCredentialsProvider().setCredentials(new AuthScope(proxy), new UsernamePasswordCredentials(getProxyUserName(), getProxyPassword()));
		}
		return client;
	}
}