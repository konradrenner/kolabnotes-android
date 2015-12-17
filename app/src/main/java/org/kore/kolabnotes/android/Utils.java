package org.kore.kolabnotes.android;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.TargetApi;
import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Outline;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;

import org.kore.kolab.notes.Color;
import org.kore.kolab.notes.Note;
import org.kore.kolab.notes.Tag;
import org.kore.kolabnotes.android.content.AccountIdentifier;
import org.kore.kolabnotes.android.content.NoteSorting;
import org.kore.kolabnotes.android.security.AuthenticatorActivity;
import org.kore.kolabnotes.android.widget.ListWidget;
import org.kore.kolabnotes.android.widget.StickyNoteWidget;

import java.sql.Timestamp;
import java.util.ArrayList;

public class Utils {

    private static final int CYAN = 210;
    private static final int RED = 0;
    private static final int RED_ORANGE = 20;
    private static final int HUE = 0;
    private static final int SATURATION = 1;
    private static final int BRIGHTNESS = 2;

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

                if("classification".equalsIgnoreCase(values[i].toString())){
                    continue;
                }

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

    public static void saveLastSyncTime(Context context,String accountName) {
        SharedPreferences.Editor prefs = context.getSharedPreferences("org.kore.kolabnotes.android.async.KolabSyncAdapter", 0).edit();
        prefs.putLong("lastSyncTst_"+accountName, System.currentTimeMillis());
        prefs.commit();
    }

    public static Timestamp getLastSyncTime(Context context,String accountName) {
        SharedPreferences prefs = context.getSharedPreferences("org.kore.kolabnotes.android.async.KolabSyncAdapter", 0);
        if(prefs == null){
            Log.d("getLastSyncTime","KolabSyncAdapter prefs are null");
            return null;
        }
        long millis = prefs.getLong("lastSyncTst_"+accountName, -1);
        if(millis < 0){
            return null;
        }

        return new Timestamp(millis);
    }

    public static boolean getShowMetainformation(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if(prefs == null){
            Log.d("getShowMetainformation","PreferenceManager prefs are null");
            return true;
        }
        return prefs.getBoolean("pref_metainformation", true);

    }

    public static boolean getShowSyncNotifications(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if(prefs == null){
            Log.d("getShowSyncNotification","PreferenceManager prefs are null");
            return true;
        }
        return prefs.getBoolean("pref_show_sync_notifications", true);

    }

    public static boolean getShowCharacteristics(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if(prefs == null){
            Log.d("getNoteSorting","MainActivity prefs are null");
            return true;
        }
        return prefs.getBoolean("pref_characteristics", true);

    }

    public static boolean clearConflictWithLatest(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if(prefs == null){
            Log.d("latest","PreferenceManager prefs are null");
            return true;
        }
        return "LATEST".equalsIgnoreCase(prefs.getString("sync_conflict", "LATEST"));
    }

    public static boolean clearConflictWithServer(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if(prefs == null){
            Log.d("latest","PreferenceManager prefs are null");
            return true;
        }
        return "SERVER".equalsIgnoreCase(prefs.getString("sync_conflict","LATEST"));
    }

    public static boolean clearConflictWithLocal(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if(prefs == null){
            Log.d("latest","PreferenceManager prefs are null");
            return true;
        }
        return "LOCAL".equalsIgnoreCase(prefs.getString("sync_conflict","LATEST"));
    }

    public static NoteSorting getNoteSorting(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if(prefs == null){
            Log.d("getNoteSorting","MainActivity prefs are null");
            return new NoteSorting();
        }
        String direction = prefs.getString("pref_direction", null);
        String column = prefs.getString("pref_column", null);

        if(TextUtils.isEmpty(direction) || TextUtils.isEmpty(column)){
            Log.d("getNoteSorting","column:"+column+"; or direction:"+direction+"; is empty, so default ordering will be returned");
            return new NoteSorting();
        }

        return new NoteSorting(column.toLowerCase(), NoteSorting.Direction.valueOf(direction));
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

    public static void setToolbarTextAndIconColor(final Activity activity, final Toolbar toolbar, final boolean lightText){

        setOverflowButtonColor(activity,lightText);
        if(lightText){
            toolbar.setTitleTextColor(android.graphics.Color.WHITE);

            toolbar.getNavigationIcon().clearColorFilter();

            for(int i=0; i< toolbar.getMenu().size(); i++){
                final MenuItem item = toolbar.getMenu().getItem(i);
                if(item.getIcon() != null) {
                    final Drawable drawable = item.getIcon().mutate();
                    drawable.clearColorFilter();
                    item.setIcon(drawable);
                }
            }
        }else{
            //To generate negative image
            float[] colorMatrix_Negative = {
                    -1.0f, 0, 0, 0, 255, //red
                    0, -1.0f, 0, 0, 255, //green
                    0, 0, -1.0f, 0, 255, //blue
                    0, 0, 0, 1.0f, 0 //alpha
            };

            ColorFilter colorFilter_Negative = new ColorMatrixColorFilter(colorMatrix_Negative);

            toolbar.setTitleTextColor(android.graphics.Color.BLACK);

            final Drawable navIcon = toolbar.getNavigationIcon().mutate();
            navIcon.setColorFilter(colorFilter_Negative);
            toolbar.setNavigationIcon(navIcon);

            for(int i=0; i< toolbar.getMenu().size(); i++){
                final MenuItem item = toolbar.getMenu().getItem(i);
                if(item.getIcon() != null) {
                    final Drawable drawable = item.getIcon().mutate();
                    drawable.clearColorFilter();
                    drawable.setColorFilter(colorFilter_Negative);
                    item.setIcon(drawable);
                }
            }
        }
    }

    public static void setOverflowButtonColor(final Activity activity, final boolean lightColor){
        final String overflowDescription = activity.getString(R.string.abc_action_menu_overflow_description);
        final ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
        final ViewTreeObserver viewTreeObserver = decorView.getViewTreeObserver();

        viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                final ArrayList<View> outViews = new ArrayList<View>();
                decorView.findViewsWithText(outViews, overflowDescription, View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION);

                if(outViews.isEmpty()){
                    return;
                }

                ImageView overflow = (ImageView)outViews.get(0);
                overflow.setColorFilter(lightColor ? android.graphics.Color.WHITE : android.graphics.Color.BLACK);
                decorView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
    }

    public static boolean useLightTextColor(Context context, Color colorOfNote){
        if(colorOfNote == null){
            return false;
        }

        float[] HSV = new float[3];
        android.graphics.Color.colorToHSV(android.graphics.Color.parseColor(colorOfNote.getHexcode()), HSV);

        if (((HSV[HUE] >= CYAN || (HSV[HUE] >= RED && HSV[HUE] <= RED_ORANGE)) && HSV[SATURATION] >= 0.5) || HSV[BRIGHTNESS] <= 0.8) {
            return true;
        }
        return false;
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
        if(!equals(one.getClassification(), two.getClassification())){
            return true;
        }
        if(!equals(one.getColor(), two.getColor())){
            return true;
        }
        if(!equals(one.getSummary(), two.getSummary())){
            return true;
        }
        if(one.getCategories().size() != two.getCategories().size() || !one.getCategories().containsAll(two.getCategories())){
            return true;
        }

        return false;
    }

    public static boolean equals(Object o1, Object o2){
        if(o1 == null){
            return o2 == null;
        }

        return o1.equals(o2);
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
