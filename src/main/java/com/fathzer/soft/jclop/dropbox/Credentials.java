package com.fathzer.soft.jclop.dropbox;

import java.io.Serializable;

class Credentials implements Serializable {
	private static final long serialVersionUID = 1L;

	private String accessToken;
	private long expiresAt;
	private String refreshToken;
	
	public Credentials(String accessToken, long expiresAt, String refreshToken) {
		this.accessToken = accessToken;
		this.expiresAt = expiresAt;
		this.refreshToken = refreshToken;
		testConsistency();
	}

	static Credentials fromLongLived(String token) {
		return new Credentials(token, Long.MAX_VALUE, null);
	}
	static Credentials fromRefresh(String token) {
		return new Credentials("fake", 0, token);
	}
	
	private void testConsistency() {
		final boolean hasAccess = accessToken!=null && !accessToken.trim().isEmpty();
		final boolean hasRefresh = refreshToken!=null && !refreshToken.trim().isEmpty();
		if ((!hasRefresh && expiresAt<=0) || !(hasAccess || hasRefresh)) {
			throw new IllegalArgumentException();
		}
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((accessToken == null) ? 0 : accessToken.hashCode());
		result = prime * result + (int) (expiresAt ^ (expiresAt >>> 32));
		result = prime * result + ((refreshToken == null) ? 0 : refreshToken.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		Credentials other = (Credentials) obj;
		return nullSafeEquals(accessToken, other.accessToken) && nullSafeEquals(refreshToken, refreshToken) && expiresAt==other.expiresAt;
	}

	public String getAccessToken() {
		return accessToken;
	}

	public String getRefreshToken() {
		return refreshToken;
	}
	
	public long getExpiresAt() {
		return expiresAt;
	}

	private static boolean nullSafeEquals(Object obj, Object other) {
		return obj==null ? other==null : obj.equals(other);
	}

	@Override
	public String toString() {
		return "Credentials [accessToken=" + accessToken + ", expiresAt=" + expiresAt + ", refreshToken=" + refreshToken
				+ "]";
	}
}
