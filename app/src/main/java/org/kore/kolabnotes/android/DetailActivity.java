package org.kore.kolabnotes.android;

import android.animation.Animator;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.support.v7.widget.ShareActionProvider;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.kore.kolab.notes.Note;
import org.kore.kolab.notes.Notebook;
import org.kore.kolabnotes.android.content.NoteRepository;
import org.kore.kolabnotes.android.content.NotebookRepository;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class DetailActivity extends ActionBarActivity implements ShareActionProvider.OnShareTargetSelectedListener{

    public static final String NOTE_UID = "note_uid";

    private NotebookRepository notebookRepository = new NotebookRepository(this);
    private NoteRepository noteRepository = new NoteRepository(this);

    private Toolbar toolbar;

    private Note note = null;

    private ShareActionProvider shareActionProvider;

    private Intent shareIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");

        // Handle Back Navigation :D
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DetailActivity.this.onBackPressed();
            }
        });

        initSpinner();

        Intent startIntent = getIntent();
        String uid = startIntent.getStringExtra(NOTE_UID);
        if(uid != null){
            String notebookSummary = noteRepository.getSummaryofNotebook(MainPhoneActivity.SELECTED_ACCOUNT,MainPhoneActivity.SELECTED_ROOT_FOLDER,uid);
            note = noteRepository.getByUID(MainPhoneActivity.SELECTED_ACCOUNT,MainPhoneActivity.SELECTED_ROOT_FOLDER,uid);
            EditText summary = (EditText) findViewById(R.id.detail_summary);
            EditText description =(EditText) findViewById(R.id.detail_description);
            summary.setText(note.getSummary());
            description.setText(note.getDescription());

            Spinner spinner = (Spinner) findViewById(R.id.spinner_notebook);
            SpinnerAdapter adapter = spinner.getAdapter();
            for(int i=0;i<adapter.getCount();i++){
                String nbsummary = adapter.getItem(i).toString();
                if(nbsummary.equals(notebookSummary)){
                    spinner.setSelection(i);
                    break;
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.detail_toolbar, menu);

        MenuItem item = menu.findItem(R.id.share);

        shareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(item);

        shareActionProvider.setShareIntent(shareIntent);
        shareActionProvider.setOnShareTargetSelectedListener(this);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.ok_menu:
                saveNote();
                break;
            case R.id.delete_menu:
                deleteNote();
                break;
            case R.id.edit_tag_menu:
                editTags();
                break;
        }
        return true;
    }

    void editTags(){
        //TODO
    }

    void saveNote(){
        EditText summary = (EditText) findViewById(R.id.detail_summary);
        EditText description =(EditText) findViewById(R.id.detail_description);

        if(note == null){
            Note.Identification ident = new Note.Identification(UUID.randomUUID().toString(),"kolabnotes-android");
            Timestamp now = new Timestamp(System.currentTimeMillis());
            Note.AuditInformation audit = new Note.AuditInformation(now,now);

            note = new Note(ident,audit, Note.Classification.PUBLIC, summary.getText().toString());
            note.setDescription(description.getText().toString());

            Spinner spinner = (Spinner) findViewById(R.id.spinner_notebook);
            String notebookName = spinner.getSelectedItem().toString();

            Notebook book =  notebookRepository.getBySummary(MainPhoneActivity.SELECTED_ACCOUNT,MainPhoneActivity.SELECTED_ROOT_FOLDER,notebookName);

            noteRepository.insert(MainPhoneActivity.SELECTED_ACCOUNT,MainPhoneActivity.SELECTED_ROOT_FOLDER,note,book.getIdentification().getUid());
        }else{
            note.setSummary(summary.getText().toString());
            note.setDescription(description.getText().toString());

            noteRepository.update(MainPhoneActivity.SELECTED_ACCOUNT,MainPhoneActivity.SELECTED_ROOT_FOLDER,note);
        }
    }

    void deleteNote(){
        if(note != null){
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            builder.setTitle(R.string.dialog_delete_note);
            builder.setMessage(R.string.dialog_question_delete);
            builder.setPositiveButton(R.string.yes,new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    DetailActivity.this.noteRepository.delete(MainPhoneActivity.SELECTED_ACCOUNT,MainPhoneActivity.SELECTED_ROOT_FOLDER,note);
                }
            });
            builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    //nothing
                }
            });
        }
    }

    @Override
    public boolean onShareTargetSelected(ShareActionProvider shareActionProvider, Intent intent) {
        if(note == null || note.getDescription() == null) {
            shareIntent.putExtra(Intent.EXTRA_TEXT, "");
        }else{
            shareIntent.putExtra(Intent.EXTRA_TEXT, note.getDescription());
        }
        return false;
    }

    void initSpinner(){
        Spinner spinner = (Spinner) findViewById(R.id.spinner_notebook);

        List<Notebook> notebooks = notebookRepository.getAll(MainPhoneActivity.SELECTED_ACCOUNT, MainPhoneActivity.SELECTED_ROOT_FOLDER);

        String[] notebookArr = new String[notebooks.size()];

        for(int i=0; i<notebooks.size();i++){
            notebookArr[i] = notebooks.get(i).getSummary();
        }

        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(this,android.R.layout.simple_spinner_item,notebookArr);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        //outState.putParcelable("appInfo", appInfo.getComponentName());
        super.onSaveInstanceState(outState);
    }

    View.OnClickListener fabClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
           //nothing at them moment
        }
    };

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(DetailActivity.this,MainPhoneActivity.class);

        startActivity(intent);
    }
}
