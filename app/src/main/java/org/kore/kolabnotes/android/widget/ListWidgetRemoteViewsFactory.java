package org.kore.kolabnotes.android.widget;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Application;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import org.kore.kolab.notes.Note;
import org.kore.kolab.notes.Tag;
import org.kore.kolabnotes.android.DetailActivity;
import org.kore.kolabnotes.android.R;
import org.kore.kolabnotes.android.content.ActiveAccount;
import org.kore.kolabnotes.android.content.NoteRepository;
import org.kore.kolabnotes.android.content.NotebookRepository;
import org.kore.kolabnotes.android.content.TagRepository;
import org.kore.kolabnotes.android.security.AuthenticatorActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by koni on 25.05.15.
 */
public class ListWidgetRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {

    private final Application app;
    private int appWidgetId;
    private List<Note> notes;
    private String rootFolder;
    private String accountEmail;
    private String notebookUID;
    private NoteRepository notesRepository;

    public ListWidgetRemoteViewsFactory(Application app, Intent intent) {
        this.app = app;
        appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        notes = new ArrayList<>();
    }

    @Override
    public void onCreate() {
        refreshData();
    }

    @Override
    public void onDataSetChanged() {
        refreshData();
    }

    void refreshData(){
        notes.clear();
        Context context = app.getApplicationContext();
        notesRepository = new NoteRepository(context);
        NotebookRepository notebookRepository = new NotebookRepository(context);

        String account = ListWidgetConfigureActivity.loadListWidgetAccountPref(context, appWidgetId);
        String notebook = ListWidgetConfigureActivity.loadListWidgetNotebookPref(context, appWidgetId);
        String tag = ListWidgetConfigureActivity.loadListWidgetTagPref(context, appWidgetId);

        rootFolder = "Notes";
        accountEmail = "local";
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

        List<Note> notes;
        if(notebook == null){
            notes = notesRepository.getAll(accountEmail,rootFolder);
        }else{
            notebookUID = notebookRepository.getBySummary(accountEmail,rootFolder,notebook).getIdentification().getUid();
            notes = notesRepository.getFromNotebook(accountEmail,rootFolder, notebookUID);
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

        this.notes.addAll(filtered);
    }

    @Override
    public void onDestroy() {
        ListWidgetConfigureActivity.deleteListWidgetPref(app.getApplicationContext(),appWidgetId);
    }

    @Override
    public int getCount() {
        return notes.size();
    }

    @Override
    public RemoteViews getViewAt(int position) {
        RemoteViews row = new RemoteViews(app.getPackageName(), R.layout.list_widget_row);

        Note note = notes.get(position);

        row.setTextViewText(R.id.list_widget_row_summary, note.getSummary());

        Intent i = new Intent();
        i.putExtra(DetailActivity.NOTE_UID, note.getIdentification().getUid());
        String activeAccount = ListWidgetConfigureActivity.loadListWidgetAccountPref(app.getApplicationContext(),appWidgetId);

        String correctNotebookUID;
        if(notebookUID == null){
            correctNotebookUID = notesRepository.getUIDofNotebook(accountEmail,rootFolder,note.getIdentification().getUid());
        }else{
            correctNotebookUID = notebookUID;
        }

        i.putExtra(DetailActivity.NOTEBOOK_UID, correctNotebookUID);

        row.setOnClickFillInIntent(R.id.list_widget_row_summary, i);

        return row;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }
}
