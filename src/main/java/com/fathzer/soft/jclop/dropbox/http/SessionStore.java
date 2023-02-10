package com.fathzer.soft.jclop.dropbox.http;

import com.dropbox.core.DbxSessionStore;

final class SessionStore implements DbxSessionStore {
	private String value;
	@Override
	public void set(String value) {
		this.value = value;
	}

	@Override
	public String get() {
		return value;
	}

	@Override
	public void clear() {
		this.value = null;
	}
}