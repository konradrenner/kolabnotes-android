package org.kore.kolabnotes.android.content;

import java.sql.Timestamp;

/**
 * Created by koni on 21.03.15.
 */
public class Modification {
    private String uid;
    private String rootFolder;
    private String account;
    private ModificationRepository.ModificationType type;
    private Timestamp modificationDate;

    public Modification(String account, String rootFolder, String uid, ModificationRepository.ModificationType type, Timestamp modificationDate) {
        this.uid = uid;
        this.rootFolder = rootFolder;
        this.account = account;
        this.type = type;
        this.modificationDate = modificationDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Modification that = (Modification) o;

        if (uid != null ? !uid.equals(that.uid) : that.uid != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return uid != null ? uid.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Modification{" +
                "uid='" + uid + '\'' +
                ", type=" + type +
                ", modificationDate=" + modificationDate +
                '}';
    }

    public String getUid() {
        return uid;
    }

    public ModificationRepository.ModificationType getType() {
        return type;
    }

    public Timestamp getModificationDate() {
        return modificationDate;
    }

    public String getRootFolder() {
        return rootFolder;
    }
}
