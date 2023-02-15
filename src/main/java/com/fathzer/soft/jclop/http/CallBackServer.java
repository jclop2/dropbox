package com.fathzer.soft.jclop.http;

import java.io.Closeable;
import java.io.IOException;
import java.util.function.Function;
import java.util.function.Predicate;

import org.apache.http.protocol.UriHttpRequestHandlerMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CallBackServer<T> implements Closeable {
	private static final String CALLBACK_PATH = "/auth";
	private static final Logger LOG = LoggerFactory.getLogger(CallBackServer.class);

	private BasicHttpServer server;
	private CallBackHandler handler;
	private T result;

	public static <T> CallBackServer<T> build(Function<Request, T> resultBuilder, int... ports) {
		final CallBackHandler handler = new CallBackHandler();
		BasicHttpServer server = new BasicHttpServer() {
			@Override
			protected void createHandlers(UriHttpRequestHandlerMapper reqistry) {
				reqistry.register(CALLBACK_PATH, handler);
			}
		};
		for (int port : ports) {
			server.setPort(port);
			try {
				server.start();
				final CallBackServer<T> callBackServer = new CallBackServer<>(server, handler);
				handler.setResultBuilder(r -> {
					callBackServer.result = resultBuilder.apply(r);
					return callBackServer.result!=null;
				});
				return callBackServer;
			} catch (IOException e) {
				LOG.warn("Can't open server on port "+port,e);
				server.shutdown();
			}
		}
		return null;
	}

	private CallBackServer(BasicHttpServer server, CallBackHandler handler) {
		this.server = server;
		this.handler = handler;
	}

	public String getURL() {
		return "http://127.0.0.1:"+server.getPort()+CALLBACK_PATH;
	}
	
	public void setChallenge(Predicate<Request> challenge) {
		this.handler.setChallenge(challenge);
	}
	
	public T getAuthResult() throws InterruptedException {
		synchronized (handler) {
			//TODO We should not wait forever
			this.handler.wait();
		}
		return result;
	}
	
	public void close() {
		this.server.shutdown();
	}
}
