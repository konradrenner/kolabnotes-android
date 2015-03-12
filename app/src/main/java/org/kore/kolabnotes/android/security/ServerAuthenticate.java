package org.kore.kolabnotes.android.security;

/**
 * Created by koni on 12.03.15.
 */
public interface ServerAuthenticate {
    public String userSignIn(final String user, final String pass, String authType) throws Exception;
}