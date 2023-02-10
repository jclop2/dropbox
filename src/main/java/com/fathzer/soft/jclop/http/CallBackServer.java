package com.fathzer.soft.jclop.http;

import java.io.Closeable;
import java.io.IOException;

import org.apache.http.protocol.UriHttpRequestHandlerMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.renault.sicg.tinyhttpserver.SynchronousHttpServer;

public class CallBackServer implements Closeable {
	private static final String CALLBACK_PATH = "/auth";
	private static final Logger LOG = LoggerFactory.getLogger(CallBackServer.class);
	private SynchronousHttpServer server;
	private CallBackHandler handler;

	public static CallBackServer build(int... ports) {
		final CallBackHandler handler = new CallBackHandler();
		SynchronousHttpServer server = new SynchronousHttpServer() {
			@Override
			protected void createHandlers(UriHttpRequestHandlerMapper reqistry) {
				reqistry.register(CALLBACK_PATH, handler);
			}
		};
		for (int port : ports) {
			server.setPort(port);
			try {
				server.start();
				return new CallBackServer(server, handler);
			} catch (IOException e) {
				LOG.debug("Can't open server on port "+port,e);
			}
		}
		return null;
	}

	private CallBackServer(SynchronousHttpServer server, CallBackHandler handler) {
		this.server = server;
		this.handler = handler;
	}

	public String getURL() {
		return "http://127.0.0.1:"+server.getPort()+CALLBACK_PATH;
	}
	
	public void close() throws IOException {
		this.server.shutdown();
	}

	public void wait(String challenge) {
		// TODO Should be more generic to pass a challenge validator lambda to ensure the server serves a redirection from the auth server
		
	}
}
