package com.fathzer.soft.jclop.http;

import java.io.IOException;
import java.net.SocketTimeoutException;

import org.apache.http.ConnectionClosedException;
import org.apache.http.HttpException;
import org.apache.http.HttpServerConnection;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class WorkerThread extends Thread {
	private static final Logger LOGGER = LoggerFactory.getLogger(WorkerThread.class);
	private static final ThreadGroup THREAD_GROUP = new ThreadGroup(WorkerThread.class.getName());

	private final HttpService httpservice;
	private final HttpServerConnection conn;

	public WorkerThread(final HttpService httpservice, final HttpServerConnection conn) {
		super(THREAD_GROUP, (Runnable)null);
		this.httpservice = httpservice;
		this.conn = conn;
	}

	@Override
	public void run() {
		LOGGER.trace("New connection thread");
		HttpContext context = new BasicHttpContext(null);
		try {
			while (!Thread.interrupted() && this.conn.isOpen()) {
				this.httpservice.handleRequest(this.conn, context);
			}
		} catch (SocketTimeoutException ex) {
			LOGGER.trace("Server closed the connection",ex);
		} catch (ConnectionClosedException ex) {
			LOGGER.trace("Client closed connection",ex);
		} catch (IOException ex) {
			LOGGER.warn("I/O error",ex);
		} catch (HttpException ex) {
			LOGGER.error("Unrecoverable HTTP protocol violation",ex);
		} finally {
			try {
				LOGGER.debug("Closing connection with client");
				this.conn.shutdown();
			} catch (IOException ex) {
				LOGGER.warn("Exception while closing connection", ex);
			}
		}
	}

	static void kill() {
		LOGGER.info("Stop client communications");
		THREAD_GROUP.interrupt();
	}
}