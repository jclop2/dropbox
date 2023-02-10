package com.fathzer.soft.jclop.dropbox;

import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;

import org.junit.Test;

import com.dropbox.core.http.HttpRequestor;

public class DbxConnectionDataTest {
	private Field configField;
	private Field proxyField;
	private Method getAuthMethod;
	
	private Method getPasswordAuthenticationMethod(Class<? extends Authenticator> aClass) {
		if (getAuthMethod==null) {
			try {
				getAuthMethod = aClass.getDeclaredMethod("getPasswordAuthentication");
			} catch (Exception e) {
				throw new RuntimeException("Unable to find getPasswordAuthentication method in Authenticator",e);
			}
		}
		return getAuthMethod;
	}

	@Test
	public void test() throws Exception {
		Authenticator currentAuth = getDefaultAuthenticator();
		DbxConnectionData.Builder builder = new DbxConnectionData.Builder("name", "key", "secret");
		DbxConnectionData data = builder.build();
		assertEquals("name", data.getAppName());
		assertEquals("key", data.getAppInfo().getKey());
		assertEquals("secret", data.getAppInfo().getSecret());
		assertEquals(Proxy.NO_PROXY, getProxy(data));
		assertEquals(currentAuth, getDefaultAuthenticator());
		
		// With basic proxy
		Proxy expected = new Proxy(Proxy.Type.HTTP,new InetSocketAddress("myProxy.com", 3456));
		builder.withProxy("myProxy.com", 3456);
		data = builder.build();
		assertEquals(expected, getProxy(data));
		assertEquals(currentAuth, getDefaultAuthenticator());
		
		// with Authenticate proxy
		builder.withProxy("myProxy.com", 3456, "user", "pwd");
		data = builder.build();
		assertEquals(expected, getProxy(data));
		Authenticator auth = getDefaultAuthenticator();
		PasswordAuthentication pwdAuth = (PasswordAuthentication) getPasswordAuthenticationMethod(auth.getClass()).invoke(auth);
		assertEquals("user", pwdAuth.getUserName());
		assertEquals("pwd", new String(pwdAuth.getPassword()));
		
		// Test it doesn't fail if we remove proxy
		builder.withProxy(null, 0);
		assertEquals(Proxy.NO_PROXY, getProxy(builder.build()));
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void wrongPort() {
		DbxConnectionData.Builder builder = new DbxConnectionData.Builder("name", "key", "secret");
		builder.withProxy("host.com", 0);
	}

	private Proxy getProxy(DbxConnectionData data) throws Exception {
		HttpRequestor requestor = data.getConfig().getHttpRequestor();
		if (configField==null) {
			configField = requestor.getClass().getDeclaredField("config");
			configField.setAccessible(true);
		}
		Object config = configField.get(requestor);
		if (proxyField==null) {
			proxyField = config.getClass().getDeclaredField("proxy");
			proxyField.setAccessible(true);
		}
		return (Proxy) proxyField.get(config);
	}
	
	private Authenticator getDefaultAuthenticator() {
		try {
			try {
				final Method method = Authenticator.class.getMethod("getDefault");
				return (Authenticator) method.invoke(null);
			} catch (NoSuchMethodException e) {
				// Before java 9, the method did not exists
				final Field field = Authenticator.class.getDeclaredField("theAuthenticator");
				field.setAccessible(true);
				return (Authenticator) field.get(null);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
