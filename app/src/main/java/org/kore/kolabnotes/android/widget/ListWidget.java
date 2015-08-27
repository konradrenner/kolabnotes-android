package org.kore.kolabnotes.android.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.widget.RemoteViews;

import org.kore.kolab.notes.Notebook;
import org.kore.kolabnotes.android.DetailActivity;
import org.kore.kolabnotes.android.MainActivity;
import org.kore.kolabnotes.android.R;
import org.kore.kolabnotes.android.Utils;
import org.kore.kolabnotes.android.content.AccountIdentifier;
import org.kore.kolabnotes.android.content.NotebookRepository;


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
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_list_notes);
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
        Intent intentMainActivity = new Intent(context, MainActivity.class);

        AccountIdentifier accId = Utils.getAccountIdentifierWithName(context, account);

        StringBuilder sb = new StringBuilder();

        if(!TextUtils.isEmpty(notebook)){
            sb.append(notebook);

            Notebook bySummary = new NotebookRepository(context).getBySummary(accId.getAccount(), accId.getRootFolder(), notebook);

            intent.putExtra(Utils.NOTEBOOK_UID, bySummary.getIdentification().getUid());
            intentCreate.putExtra(Utils.NOTEBOOK_UID, bySummary.getIdentification().getUid());
            intentMainActivity.putExtra(Utils.SELECTED_NOTEBOOK_NAME, notebook);
        }

        intentCreate.putExtra(Utils.INTENT_ACCOUNT_EMAIL, accId.getAccount());
        intentCreate.putExtra(Utils.INTENT_ACCOUNT_ROOT_FOLDER, accId.getRootFolder());
        PendingIntent pendingIntentCreate = PendingIntent.getActivity(context, appWidgetId, intentCreate,PendingIntent.FLAG_UPDATE_CURRENT);

        if(!TextUtils.isEmpty(tag)){
            if(sb.length() > 0){
                sb.append(" / ");
            }
            sb.append(tag);

            intentMainActivity.putExtra(Utils.SELECTED_TAG_NAME, tag);
        }

        if(sb.length() > 0){
            rv.setTextViewText(R.id.widget_text_detail, sb.toString());
        }

        intentMainActivity.putExtra(Utils.INTENT_ACCOUNT_EMAIL, accId.getAccount());
        intentMainActivity.putExtra(Utils.INTENT_ACCOUNT_ROOT_FOLDER, accId.getRootFolder());
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


