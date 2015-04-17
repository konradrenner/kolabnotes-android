package org.kore.kolabnotes.android;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.util.Pair;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;

import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.accountswitcher.AccountHeader;
import com.mikepenz.materialdrawer.model.BaseDrawerItem;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IProfile;

import org.kore.kolab.notes.Note;
import org.kore.kolab.notes.Notebook;
import org.kore.kolabnotes.android.adapter.NoteAdapter;
import org.kore.kolabnotes.android.content.NoteRepository;
import org.kore.kolabnotes.android.content.NoteTagRepository;
import org.kore.kolabnotes.android.content.NotebookRepository;
import org.kore.kolabnotes.android.content.TagRepository;
import org.kore.kolabnotes.android.itemanimator.CustomItemAnimator;
import org.kore.kolabnotes.android.itemanimator.ReboundItemAnimator;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class MainPhoneActivity extends ActionBarActivity {

    public static String SELECTED_ACCOUNT = "local";
    public static String SELECTED_ROOT_FOLDER = "Notes";

    private final DrawerItemClickedListener drawerItemClickedListener = new DrawerItemClickedListener();

    private List<Note> notesList = new ArrayList<Note>();

    private NoteAdapter mAdapter;
    private ImageButton mFabButton;
    private RecyclerView mRecyclerView;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private Drawer.Result mDrawer;

    private NoteRepository notesRepository = new NoteRepository(this);
    private NotebookRepository notebookRepository = new NotebookRepository(this);
    private TagRepository tagRepository = new TagRepository(this);
    private NoteTagRepository notetagRepository = new NoteTagRepository(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //supportRequestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_phone);

        // Set explode animation when enter and exit the activity
        //Utils.configureWindowEnterExitTransition(getWindow());

        // Handle Toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        AccountHeader.Result headerResult = new AccountHeader()
                .withActivity(this)
                .withHeaderBackground(R.drawable.drawer_header_background)
                .addProfiles(
                        new ProfileDrawerItem().withName(getResources().getString(R.string.drawer_account_local))
                )
                .withOnAccountHeaderListener(new AccountHeader.OnAccountHeaderListener() {
                    @Override
                    public boolean onProfileChanged(View view, IProfile profile, boolean currentProfile) {
                        boolean changed;
                        if(profile.getEmail() == null || profile.getEmail().trim().length() == 0){
                            changed = !SELECTED_ACCOUNT.equalsIgnoreCase("local");
                            SELECTED_ACCOUNT = "local";
                        }else{
                            changed = !SELECTED_ACCOUNT.equalsIgnoreCase(profile.getEmail());
                            SELECTED_ACCOUNT = profile.getEmail();
                        }
                        return changed;
                    }
                })
                .build();

        mDrawer = new Drawer()
                .withActivity(this)
                .withToolbar(toolbar)
                .withAccountHeader(headerResult)
                .addDrawerItems(
                        new PrimaryDrawerItem().withName(getResources().getString(R.string.drawer_item_allaccount_notes)).withTag("ALL_NOTES").withIcon(R.drawable.ic_action_group),
                        new SecondaryDrawerItem().withName(getResources().getString(R.string.drawer_item_allnotes)).withTag("ALL_NOTEBOOK").withIcon(R.drawable.ic_action_person),
                        new DividerDrawerItem(),
                        new PrimaryDrawerItem().withName(getResources().getString(R.string.drawer_item_tags)).withTag("HEADING_TAG").setEnabled(false).withDisabledTextColor(R.color.material_drawer_dark_header_selection_text).withIcon(R.drawable.ic_action_labels),
                        new DividerDrawerItem(),
                        new PrimaryDrawerItem().withName(getResources().getString(R.string.drawer_item_notebooks)).withTag("HEADING_NOTEBOOK").setEnabled(false).withDisabledTextColor(R.color.material_drawer_dark_header_selection_text).withIcon(R.drawable.ic_action_collection)
                )
                .withOnDrawerItemClickListener(drawerItemClickedListener)
                .build();

        mDrawer.setSelection(1);
        // Fab Button
        mFabButton = (ImageButton) findViewById(R.id.fab_button);
        //mFabButton.setImageDrawable(new IconicsDrawable(this, FontAwesome.Icon.faw_upload).color(Color.WHITE).actionBarSize());
        Utils.configureFab(mFabButton);
        mFabButton.setOnClickListener(new CreateButtonListener());

        mRecyclerView = (RecyclerView) findViewById(R.id.list);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setItemAnimator(new CustomItemAnimator());
        //mRecyclerView.setItemAnimator(new ReboundItemAnimator());

        mAdapter = new NoteAdapter(new ArrayList<Note>(), R.layout.row_application, this);
        mRecyclerView.setAdapter(mAdapter);

        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
        mSwipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.theme_accent));
        mSwipeRefreshLayout.setRefreshing(true);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                new InitializeApplicationsTask().execute();
            }
        });

        new InitializeApplicationsTask().execute();

        if (savedInstanceState != null) {
            //nothing at the moment
        }

        //show progress
        mRecyclerView.setVisibility(View.GONE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main_toolbar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.create_notebook_menu:
                AlertDialog newNBDialog = createNotebookDialog();
                newNBDialog.show();
                break;
            case R.id.create_tag_menu:
                AlertDialog newTagDialog = createTagDialog();
                newTagDialog.show();
                break;
            case R.id.create_search_menu:
                AlertDialog newSearchDialog = createSearchDialog();
                newSearchDialog.show();
                break;
        }
        return true;
    }

    private AlertDialog createSearchDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(R.string.dialog_input_text_search);

        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_search_note, null);

        builder.setView(view);

        builder.setPositiveButton(R.string.ok,new SearchNoteButtonListener((EditText)view.findViewById(R.id.dialog_search_input_field)));
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //nothing
            }
        });
        return builder.create();
    }

    private AlertDialog createNotebookDialog(Intent startActivity){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(R.string.dialog_input_text_notebook);

        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_text_input, null);

        builder.setView(view);

        builder.setPositiveButton(R.string.ok,new CreateNotebookButtonListener(startActivity, (EditText)view.findViewById(R.id.dialog_text_input_field)));
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //nothing
            }
        });
        return builder.create();
    }

    private AlertDialog createNotebookDialog(){
        return  createNotebookDialog(null);
    }

    private AlertDialog createTagDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(R.string.dialog_input_text_tag);

        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_text_input, null);

        builder.setView(view);

        builder.setPositiveButton(R.string.ok,new CreateTagButtonListener((EditText)view.findViewById(R.id.dialog_text_input_field)));
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //nothing
            }
        });
        return builder.create();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }


    public void animateActivity(Note note, View appIcon) {
        Intent i = new Intent(this, DetailActivity.class);
        i.putExtra(DetailActivity.NOTE_UID, note.getIdentification().getUid());

        ActivityOptionsCompat transitionActivityOptions = ActivityOptionsCompat.makeSceneTransitionAnimation(this, Pair.create((View) mFabButton, "fab"), Pair.create(appIcon, "appIcon"));
        startActivity(i, transitionActivityOptions.toBundle());
    }


    private class DrawerItemClickedListener implements Drawer.OnDrawerItemClickListener{
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l, IDrawerItem iDrawerItem) {
            if(iDrawerItem instanceof  BaseDrawerItem) {
                changeNoteSelection((BaseDrawerItem) iDrawerItem);
            }
        }

        public void changeNoteSelection(BaseDrawerItem drawerItem){
            if(drawerItem == null){
                return;
            }

            String tag = drawerItem.getTag() == null || drawerItem.getTag().toString().trim().length() == 0 ? "ALL_NOTEBOOK" :  drawerItem.getTag().toString();
            List<Note> notes;
            if("NOTEBOOK".equalsIgnoreCase(tag)){
                Notebook notebook = notebookRepository.getBySummary(SELECTED_ACCOUNT, SELECTED_ROOT_FOLDER, drawerItem.getName());
                notes = notesRepository.getFromNotebook(SELECTED_ACCOUNT,SELECTED_ROOT_FOLDER,notebook.getIdentification().getUid());
            }else if("TAG".equalsIgnoreCase(tag)){
                notes = notetagRepository.getNotesWith(SELECTED_ACCOUNT, SELECTED_ROOT_FOLDER, drawerItem.getName());
            }else{
                notes = notesRepository.getAll(SELECTED_ACCOUNT,SELECTED_ROOT_FOLDER);
            }

            if(mAdapter != null) {
                mAdapter.clearNotes();
                mAdapter.addNotes(notes);
            }
        }
    }

    private class InitializeApplicationsTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            mAdapter.clearNotes();
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... params) {
            notesList.clear();

            //Query the notes
            final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

            List<Note> notes = notesRepository.getAll(SELECTED_ACCOUNT,SELECTED_ROOT_FOLDER);
            for (Note note : notes) {
                notesList.add(note);
            }
            Collections.sort(notesList);
            notesRepository.close();

            //Query the tags
            for (String tag : tagRepository.getAll()) {
                mDrawer.addItem(new PrimaryDrawerItem().withName(tag).withTag("TAG"));
            }

            //Query the notebooks
            for (Notebook notebook : notebookRepository.getAll(SELECTED_ACCOUNT,SELECTED_ROOT_FOLDER)) {
                mDrawer.addItem(new SecondaryDrawerItem().withName(notebook.getSummary()).withTag("NOTEBOOK"));
            }

            orderDrawerItems(mDrawer);
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            //handle visibility
            mRecyclerView.setVisibility(View.VISIBLE);

            //set data for list
            mAdapter.clearNotes();
            mAdapter.addNotes(notesList);
            mSwipeRefreshLayout.setRefreshing(false);

            super.onPostExecute(result);
        }

    }

    class CreateButtonListener implements View.OnClickListener{
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(MainPhoneActivity.this,DetailActivity.class);
            if(notebookRepository.getAll(SELECTED_ACCOUNT,SELECTED_ROOT_FOLDER).isEmpty()){
                //Create first a notebook, so that note creation is possible
                createNotebookDialog(intent).show();
            }else{
                startActivity(intent);
            }
        }
    }

    public class CreateTagButtonListener implements DialogInterface.OnClickListener{
        private final EditText textField;

        public CreateTagButtonListener(EditText textField) {
            this.textField = textField;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if(textField == null || textField.getText() == null || textField.getText().toString().trim().length() == 0){
                return;
            }

            String value = textField.getText().toString();

            if(tagRepository.insert(value)) {

                mDrawer.addItem(new SecondaryDrawerItem().withName(value).withTag("TAG"));

                orderDrawerItems(mDrawer);
            }
        }
    }

    public class SearchNoteButtonListener implements DialogInterface.OnClickListener{

        private final EditText textField;

        public SearchNoteButtonListener(EditText textField) {
            this.textField = textField;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if(textField == null || textField.getText() == null || textField.getText().toString().trim().length() == 0){
                return;
            }

            IDrawerItem drawerItem = mDrawer.getDrawerItems().get(mDrawer.getCurrentSelection());
            if(drawerItem instanceof BaseDrawerItem){
                BaseDrawerItem item = (BaseDrawerItem)drawerItem;
                String tag = item.getTag() == null || item.getTag().toString().trim().length() == 0 ? null : item.getTag().toString();

                List<Note> notes;
                if("NOTEBOOK".equalsIgnoreCase(tag)){
                    notes = notesRepository.getFromNotebookWithSummary(SELECTED_ACCOUNT,
                            SELECTED_ROOT_FOLDER,
                            notebookRepository.getBySummary(SELECTED_ACCOUNT,SELECTED_ROOT_FOLDER,item.getName()).getIdentification().getUid(),
                            textField.getText().toString());
                }else if("TAG".equalsIgnoreCase(tag)){
                    List<Note> unfiltered = notetagRepository.getNotesWith(SELECTED_ACCOUNT, SELECTED_ROOT_FOLDER, item.getName());
                    notes = new ArrayList<Note>();
                    for(Note note : unfiltered){
                        String summary = note.getSummary().toLowerCase();
                        if(summary.contains(textField.getText().toString().toLowerCase())){
                            notes.add(note);
                        }
                    }
                }else{
                    notes = notesRepository.getFromNotebookWithSummary(SELECTED_ACCOUNT,SELECTED_ROOT_FOLDER,null,textField.getText().toString());
                }

                notesList.clear();
                notesList.addAll(notes);
            }
        }
    }

    public class CreateNotebookButtonListener implements DialogInterface.OnClickListener{

        private final EditText textField;
        private Intent intent;

        public CreateNotebookButtonListener(Intent startActivity, EditText textField) {
            this.textField = textField;
            intent = startActivity;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if(textField == null || textField.getText() == null || textField.getText().toString().trim().length() == 0){
                return;
            }

            Note.Identification ident = new Note.Identification(UUID.randomUUID().toString(),"kolabnotes-android");
            Timestamp now = new Timestamp(System.currentTimeMillis());
            Note.AuditInformation audit = new Note.AuditInformation(now,now);

            String value = textField.getText().toString();

            Notebook nb = new Notebook(ident,audit, Note.Classification.PUBLIC, value);
            nb.setDescription(value);
            if(notebookRepository.insert(SELECTED_ACCOUNT, SELECTED_ROOT_FOLDER, nb)) {

                mDrawer.addItem(new SecondaryDrawerItem().withName(value).withTag("NOTEBOOK"));

                orderDrawerItems(mDrawer, value);
            }

            if(intent != null){
                startActivity(intent);
            }
        }
    }

    void orderDrawerItems(Drawer.Result drawer){
        orderDrawerItems(drawer,null);
    }

    void orderDrawerItems(Drawer.Result drawer, String selectionName){
        ArrayList<IDrawerItem> items = drawer.getDrawerItems();

        List<String> tags = new ArrayList<>();
        List<String> notebooks = new ArrayList<>();

        boolean notebookSelected = true;
        boolean allnotesSelected = false;
        String selected = null;

        int selection = drawer.getCurrentSelection();
        for(IDrawerItem item : items){
            if(item instanceof BaseDrawerItem){
                BaseDrawerItem base = (BaseDrawerItem)item;

                String type = base.getTag().toString();
                if(type.equalsIgnoreCase("TAG")){
                    tags.add(base.getName());
                    if(selection == 0){
                        notebookSelected = false;
                        selected = base.getName();
                    }
                }else if(type.equalsIgnoreCase("NOTEBOOK")){
                    notebooks.add(base.getName());
                    if(selection == 0){
                        selected = base.getName();
                    }
                }else if(type.equalsIgnoreCase("ALL_NOTEBOOK")){
                    if(selection == 0){
                        notebookSelected = false;
                        allnotesSelected = true;
                        selected = base.getName();
                    }
                }
            }
            selection--;
        }

        if(selectionName != null){
            selected = selectionName;
            notebookSelected = true;
        }

        Collections.sort(tags);
        Collections.sort(notebooks);

        drawer.getDrawerItems().clear();

        drawer.getDrawerItems().add(new PrimaryDrawerItem().withName(getResources().getString(R.string.drawer_item_allaccount_notes)).withTag("ALL_NOTES").withIcon(R.drawable.ic_action_group));
        drawer.getDrawerItems().add(new SecondaryDrawerItem().withName(getResources().getString(R.string.drawer_item_allnotes)).withTag("ALL_NOTEBOOK").withIcon(R.drawable.ic_action_person));
        drawer.getDrawerItems().add(new DividerDrawerItem());
        drawer.getDrawerItems().add(new PrimaryDrawerItem().withName(getResources().getString(R.string.drawer_item_tags)).withTag("HEADING_TAG").setEnabled(false).withDisabledTextColor(R.color.material_drawer_dark_header_selection_text).withIcon(R.drawable.ic_action_labels));

        int idx = 4;
        for(String tag : tags){
            drawer.getDrawerItems().add(new SecondaryDrawerItem().withName(tag).withTag("TAG"));

            idx++;
            if(!notebookSelected && !allnotesSelected && tag.equals(selected)){
                selection = idx;
            }
        }

        drawer.getDrawerItems().add(new DividerDrawerItem());

        drawer.getDrawerItems().add(new PrimaryDrawerItem().withName(getResources().getString(R.string.drawer_item_notebooks)).withTag("HEADING_NOTEBOOK").setEnabled(false).withDisabledTextColor(R.color.material_drawer_dark_header_selection_text).withIcon(R.drawable.ic_action_collection));

        idx = idx+2;
        if(notebookSelected){
            selection = idx;
        }
        BaseDrawerItem selectedItem = null;
        for(String notebook : notebooks){
            BaseDrawerItem item = new SecondaryDrawerItem().withName(notebook).withTag("NOTEBOOK");
            drawer.getDrawerItems().add(item);

            idx++;
            if((allnotesSelected || notebookSelected) && notebook.equals(selected)){
                selection = idx;
                selectedItem = item;
            }
        }

        if(selection < 1){
            //default if nothing is selected, choose "all notes form actual account"
            selection = 1;
        }

        drawer.setSelection(selection);
        drawerItemClickedListener.changeNoteSelection(selectedItem);
    }
}
