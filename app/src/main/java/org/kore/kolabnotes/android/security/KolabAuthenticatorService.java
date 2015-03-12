package org.kore.kolabnotes.android.security;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Created by koni on 12.03.15.
 */
public class KolabAuthenticatorService  extends Service{
    @Override
    public IBinder onBind(Intent intent) {

        KolabAccountAuthenticator authenticator = new KolabAccountAuthenticator(this);
        return authenticator.getIBinder();
    }
}
