package com.fathzer.soft.jclop.http;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;

import org.apache.http.ConnectionClosedException;
import org.apache.http.HttpException;
import org.apache.http.HttpServerConnection;
import org.apache.http.impl.DefaultBHttpServerConnectionFactory;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ClientConnection implements Runnable {
	private static final Logger LOGGER = LoggerFactory.getLogger(ClientConnection.class);

	private final HttpService httpservice;
	private final Socket socket;

	public ClientConnection(final HttpService httpservice, final Socket socket) {
		this.httpservice = httpservice;
		this.socket = socket;
	}

	@Override
	public void run() {
		LOGGER.trace("New connection thread");
		HttpContext context = new BasicHttpContext(null);
		try (HttpServerConnection conn=DefaultBHttpServerConnectionFactory.INSTANCE.createConnection(socket)) {
			conn.setSocketTimeout(5000);
			while (!Thread.interrupted() && conn.isOpen()) {
				this.httpservice.handleRequest(conn, context);
			}
		} catch (SocketTimeoutException ex) {
			LOGGER.trace("Server closed the connection",ex);
		} catch (ConnectionClosedException ex) {
			LOGGER.trace("Client closed connection",ex);
		} catch (IOException ex) {
			LOGGER.warn("I/O error",ex);
		} catch (HttpException ex) {
			LOGGER.error("Unrecoverable HTTP protocol violation",ex);
		}
		LOGGER.debug("Connection with client is closed");
	}
}