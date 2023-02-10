package com.fathzer.soft.jclop.dropbox.http;

import java.net.URI;
import java.util.Locale;

import com.dropbox.core.DbxApiException;
import com.dropbox.core.DbxAuthFinish;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxStandardSessionStore;
import com.dropbox.core.DbxWebAuth;
import com.dropbox.core.TokenAccessType;
import com.dropbox.core.v2.DbxClientV2;
import com.fathzer.soft.ajlib.swing.Browser;
import com.fathzer.soft.jclop.dropbox.DbxConnectionData;
import com.fathzer.soft.jclop.dropbox.swing.ConnectionDialog;
import com.fathzer.soft.jclop.dropbox.swing.MessagePack;
import com.fathzer.soft.jclop.http.CallBackServer;

public class Test {

	public static void main(String[] args) throws Exception {
		final DbxConnectionData ctxData = new DbxConnectionData.Builder(args[0],args[1],args[2]).build();
		CallBackServer server=CallBackServer.build(60947,62955,64825);
		if (server==null) {
			System.out.println("No port available");
			return;
		}
	    try {
		    final SessionStore sessionStore = new SessionStore();
			final DbxWebAuth.Request authRequest = DbxWebAuth.newRequestBuilder()
	    		.withRedirectUri(server.getURL(),sessionStore)
	    		.withTokenAccessType(TokenAccessType.OFFLINE)
	            .build();
	        final String authorizeUrl = new DbxWebAuth(ctxData.getConfig(), ctxData.getAppInfo()).authorize(authRequest);
	    	Browser.show(URI.create(authorizeUrl), null, MessagePack.getString("com.fathzer.soft.jclop.dropbox.ConnectionDialog.error.unableToLaunchBrowser.title", Locale.getDefault())); //$NON-NLS-1$
	    	synchronized(server) {
	    		server.wait(sessionStore.get());
	    	}
	    } finally {
	    	server.close();
	    }
		
		
//		final ConnectionDialog dialog = new ConnectionDialog(null, ctxData, Locale.FRANCE);
//		dialog.setVisible(true);
//		DbxAuthFinish result = dialog.getResult();
//		System.out.println (result.getRefreshToken());
//		System.out.println (result.getAccessToken() +" -> "+result.getExpiresAt());
//		String refreshToken = result.getRefreshToken();

		
//		final String refreshToken = System.getProperty("refresh");
//		final String eternalAccessToken = System.getProperty("access");
//		
//		final Credentials cred = Credentials.fromRefresh(refreshToken); 
//		final Credentials cred = Credentials.fromLongLived(eternalAccessToken); 
//		DbxClientV2 api = new DbxClientV2(ctxData.getConfig(), cred.toDbx(ctxData.getAppInfo()));
//		System.out.println (api.users().getCurrentAccount().getName());
	}

}
