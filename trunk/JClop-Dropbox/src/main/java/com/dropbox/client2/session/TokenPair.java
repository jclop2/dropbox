package com.dropbox.client2.session;

import java.io.Serializable;

/**
 * <p>
 * Just two strings -- a "key" and a "secret". Used by OAuth in several
 * places (consumer key/secret, request token/secret, access token/secret).
 * Use specific subclasses instead of using this class directly.
 * </p>
 */
public abstract class TokenPair implements Serializable {
	private static final long serialVersionUID = -42727403799660313L;

	public final String key;
    public final String secret;

    public TokenPair(String key, String secret) {
        this.key = key;
        this.secret = secret;
    }
}
