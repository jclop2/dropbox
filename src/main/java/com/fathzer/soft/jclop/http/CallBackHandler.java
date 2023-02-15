package com.fathzer.soft.jclop.http;

import java.io.IOException;
import java.util.function.Function;
import java.util.function.Predicate;

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

class CallBackHandler implements HttpRequestHandler {
	private Predicate<Request> challenge;
	private Function<Request, Boolean> resultBuilder;
	
	void setChallenge(Predicate<Request> challenge) {
		this.challenge = challenge;
	}

	void setResultBuilder(Function<Request, Boolean> resultBuilder) {
		this.resultBuilder = resultBuilder;
	}

	@Override
	public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException, IOException {
		final Request req = new Request(request);
		if (challenge.test(req)) {
			handleChallengedRequest(response, req);
		} else {
			response.setStatusCode(400);
			response.setEntity(toEntity("<h1>Something is wrong with your request</h1>"));
		}
	}

	private void handleChallengedRequest(HttpResponse response, Request req) {
		try {
			final boolean granted = resultBuilder.apply(req);
			response.setStatusCode(200);
			if (granted) {
				response.setStatusCode(200);
				response.setEntity(toEntity("<h1>Your logged in</h1>"));
			} else {
				response.setStatusCode(401);
				response.setEntity(toEntity("<h1>The access was not granted</h1>"));
			}
		} catch (IllegalArgumentException e) {
			response.setStatusCode(400);
			response.setEntity(toEntity("<h1>The server returned a response that I can't understand</h1>"));
		}
		synchronized (this) {
			this.notifyAll();
		}
	}
	
	private static HttpEntity toEntity(String body) {
		final String content = "<html><head></head><body>"+body+"</body></html>";
		return new StringEntity(content, ContentType.TEXT_HTML);
	}
}
