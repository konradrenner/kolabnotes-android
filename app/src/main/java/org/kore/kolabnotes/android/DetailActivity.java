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
import android.view.LayoutInflater;
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
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.kore.kolab.notes.Note;
import org.kore.kolab.notes.Notebook;
import org.kore.kolabnotes.android.content.ActiveAccount;
import org.kore.kolabnotes.android.content.ActiveAccountRepository;
import org.kore.kolabnotes.android.content.NoteRepository;
import org.kore.kolabnotes.android.content.NoteTagRepository;
import org.kore.kolabnotes.android.content.NotebookRepository;
import org.kore.kolabnotes.android.content.TagRepository;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class DetailActivity extends ActionBarActivity implements ShareActionProvider.OnShareTargetSelectedListener{

    public static final String NOTE_UID = "note_uid";
    public static final String NOTEBOOK_UID = "notebook_uid";

    private NotebookRepository notebookRepository = new NotebookRepository(this);
    private NoteRepository noteRepository = new NoteRepository(this);
    private NoteTagRepository noteTagRepository = new NoteTagRepository(this);
    private TagRepository tagRepository = new TagRepository(this);
    private ActiveAccountRepository activeAccountRepository = new ActiveAccountRepository(this);

    private Toolbar toolbar;

    private Note note = null;

    private ShareActionProvider shareActionProvider;

    private Intent shareIntent;

    private Note.Classification selectedClassification;

    private Set<String> selectedTags = new LinkedHashSet<>();

    private List<String> allTags = new ArrayList<>();

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

        allTags.addAll(tagRepository.getAll());

        Intent startIntent = getIntent();
        String uid = startIntent.getStringExtra(NOTE_UID);
        String notebook = startIntent.getStringExtra(NOTEBOOK_UID);
        if(uid != null){
            ActiveAccount activeAccount = activeAccountRepository.getActiveAccount();
            String notebookSummary = notebookRepository.getByUID(  activeAccount.getAccount(), activeAccount.getRootFolder(), notebook).getSummary();
            note = noteRepository.getByUID(activeAccount.getAccount(), activeAccount.getRootFolder(),uid);
            EditText summary = (EditText) findViewById(R.id.detail_summary);
            EditText description =(EditText) findViewById(R.id.detail_description);
            summary.setText(note.getSummary());
            description.setText(note.getDescription());

            selectedClassification = note.getClassification();
            selectedTags.addAll(note.getCategories());

            setSpinnerSelection(notebookSummary);
        }else if(notebook != null){
            ActiveAccount activeAccount = activeAccountRepository.getActiveAccount();
            String notebookSummary = notebookRepository.getByUID(activeAccount.getAccount(), activeAccount.getRootFolder(), notebook).getSummary();
            setSpinnerSelection(notebookSummary);
        }
    }

    void setSpinnerSelection(String notebookSummary){
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
            case R.id.change_classification:
                editClassification();
                break;
        }
        return true;
    }

    void editClassification(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(R.string.dialog_change_classification);

        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_classification, null);

        builder.setView(view);

        builder.setPositiveButton(R.string.ok, new OnClassificationChange(view));
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //nothing
            }
        });

        if(selectedClassification == null){
            ((RadioButton) view.findViewById(R.id.radio_public)).toggle();
        }else {

            switch (selectedClassification) {
                case PUBLIC:
                    ((RadioButton) view.findViewById(R.id.radio_public)).toggle();
                    break;
                case CONFIDENTIAL:
                    ((RadioButton) view.findViewById(R.id.radio_confidential)).toggle();
                    break;
                case PRIVATE:
                    ((RadioButton) view.findViewById(R.id.radio_private)).toggle();
                    break;
            }
        }

        builder.show();
    }

    class OnClassificationChange implements DialogInterface.OnClickListener {

        private final View view;

        public OnClassificationChange(View view){
            this.view = view;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            RadioGroup group = (RadioGroup) view.findViewById(R.id.dialog_classification);
            switch(group.getCheckedRadioButtonId()){
                case R.id.radio_public:
                    DetailActivity.this.selectedClassification = Note.Classification.PUBLIC;
                    break;
                case R.id.radio_confidential:
                    DetailActivity.this.selectedClassification = Note.Classification.CONFIDENTIAL;
                    break;
                case R.id.radio_private:
                    DetailActivity.this.selectedClassification = Note.Classification.PRIVATE;
                    break;
            }
        }
    }

    void editTags(){
        final ArrayList<Integer> selectedItems=new ArrayList<Integer> ();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.dialog_change_tags);

        final String[] tagArr = allTags.toArray(new String[allTags.size()]);
        final boolean[] selectionArr = new boolean[tagArr.length];

        for(int i=0;i<tagArr.length;i++){
            if(selectedTags.contains(tagArr[i])){
                selectionArr[i] = true;
            }
        }

        builder.setMultiChoiceItems(tagArr, selectionArr,
                new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int indexSelected,
                                        boolean isChecked) {
                        if (isChecked) {
                            // If the user checked the item, add it to the selected items
                            selectedItems.add(indexSelected);
                        } else if (selectedItems.contains(indexSelected)) {
                            // Else, if the item is already in the array, remove it
                            selectedItems.remove(Integer.valueOf(indexSelected));
                        }
                    }
                })
                // Set the action buttons
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        selectedTags.clear();
                        for (int i = 0; i < selectedItems.size(); i++) {
                            selectedTags.add(tagArr[selectedItems.get(i)]);
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // nothing

                    }
                });

        builder.show();
    }

    void saveNote(){
        EditText summary = (EditText) findViewById(R.id.detail_summary);
        EditText description =(EditText) findViewById(R.id.detail_description);

        String notebookName;
        if(note == null){
            final String uuid = UUID.randomUUID().toString();
            Note.Identification ident = new Note.Identification(uuid,"kolabnotes-android");
            Timestamp now = new Timestamp(System.currentTimeMillis());
            Note.AuditInformation audit = new Note.AuditInformation(now,now);

            note = new Note(ident,audit, selectedClassification == null ? Note.Classification.PUBLIC : selectedClassification, summary.getText().toString());
            note.setDescription(description.getText().toString());

            Spinner spinner = (Spinner) findViewById(R.id.spinner_notebook);
            notebookName = spinner.getSelectedItem().toString();

            Notebook book =  notebookRepository.getBySummary(  activeAccountRepository.getActiveAccount().getAccount(), activeAccountRepository.getActiveAccount().getRootFolder(),notebookName);

            noteRepository.insert(  activeAccountRepository.getActiveAccount().getAccount(), activeAccountRepository.getActiveAccount().getRootFolder(),note,book.getIdentification().getUid());
            noteTagRepository.delete(  activeAccountRepository.getActiveAccount().getAccount(), activeAccountRepository.getActiveAccount().getRootFolder(),uuid);
            for(String tag : selectedTags){
                noteTagRepository.insert(  activeAccountRepository.getActiveAccount().getAccount(), activeAccountRepository.getActiveAccount().getRootFolder(),uuid,tag);
            }
        }else{
            final String uuid = note.getIdentification().getUid();
            note.setSummary(summary.getText().toString());
            note.setDescription(description.getText().toString());
            note.setClassification(selectedClassification);

            Spinner spinner = (Spinner) findViewById(R.id.spinner_notebook);
            notebookName = spinner.getSelectedItem().toString();

            Notebook book =  notebookRepository.getBySummary(  activeAccountRepository.getActiveAccount().getAccount(), activeAccountRepository.getActiveAccount().getRootFolder(),notebookName);

            noteRepository.update(  activeAccountRepository.getActiveAccount().getAccount(), activeAccountRepository.getActiveAccount().getRootFolder(),note,book.getIdentification().getUid());

            noteTagRepository.delete(  activeAccountRepository.getActiveAccount().getAccount(), activeAccountRepository.getActiveAccount().getRootFolder(),uuid);
            for(String tag : selectedTags){
                noteTagRepository.insert(  activeAccountRepository.getActiveAccount().getAccount(), activeAccountRepository.getActiveAccount().getRootFolder(),uuid,tag);
            }
        }

        Intent returnIntent = new Intent();
        returnIntent.putExtra("selectedNotebookName",notebookName);
        setResult(RESULT_OK,returnIntent);

        finish();
    }

    void deleteNote(){
        if(note != null){
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            builder.setTitle(R.string.dialog_delete_note);
            builder.setMessage(R.string.dialog_question_delete);
            builder.setPositiveButton(R.string.yes,new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    DetailActivity.this.noteRepository.delete(  activeAccountRepository.getActiveAccount().getAccount(), activeAccountRepository.getActiveAccount().getRootFolder(),note);

                    Intent intent = new Intent(DetailActivity.this, MainPhoneActivity.class);

                    startActivity(intent);
                }
            });
            builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    //nothing
                }
            });
            builder.show();
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

        List<Notebook> notebooks = notebookRepository.getAll(  activeAccountRepository.getActiveAccount().getAccount(),  activeAccountRepository.getActiveAccount().getRootFolder());

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

    @Override
    public void onBackPressed() {
        Intent returnIntent = new Intent();
        setResult(RESULT_CANCELED,returnIntent);

        finish();
    }
}
