package org.kore.kolabnotes.android;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.widget.RemoteViews;

import org.kore.kolab.notes.Note;
import org.kore.kolab.notes.Tag;
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
        // There may be multiple widgets active, so update all of them
        NoteRepository notesRepository = new NoteRepository(context);
        NotebookRepository notebookRepository = new NotebookRepository(context);

        final int N = appWidgetIds.length;
        for (int i = 0; i < N; i++) {
            updateAppWidget(context, appWidgetManager, appWidgetIds[i], notesRepository,notebookRepository);
        }
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
                                int appWidgetId, NoteRepository notesRepository, NotebookRepository notebookRepository) {

        String account = ListWidgetConfigureActivity.loadListWidgetAccountPref(context, appWidgetId);
        String notebook = ListWidgetConfigureActivity.loadListWidgetNotebookPref(context, appWidgetId);
        String tag = ListWidgetConfigureActivity.loadListWidgetTagPref(context, appWidgetId);
        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.list_widget);
        views.setTextViewText(R.id.widget_text, account);

        String rootFolder = "Notes";
        String email = "local";
        if(account != null) {
            AccountManager accountManager = AccountManager.get(context);
            Account[] accounts = accountManager.getAccountsByType(AuthenticatorActivity.ARG_ACCOUNT_TYPE);

            for (Account acc : accounts) {
                if(account.equals(accountManager.getUserData(acc, AuthenticatorActivity.KEY_ACCOUNT_NAME))){
                    email = accountManager.getUserData(acc, AuthenticatorActivity.KEY_EMAIL);
                    rootFolder = accountManager.getUserData(acc, AuthenticatorActivity.KEY_ROOT_FOLDER);
                }
            }
        }

        List<Note> notes;
        if(notebook == null){
            notes = notesRepository.getAll(email,rootFolder);
        }else{
            notes = notesRepository.getFromNotebook(email,rootFolder, notebookRepository.getBySummary(email,rootFolder,notebook).getIdentification().getUid());
        }

        ArrayList<Note> filtered = new ArrayList<>();
        if(tag != null){
            Tag tagObject = new Tag(tag);
            for(Note note : notes){
                if(note.getCategories().contains(tagObject)){
                    filtered.add(note);
                }
            }
        }else{
            filtered.addAll(notes);
        }

        Collections.sort(filtered);

        //TODO set listview data

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}


