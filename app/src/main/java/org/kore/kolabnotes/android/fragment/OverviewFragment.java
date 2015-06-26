package org.kore.kolabnotes.android.fragment;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;

import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.accountswitcher.AccountHeader;
import com.mikepenz.materialdrawer.accountswitcher.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.model.BaseDrawerItem;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IProfile;

import org.kore.kolab.notes.AuditInformation;
import org.kore.kolab.notes.Identification;
import org.kore.kolab.notes.Note;
import org.kore.kolab.notes.Notebook;
import org.kore.kolabnotes.android.DetailActivity;
import org.kore.kolabnotes.android.MainActivity;
import org.kore.kolabnotes.android.R;
import org.kore.kolabnotes.android.Utils;
import org.kore.kolabnotes.android.adapter.NoteAdapter;
import org.kore.kolabnotes.android.content.ActiveAccount;
import org.kore.kolabnotes.android.content.ActiveAccountRepository;
import org.kore.kolabnotes.android.content.DataCache;
import org.kore.kolabnotes.android.content.DataCaches;
import org.kore.kolabnotes.android.content.NoteRepository;
import org.kore.kolabnotes.android.content.NoteTagRepository;
import org.kore.kolabnotes.android.content.NotebookRepository;
import org.kore.kolabnotes.android.content.TagRepository;
import org.kore.kolabnotes.android.security.AuthenticatorActivity;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Fragment which displays the notes overview and implements the logic for the overview
 */
public class OverviewFragment extends Fragment implements NoteAdapter.NoteSelectedListener{

    public static final int DETAIL_ACTIVITY_RESULT_CODE = 1;

    private final DrawerItemClickedListener drawerItemClickedListener = new DrawerItemClickedListener();


    private NoteAdapter mAdapter;
    private ImageButton mFabButton;
    private RecyclerView mRecyclerView;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    private AccountManager mAccountManager;
    private String selectedNotebookName;
    private boolean fromDetailActivity = false;
    private DataCaches dataCache;

    private NoteRepository notesRepository;
    private NotebookRepository notebookRepository;
    private TagRepository tagRepository;
    private NoteTagRepository notetagRepository;
    private ActiveAccountRepository activeAccountRepository;
    private Toolbar toolbar;

    private Drawer mDrawer;
    private AccountHeader mAccount;

    private MainActivity activity;

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_overview,
                container,
                false);
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        this.activity = (MainActivity)activity;

        dataCache = new DataCaches(activity);
        notesRepository = new NoteRepository(activity);
        notebookRepository = new NotebookRepository(activity);
        tagRepository = new TagRepository(activity);
        notetagRepository = new NoteTagRepository(activity);
        activeAccountRepository = new ActiveAccountRepository(activity);

        initCachesAsync(activeAccountRepository.getActiveAccount());
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Handle Toolbar
        toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);
        activity.setSupportActionBar(toolbar);
        activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        setHasOptionsMenu(true);

        mAccountManager = AccountManager.get(activity);
        Account[] accounts = mAccountManager.getAccountsByType(AuthenticatorActivity.ARG_ACCOUNT_TYPE);

        ProfileDrawerItem[] profiles = new ProfileDrawerItem[accounts.length+1];
        profiles[0] = new ProfileDrawerItem().withName(getResources().getString(R.string.drawer_account_local)).withTag("Notes");

        for(int i=0;i<accounts.length;i++) {
            String email = mAccountManager.getUserData(accounts[i],AuthenticatorActivity.KEY_EMAIL);
            String name = mAccountManager.getUserData(accounts[i],AuthenticatorActivity.KEY_ACCOUNT_NAME);
            String rootFolder = mAccountManager.getUserData(accounts[i],AuthenticatorActivity.KEY_ROOT_FOLDER);

            profiles[i+1] = new ProfileDrawerItem().withName(name).withTag(rootFolder).withEmail(email);
        }

        mAccount = new AccountHeaderBuilder()
                .withActivity(this.activity)
                .withHeaderBackground(R.drawable.drawer_header_background)
                .addProfiles(profiles)
                .withOnAccountHeaderListener(new ProfileChanger())
                .build();
        mDrawer = new DrawerBuilder()
                .withActivity(this.activity)
                .withToolbar(toolbar)
                .withAccountHeader(mAccount)
                .withOnDrawerItemClickListener(getDrawerItemClickedListener())
                .build();

        addDrawerStandardItems(mDrawer);

        mDrawer.setSelection(1);

        ContentResolver.addStatusChangeListener(ContentResolver.SYNC_OBSERVER_TYPE_ACTIVE, activity);

        mAccountManager = AccountManager.get(activity);

        // Fab Button
        mFabButton = (ImageButton) getActivity().findViewById(R.id.fab_button);
        //mFabButton.setImageDrawable(new IconicsDrawable(this, FontAwesome.Icon.faw_upload).color(Color.WHITE).actionBarSize());
        Utils.configureFab(mFabButton);
        mFabButton.setOnClickListener(new CreateButtonListener());

        mRecyclerView = (RecyclerView) getActivity().findViewById(R.id.list);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(activity));
        //mRecyclerView.setItemAnimator(new CustomItemAnimator());
        //mRecyclerView.setItemAnimator(new ReboundItemAnimator());

        mAdapter = new NoteAdapter(new ArrayList<Note>(), R.layout.row_application, activity, this);
        mRecyclerView.setAdapter(mAdapter);

        mSwipeRefreshLayout = (SwipeRefreshLayout) getActivity().findViewById(R.id.swipe_container);
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

                    ContentResolver.requestSync(selectedAccount,MainActivity.AUTHORITY, settingsBundle);
                }else{
                    getActivity().runOnUiThread(new Runnable() {
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

    void initCachesAsync(final ActiveAccount account){
        new Thread(new Runnable() {
            @Override
            public void run() {
                dataCache.reloadTags();
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                dataCache.getNoteCache(account).reloadData();
            }
        }).start();
    }

    public DrawerItemClickedListener getDrawerItemClickedListener(){
        return drawerItemClickedListener;
    }

    @Override
    public void onSelect(Note note) {
        Intent i = new Intent(activity, DetailActivity.class);
        i.putExtra(Utils.NOTE_UID, note.getIdentification().getUid());
        ActiveAccount activeAccount = activeAccountRepository.getActiveAccount();
        if(selectedNotebookName != null) {
            i.putExtra(Utils.NOTEBOOK_UID, notebookRepository.getBySummary(activeAccount.getAccount(), activeAccount.getRootFolder(), selectedNotebookName).getIdentification().getUid());
        }

        startActivityForResult(i,DETAIL_ACTIVITY_RESULT_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == DETAIL_ACTIVITY_RESULT_CODE) {
            String nbName = data.getStringExtra("selectedNotebookName");
            setNotebookNameFromDetail(nbName);
        }
    }

    public void setNotebookNameFromDetail(String name){
        selectedNotebookName = name;
        fromDetailActivity = true;
    }

    @Override
    public void onResume(){
        super.onResume();

        Intent startIntent = getActivity().getIntent();
        String email = startIntent.getStringExtra(Utils.INTENT_ACCOUNT_EMAIL);
        String rootFolder = startIntent.getStringExtra(Utils.INTENT_ACCOUNT_ROOT_FOLDER);
        String notebookUID = startIntent.getStringExtra(Utils.NOTEBOOK_UID);

        ActiveAccount activeAccount;
        if(email != null && rootFolder != null) {
            activeAccount = activeAccountRepository.switchAccount(email,rootFolder);
        }else{
            activeAccount = activeAccountRepository.getActiveAccount();
        }

        if(notebookUID != null){
            selectedNotebookName = notebookRepository.getByUID(activeAccount.getAccount(),activeAccount.getRootFolder(),notebookUID).getSummary();
        }

        AccountHeader accountHeader = mAccount;
        for(IProfile profile : accountHeader.getProfiles()){
            if(profile instanceof ProfileDrawerItem){
                ProfileDrawerItem item = (ProfileDrawerItem)profile;

                if(activeAccount.getAccount().equals(item.getEmail()) && activeAccount.getRootFolder().equals(item.getTag().toString())){
                    accountHeader.setActiveProfile(profile);
                    break;
                }
            }
        }


        if(fromDetailActivity){
            if(selectedNotebookName != null) {
                Notebook nb = notebookRepository.getBySummary(activeAccount.getAccount(), activeAccount.getRootFolder(), selectedNotebookName);

                //GitHub Issue 31
                if (nb != null) {
                    notebookUID = nb.getIdentification().getUid();
                }
            }
            fromDetailActivity = false;

            //Refresh the loaded data because it could be that something changed, after coming back from detail activity
            this.dataCache.reloadTags();
            this.dataCache.getNoteCache(activeAccount).reloadData();
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
            long ts = System.currentTimeMillis();
            if(activeAccount == null) {
                activeAccount = activeAccountRepository.switchAccount(account, rootFolder);
            }

            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    toolbar.setTitle(Utils.getNameOfActiveAccount(activity, activeAccount.getAccount(), activeAccount.getRootFolder()));
                }
            });

            List<Note> notes;
            DataCache noteCache = dataCache.getNoteCache(activeAccount);
            if(notebookUID == null){
                notes = noteCache.getNotes();
            }else{
                notes = noteCache.getNotesFromNotebook(notebookUID);
            }

            List<String> tags = dataCache.getTags();
            List<Notebook> notebooks = noteCache.getNotebooks();

            getActivity().runOnUiThread(new ReloadDataThread(notebooks, notes, tags));
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

    public void refreshFinished(Account selectedAccount){
        if(selectedAccount == null || !ContentResolver.isSyncActive(selectedAccount,MainActivity.AUTHORITY)){
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    reloadData();

                    mSwipeRefreshLayout.setRefreshing(false);
                }
            });
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu,inflater);
        inflater.inflate(R.menu.main_toolbar, menu);
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
                Intent intent = new Intent(activity,AuthenticatorActivity.class);

                startActivity(intent);
                break;
        }
        return true;
    }

    private AlertDialog createSearchDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        builder.setTitle(R.string.dialog_input_text_search);

        LayoutInflater inflater = activity.getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_search_note, null);

        builder.setView(view);

        builder.setPositiveButton(R.string.ok, new SearchNoteButtonListener((EditText) view.findViewById(R.id.dialog_search_input_field)));
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //nothing
            }
        });
        return builder.create();
    }

    private AlertDialog createNotebookDialog(Intent startActivity){
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        builder.setTitle(R.string.dialog_input_text_notebook);

        LayoutInflater inflater = activity.getLayoutInflater();
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
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        builder.setTitle(R.string.dialog_input_text_tag);

        LayoutInflater inflater = activity.getLayoutInflater();
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
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }


    private class DrawerItemClickedListener implements Drawer.OnDrawerItemClickListener{
        @Override
        public boolean onItemClick(AdapterView<?> adapterView, View view, int i, long l, IDrawerItem iDrawerItem) {
            if(iDrawerItem instanceof BaseDrawerItem) {
                changeNoteSelection((BaseDrawerItem) iDrawerItem);
            }

            //because after a selection the drawer should close
            return false;
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
            run();
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            //handle visibility
            mRecyclerView.setVisibility(View.VISIBLE);

            mSwipeRefreshLayout.setRefreshing(false);

            super.onPostExecute(result);
        }

    }

    final synchronized void reloadData(List<Notebook> notebooks, List<Note> notes, List<String> tags){
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

        if(mAdapter == null){
            mAdapter = new NoteAdapter(new ArrayList<Note>(), R.layout.row_application, activity,this);
        }

        mAdapter.clearNotes();
        if(notes.size() == 0){
            mAdapter.notifyDataSetChanged();
        }else {
            mAdapter.addNotes(notes);
        }
    }

    final void reloadData(){
        ActiveAccount activeAccount = activeAccountRepository.getActiveAccount();
        DataCache noteCache = this.dataCache.getNoteCache(activeAccount);
        noteCache.reloadData();
        this.dataCache.reloadTags();
        reloadData(noteCache.getNotebooks(), noteCache.getNotes(), dataCache.getTags());
    }

    class CreateButtonListener implements View.OnClickListener{
        @Override
        public void onClick(View v) {
            ActiveAccount activeAccount = activeAccountRepository.getActiveAccount();
            Intent intent = new Intent(activity,DetailActivity.class);

            Notebook notebook = selectedNotebookName == null ? null : notebookRepository.getBySummary(activeAccount.getAccount(), activeAccount.getRootFolder(), selectedNotebookName);

            if(notebookRepository.getAll(activeAccount.getAccount(),activeAccount.getRootFolder()).isEmpty()){
                //Create first a notebook, so that note creation is possible
                createNotebookDialog(intent).show();
            }else{
                if(notebook != null) {
                    intent.putExtra(Utils.NOTEBOOK_UID, notebook.getIdentification().getUid());
                }
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

                List<Notebook> notebooks = notebookRepository.getAll(activeAccount.getAccount(), activeAccount.getRootFolder());
                List<String> tags = tagRepository.getAll();

                getActivity().runOnUiThread(new ReloadDataThread(notebooks, notes, tags));
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

            Identification ident = new Identification(UUID.randomUUID().toString(),"kolabnotes-android");
            Timestamp now = new Timestamp(System.currentTimeMillis());
            AuditInformation audit = new AuditInformation(now,now);

            String value = textField.getText().toString();
            selectedNotebookName = value;

            Notebook nb = new Notebook(ident,audit, Note.Classification.PUBLIC, value);
            nb.setDescription(value);
            if(notebookRepository.insert(activeAccount.getAccount(), activeAccount.getRootFolder(), nb)) {
                mDrawer.addItem(new SecondaryDrawerItem().withName(value).withTag("NOTEBOOK"));

                orderDrawerItems(mDrawer, value);
            }

            if(intent != null){
                intent.putExtra(Utils.NOTEBOOK_UID,nb.getIdentification().getUid());
                startActivityForResult(intent,DETAIL_ACTIVITY_RESULT_CODE);
            }
        }
    }

    void orderDrawerItems(Drawer drawer){
        orderDrawerItems(drawer,null);
    }

    class Orderer implements Runnable{
        private final Drawer drawer;
        private final String selectionName;

        Orderer(Drawer drawer, String selectionName) {
            this.drawer = drawer;
            this.selectionName = selectionName;
        }

        private Orderer(Drawer drawer) {
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

                SecondaryDrawerItem item = new SecondaryDrawerItem().withName(tag).withTag("TAG");
                item.setTextColorRes(R.color.abc_primary_text_material_light);
                drawer.getDrawerItems().add(item);

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
                item.setTextColorRes(R.color.abc_primary_text_material_light);
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

    void orderDrawerItems(Drawer drawer, String selectionName){
        getActivity().runOnUiThread(new Orderer(drawer, selectionName));
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

    public final void addDrawerStandardItems(Drawer drawer){
        drawer.getDrawerItems().add(new PrimaryDrawerItem().withName(getResources().getString(R.string.drawer_item_allaccount_notes)).withTag("ALL_NOTES").withIcon(R.drawable.ic_action_group));
        drawer.getDrawerItems().add(new PrimaryDrawerItem().withName(getResources().getString(R.string.drawer_item_allnotes)).withTag("ALL_NOTEBOOK").withIcon(R.drawable.ic_action_person));
        drawer.getDrawerItems().add(new DividerDrawerItem());
        drawer.getDrawerItems().add(new PrimaryDrawerItem().withName(getResources().getString(R.string.drawer_item_tags)).withTag("HEADING_TAG").setEnabled(false).withDisabledTextColor(R.color.material_drawer_dark_header_selection_text).withIcon(R.drawable.ic_action_labels));

    }
}
