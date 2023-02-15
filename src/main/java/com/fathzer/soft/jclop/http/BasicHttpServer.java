package com.fathzer.soft.jclop.http;

import java.io.IOException;

import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpProcessorBuilder;
import org.apache.http.protocol.HttpService;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.protocol.ResponseServer;
import org.apache.http.protocol.UriHttpRequestHandlerMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class BasicHttpServer {
	protected static final Logger LOGGER = LoggerFactory.getLogger(BasicHttpServer.class);

	protected int port;
	protected HttpProcessor httpproc;
	private RequestListenerThread rqThread;
	private volatile boolean isStarted;

	protected BasicHttpServer() {
		this.port = 8080;

		// Create HTTP protocol processing chain
		this.httpproc = HttpProcessorBuilder.create()
				.add(new ResponseDate())
				.add(new ResponseServer("Test/1.1"))
				.add(new ResponseContent())
				.add(new ResponseConnControl()).build();
	}

	public void start() throws IOException {
		checkNotStarted();
		// Set up request handlers
		UriHttpRequestHandlerMapper registry = new UriHttpRequestHandlerMapper();

		// Set up the HTTP service
		HttpService httpService = new HttpService(httpproc, registry);
		rqThread = new RequestListenerThread(port, httpService, null);
		port = rqThread.getPort();

		createHandlers(registry);

		rqThread.start();
		synchronized(this) {
			this.isStarted = true;
			notifyAll();
		}
	}
	
	protected abstract void createHandlers(UriHttpRequestHandlerMapper registry);

	public synchronized void shutdown() {
		if (rqThread!=null) {
			rqThread.interrupt();
			isStarted = false;
		}
	}

	public boolean isStarted() {
		return isStarted;
	}
	
	public synchronized void waitForStarted() throws InterruptedException {
		while (!isStarted) {
			wait();
		}
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		checkNotStarted();
		this.port = port;
	}

	protected void checkNotStarted() {
		if (isStarted()) {
			throw new IllegalStateException("Can't be changed after server was started");
		}
	}
}
