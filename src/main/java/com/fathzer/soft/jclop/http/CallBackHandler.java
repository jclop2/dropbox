package com.fathzer.soft.jclop.http;

import java.io.IOException;
import java.util.function.Predicate;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import com.renault.sicg.tinyhttpserver.Utils;

class CallBackHandler implements HttpRequestHandler {
	private Predicate<Request> challenge;
	
	void setChallenge(Predicate<Request> challenge) {
		this.challenge = challenge;
	}

	@Override
	public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException, IOException {
		Request req = new Request(request);
		System.out.println (req.getHeaders());
		System.out.println(req.getParameters());
		response.setStatusCode(200);
		response.setEntity(Utils.toEntity("<h1>Your logged</h1>", false));
	}
}
