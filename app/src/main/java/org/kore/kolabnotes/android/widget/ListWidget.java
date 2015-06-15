package org.kore.kolabnotes.android.widget;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.view.View;
import android.widget.RemoteViews;

import org.apache.http.params.CoreConnectionPNames;
import org.kore.kolab.notes.Note;
import org.kore.kolab.notes.Tag;
import org.kore.kolabnotes.android.DetailActivity;
import org.kore.kolabnotes.android.MainPhoneActivity;
import org.kore.kolabnotes.android.R;
import org.kore.kolabnotes.android.content.NoteRepository;
import org.kore.kolabnotes.android.content.NotebookRepository;
import org.kore.kolabnotes.android.security.AuthenticatorActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in {@link ListWidgetConfigureActivity ListWidgetConfigureActivity}
 */
public class ListWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // update each of the app widgets with the remote adapter
        for (int i = 0; i < appWidgetIds.length; ++i) {
            updateAppWidget(context,appWidgetManager,appWidgetIds[i]);
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        // When the user deletes the widget, delete the preference associated with it.
        final int N = appWidgetIds.length;
        for (int i = 0; i < N; i++) {
            ListWidgetConfigureActivity.deleteListWidgetPref(context, appWidgetIds[i]);
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

        String account = ListWidgetConfigureActivity.loadListWidgetAccountPref(context, appWidgetId);
        String notebook = ListWidgetConfigureActivity.loadListWidgetNotebookPref(context, appWidgetId);
        String tag = ListWidgetConfigureActivity.loadListWidgetTagPref(context, appWidgetId);

        // Set up the intent that starts the StackViewService, which will
        // provide the views for this collection.
        Intent intent = new Intent(context, ListWidgetService.class);
        // Add the app widget ID to the intent extras.
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
        // Instantiate the RemoteViews object for the app widget layout.

        RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.list_widget);
        // Set up the RemoteViews object to use a RemoteViews adapter.
        // This adapter connects
        // to a RemoteViewsService  through the specified intent.
        // This is how you populate the data.
        rv.setRemoteAdapter(appWidgetId,R.id.widget_list_notes, intent);

        // The empty view is displayed when the collection has no items.
        // It should be in the same layout used to instantiate the RemoteViews
        // object above.
        //rv.setEmptyView(R.id.stack_view, R.id.empty_view);
        String correctAccountName = account;
        if("local".equals(account)){
            correctAccountName = context.getResources().getString(R.string.drawer_account_local);
        }
        rv.setTextViewText(R.id.widget_text, correctAccountName);

        Intent intentCreate = new Intent(context, DetailActivity.class);

        StringBuilder sb = new StringBuilder();
        if(!TextUtils.isEmpty(notebook)){
            sb.append(notebook);

            String rootFolder = "Notes";
            String email = "local";
            if(!account.equals("local")) {
                AccountManager accountManager = AccountManager.get(context);
                Account[] accounts = AccountManager.get(context).getAccountsByType(AuthenticatorActivity.ARG_ACCOUNT_TYPE);

                for (Account acc : accounts) {
                    if(account.equals(accountManager.getUserData(acc, AuthenticatorActivity.KEY_ACCOUNT_NAME))){
                        email = accountManager.getUserData(acc, AuthenticatorActivity.KEY_EMAIL);
                        rootFolder = accountManager.getUserData(acc, AuthenticatorActivity.KEY_ROOT_FOLDER);
                    }
                }
            }

            intent.putExtra(DetailActivity.NOTEBOOK_UID, new NotebookRepository(context).getBySummary(email,rootFolder,notebook));
        }
        PendingIntent pendingIntentCreate = PendingIntent.getActivity(context, appWidgetId, intentCreate,PendingIntent.FLAG_UPDATE_CURRENT);

        if(!TextUtils.isEmpty(notebook)){
            if(sb.length() > 0){
                sb.append(" / ");
            }
            sb.append(tag);
        }

        if(sb.length() > 0){
            rv.setTextViewText(R.id.widget_text_detail, sb.toString());
        }
        Intent intentMainActivity = new Intent(context, MainPhoneActivity.class);
        PendingIntent pendingIntentMainActivity = PendingIntent.getActivity(context, appWidgetId, intentMainActivity,PendingIntent.FLAG_UPDATE_CURRENT);


        rv.setOnClickPendingIntent(R.id.imageButton_icon,pendingIntentMainActivity);
        rv.setOnClickPendingIntent(R.id.imageButton_add,pendingIntentCreate);

        Intent clickIntent=new Intent(context, DetailActivity.class);
        PendingIntent clickPI=PendingIntent.getActivity(context, 0, clickIntent,PendingIntent.FLAG_UPDATE_CURRENT);
        rv.setPendingIntentTemplate(R.id.widget_list_notes, clickPI);
        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, rv);
    }
}


