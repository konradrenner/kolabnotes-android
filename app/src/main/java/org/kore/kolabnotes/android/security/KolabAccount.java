package org.kore.kolabnotes.android.security;

import org.kore.kolab.notes.AccountInformation;

/**
 * Created by koni on 18.04.15.
 */
public class KolabAccount {

    private final String accountName;
    private final String rootFolder;
    private final AccountInformation accountInformation;

    public KolabAccount(String accountName, String rootFolder, AccountInformation accountInformation) {
        this.accountName = accountName;
        this.rootFolder = rootFolder;
        this.accountInformation = accountInformation;
    }

    public String getAccountName() {
        return accountName;
    }

    public String getRootFolder() {
        return rootFolder;
    }

    public AccountInformation getAccountInformation() {
        return accountInformation;
    }

    @Override
    public String toString() {
        return "KolabAccount{" +
                "accountName='" + accountName + '\'' +
                ", rootFolder='" + rootFolder + '\'' +
                ", accountInformation=" + accountInformation +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        KolabAccount that = (KolabAccount) o;

        if (accountName != null ? !accountName.equals(that.accountName) : that.accountName != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return accountName != null ? accountName.hashCode() : 0;
    }
}
