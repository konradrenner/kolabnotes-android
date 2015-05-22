package org.kore.kolabnotes.android.content;

import java.io.Serializable;

/**
 * Created by koni on 30.04.15.
 */
public class ActiveAccount implements Serializable {

    private final String account;
    private final String rootFolder;

    public ActiveAccount(String account, String rootFolder) {
        this.account = account;
        this.rootFolder = rootFolder;
    }

    public String getAccount() {
        return account;
    }

    public String getRootFolder() {
        return rootFolder;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ActiveAccount that = (ActiveAccount) o;

        if (!account.equals(that.account)) return false;
        if (!rootFolder.equals(that.rootFolder)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = account.hashCode();
        result = 31 * result + rootFolder.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "ActiveAccount{" +
                "account='" + account + '\'' +
                ", rootFolder='" + rootFolder + '\'' +
                '}';
    }
}
