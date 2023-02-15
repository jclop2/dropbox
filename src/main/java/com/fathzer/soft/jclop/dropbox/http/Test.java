package com.fathzer.soft.jclop.dropbox.http;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dropbox.core.DbxWebAuth;
import com.dropbox.core.TokenAccessType;
import com.fathzer.soft.ajlib.swing.Browser;
import com.fathzer.soft.jclop.dropbox.DbxConnectionData;
import com.fathzer.soft.jclop.dropbox.swing.MessagePack;
import com.fathzer.soft.jclop.http.CallBackServer;
import com.fathzer.soft.jclop.http.Request;

public class Test {
	private static final Logger log = LoggerFactory.getLogger(Test.class);
	
	public static void main(String[] args) throws Exception {
		Function<Request, String> resBuilder = r -> {
			final List<String> error = r.getParameters().get("error");
			if (error!=null) {
				return null;
			} else {
				final List<String> code = r.getParameters().get("code");
				if (code==null || code.size()!=1) {
					throw new IllegalArgumentException();
				}
				return code.get(0);
			}
		};
		
		final CallBackServer<String> server=CallBackServer.build(resBuilder, 60947,62955,64825);
		if (server==null) {
			log.debug("No port available");
			return;
		}
	    try {
	    	Test test = new Test(new DbxConnectionData.Builder(args[0],args[1],args[2]).build());
	    	Browser.show(test.getURI(server), null, MessagePack.getString("com.fathzer.soft.jclop.dropbox.ConnectionDialog.error.unableToLaunchBrowser.title", Locale.getDefault())); //$NON-NLS-1$
	    } finally {
	    	System.out.println("Result is "+server.getAuthResult());
	    	server.close();
	    }
	}

	private DbxConnectionData ctxData;

	public Test(DbxConnectionData ctxData) {
		this.ctxData = ctxData;
	}
	
	protected <T> URI getURI(CallBackServer<T> server) {
	    final SessionStore sessionStore = new SessionStore();
		final DbxWebAuth.Request authRequest = DbxWebAuth.newRequestBuilder()
    		.withRedirectUri(server.getURL(),sessionStore)
    		.withTokenAccessType(TokenAccessType.OFFLINE)
            .build();
		server.setChallenge(r -> {
			final List<String> list = r.getParameters().get("state");
			return list!=null && list.contains(sessionStore.get());
		});
        return URI.create(new DbxWebAuth(ctxData.getConfig(), ctxData.getAppInfo()).authorize(authRequest));
	}
}
