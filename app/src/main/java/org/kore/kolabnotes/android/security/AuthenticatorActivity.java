package org.kore.kolabnotes.android.security;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import org.kore.kolab.notes.AccountInformation;
import org.kore.kolabnotes.android.MainActivity;
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

    // Sync interval constants
    public static final long SECONDS_PER_MINUTE = 60L;
    public static final long MINUTES_PER_HOUR = 60L;
    public static final long HOURS_PER_DAY = 24L;

    public final static String KEY_ACCOUNT_NAME = "account_name";
    public final static String KEY_ROOT_FOLDER = "root_folder";
    public final static String KEY_SERVER = "server_url";
    public final static String KEY_EMAIL = "email";
    public final static String KEY_PORT = "port";
    public final static String KEY_SSL = "enablessl";
    public final static String KEY_KOLAB = "enablekolab";


    private final String TAG = this.getClass().getSimpleName();

    private AccountManager mAccountManager;

    private Switch mExtendedOptions;
    private EditText mRootFolderView;
    private EditText mPortView;
    private CheckBox mEnableSSLView;
    private EditText mSyncView;
    private Switch mKolabView;
    private EditText mIMAPServerView;
    private Spinner mAccountType;
    private Spinner mIntervallType;

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

        mPortView = (EditText) findViewById(R.id.port_number);
        mEnableSSLView = (CheckBox) findViewById(R.id.enable_ssl);
        mSyncView = (EditText) findViewById(R.id.sync_intervall);
        mKolabView = (Switch) findViewById(R.id.enable_kolab);
        mRootFolderView = (EditText)findViewById(R.id.imap_root_folder);
        mIMAPServerView = (EditText)findViewById(R.id.imap_server_url);
        mAccountType = (Spinner)findViewById(R.id.spinner_accountType);
        mIntervallType = (Spinner)findViewById(R.id.spinner_intervall);

        mExtendedOptions = (Switch) findViewById(R.id.enable_more_config);
        mExtendedOptions.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b){
                   showExtendedOptions();
                }else{
                    hideExtendedOptions();
                }
            }
        });

        initAccountTypeSpinner();
        initIntervallSpinner();

        hideExtendedOptions();
    }

    void initAccountTypeSpinner(){
        String[] values = {"KolabNow","Kolab Server","IMAP Server"};

        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(this, R.layout.accounttype_spinner_item, values);
        adapter.setDropDownViewResource(R.layout.accounttype_spinner_item);
        mAccountType.setAdapter(adapter);
        mAccountType.setSelection(0);
        mAccountType.setOnItemSelectedListener(new AccountTypeSelectedListener());
    }

    void initIntervallSpinner(){
        String[] values = getResources().getStringArray(R.array.sync_intervall_types);

        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(this, R.layout.intervall_spinner_item, values);
        adapter.setDropDownViewResource(R.layout.intervall_spinner_item);
        mIntervallType.setAdapter(adapter);
        mIntervallType.setSelection(1);
    }

    void showExtendedOptions(){
        mPortView.setVisibility(View.VISIBLE);
        mEnableSSLView.setVisibility(View.VISIBLE);
        mSyncView.setVisibility(View.VISIBLE);
        mKolabView.setVisibility(View.VISIBLE);
        mRootFolderView.setVisibility(View.VISIBLE);
        mIntervallType.setVisibility(View.VISIBLE);
    }

    void hideExtendedOptions(){
        mPortView.setVisibility(View.INVISIBLE);
        mEnableSSLView.setVisibility(View.INVISIBLE);
        mSyncView.setVisibility(View.INVISIBLE);
        mKolabView.setVisibility(View.INVISIBLE);
        mRootFolderView.setVisibility(View.INVISIBLE);
        mIntervallType.setVisibility(View.INVISIBLE);
    }

    public void submit() {

        final EditText mAccountNameView = (EditText)findViewById(R.id.accountName);
        final AutoCompleteTextView mEmailView = (AutoCompleteTextView) findViewById(R.id.email);
        final EditText mPasswordView = (EditText) findViewById(R.id.accountPassword);

        Log.d("kolabnotes", TAG + "> Started authenticating");

        Bundle data = new Bundle();
        try {

            // Reset errors.
            mEmailView.setError(null);
            mPasswordView.setError(null);
            mAccountNameView.setError(null);
            mRootFolderView.setError(null);
            mIMAPServerView.setError(null);
            mSyncView.setError(null);

            // Store values at the time of the login attempt.
            String email = mEmailView.getText().toString();
            String password = mPasswordView.getText().toString();
            String accountName = mAccountNameView.getText().toString();
            String rootFolder = mRootFolderView.getText() == null || mRootFolderView.getText().toString().trim().length() == 0 ? "Notes" : mRootFolderView.getText().toString();
            String imapServer = mIMAPServerView.getText().toString();
            int syncIntervall = mSyncView.getText() == null || mSyncView.getText().toString().trim().length() == 0 ? 24 : Integer.valueOf(mSyncView.getText().toString());
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
            }else if (TextUtils.isEmpty(email)) {
                mEmailView.setError(getString(R.string.error_field_required));
                focusView = mEmailView;
                cancel = true;
            }else if (TextUtils.isEmpty(password)) {
                mPasswordView.setError(getString(R.string.error_field_required));
                focusView = mPasswordView;
                cancel = true;
            }else if (syncIntervall < 1 || syncIntervall > 90) {
                mSyncView.setError(getString(R.string.error_field_to_low));
                focusView = mSyncView;
                cancel = true;
            }


            if (cancel) {
                // There was an error; don't attempt login and focus the first
                // form field with an error.
                focusView.requestFocus();
            } else {

                AccountInformation.Builder builder = AccountInformation.createForHost(imapServer).username(email).password(password).port(port);

                if(!mEnableSSLView.isChecked()){
                    builder.disableSSL();
                }

                if(!mKolabView.isChecked()){
                    builder.disableFolderAnnotation();
                }

                Account account = new Account(accountName, ARG_ACCOUNT_TYPE);
                AccountInformation accountInformation = builder.build();
                KolabAccount serverInfo = new KolabAccount(accountName,rootFolder,accountInformation);

                Bundle userData = createAuthBundle(serverInfo);

                if (mAccountManager.addAccountExplicitly(account, password, userData)) {
                    Toast.makeText(getBaseContext(), R.string.signup_ok, Toast.LENGTH_SHORT).show();

                    ContentResolver.setIsSyncable(account, MainActivity.AUTHORITY, 1);
                    ContentResolver.setSyncAutomatically(account, MainActivity.AUTHORITY, true);



                    ContentResolver.addPeriodicSync(account,
                            MainActivity.AUTHORITY,
                            Bundle.EMPTY,
                            calculateIntervall(syncIntervall));

                    Intent intent = new Intent(this,MainActivity.class);

                    startActivity(intent);
                }else{
                    Toast.makeText(getBaseContext(), R.string.error_duplicate_account, Toast.LENGTH_LONG).show();
                }
            }

        } catch (Exception e) {
            Toast.makeText(getBaseContext(), R.string.error_generic, Toast.LENGTH_LONG).show();
        }
    }

    private long calculateIntervall(long given){
        if(mIntervallType.getSelectedItemPosition() == 0){
            return given*SECONDS_PER_MINUTE;
        }else if(mIntervallType.getSelectedItemPosition() == 0){
            return given*MINUTES_PER_HOUR*SECONDS_PER_MINUTE;
        }
        return given*HOURS_PER_DAY*MINUTES_PER_HOUR*SECONDS_PER_MINUTE;
    }

    private Bundle createAuthBundle(KolabAccount kolabAccount) {
        Bundle bundle = new Bundle();
        bundle.putString(KEY_ACCOUNT_NAME, kolabAccount.getAccountName());
        bundle.putString(KEY_ROOT_FOLDER, kolabAccount.getRootFolder());
        bundle.putString(KEY_SERVER, kolabAccount.getAccountInformation().getHost());
        bundle.putString(KEY_EMAIL, kolabAccount.getAccountInformation().getUsername());
        bundle.putString(KEY_PORT, Integer.toString(kolabAccount.getAccountInformation().getPort()));
        bundle.putString(KEY_SSL, Boolean.toString(kolabAccount.getAccountInformation().isSSLEnabled()));
        bundle.putString(KEY_KOLAB, Boolean.toString(kolabAccount.getAccountInformation().isFolderAnnotationEnabled()));
        return bundle;
    }

    class AccountTypeSelectedListener implements AdapterView.OnItemSelectedListener{
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            if(position == 0){
                setKolabNowValues();
            }else if(position == 1){
                setKolabValues();
            }else{
                setIMAPValues();
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            //nothing
        }

        private void setKolabNowValues(){
            mPortView.setText("993");
            mEnableSSLView.setChecked(true);
            mKolabView.setChecked(true);
            mIMAPServerView.setText("imap.kolabnow.com");
        }

        private void setKolabValues(){
            mKolabView.setChecked(true);
            mIMAPServerView.setText("");
        }

        private void setIMAPValues(){
            mKolabView.setChecked(false);
            mIMAPServerView.setText("");
        }
    }
}