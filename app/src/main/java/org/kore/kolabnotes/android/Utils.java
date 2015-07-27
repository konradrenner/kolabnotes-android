package org.kore.kolabnotes.android;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.TargetApi;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Outline;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;

import org.kore.kolab.notes.Note;
import org.kore.kolab.notes.NotesRepository;
import org.kore.kolab.notes.Tag;
import org.kore.kolabnotes.android.content.AccountIdentifier;
import org.kore.kolabnotes.android.content.DatabaseHelper;
import org.kore.kolabnotes.android.content.NoteSorting;
import org.kore.kolabnotes.android.security.AuthenticatorActivity;
import org.kore.kolabnotes.android.widget.ListWidget;
import org.kore.kolabnotes.android.widget.StickyNoteWidget;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;

public class Utils {

    public enum SortingColumns{
        summary {
            @Override
            public int compare(Note note1, Note note2, NoteSorting.Direction direction) {
                int sorting = 0;
                if(direction == NoteSorting.Direction.ASC){
                    sorting = note1.getSummary().toLowerCase().compareTo(note2.getSummary().toLowerCase());

                    if(sorting == 0){
                        sorting = note1.getAuditInformation().compareTo(note2.getAuditInformation());
                    }
                }else{
                    sorting = note2.getSummary().toLowerCase().compareTo(note1.getSummary().toLowerCase());

                    if(sorting == 0){
                        sorting = note2.getAuditInformation().compareTo(note1.getAuditInformation());
                    }
                }
                return sorting;
            }
        },lastModificationDate {
            @Override
            public int compare(Note note1, Note note2, NoteSorting.Direction direction) {
                int sorting = 0;
                if(direction == NoteSorting.Direction.ASC){
                    sorting = note1.getAuditInformation().getLastModificationDate().compareTo(note2.getAuditInformation().getLastModificationDate());

                    if(sorting == 0){
                        sorting = note1.getAuditInformation().compareTo(note2.getAuditInformation());
                    }
                }else{
                    sorting = note2.getAuditInformation().getLastModificationDate().compareTo(note1.getAuditInformation().getLastModificationDate());

                    if(sorting == 0){
                        sorting = note2.getAuditInformation().compareTo(note1.getAuditInformation());
                    }
                }
                return sorting;
            }
        },creationDate {
            @Override
            public int compare(Note note1, Note note2, NoteSorting.Direction direction) {
                int sorting = 0;
                if(direction == NoteSorting.Direction.ASC){
                    sorting = note1.getAuditInformation().getCreationDate().compareTo(note2.getAuditInformation().getCreationDate());

                    if(sorting == 0){
                        sorting = note1.getAuditInformation().compareTo(note2.getAuditInformation());
                    }
                }else{
                    sorting = note2.getAuditInformation().getCreationDate().compareTo(note1.getAuditInformation().getCreationDate());

                    if(sorting == 0){
                        sorting = note2.getAuditInformation().compareTo(note1.getAuditInformation());
                    }
                }
                return sorting;
            }
        },classification {
            @Override
            public int compare(Note note1, Note note2, NoteSorting.Direction direction) {
                int sorting = 0;
                if(direction == NoteSorting.Direction.ASC){
                    sorting = note1.getClassification().compareTo(note2.getClassification());

                    if(sorting == 0){
                        sorting = note1.getAuditInformation().compareTo(note2.getAuditInformation());
                    }
                }else{
                    sorting = note2.getClassification().compareTo(note1.getClassification());

                    if(sorting == 0){
                        sorting = note2.getAuditInformation().compareTo(note1.getAuditInformation());
                    }
                }
                return sorting;
            }
        },color {
            @Override
            public int compare(Note note1, Note note2, NoteSorting.Direction direction) {
                int sorting = 0;
                String note1Color = note1.getColor() == null ? "" : note1.getColor().getHexcode();
                String note2Color = note2.getColor() == null ? "" : note2.getColor().getHexcode();
                if(direction == NoteSorting.Direction.ASC){
                    sorting = note1Color.compareTo(note2Color);

                    if(sorting == 0){
                        sorting = note1.getAuditInformation().compareTo(note2.getAuditInformation());
                    }
                }else{
                    sorting = note2Color.compareTo(note1Color);

                    if(sorting == 0){
                        sorting = note2.getAuditInformation().compareTo(note1.getAuditInformation());
                    }
                }
                return sorting;
            }
        };

        public abstract int compare(Note note1, Note note2, NoteSorting.Direction direction);

        public static String[] valuesToStringArray(){
            final SortingColumns[] values = values();
            String[] arr = new String[values.length];

            for(int i=0;i<values.length;i++){
                arr[i] = values[i].toString();
            }
            return arr;
        }
    }

    public static final String INTENT_ACCOUNT_EMAIL = "intent_account_email";
    public static final String INTENT_ACCOUNT_ROOT_FOLDER = "intent_account_rootfolder";
    public static final String NOTE_UID = "note_uid";
    public static final String NOTEBOOK_UID = "notebook_uid";
    public static final String SELECTED_NOTEBOOK_NAME = "selectedNotebookName";
    public static final String SELECTED_TAG_NAME = "selectedNotebookTag";
    public static final String RELOAD_DATA_AFTER_DETAIL = "reloadDataAfterDetail";
    public static final int READ_REQUEST_CODE = 42;
    /*
    public static void configureWindowEnterExitTransition(Window w) {
        Explode ex = new Explode();
        ex.setInterpolator(new PathInterpolator(0.4f, 0, 1, 1));
        w.setExitTransition(ex);
        w.setEnterTransition(ex);
    }
    */

    public static void saveNoteSorting(Context context, NoteSorting noteSorting) {
        SharedPreferences.Editor prefs = context.getSharedPreferences("org.kore.kolabnotes.android.widget.MainActivity", 0).edit();
        prefs.putString("direction", noteSorting.getDirection().toString());
        prefs.putString("column", noteSorting.getColumnName());
        prefs.commit();
    }

    public static NoteSorting getNoteSorting(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("org.kore.kolabnotes.android.widget.MainActivity", 0);
        if(prefs == null){
            Log.d("getNoteSorting","MainActivity prefs are null");
            return new NoteSorting();
        }
        String direction = prefs.getString("direction", null);
        String column = prefs.getString("column", null);

        if(TextUtils.isEmpty(direction) || TextUtils.isEmpty(column)){
            Log.d("getNoteSorting","column:"+column+"; or direction:"+direction+"; is empty, so default ordering will be returned");
            return new NoteSorting();
        }

        return new NoteSorting(column, NoteSorting.Direction.valueOf(direction));
    }

    public static boolean getReloadDataAfterDetail(Context context){
        SharedPreferences sharedPref = context.getSharedPreferences("org.kore.kolabnotes.android.pref",Context.MODE_PRIVATE);
        return sharedPref.getBoolean(Utils.RELOAD_DATA_AFTER_DETAIL,false);
    }

    public static void setReloadDataAfterDetail(Context context, boolean value){
        SharedPreferences sharedPref = context.getSharedPreferences("org.kore.kolabnotes.android.pref",Context.MODE_PRIVATE);
        if(value){
            sharedPref.edit().putBoolean(Utils.RELOAD_DATA_AFTER_DETAIL,value).commit();
        }else{
            sharedPref.edit().remove(Utils.RELOAD_DATA_AFTER_DETAIL).commit();
        }
    }


    public static String getSelectedTagName(Context context){
        SharedPreferences sharedPref = context.getSharedPreferences("org.kore.kolabnotes.android.pref",Context.MODE_PRIVATE);
        return sharedPref.getString(Utils.SELECTED_TAG_NAME,null);
    }

    public static void setSelectedTagName(Context context, String name){
        SharedPreferences sharedPref = context.getSharedPreferences("org.kore.kolabnotes.android.pref",Context.MODE_PRIVATE);
        if(name == null){
            sharedPref.edit().remove(Utils.SELECTED_TAG_NAME).commit();
        }else{
            sharedPref.edit().putString(Utils.SELECTED_TAG_NAME,name).commit();
        }
    }

    public static String getSelectedNotebookName(Context context){
        SharedPreferences sharedPref = context.getSharedPreferences("org.kore.kolabnotes.android.pref",Context.MODE_PRIVATE);
        return sharedPref.getString(Utils.SELECTED_NOTEBOOK_NAME,null);
    }

    public static void setSelectedNotebookName(Context context, String name){
        SharedPreferences sharedPref = context.getSharedPreferences("org.kore.kolabnotes.android.pref", Context.MODE_PRIVATE);
        if(name == null){
            sharedPref.edit().remove(Utils.SELECTED_NOTEBOOK_NAME).commit();
        }else{
            sharedPref.edit().putString(Utils.SELECTED_NOTEBOOK_NAME,name).commit();
        }
    }

    public static void configureFab(View fabButton) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            fabButton.setOutlineProvider(new ViewOutlineProvider() {
                @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                @Override
                public void getOutline(View view, Outline outline) {
                    int fabSize = view.getContext().getResources().getDimensionPixelSize(R.dimen.fab_size);
                    outline.setOval(0, 0, fabSize, fabSize);
                }
            });
        } else {
            ((ImageButton) fabButton).setScaleType(ImageView.ScaleType.FIT_CENTER);
        }
    }

    public static void setElevation(View view, float elevation){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            view.setElevation(elevation);
        }
    }
    
    public static final String getNameOfActiveAccount(Context context, String pemail, String prootFolder){
        AccountManager accountManager = AccountManager.get(context);
        Account[] accounts = accountManager.getAccountsByType(AuthenticatorActivity.ARG_ACCOUNT_TYPE);

        for(int i=0;i<accounts.length;i++) {
            String email = accountManager.getUserData(accounts[i],AuthenticatorActivity.KEY_EMAIL);
            String name = accountManager.getUserData(accounts[i],AuthenticatorActivity.KEY_ACCOUNT_NAME);
            String rootFolder = accountManager.getUserData(accounts[i],AuthenticatorActivity.KEY_ROOT_FOLDER);

            if(pemail.equals(email) && prootFolder.equals(rootFolder)){
                return name;
            }
        }

        return context.getResources().getString(R.string.drawer_account_local);
    }

    public static final AccountIdentifier getAccountIdentifierWithName(Context context, String account){
        String rootFolder = "Notes";
        String email = "local";
        if(account != null && !account.equals("local")) {
            AccountManager accountManager = AccountManager.get(context);
            Account[] accounts = AccountManager.get(context).getAccountsByType(AuthenticatorActivity.ARG_ACCOUNT_TYPE);

            for (Account acc : accounts) {
                if(account.equals(accountManager.getUserData(acc, AuthenticatorActivity.KEY_ACCOUNT_NAME))){
                    email = accountManager.getUserData(acc, AuthenticatorActivity.KEY_EMAIL);
                    rootFolder = accountManager.getUserData(acc, AuthenticatorActivity.KEY_ROOT_FOLDER);
                }
            }
        }

        return new AccountIdentifier(email,rootFolder);
    }

    /**
     * Creates a exact copy of an note
     *
     * @param source
     * @return Note
     */
    public static final Note copy(Note source){
        Note note = new Note(source.getIdentification(),source.getAuditInformation(),source.getClassification(),source.getSummary());
        note.setDescription(source.getDescription());
        note.setColor(source.getColor());
        note.setAttachment(source.getAttachment());
        note.addCategories(source.getCategories().toArray(new Tag[source.getCategories().size()]));

        return note;
    }

    public static void updateWidgetsForChange(Context context){
        Class<StickyNoteWidget> stickyNoteWidgetClass = StickyNoteWidget.class;
        Class<ListWidget> listWidgetClass = ListWidget.class;
        Intent stickyIntent = new Intent(context,stickyNoteWidgetClass);
        Intent listIntent = new Intent(context, listWidgetClass);

        int[] stickyIds = AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, stickyNoteWidgetClass));
        int[] listIds = AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, listWidgetClass));

        stickyIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        stickyIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS,stickyIds);

        listIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        listIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS,listIds);

        context.sendBroadcast(stickyIntent);
        context.sendBroadcast(listIntent);
    }

    public static final boolean differentMutableData(Note one, Note two){
        if(!Objects.equals(one.getClassification(),two.getClassification())){
            return true;
        }
        if(!Objects.equals(one.getColor(),two.getColor())){
            return true;
        }
        if(!Objects.equals(one.getAttachment(),two.getAttachment())){
            return true;
        }
        if(!Objects.equals(one.getSummary(),two.getSummary())){
            return true;
        }
        if(one.getCategories().size() != two.getCategories().size() || !one.getCategories().containsAll(two.getCategories())){
            return true;
        }

        return false;
    }

    public static void initColumnSpinner(Context context, Spinner spinner, int spinnerLayout, AdapterView.OnItemSelectedListener listener){
        initColumnSpinner(context,spinner, spinnerLayout,listener,null);
    }


    public static void initColumnSpinner(Context context, Spinner spinner, int spinnerLayout, AdapterView.OnItemSelectedListener listener, String selection){
        int select = 1;
        if(selection != null){
            final String[] strings = SortingColumns.valuesToStringArray();
            for(int i=0;i<strings.length;i++){
                if(strings[i].trim().equalsIgnoreCase(selection.trim())){
                    select = i;
                    break;
                }
            }
        }

        final String[] columnNames = context.getResources().getStringArray(R.array.sorting_columns);
        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(context,spinnerLayout,columnNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(listener);
        spinner.setSelection(select);
    }

    public static String getColumnNameOfSelection(int selection){
        final SortingColumns[] values = SortingColumns.values();
        return values[selection].toString();
    }

    public static boolean isTablet(Resources res){
            return (res.getConfiguration().screenLayout
                    & Configuration.SCREENLAYOUT_SIZE_MASK)
                    >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }
}
