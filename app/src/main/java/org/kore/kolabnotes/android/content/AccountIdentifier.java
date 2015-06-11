package org.kore.kolabnotes.android.content;

import java.io.Serializable;

/**
 * Created by koni on 11.06.15.
 */
public class AccountIdentifier implements Serializable{

    private final String accountEmail;
    private final String rootFolder;

    public AccountIdentifier(String accountEmail, String rootFolder) {
        this.accountEmail = accountEmail;
        this.rootFolder = rootFolder;
    }

    public String getAccount() {
        return accountEmail;
    }

    public String getRootFolder() {
        return rootFolder;
    }

    @Override
    public String toString() {
        return "AccountIdentifier{" +
                "accountEmail='" + accountEmail + '\'' +
                ", rootFolder='" + rootFolder + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AccountIdentifier that = (AccountIdentifier) o;

        if (!accountEmail.equals(that.accountEmail)) return false;
        if (!rootFolder.equals(that.rootFolder)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = accountEmail.hashCode();
        result = 31 * result + rootFolder.hashCode();
        return result;
    }
}
