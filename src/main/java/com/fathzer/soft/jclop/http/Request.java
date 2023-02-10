package com.fathzer.soft.jclop.http;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

public class Request {
	private Map<String,List<String>> headers;
	private Map<String,List<String>> parameters;

	Request(HttpRequest http) {
		this.parameters = getParams(http);
		this.headers = getHeaders(http);
	}
	
	private Map<String, List<String>> getHeaders(HttpRequest http) {
		Header[] httpHeaders = http.getAllHeaders();
		if (httpHeaders.length==0) {
			return Collections.emptyMap();
		} else {
			return fromNameValues(Arrays.asList(httpHeaders));
		}
	}

	private Map<String,List<String>> getParams(HttpRequest request) {
		String query = request.getRequestLine().getUri();
		final int pathEnd = query.indexOf('?');
		if (pathEnd<0) {
			return Collections.emptyMap();
		} else {
			return fromNameValues(URLEncodedUtils.parse(query.substring(pathEnd+1), StandardCharsets.UTF_8));
		}
	}

	private Map<String, List<String>> fromNameValues(final List<? extends NameValuePair> asList) {
		final Map<String, List<String>> result = new HashMap<>();
		for (NameValuePair pair:asList) {
			final List<String> lst = result.computeIfAbsent(pair.getName(), k->new LinkedList<String>());
			lst.add(pair.getValue());
		}
		return result;
	}

	public Map<String, List<String>> getHeaders() {
		return headers;
	}

	public Map<String, List<String>> getParameters() {
		return parameters;
	}
}
