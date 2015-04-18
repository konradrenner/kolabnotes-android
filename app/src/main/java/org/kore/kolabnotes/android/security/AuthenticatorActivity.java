package org.kore.kolabnotes.android.security;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.SyncStateContract;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.kore.kolab.notes.AccountInformation;
import org.kore.kolab.notes.RemoteNotesRepository;
import org.kore.kolabnotes.android.R;

/**
 * The Authenticator activity.
 *
 * Called by the Authenticator and in charge of identifing the user.
 *
 * It sends back to the Authenticator the result.
 */
public class AuthenticatorActivity extends AccountAuthenticatorActivity {

    public final static String ARG_ACCOUNT_TYPE = "kore.kolabnotes";

    public static final String KEY_ERROR_MESSAGE = "ERR_MSG";

    public final static String KEY_ACCOUNT_NAME = "account_name";
    public final static String KEY_ROOT_FOLDER = "root_folder";
    public final static String KEY_SERVER = "server_url";
    public final static String KEY_EMAIL = "email";
    public final static String KEY_PORT = "port";


    private final int REQ_SIGNUP = 1;

    private final String TAG = this.getClass().getSimpleName();

    private AccountManager mAccountManager;
    private String mAuthTokenType;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_kolab_login);
        mAccountManager = AccountManager.get(getBaseContext());

        findViewById(R.id.submit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submit();
            }
        });
    }
    public void submit() {

        final EditText mAccountNameView = (EditText)findViewById(R.id.accountName);
        final EditText mRootFolderView = (EditText)findViewById(R.id.imap_root_folder);
        final EditText mIMAPServerView = (EditText)findViewById(R.id.imap_server_url);
        final AutoCompleteTextView mEmailView = (AutoCompleteTextView) findViewById(R.id.email);
        final EditText mPasswordView = (EditText) findViewById(R.id.accountPassword);
        final EditText mPortView = (EditText) findViewById(R.id.port_number);

        final String accountType = getIntent().getStringExtra(ARG_ACCOUNT_TYPE);

        Log.d("kolabnotes", TAG + "> Started authenticating");

        Bundle data = new Bundle();
        try {

            // Reset errors.
            mEmailView.setError(null);
            mPasswordView.setError(null);
            mAccountNameView.setError(null);
            mRootFolderView.setError(null);
            mIMAPServerView.setError(null);

            // Store values at the time of the login attempt.
            String email = mEmailView.getText().toString();
            String password = mPasswordView.getText().toString();
            String accountName = mAccountNameView.getText().toString();
            String rootFolder = mRootFolderView.getText().toString();
            String imapServer = mIMAPServerView.getText().toString();
            int port = mPortView.getText() == null || mPortView.getText().toString().trim().length() == 0 ? 993 : Integer.valueOf(mPortView.getText().toString());

            boolean cancel = false;
            View focusView = null;

            // Check for a valid email address.
            if (TextUtils.isEmpty(accountName)) {
                mAccountNameView.setError(getString(R.string.error_field_required));
                focusView = mAccountNameView;
                cancel = true;
            }else if (TextUtils.isEmpty(imapServer)) {
                mIMAPServerView.setError(getString(R.string.error_field_required));
                focusView = mIMAPServerView;
                cancel = true;
            }else if (TextUtils.isEmpty(rootFolder)) {
                mRootFolderView.setError(getString(R.string.error_field_required));
                focusView = mRootFolderView;
                cancel = true;
            }else if (TextUtils.isEmpty(email)) {
                mEmailView.setError(getString(R.string.error_field_required));
                focusView = mEmailView;
                cancel = true;
            }else if (TextUtils.isEmpty(password)) {
                mPasswordView.setError(getString(R.string.error_field_required));
                focusView = mPasswordView;
                cancel = true;
            }


            if (cancel) {
                // There was an error; don't attempt login and focus the first
                // form field with an error.
                focusView.requestFocus();
            } else {

                Account account = new Account(accountName, ARG_ACCOUNT_TYPE);
                AccountInformation accountInformation = AccountInformation.createForHost(imapServer).username(email).password(password).port(port).build();
                KolabAccount serverInfo = new KolabAccount(accountName,rootFolder,accountInformation);

                Bundle userData = createAuthBundle(serverInfo);

                if (!mAccountManager.addAccountExplicitly(account, password, userData)) {
                    Toast.makeText(getBaseContext(), R.string.error_duplicate_account, Toast.LENGTH_LONG).show();
                }
            }

        } catch (Exception e) {
            Toast.makeText(getBaseContext(), R.string.error_generic, Toast.LENGTH_LONG).show();
        }
    }

    private Bundle createAuthBundle(KolabAccount kolabAccount) {
        Bundle bundle = new Bundle();
        bundle.putString(KEY_ACCOUNT_NAME, kolabAccount.getAccountName());
        bundle.putString(KEY_ROOT_FOLDER, kolabAccount.getRootFolder());
        bundle.putString(KEY_SERVER, kolabAccount.getAccountInformation().getHost());
        bundle.putString(KEY_EMAIL, kolabAccount.getAccountInformation().getUsername());
        bundle.putInt(KEY_PORT, kolabAccount.getAccountInformation().getPort());
        return bundle;
    }
}