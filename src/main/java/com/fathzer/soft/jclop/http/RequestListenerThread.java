package com.fathzer.soft.jclop.http;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ServerSocket;
import java.net.Socket;

import javax.net.ssl.SSLServerSocketFactory;

import org.apache.http.HttpConnectionFactory;
import org.apache.http.HttpServerConnection;
import org.apache.http.impl.DefaultBHttpServerConnection;
import org.apache.http.impl.DefaultBHttpServerConnectionFactory;
import org.apache.http.protocol.HttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class RequestListenerThread extends Thread {
	private static final Logger LOGGER = LoggerFactory.getLogger(RequestListenerThread.class);

	private final HttpConnectionFactory<DefaultBHttpServerConnection> connFactory;
	private final ServerSocket serversocket;
	private final HttpService httpService;

	public RequestListenerThread(final int port, final HttpService httpService, final SSLServerSocketFactory sf) throws IOException {
		this.connFactory = DefaultBHttpServerConnectionFactory.INSTANCE;
		this.serversocket = sf != null ? sf.createServerSocket(port) : new ServerSocket(port);
		this.httpService = httpService;
	}
	
	int getPort() {
		return this.serversocket.getLocalPort();
	}

	@Override
	public void run() {
		LOGGER.info("Listening on port {}", this.serversocket.getLocalPort());
		while (!Thread.interrupted()) {
			try {
				// Set up HTTP connection
				Socket socket = this.serversocket.accept();
				LOGGER.trace("Incoming connection from {}", socket.getInetAddress());
				HttpServerConnection conn = this.connFactory.createConnection(socket);
				conn.setSocketTimeout(5000);
				// Start worker thread
				Thread t = new WorkerThread(this.httpService, conn);
				t.start();
			} catch (InterruptedIOException ex) {
				LOGGER.info("Thread was interrupted",ex);
				break;
			} catch (IOException e) {
				if (!Thread.interrupted()) {
					// If thread is interrupted, receiving an exception is normal
					LOGGER.warn("I/O error initializing connection thread: ", e);
				}
				break;
			}
		}
		LOGGER.info("Stop listening on port {}", this.serversocket.getLocalPort());
	}

	@Override
	public void interrupt() {
		super.interrupt();
		try {
			this.serversocket.close();
		} catch (IOException e) {
			LOGGER.warn("IOException closing the socket");
		}
	}
}