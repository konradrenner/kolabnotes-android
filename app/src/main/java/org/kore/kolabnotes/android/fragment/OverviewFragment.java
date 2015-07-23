package org.kore.kolabnotes.android.fragment;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

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
import org.kore.kolabnotes.android.content.NoteSorting;
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
    private boolean tabletMode;

    private boolean initPhase;

    private boolean preventBlankDisplaying;

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
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        initPhase = true;
        // Handle Toolbar
        toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);
        activity.setSupportActionBar(toolbar);
        activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        tabletMode = isTablet();

        setHasOptionsMenu(true);

        mAccountManager = AccountManager.get(activity);
        Account[] accounts = mAccountManager.getAccountsByType(AuthenticatorActivity.ARG_ACCOUNT_TYPE);

        ProfileDrawerItem[] profiles = new ProfileDrawerItem[accounts.length+1];
        profiles[0] = new ProfileDrawerItem().withName(getResources().getString(R.string.drawer_account_local)).withTag("Notes");

        for(int i=0;i<accounts.length;i++) {
            String email = mAccountManager.getUserData(accounts[i],AuthenticatorActivity.KEY_EMAIL);
            String name = mAccountManager.getUserData(accounts[i],AuthenticatorActivity.KEY_ACCOUNT_NAME);
            String rootFolder = mAccountManager.getUserData(accounts[i],AuthenticatorActivity.KEY_ROOT_FOLDER);

            ProfileDrawerItem item = new ProfileDrawerItem().withName(name).withTag(rootFolder).withEmail(email);

            //GitHub issue 47
            item.setNameShown(true);
            if(name != null && name.equals(email)){
                item.setNameShown(false);
                item.withName(null);
            }

            profiles[i+1] = item;
        }

        mAccount = new AccountHeaderBuilder()
                .withActivity(this.activity)
                .withHeaderBackground(R.drawable.drawer_header_background)
                .addProfiles(profiles)
                .withCompactStyle(true)
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

        mRecyclerView = (RecyclerView) activity.findViewById(R.id.list);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(activity));
        //mRecyclerView.setItemAnimator(new CustomItemAnimator());
        //mRecyclerView.setItemAnimator(new ReboundItemAnimator());

        mAdapter = new NoteAdapter(new ArrayList<Note>(), R.layout.row_note_overview, activity, this);
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

    public void openDrawer(){
        mDrawer.openDrawer();
    }

    public void displayBlankFragment(){
        Log.d("displayBlankFragment","tabletMode:"+tabletMode);
        if(tabletMode){
            BlankFragment blank = BlankFragment.newInstance();
            FragmentTransaction ft = getFragmentManager(). beginTransaction();
            ft.replace(R.id.details_fragment, blank);
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            ft.commit();
        }
    }

    public DrawerItemClickedListener getDrawerItemClickedListener(){
        return drawerItemClickedListener;
    }

    void setDetailFragment(Note note, boolean sameSelection){
        DetailFragment detail = DetailFragment.newInstance(note.getIdentification().getUid(),null);
        if (detail.getNote() == null || !sameSelection) {

            String notebook = null;
            String selectedNotebookName = Utils.getSelectedNotebookName(activity);
            if (selectedNotebookName != null) {
                ActiveAccount activeAccount = activeAccountRepository.getActiveAccount();
                notebook = notebookRepository.getBySummary(activeAccount.getAccount(), activeAccount.getRootFolder(), selectedNotebookName).getIdentification().getUid();
            }
            detail.setStartNotebook(notebook);

            FragmentTransaction ft = getFragmentManager(). beginTransaction();
            ft.replace(R.id.details_fragment, detail);
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            ft.commit();
        }
    }

    @Override
    public void onSelect(final Note note,final boolean sameSelection) {
        if(tabletMode){
            Fragment fragment = getFragmentManager().findFragmentById(R.id.details_fragment);
            if(fragment instanceof  DetailFragment){
                DetailFragment detail = (DetailFragment)fragment;
                boolean changes = detail.checkDifferences();

                if(changes) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(activity);

                    builder.setTitle(R.string.dialog_cancel_warning);
                    builder.setMessage(R.string.dialog_question_cancel);
                    builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            setDetailFragment(note,sameSelection);
                        }
                    });
                    builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //nothing
                        }
                    });
                    builder.show();
                }else{
                    setDetailFragment(note,sameSelection);
                }
            }else{
                setDetailFragment(note,sameSelection);
            }
        }else {
            Intent i = new Intent(activity, DetailActivity.class);
            i.putExtra(Utils.NOTE_UID, note.getIdentification().getUid());

            String selectedNotebookName = Utils.getSelectedNotebookName(activity);
            if (selectedNotebookName != null) {
                ActiveAccount activeAccount = activeAccountRepository.getActiveAccount();
                i.putExtra(Utils.NOTEBOOK_UID, notebookRepository.getBySummary(activeAccount.getAccount(), activeAccount.getRootFolder(), selectedNotebookName).getIdentification().getUid());
            }

            startActivityForResult(i, DETAIL_ACTIVITY_RESULT_CODE);
        }
    }

    public void preventBlankDisplaying(){
        this.preventBlankDisplaying = true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == DETAIL_ACTIVITY_RESULT_CODE){
            setFromDetail();
        }
    }

    public void setFromDetail(){
        fromDetailActivity = true;
    }

    @Override
    public void onResume(){
        super.onResume();

        toolbar.setBackgroundColor(getResources().getColor(R.color.theme_default_primary));
        //displayBlankFragment();

        Intent startIntent = getActivity().getIntent();
        String email = startIntent.getStringExtra(Utils.INTENT_ACCOUNT_EMAIL);
        String rootFolder = startIntent.getStringExtra(Utils.INTENT_ACCOUNT_ROOT_FOLDER);
        //if called from the widget
        String notebookUID = startIntent.getStringExtra(Utils.NOTEBOOK_UID);

        ActiveAccount activeAccount;
        if(email != null && rootFolder != null) {
            activeAccount = activeAccountRepository.switchAccount(email,rootFolder);
        }else{
            activeAccount = activeAccountRepository.getActiveAccount();
        }

        //if called from the widget
        if(notebookUID != null){
            Utils.setSelectedNotebookName(activity, notebookRepository.getByUID(activeAccount.getAccount(),activeAccount.getRootFolder(),notebookUID).getSummary());
        }

        String selectedNotebookName = Utils.getSelectedNotebookName(activity);


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

        if(initPhase){
            initPhase = false;
            return;
        }

        if(fromDetailActivity || tabletMode){
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
        private boolean changeDrawerAccount;
        private boolean resetDrawerSelection;

        AccountChangeThread(String account, String rootFolder) {
            this.account = account;
            this.rootFolder = rootFolder;
            notebookUID = null;
            changeDrawerAccount = true;
            resetDrawerSelection = false;
        }

        AccountChangeThread(ActiveAccount activeAccount) {
            this(activeAccount.getAccount(),activeAccount.getRootFolder());
            this.activeAccount = activeAccount;
        }

        AccountChangeThread(ActiveAccount activeAccount, String notebookUID) {
            this(activeAccount);
            this.notebookUID = notebookUID;
        }

        public void disableProfileChangeing(){
            changeDrawerAccount = false;
        }

        public void resetDrawerSelection(){
            this.resetDrawerSelection = true;
        }


        @Override
        public void run() {
            if(activeAccount == null) {
                activeAccount = activeAccountRepository.switchAccount(account, rootFolder);
            }

            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String name = Utils.getNameOfActiveAccount(activity, activeAccount.getAccount(), activeAccount.getRootFolder());
                    if(changeDrawerAccount){
                        final ArrayList<IProfile> profiles = mAccount.getProfiles();
                        for(IProfile profile : profiles){
                            String profileName = profile.getName() == null ? profile.getEmail() : profile.getName();
                            if(name.equals(profileName)){
                                mAccount.setActiveProfile(profile,false);
                                break;
                            }
                        }
                    }
                    toolbar.setTitle(name);
                }
            });

            List<Note> notes;
            DataCache noteCache = dataCache.getNoteCache(activeAccount);
            String selectedTagName = Utils.getSelectedTagName(activity);
            if(resetDrawerSelection || (notebookUID == null && selectedTagName == null)){
                if(resetDrawerSelection){
                    Utils.setSelectedNotebookName(activity, null);
                    Utils.setSelectedTagName(activity, null);
                }
                notes = noteCache.getNotes();
            }else if(selectedTagName != null){
                notes = notetagRepository.getNotesWith(activeAccount.getAccount(), activeAccount.getRootFolder(), selectedTagName, Utils.getNoteSorting(activity));
            }else{
                notes = noteCache.getNotesFromNotebook(notebookUID);
            }

            List<String> tags = dataCache.getTags();
            List<Notebook> notebooks = noteCache.getNotebooks();

            if(preventBlankDisplaying){
                preventBlankDisplaying = false;
            }else {
                displayBlankFragment();
            }

            getActivity().runOnUiThread(new ReloadDataThread(notebooks, notes, tags));
        }
    }



    public class ReloadDataThread extends Thread{
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
            case R.id.delete_notebook_menu:
                AlertDialog deleteNBDialog = deleteNotebookDialog();

                int selection = mDrawer.getCurrentSelection();
                final IDrawerItem drawerItem = mDrawer.getDrawerItems().get(selection);
                String tag = drawerItem.getTag() == null || drawerItem.getTag().toString().trim().length() == 0 ? null : drawerItem.getTag().toString();

                if(tag == null || !tag.equals("NOTEBOOK")){
                    Toast.makeText(activity,R.string.no_nb_selected,Toast.LENGTH_LONG).show();
                }else {
                    deleteNBDialog.show();
                }
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
            case R.id.create_sort_menu:
                AlertDialog newSortingDialog = createSortingDialog();
                newSortingDialog.show();
                break;
            default:
                activity.dispatchMenuEvent(item);
                break;
        }
        return true;
    }

    private AlertDialog deleteNotebookDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        builder.setTitle(R.string.dialog_delete_nb_warning);
        builder.setMessage(R.string.dialog_question_delete_nb);
        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int selection = mDrawer.getCurrentSelection();
                final IDrawerItem drawerItem = mDrawer.getDrawerItems().get(selection);
                ActiveAccount activeAccount = activeAccountRepository.getActiveAccount();

                BaseDrawerItem base = (BaseDrawerItem)drawerItem;
                notebookRepository.delete(activeAccount.getAccount(), activeAccount.getRootFolder(), notebookRepository.getBySummary(activeAccount.getAccount(), activeAccount.getRootFolder(),base.getName()));
                mDrawer.removeItem(selection);

                Utils.setSelectedNotebookName(activity, null);
                Utils.setSelectedTagName(activity,null);

                mDrawer.setSelection(1);

                orderDrawerItems(mDrawer, null);
            }
        });
        builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //nothing
            }
        });
        return builder.create();
    }

    private AlertDialog createSortingDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        builder.setTitle(R.string.title_dialog_sorting);

        LayoutInflater inflater = activity.getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_change_sorting, null);

        RadioGroup radioGroup = (RadioGroup) view.findViewById(R.id.radio_group_sort_direction);
        Spinner columns = (Spinner) view.findViewById(R.id.spinner_sort_column);

        NoteSorting noteSorting = Utils.getNoteSorting(activity);

        Utils.initColumnSpinner(activity,columns,R.layout.sorting_spinner_item , null, noteSorting.getColumnName());

        if(NoteSorting.Direction.DESC == noteSorting.getDirection()){
            ((RadioButton) view.findViewById(R.id.radio_desc)).toggle();
        }else{
            ((RadioButton) view.findViewById(R.id.radio_asc)).toggle();
        }

        builder.setView(view);

        builder.setPositiveButton(R.string.ok, new SortingButtonListener(columns,radioGroup));
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //nothing
            }
        });
        return builder.create();
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
                notes = notesRepository.getFromNotebook(activeAccount.getAccount(),activeAccount.getRootFolder(),notebook.getIdentification().getUid(),Utils.getNoteSorting(activity));

                Utils.setSelectedNotebookName(activity, notebook.getSummary());
                Utils.setSelectedTagName(activity,null);
            }else if("TAG".equalsIgnoreCase(tag)){
                notes = notetagRepository.getNotesWith(activeAccount.getAccount(), activeAccount.getRootFolder(), drawerItem.getName(),Utils.getNoteSorting(activity));
                Utils.setSelectedNotebookName(activity, null);
                Utils.setSelectedTagName(activity,drawerItem.getName());
            }else if("ALL_NOTES".equalsIgnoreCase(tag)){
                notes = notesRepository.getAll(Utils.getNoteSorting(activity));
                Utils.setSelectedNotebookName(activity, null);
                Utils.setSelectedTagName(activity,null);
            }else{
                notes = notesRepository.getAll(activeAccount.getAccount(),activeAccount.getRootFolder(),Utils.getNoteSorting(activity));
                Utils.setSelectedNotebookName(activity, null);
                Utils.setSelectedTagName(activity,null);
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
            mAdapter = new NoteAdapter(new ArrayList<Note>(), R.layout.row_note_overview, activity,this);
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

            String selectedNotebookName = Utils.getSelectedNotebookName(activity);
            Notebook notebook = selectedNotebookName == null ? null : notebookRepository.getBySummary(activeAccount.getAccount(), activeAccount.getRootFolder(), selectedNotebookName);

            if(notebookRepository.getAll(activeAccount.getAccount(),activeAccount.getRootFolder()).isEmpty()){
                //Create first a notebook, so that note creation is possible
                createNotebookDialog(intent).show();
            }else{

                if(tabletMode){
                    String notebookUID = null;
                    if (notebook != null) {
                        notebookUID = notebook.getIdentification().getUid();
                    }

                    DetailFragment detail = DetailFragment.newInstance(null,notebookUID);
                    FragmentTransaction ft = getFragmentManager(). beginTransaction();
                    ft.replace(R.id.details_fragment, detail);
                    ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                    ft.commit();
                }else {
                    if (notebook != null) {
                        intent.putExtra(Utils.NOTEBOOK_UID, notebook.getIdentification().getUid());
                    }
                    startActivityForResult(intent, DETAIL_ACTIVITY_RESULT_CODE);
                }
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

                displayBlankFragment();
            }
        }
    }

    public class SortingButtonListener implements DialogInterface.OnClickListener {

        private final Spinner columns;
        private final RadioGroup direction;

        public SortingButtonListener(Spinner columnSpinner, RadioGroup direction) {
            this.columns = columnSpinner;
            this.direction = direction;
        }

        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            String column = Utils.getColumnNameOfSelection(columns.getSelectedItemPosition());

            NoteSorting.Direction dir;

            if(direction.getCheckedRadioButtonId() == R.id.radio_asc){
                dir = NoteSorting.Direction.ASC;
            }else{
                dir = NoteSorting.Direction.DESC;
            }

            NoteSorting noteSorting = new NoteSorting(column,dir);

            Log.d("onClick","Changing sorting:"+ noteSorting);

            Utils.saveNoteSorting(activity, noteSorting);

            ActiveAccount activeAccount = activeAccountRepository.getActiveAccount();

            IDrawerItem drawerItem = mDrawer.getDrawerItems().get(mDrawer.getCurrentSelection());
            if(drawerItem instanceof BaseDrawerItem){
                BaseDrawerItem item = (BaseDrawerItem)drawerItem;
                String tag = item.getTag() == null || item.getTag().toString().trim().length() == 0 ? null : item.getTag().toString();

                List<Note> notes;
                if("NOTEBOOK".equalsIgnoreCase(tag)){
                    notes = notesRepository.getFromNotebook(activeAccount.getAccount(),
                            activeAccount.getRootFolder(),
                            notebookRepository.getBySummary(activeAccount.getAccount(),activeAccount.getRootFolder(),item.getName()).getIdentification().getUid(),
                            noteSorting);
                }else if("TAG".equalsIgnoreCase(tag)){
                    notes = notetagRepository.getNotesWith(activeAccount.getAccount(), activeAccount.getRootFolder(), item.getName(), noteSorting);
                }else if("ALL_NOTES".equalsIgnoreCase(tag)){
                    notes = notesRepository.getAll(noteSorting);
                }else{
                    notes = notesRepository.getAll(activeAccount.getAccount(),activeAccount.getRootFolder(), noteSorting);
                }

                List<Notebook> notebooks = notebookRepository.getAll(activeAccount.getAccount(), activeAccount.getRootFolder());
                List<String> tags = tagRepository.getAll();

                displayBlankFragment();

                getActivity().runOnUiThread(new ReloadDataThread(notebooks, notes, tags));
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
                            textField.getText().toString(),
                            Utils.getNoteSorting(activity));
                }else if("TAG".equalsIgnoreCase(tag)){
                    List<Note> unfiltered = notetagRepository.getNotesWith(activeAccount.getAccount(), activeAccount.getRootFolder(), item.getName(),Utils.getNoteSorting(activity));
                    notes = new ArrayList<Note>();
                    for(Note note : unfiltered){
                        String summary = note.getSummary().toLowerCase();
                        if(summary.contains(textField.getText().toString().toLowerCase())){
                            notes.add(note);
                        }
                    }
                }else{
                    notes = notesRepository.getFromNotebookWithSummary(activeAccount.getAccount(),activeAccount.getRootFolder(),null,textField.getText().toString(),Utils.getNoteSorting(activity));
                }

                List<Notebook> notebooks = notebookRepository.getAll(activeAccount.getAccount(), activeAccount.getRootFolder());
                List<String> tags = tagRepository.getAll();

                displayBlankFragment();

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

            Utils.setSelectedNotebookName(activity,value);

            Notebook nb = new Notebook(ident,audit, Note.Classification.PUBLIC, value);
            nb.setDescription(value);
            if(notebookRepository.insert(activeAccount.getAccount(), activeAccount.getRootFolder(), nb)) {
                mDrawer.addItem(new SecondaryDrawerItem().withName(value).withTag("NOTEBOOK"));

                orderDrawerItems(mDrawer, value);
            }

            if(intent != null){
                if(tabletMode){
                    DetailFragment detail = DetailFragment.newInstance(null,nb.getIdentification().getUid());
                    FragmentTransaction ft = getFragmentManager(). beginTransaction();
                    ft.replace(R.id.details_fragment, detail);
                    ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                    ft.commit();
                }else {

                    intent.putExtra(Utils.NOTEBOOK_UID, nb.getIdentification().getUid());
                    startActivityForResult(intent, DETAIL_ACTIVITY_RESULT_CODE);
                }
            }else{
                displayBlankFragment();
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

            String selectedNotebookName = Utils.getSelectedNotebookName(activity);
            String selectedTagName = Utils.getSelectedTagName(activity);
            if(selectedNotebookName != null){
                selected = selectedNotebookName;
                notebookSelected = true;
            }else if(selectionName != null){
                selected = selectionName;
                notebookSelected = true;
            }else if(selectedTagName != null){
                selected = selectedTagName;
                notebookSelected = false;
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
                AccountChangeThread thread = new AccountChangeThread(account,rootFolder);
                thread.disableProfileChangeing();
                thread.resetDrawerSelection();
                thread.start();
                mDrawer.setSelection(1);
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

    public boolean isTablet() {
        return (getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK)
                >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }
}
