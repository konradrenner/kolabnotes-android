package org.kore.kolabnotes.android.widget;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import org.kore.kolab.notes.Note;
import org.kore.kolabnotes.android.R;
import org.kore.kolabnotes.android.Utils;
import org.kore.kolabnotes.android.adapter.NoteListAdapater;
import org.kore.kolabnotes.android.content.DatabaseHelper;
import org.kore.kolabnotes.android.content.NoteRepository;
import org.kore.kolabnotes.android.content.NoteSorting;
import org.kore.kolabnotes.android.security.AuthenticatorActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The configuration screen for the {@link StickyNoteWidget StickyNoteWidget} AppWidget.
 */
public class StickyNoteWidgetConfigureActivity extends Activity {

    int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    public static final String PREFS_NAME = "org.kore.kolabnotes.android.widget.StickyNoteWidget";
    public static final String PREF_PREFIX_KEY_NOTE = "appwidget_note_sticky_";
    public static final String PREF_PREFIX_KEY_ACCOUNT = "appwidget_account_sticky_";

    private NoteRepository noteRepository;
    private AccountManager mAccountManager;
    private Spinner accountSpinner;
    private Spinner noteSpinner;

    private Account selectedAccount;
    private String selectedNote;

    private String localAccountName;

    public StickyNoteWidgetConfigureActivity() {
        super();
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // Set the result to CANCELED.  This will cause the widget host to cancel
        // out of the widget placement if the user presses the back button.
        setResult(RESULT_CANCELED);

        setContentView(R.layout.sticky_note_widget_configure);
        findViewById(R.id.add_button).setOnClickListener(mOnClickListener);

        noteRepository = new NoteRepository(this);

        accountSpinner = (Spinner) findViewById(R.id.spinner_account);
        noteSpinner = (Spinner) findViewById(R.id.spinner_note);

        localAccountName = getResources().getString(R.string.drawer_account_local);

        // Find the widget id from the intent.
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        // If this activity was started with an intent without an app widget ID, finish with an error.
        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }

        mAccountManager = AccountManager.get(this);
        initSpinners();
    }

    View.OnClickListener mOnClickListener = new View.OnClickListener() {
        public void onClick(View v) {

            if(selectedNote == null){
                Toast.makeText(getApplicationContext(),getResources().getString(R.string.no_selection), Toast.LENGTH_SHORT);
            }else {

                final Context context = StickyNoteWidgetConfigureActivity.this;

                // When the button is clicked, store the string locally
                if (selectedAccount == null) {
                    saveStickyNoteWidgetPref(context, mAppWidgetId, "local", selectedNote);
                } else {
                    saveStickyNoteWidgetPref(context, mAppWidgetId, mAccountManager.getUserData(selectedAccount, AuthenticatorActivity.KEY_ACCOUNT_NAME), selectedNote);
                }

                // It is the responsibility of the configuration activity to update the app widget
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                StickyNoteWidget.updateAppWidget(context, appWidgetManager, mAppWidgetId,noteRepository);

                // Make sure we pass back the original appWidgetId
                Intent resultValue = new Intent();
                resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
                setResult(RESULT_OK, resultValue);
                finish();
            }
        }
    };

    static void saveStickyNoteWidgetPref(Context context, int appWidgetId, String accountName, String note) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putString(PREF_PREFIX_KEY_ACCOUNT + appWidgetId, accountName);
        prefs.putString(PREF_PREFIX_KEY_NOTE + appWidgetId, note);
        prefs.commit();
    }

    static String loadStickyNoteWidgetAccountPref(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        return prefs.getString(PREF_PREFIX_KEY_ACCOUNT + appWidgetId, null);
    }

    static String loadStickyNoteWidgetNoteUIDPref(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        return prefs.getString(PREF_PREFIX_KEY_NOTE + appWidgetId, null);
    }

    static void deleteStickyNoteWidgetPref(Context context, int appWidgetId) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.remove(PREF_PREFIX_KEY_ACCOUNT + appWidgetId);
        prefs.remove(PREF_PREFIX_KEY_NOTE + appWidgetId);
        prefs.commit();
    }

    void initSpinners(){
        initAccountSpinner();
        updateNoteSpinner();
    }

    void initAccountSpinner(){
        Account[] accounts = mAccountManager.getAccountsByType(AuthenticatorActivity.ARG_ACCOUNT_TYPE);

        String[] accountNames = new String[accounts.length+1];

        accountNames[0] = localAccountName;
        for(int i=0; i< accounts.length;i++){
            accountNames[i+1] = mAccountManager.getUserData(accounts[i],AuthenticatorActivity.KEY_ACCOUNT_NAME);
        }

        Arrays.sort(accountNames, 1, accountNames.length);

        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(this,R.layout.widget_config_spinner_item,accountNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        accountSpinner.setAdapter(adapter);
        accountSpinner.setOnItemSelectedListener(new OnAccountItemClicked());
        accountSpinner.setSelection(0);
    }

    class OnAccountItemClicked implements AdapterView.OnItemSelectedListener{

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            String name = parent.getSelectedItem().toString();

            if(localAccountName.equalsIgnoreCase(name)){
                selectedAccount = null;
            }else{
                Account[] accounts = mAccountManager.getAccountsByType(AuthenticatorActivity.ARG_ACCOUNT_TYPE);

                for(Account account : accounts){
                    if(name.equals(mAccountManager.getUserData(account, AuthenticatorActivity.KEY_ACCOUNT_NAME))){
                        selectedAccount = account;
                        break;
                    }
                }
            }

            updateNoteSpinner();
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            //nothing
        }
    }

    void updateNoteSpinner(){
        String rootFolder;
        String email;
        if(selectedAccount == null){
            rootFolder = "Notes";
            email = "local";
        }else{
            rootFolder = mAccountManager.getUserData(selectedAccount,AuthenticatorActivity.KEY_ROOT_FOLDER);
            email = mAccountManager.getUserData(selectedAccount,AuthenticatorActivity.KEY_EMAIL);
        }

        List<Note> notes = new ArrayList<>(noteRepository.getAll(email, rootFolder, new NoteSorting(Utils.SortingColumns.summary, NoteSorting.Direction.ASC)));

        Collections.sort(notes);

        if(notes.size() > 0) {
            selectedNote = notes.get(0).getIdentification().getUid();
        }else{
            selectedNote = null;
        }

        NoteListAdapater adapter = new NoteListAdapater(this, R.layout.list_note_row, notes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        noteSpinner.setAdapter(adapter);
        noteSpinner.setSelection(0);
        noteSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedNote = ((Note)parent.getSelectedItem()).getIdentification().getUid();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //nothing
            }
        });
    }

}



