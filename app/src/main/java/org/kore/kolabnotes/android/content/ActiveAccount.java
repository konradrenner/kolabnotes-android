package org.kore.kolabnotes.android.content;

import java.io.Serializable;

/**
 * Created by koni on 30.04.15.
 */
public class ActiveAccount extends AccountIdentifier {
    public ActiveAccount(String accountEmail, String rootFolder) {
        super(accountEmail, rootFolder);
    }

    //nothing special at the moment
}
