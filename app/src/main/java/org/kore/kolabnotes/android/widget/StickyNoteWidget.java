package org.kore.kolabnotes.android.widget;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Log;
import android.widget.RemoteViews;

import org.kore.kolab.notes.Color;
import org.kore.kolab.notes.Note;
import org.kore.kolabnotes.android.DetailActivity;
import org.kore.kolabnotes.android.R;
import org.kore.kolabnotes.android.Utils;
import org.kore.kolabnotes.android.content.NoteRepository;
import org.kore.kolabnotes.android.security.AuthenticatorActivity;

/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in {@link StickyNoteWidgetConfigureActivity StickyNoteWidgetConfigureActivity}
 */
public class StickyNoteWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        NoteRepository notesRepository = new NoteRepository(context);
        final int N = appWidgetIds.length;
        for (int i = 0; i < N; i++) {
            updateAppWidget(context, appWidgetManager, appWidgetIds[i],notesRepository);
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        // When the user deletes the widget, delete the preference associated with it.
        final int N = appWidgetIds.length;
        for (int i = 0; i < N; i++) {
            StickyNoteWidgetConfigureActivity.deleteStickyNoteWidgetPref(context, appWidgetIds[i]);
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
                                int appWidgetId, NoteRepository notesRepository) {

        String account = StickyNoteWidgetConfigureActivity.loadStickyNoteWidgetAccountPref(context, appWidgetId);
        String rootFolder = "Notes";
        String accountEmail = "local";
        if(account != null && !account.equals("local")) {
            AccountManager accountManager = AccountManager.get(context);
            Account[] accounts = accountManager.getAccountsByType(AuthenticatorActivity.ARG_ACCOUNT_TYPE);

            for (Account acc : accounts) {
                if(account.equals(accountManager.getUserData(acc, AuthenticatorActivity.KEY_ACCOUNT_NAME))){
                    accountEmail = accountManager.getUserData(acc, AuthenticatorActivity.KEY_EMAIL);
                    rootFolder = accountManager.getUserData(acc, AuthenticatorActivity.KEY_ROOT_FOLDER);
                }
            }
        }

        String noteUID = StickyNoteWidgetConfigureActivity.loadStickyNoteWidgetNoteUIDPref(context, appWidgetId);

        Note note = notesRepository.getByUID(accountEmail, rootFolder, noteUID);

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.sticky_note_widget);
        if(note == null){
            views.setTextViewText(R.id.sticky_note_summary, context.getResources().getString(R.string.note_not_found));

            views.setTextViewText(R.id.sticky_note_description, "");
        }else {

            // Construct the RemoteViews object
            String uidofNotebook = notesRepository.getUIDofNotebook(accountEmail, rootFolder, noteUID);
            Intent intentMainActivity = new Intent(context, DetailActivity.class);
            intentMainActivity.putExtra(Utils.NOTE_UID, noteUID);
            intentMainActivity.putExtra(Utils.NOTEBOOK_UID, uidofNotebook);
            intentMainActivity.putExtra(Utils.INTENT_ACCOUNT_EMAIL, accountEmail);
            intentMainActivity.putExtra(Utils.INTENT_ACCOUNT_ROOT_FOLDER, rootFolder);
            PendingIntent pendingIntentMainActivity = PendingIntent.getActivity(context, appWidgetId, intentMainActivity, PendingIntent.FLAG_UPDATE_CURRENT);

            Log.d("updateAppWidget", "uiDofNotebook:" + uidofNotebook);

            views.setOnClickPendingIntent(R.id.sticky_note_summary, pendingIntentMainActivity);

            views.setTextViewText(R.id.sticky_note_summary, note.getSummary());

            Spanned fromHtml;
            if(TextUtils.isEmpty(note.getDescription())){
                fromHtml = new SpannableString("");
            }else{
                fromHtml = Html.fromHtml(note.getDescription());
            }

            views.setTextViewText(R.id.sticky_note_description, fromHtml);

            Color noteColor = note.getColor();

            if (noteColor != null) {
                int color = android.graphics.Color.parseColor(noteColor.getHexcode());
                views.setInt(R.id.sticky_note_summary, "setBackgroundColor", color);
                views.setInt(R.id.sticky_note_description, "setBackgroundColor", color);
            }
        }

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}


