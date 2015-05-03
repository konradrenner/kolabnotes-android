package org.kore.kolabnotes.android;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SyncResult;
import android.content.SyncStatusObserver;
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
import org.kore.kolabnotes.android.async.KolabSyncAdapter;
import org.kore.kolabnotes.android.content.ActiveAccount;
import org.kore.kolabnotes.android.content.ActiveAccountRepository;
import org.kore.kolabnotes.android.content.NoteRepository;
import org.kore.kolabnotes.android.content.NoteTagRepository;
import org.kore.kolabnotes.android.content.NotebookRepository;
import org.kore.kolabnotes.android.content.TagRepository;
import org.kore.kolabnotes.android.itemanimator.CustomItemAnimator;
import org.kore.kolabnotes.android.itemanimator.ReboundItemAnimator;
import org.kore.kolabnotes.android.security.AuthenticatorActivity;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainPhoneActivity extends ActionBarActivity implements SyncStatusObserver{

    public static final int DETAIL_ACTIVITY_RESULT_CODE = 1;

    public static final String AUTHORITY = "kore.kolabnotes";

    private final DrawerItemClickedListener drawerItemClickedListener = new DrawerItemClickedListener();

    private List<Note> notesList = new ArrayList<Note>();

    private NoteAdapter mAdapter;
    private ImageButton mFabButton;
    private RecyclerView mRecyclerView;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private Drawer.Result mDrawer;
    private AccountHeader.Result mAccount;
    private AccountManager mAccountManager;
    private String selectedNotebookName;
    private boolean fromDetailActivity = false;

    private NoteRepository notesRepository = new NoteRepository(this);
    private NotebookRepository notebookRepository = new NotebookRepository(this);
    private TagRepository tagRepository = new TagRepository(this);
    private NoteTagRepository notetagRepository = new NoteTagRepository(this);
    private ActiveAccountRepository activeAccountRepository = new ActiveAccountRepository(this);

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

        ContentResolver.addStatusChangeListener(ContentResolver.SYNC_OBSERVER_TYPE_ACTIVE, this);

        mAccountManager = AccountManager.get(this);

        Account[] accounts = mAccountManager.getAccountsByType(AuthenticatorActivity.ARG_ACCOUNT_TYPE);

        ProfileDrawerItem[] profiles = new ProfileDrawerItem[accounts.length+1];
        profiles[0] = new ProfileDrawerItem().withName(getResources().getString(R.string.drawer_account_local)).withTag("Notes");

        for(int i=0;i<accounts.length;i++) {
            String email = mAccountManager.getUserData(accounts[i],AuthenticatorActivity.KEY_EMAIL);
            String name = mAccountManager.getUserData(accounts[i],AuthenticatorActivity.KEY_ACCOUNT_NAME);
            String rootFolder = mAccountManager.getUserData(accounts[i],AuthenticatorActivity.KEY_ROOT_FOLDER);

            profiles[i+1] = new ProfileDrawerItem().withName(name).withTag(rootFolder).withEmail(email);
        }
        mAccount = new AccountHeader()
                .withActivity(this)
                .withHeaderBackground(R.drawable.drawer_header_background)
                .addProfiles(profiles)
                .withOnAccountHeaderListener(new ProfileChanger())
                .build();
        mDrawer = new Drawer()
                .withActivity(this)
                .withToolbar(toolbar)
                .withAccountHeader(mAccount)
                .withOnDrawerItemClickListener(drawerItemClickedListener)
                .build();

        addDrawerStandardItems(mDrawer);

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
                ActiveAccount activeAccount = activeAccountRepository.getActiveAccount();
                if(!"local".equalsIgnoreCase(activeAccount.getAccount())) {
                    Account[] accounts = mAccountManager.getAccountsByType(AuthenticatorActivity.ARG_ACCOUNT_TYPE);
                    Account selectedAccount = null;

                    for (Account acc : accounts) {
                        String email = mAccountManager.getUserData(acc, AuthenticatorActivity.KEY_EMAIL);
                        if (activeAccount.getAccount().equalsIgnoreCase(email)) {
                            selectedAccount = acc;
                            break;
                        }
                    }

                    if(selectedAccount == null){
                        return;
                    }

                    Bundle settingsBundle = new Bundle();
                    settingsBundle.putBoolean(
                            ContentResolver.SYNC_EXTRAS_MANUAL, true);
                    settingsBundle.putBoolean(
                            ContentResolver.SYNC_EXTRAS_EXPEDITED, true);

                    ContentResolver.requestSync(selectedAccount, AUTHORITY, settingsBundle);
                }else{
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            reloadData();

                            mSwipeRefreshLayout.setRefreshing(false);
                        }
                    });
                }
            }
        });

        new InitializeApplicationsTask().execute();

        if (savedInstanceState != null) {
            //nothing at the moment
        }

        //show progress
        mRecyclerView.setVisibility(View.GONE);
    }

    class ProfileChanger implements AccountHeader.OnAccountHeaderListener{
        @Override
        public boolean onProfileChanged(View view, IProfile profile, boolean current) {
            final ActiveAccount activeAccount = activeAccountRepository.getActiveAccount();
            String account;
            String rootFolder;
            boolean changed;
            if(profile.getEmail() == null || profile.getEmail().trim().length() == 0){
                changed = !activeAccount.getAccount().equalsIgnoreCase("local");
                account = "local";
                rootFolder = ((ProfileDrawerItem)profile).getTag().toString();
            }else{
                changed = !activeAccount.getAccount().equalsIgnoreCase(profile.getEmail());
                account = profile.getEmail();
                rootFolder = ((ProfileDrawerItem)profile).getTag().toString();
            }

            if(changed){
                new AccountChangeThread(account,rootFolder).start();
            }

            mDrawer.closeDrawer();
            return changed;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == DETAIL_ACTIVITY_RESULT_CODE) {
            if(resultCode == RESULT_OK){
                String nbName = data.getStringExtra("selectedNotebookName");
                selectedNotebookName = nbName;
                fromDetailActivity = true;
            }else if (resultCode == RESULT_CANCELED) {
                //do nothing at the moment
            }
        }
    }

    @Override
    public void onResume(){
        super.onResume();
        ActiveAccount activeAccount = activeAccountRepository.getActiveAccount();
        for(IProfile profile : mAccount.getProfiles()){
            if(profile instanceof ProfileDrawerItem){
                ProfileDrawerItem item = (ProfileDrawerItem)profile;

                if(activeAccount.getAccount().equals(item.getEmail()) && activeAccount.getRootFolder().equals(item.getTag().toString())){
                    mAccount.setActiveProfile(profile);
                    break;
                }
            }
        }

        String notebookUID = null;
        if(fromDetailActivity && selectedNotebookName != null){
            notebookUID = notebookRepository.getBySummary(activeAccount.getAccount(),activeAccount.getRootFolder(),selectedNotebookName).getIdentification().getUid();
            fromDetailActivity = false;
        }
        new AccountChangeThread(activeAccount,notebookUID).run();
    }

    class AccountChangeThread extends Thread{

        private final String account;
        private final String rootFolder;
        private ActiveAccount activeAccount;
        private String notebookUID;

        AccountChangeThread(String account, String rootFolder) {
            this.account = account;
            this.rootFolder = rootFolder;
            notebookUID = null;
        }

        AccountChangeThread(ActiveAccount activeAccount) {
            this(activeAccount.getAccount(),activeAccount.getRootFolder());
            this.activeAccount = activeAccount;
        }

        AccountChangeThread(ActiveAccount activeAccount, String notebookUID) {
            this(activeAccount);
            this.notebookUID = notebookUID;
        }

        @Override
        public void run() {

            if(activeAccount == null) {
                activeAccount = activeAccountRepository.switchAccount(account, rootFolder);
            }

            List<Note> notes;
            if(notebookUID == null){
                notes = notesRepository.getAll(account,rootFolder);
            }else{
                notes = notesRepository.getFromNotebook(account,rootFolder,notebookUID);
            }

            List<String> tags = tagRepository.getAll();
            List<Notebook> notebooks = notebookRepository.getAll(account, rootFolder);

            runOnUiThread(new ReloadDataThread(notebooks,notes,tags));
        }
    }



    class ReloadDataThread extends Thread{
        private final List<Notebook> notebooks;
        private final List<Note> notes;
        private final List<String> tags;

        ReloadDataThread(List<Notebook> notebooks, List<Note> notes, List<String> tags) {
            this.notebooks = notebooks;
            this.notes = notes;
            this.tags = tags;
        }

        @Override
        public void run() {
            reloadData(notebooks, notes, tags);
        }
    }


    @Override
    public void onStatusChanged(int which) {
        AccountManager accountManager = AccountManager.get(this);
        Account[] accounts = accountManager.getAccountsByType(AuthenticatorActivity.ARG_ACCOUNT_TYPE);

        if (accounts.length <= 0) {
            return;
        }

        Account selectedAccount = null;

        ActiveAccount activeAccount = activeAccountRepository.getActiveAccount();

        for (Account acc : accounts) {
            String email = mAccountManager.getUserData(acc, AuthenticatorActivity.KEY_EMAIL);
            if (activeAccount.getAccount().equalsIgnoreCase(email)) {
                selectedAccount = acc;
                break;
            }
        }

        if(selectedAccount == null || !ContentResolver.isSyncActive(selectedAccount,AUTHORITY)){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    reloadData();

                    mSwipeRefreshLayout.setRefreshing(false);
                }
            });
        }
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
            case R.id.create_account_menu:
                Intent intent = new Intent(this,AuthenticatorActivity.class);

                startActivity(intent);
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
        ActiveAccount activeAccount = activeAccountRepository.getActiveAccount();
        i.putExtra(DetailActivity.NOTEBOOK_UID, notesRepository.getUIDofNotebook(activeAccount.getAccount(),activeAccount.getRootFolder(),note.getIdentification().getUid()));

        ActivityOptionsCompat transitionActivityOptions = ActivityOptionsCompat.makeSceneTransitionAnimation(this, Pair.create((View) mFabButton, "fab"), Pair.create(appIcon, "appIcon"));
        startActivityForResult(i,DETAIL_ACTIVITY_RESULT_CODE,transitionActivityOptions.toBundle());
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
            if(mAdapter != null) {
                mAdapter.clearNotes();
            }

            String tag = drawerItem.getTag() == null || drawerItem.getTag().toString().trim().length() == 0 ? "ALL_NOTEBOOK" :  drawerItem.getTag().toString();
            List<Note> notes;
            ActiveAccount activeAccount = activeAccountRepository.getActiveAccount();
            if("NOTEBOOK".equalsIgnoreCase(tag)){
                Notebook notebook = notebookRepository.getBySummary(activeAccount.getAccount(), activeAccount.getRootFolder(), drawerItem.getName());
                notes = notesRepository.getFromNotebook(activeAccount.getAccount(),activeAccount.getRootFolder(),notebook.getIdentification().getUid());
                selectedNotebookName = notebook.getSummary();
            }else if("TAG".equalsIgnoreCase(tag)){
                notes = notetagRepository.getNotesWith(activeAccount.getAccount(), activeAccount.getRootFolder(), drawerItem.getName());
                selectedNotebookName = null;
            }else if("ALL_NOTES".equalsIgnoreCase(tag)){
                notes = notesRepository.getAll();
                selectedNotebookName = null;
            }else{
                notes = notesRepository.getAll(activeAccount.getAccount(),activeAccount.getRootFolder());
                selectedNotebookName = null;
            }

            if(mAdapter != null) {
                if(notes.size() == 0){
                    mAdapter.notifyDataSetChanged();
                }else {
                    mAdapter.addNotes(notes);
                }
            }
        }
    }

    private class InitializeApplicationsTask extends AsyncTask<Void, Void, Void> implements Runnable{

        @Override
        protected void onPreExecute() {
            mAdapter.clearNotes();
            super.onPreExecute();
        }

        @Override
        public void run() {
            //Query the notes
            final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

            ActiveAccount activeAccount = activeAccountRepository.getActiveAccount();

            new AccountChangeThread(activeAccount).run();
        }

        @Override
        protected Void doInBackground(Void... params) {
            runOnUiThread(this);
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

    final void reloadData(List<Notebook> notebooks, List<Note> notes, List<String> tags){
        mDrawer.getDrawerItems().clear();

        addDrawerStandardItems(mDrawer);
        //Query the tags
        for (String tag : tags) {
            mDrawer.getDrawerItems().add(new PrimaryDrawerItem().withName(tag).withTag("TAG"));
        }

        //Query the notebooks
        for (Notebook notebook : notebooks) {
            mDrawer.getDrawerItems().add(new SecondaryDrawerItem().withName(notebook.getSummary()).withTag("NOTEBOOK"));
        }

        orderDrawerItems(mDrawer);

        if(mAdapter != null) {
            mAdapter.clearNotes();
            if(notes.size() == 0){
                mAdapter.notifyDataSetChanged();
            }else {
                mAdapter.addNotes(notes);
            }
        }
    }

    final void reloadData(){
        ActiveAccount activeAccount = activeAccountRepository.getActiveAccount();
        reloadData(notebookRepository.getAll(activeAccount.getAccount(), activeAccount.getRootFolder()),notesRepository.getAll(activeAccount.getAccount(),activeAccount.getRootFolder()),tagRepository.getAll());
    }

    class CreateButtonListener implements View.OnClickListener{
        @Override
        public void onClick(View v) {
            ActiveAccount activeAccount = activeAccountRepository.getActiveAccount();
            Intent intent = new Intent(MainPhoneActivity.this,DetailActivity.class);
            intent.putExtra(DetailActivity.NOTEBOOK_UID,notebookRepository.getBySummary(activeAccount.getAccount(),activeAccount.getRootFolder(),selectedNotebookName));
            if(notebookRepository.getAll(activeAccount.getAccount(),activeAccount.getRootFolder()).isEmpty()){
                //Create first a notebook, so that note creation is possible
                createNotebookDialog(intent).show();
            }else{
                startActivityForResult(intent,DETAIL_ACTIVITY_RESULT_CODE);
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

            ActiveAccount activeAccount = activeAccountRepository.getActiveAccount();

            IDrawerItem drawerItem = mDrawer.getDrawerItems().get(mDrawer.getCurrentSelection());
            if(drawerItem instanceof BaseDrawerItem){
                BaseDrawerItem item = (BaseDrawerItem)drawerItem;
                String tag = item.getTag() == null || item.getTag().toString().trim().length() == 0 ? null : item.getTag().toString();

                List<Note> notes;
                if("NOTEBOOK".equalsIgnoreCase(tag)){
                    notes = notesRepository.getFromNotebookWithSummary(activeAccount.getAccount(),
                            activeAccount.getRootFolder(),
                            notebookRepository.getBySummary(activeAccount.getAccount(),activeAccount.getRootFolder(),item.getName()).getIdentification().getUid(),
                            textField.getText().toString());
                }else if("TAG".equalsIgnoreCase(tag)){
                    List<Note> unfiltered = notetagRepository.getNotesWith(activeAccount.getAccount(), activeAccount.getRootFolder(), item.getName());
                    notes = new ArrayList<Note>();
                    for(Note note : unfiltered){
                        String summary = note.getSummary().toLowerCase();
                        if(summary.contains(textField.getText().toString().toLowerCase())){
                            notes.add(note);
                        }
                    }
                }else{
                    notes = notesRepository.getFromNotebookWithSummary(activeAccount.getAccount(),activeAccount.getRootFolder(),null,textField.getText().toString());
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

            ActiveAccount activeAccount = activeAccountRepository.getActiveAccount();

            Note.Identification ident = new Note.Identification(UUID.randomUUID().toString(),"kolabnotes-android");
            Timestamp now = new Timestamp(System.currentTimeMillis());
            Note.AuditInformation audit = new Note.AuditInformation(now,now);

            String value = textField.getText().toString();
            selectedNotebookName = value;

            Notebook nb = new Notebook(ident,audit, Note.Classification.PUBLIC, value);
            nb.setDescription(value);
            if(notebookRepository.insert(activeAccount.getAccount(), activeAccount.getRootFolder(), nb)) {

                mDrawer.addItem(new SecondaryDrawerItem().withName(value).withTag("NOTEBOOK"));

                orderDrawerItems(mDrawer, value);
            }

            if(intent != null){
                startActivityForResult(intent,DETAIL_ACTIVITY_RESULT_CODE);
            }
        }
    }

    void orderDrawerItems(Drawer.Result drawer){
        orderDrawerItems(drawer,null);
    }

    class Orderer implements Runnable{
        private final Drawer.Result drawer;
        private final String selectionName;

        Orderer(Drawer.Result drawer, String selectionName) {
            this.drawer = drawer;
            this.selectionName = selectionName;
        }

        private Orderer(Drawer.Result drawer) {
            this.drawer = drawer;
            this.selectionName = null;
        }

        @Override
        public void run() {
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

            if(selectedNotebookName != null){
                selected = selectedNotebookName;
                notebookSelected = true;
            }else if(selectionName != null){
                selected = selectionName;
                notebookSelected = true;
            }

            Collections.sort(tags);
            Collections.sort(notebooks);

            drawer.getDrawerItems().clear();

            addDrawerStandardItems(drawer);

            int idx = 3;
            Set<String> displayedTags = new HashSet<>();
            for(String tag : tags){
                if(displayedTags.contains(tag)){
                    continue;
                }
                displayedTags.add(tag);
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

            if(selection < 1 || (selectedItem != null && selection >= drawer.getDrawerItems().size())){
                //default if nothing is selected, choose "all notes form actual account"
                selection = 1;
            }

            drawer.setSelection(selection);
            drawerItemClickedListener.changeNoteSelection(selectedItem);
        }
    }

    private final void addDrawerStandardItems(Drawer.Result drawer){
        drawer.getDrawerItems().add(new PrimaryDrawerItem().withName(getResources().getString(R.string.drawer_item_allaccount_notes)).withTag("ALL_NOTES").withIcon(R.drawable.ic_action_group));
        drawer.getDrawerItems().add(new SecondaryDrawerItem().withName(getResources().getString(R.string.drawer_item_allnotes)).withTag("ALL_NOTEBOOK").withIcon(R.drawable.ic_action_person));
        drawer.getDrawerItems().add(new DividerDrawerItem());
        drawer.getDrawerItems().add(new PrimaryDrawerItem().withName(getResources().getString(R.string.drawer_item_tags)).withTag("HEADING_TAG").setEnabled(false).withDisabledTextColor(R.color.material_drawer_dark_header_selection_text).withIcon(R.drawable.ic_action_labels));

    }

    void orderDrawerItems(Drawer.Result drawer, String selectionName){
        runOnUiThread(new Orderer(drawer,selectionName));
    }
}
