package org.kore.kolabnotes.android.security;

/**
 * Created by koni on 12.03.15.
 */
public class AccountGeneral {

    /**
     * Account type id
     */
    public static final String ACCOUNT_TYPE = "kore.kolabnotes";

    /**
     * Account name
     */
    public static final String ACCOUNT_NAME = "Kolab Notes";

    /**
     * Auth token types
     */
    public static final String AUTHTOKEN_TYPE_READ_ONLY = "Read only";
    public static final String AUTHTOKEN_TYPE_READ_ONLY_LABEL = "Read only access to a Kolab Notes account";

    public static final String AUTHTOKEN_TYPE_FULL_ACCESS = "Full access";
    public static final String AUTHTOKEN_TYPE_FULL_ACCESS_LABEL = "Full access to a Kolab Notes account";

    public static final ServerAuthenticate sServerAuthenticate = new ParseComServerAuthenticate();
}
