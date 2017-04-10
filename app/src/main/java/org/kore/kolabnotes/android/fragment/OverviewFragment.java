package org.kore.kolabnotes.android.fragment;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.NotificationCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;
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

import org.kore.kolab.notes.Attachment;
import org.kore.kolab.notes.AuditInformation;
import org.kore.kolab.notes.Colors;
import org.kore.kolab.notes.Identification;
import org.kore.kolab.notes.Note;
import org.kore.kolab.notes.Notebook;
import org.kore.kolab.notes.SharedNotebook;
import org.kore.kolab.notes.Tag;
import org.kore.kolab.notes.local.LocalNotesRepository;
import org.kore.kolab.notes.v3.KolabNotesParserV3;
import org.kore.kolabnotes.android.ColorCircleDrawable;
import org.kore.kolabnotes.android.DetailActivity;
import org.kore.kolabnotes.android.MainActivity;
import org.kore.kolabnotes.android.R;
import org.kore.kolabnotes.android.TagListActivity;
import org.kore.kolabnotes.android.Utils;
import org.kore.kolabnotes.android.adapter.NoteAdapter;
import org.kore.kolabnotes.android.content.AccountIdentifier;
import org.kore.kolabnotes.android.content.ActiveAccount;
import org.kore.kolabnotes.android.content.ActiveAccountRepository;
import org.kore.kolabnotes.android.content.AttachmentRepository;
import org.kore.kolabnotes.android.content.ModificationRepository;
import org.kore.kolabnotes.android.content.NoteRepository;
import org.kore.kolabnotes.android.content.NoteSorting;
import org.kore.kolabnotes.android.content.NoteTagRepository;
import org.kore.kolabnotes.android.content.NotebookRepository;
import org.kore.kolabnotes.android.content.TagRepository;
import org.kore.kolabnotes.android.security.AuthenticatorActivity;
import org.kore.kolabnotes.android.setting.SettingsActivity;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import yuku.ambilwarna.AmbilWarnaDialog;

/**
 * Fragment which displays the notes overview and implements the logic for the overview
 */
public class OverviewFragment extends Fragment implements /*NoteAdapter.NoteSelectedListener,*/ NoteAdapter.ViewHolder.ClickListener{

    public static final int DETAIL_ACTIVITY_RESULT_CODE = 1;
    public static final int TAG_LIST_ACTIVITY_RESULT_CODE = 1;
    private static final String TAG_ACTION_MODE = "ActionMode";
    private static final String TAG_SELECTED_NOTES = "SelectedNotes";
    private static final String TAG_SELECTABLE_ADAPTER = "SelectableAdapter";
    private static final String KEY_SEARCH_QUERY = "KEY_SEARCH_QUERY";

    private final DrawerItemClickedListener drawerItemClickedListener = new DrawerItemClickedListener();


    private NoteAdapter mAdapter;
    private FloatingActionButton mFabButton;
    private RecyclerView mRecyclerView;
    private TextView mEmptyView;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private SearchView mSearchView;
    private String mSearchKeyWord;
    private Snackbar mSnackbarDelete;

    private ActionMode mActionMode;
    private ActionModeCallback mActionModeCallback = new ActionModeCallback();
    private boolean isInActionMode = false;
    private HashMap<Integer, String> mSelectedNotes = new HashMap<Integer, String>();

    private AccountManager mAccountManager;

    private NoteRepository notesRepository;
    private NotebookRepository notebookRepository;
    private TagRepository tagRepository;
    private NoteTagRepository notetagRepository;
    private ActiveAccountRepository activeAccountRepository;
    private AttachmentRepository attachmentRepository;
    private ModificationRepository modificationRepository;
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

        notesRepository = new NoteRepository(activity);
        notebookRepository = new NotebookRepository(activity);
        tagRepository = new TagRepository(activity);
        notetagRepository = new NoteTagRepository(activity);
        activeAccountRepository = new ActiveAccountRepository(activity);
        attachmentRepository = new AttachmentRepository(activity);
        modificationRepository = new ModificationRepository(activity);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        initPhase = true;
        // Handle Toolbar
        toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);
        activity.setSupportActionBar(toolbar);
        activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        tabletMode = Utils.isTablet(getResources());

        setHasOptionsMenu(true);

        mAccountManager = AccountManager.get(activity);
        Account[] accounts = mAccountManager.getAccountsByType(AuthenticatorActivity.ARG_ACCOUNT_TYPE);

        Set<AccountIdentifier> allAccounts = activeAccountRepository.getAllAccounts();

        if(allAccounts.size() == 0){
            allAccounts = activeAccountRepository.initAccounts();
        }

        //For accounts cleanup
        Set<AccountIdentifier> accountsForDeletion = new LinkedHashSet<>(allAccounts);
        accountsForDeletion.remove(new AccountIdentifier("local","Notes"));

        ProfileDrawerItem[] profiles = new ProfileDrawerItem[accounts.length+1];
        profiles[0] = new ProfileDrawerItem().withName(getResources().getString(R.string.drawer_account_local)).withTag("Notes").withIcon(getResources().getDrawable(R.drawable.ic_local_account));

        for(int i=0;i<accounts.length;i++) {
            String email = mAccountManager.getUserData(accounts[i],AuthenticatorActivity.KEY_EMAIL);
            String name = mAccountManager.getUserData(accounts[i],AuthenticatorActivity.KEY_ACCOUNT_NAME);
            String rootFolder = mAccountManager.getUserData(accounts[i],AuthenticatorActivity.KEY_ROOT_FOLDER);
            String accountType = mAccountManager.getUserData(accounts[i], AuthenticatorActivity.KEY_ACCOUNT_TYPE);

            ProfileDrawerItem item = new ProfileDrawerItem().withName(name).withTag(rootFolder).withEmail(email);

            if(accountType != null) {
                int type = Integer.parseInt(accountType);

                if(type == AuthenticatorActivity.ID_ACCOUNT_TYPE_KOLABNOW){
                    item.withIcon(getResources().getDrawable(R.drawable.ic_kolabnow));
                }else if(type == AuthenticatorActivity.ID_ACCOUNT_TYPE_KOLAB){
                    item.withIcon(getResources().getDrawable(R.drawable.ic_kolab));
                }else{
                    item.withIcon(getResources().getDrawable(R.drawable.ic_imap));
                }
            }

            //GitHub issue 47
            item.setNameShown(true);
            if(name != null && name.equals(email)){
                item.setNameShown(false);
                item.withName(null);
            }

            profiles[i+1] = item;

            accountsForDeletion.remove(new AccountIdentifier(email,rootFolder));
        }

        cleanupAccounts(accountsForDeletion);


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
        mFabButton = (FloatingActionButton) getActivity().findViewById(R.id.fab_button);
        //mFabButton.setImageDrawable(new IconicsDrawable(this, FontAwesome.Icon.faw_upload).color(Color.WHITE).actionBarSize());
        Utils.configureFab(mFabButton);
        mFabButton.setOnClickListener(new CreateButtonListener());

        mRecyclerView = (RecyclerView) activity.findViewById(R.id.list);
        mEmptyView = (TextView) activity.findViewById(R.id.empty_view_overview);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(activity));
        //mRecyclerView.setItemAnimator(new CustomItemAnimator());
        //mRecyclerView.setItemAnimator(new ReboundItemAnimator());
        final ActiveAccount activeAccount = activeAccountRepository.getActiveAccount();

        mAdapter = new NoteAdapter(new ArrayList<Note>(), R.layout.row_note_overview, activity, this, attachmentRepository.getNoteIDsWithAttachments(activeAccount.getAccount(),activeAccount.getRootFolder()));
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

                            //issue 154
                            mDrawer.setSelection(1);
                        }
                    });
                }
            }
        });

        new InitializeApplicationsTask().execute();

        if (savedInstanceState != null) {
            mSelectedNotes = (HashMap<Integer, String>)savedInstanceState.getSerializable(TAG_SELECTED_NOTES);
            if (savedInstanceState.getBoolean(TAG_ACTION_MODE, false)){
                mActionMode = activity.startActionMode(mActionModeCallback);
                mAdapter.setSelectedItems(savedInstanceState.getIntegerArrayList(TAG_SELECTABLE_ADAPTER));
                mActionMode.setTitle(String.valueOf(mAdapter.getSelectedItemCount()));
            }
        }

        //show progress
        mRecyclerView.setVisibility(View.GONE);

        setListState();
    }

    private void cleanupAccounts(Set<AccountIdentifier> accountsForDeletion){
        Thread cleanupThread = new Thread(new AccountsCleaner(accountsForDeletion));
        cleanupThread.start();
    }

    final class AccountsCleaner implements Runnable{

        private final Set<AccountIdentifier> accountsForDeletion;

        public  AccountsCleaner(Set<AccountIdentifier> accountsForDeletion){
            this.accountsForDeletion = accountsForDeletion;
        }

        @Override
        public void run() {
            for(AccountIdentifier identifier : accountsForDeletion){
                String email = identifier.getAccount();
                String rootFolder = identifier.getRootFolder();
                activeAccountRepository.deleteAccount(identifier.getAccount(),identifier.getRootFolder());

                notesRepository.cleanAccount(email, rootFolder);
                notetagRepository.cleanAccount(email,rootFolder);
                tagRepository.cleanAccount(email,rootFolder);
                attachmentRepository.cleanAccount(email, rootFolder);
                modificationRepository.cleanAccount(email,rootFolder);

                Log.d("AccountsCleaner","Cleaned account:"+identifier);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mSnackbarDelete != null) {
            mSnackbarDelete.dismiss();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putIntegerArrayList(TAG_SELECTABLE_ADAPTER, mAdapter.getSelectedItems());
        outState.putSerializable(TAG_SELECTED_NOTES, mSelectedNotes);
        outState.putBoolean(TAG_ACTION_MODE, isInActionMode);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onItemClicked(int position, Note note) {
        if (mActionMode == null) {
            boolean same = false;
            select(note, same);
        } else {
            toggleSelection(position);
            mSelectedNotes.put(position, note.getIdentification().getUid());
        }
    }

    @Override
    public boolean onItemLongClicked(int position, Note note) {
        if (mActionMode == null) {
            mActionMode = activity.startActionMode(mActionModeCallback);

            //display blank fragment, because the shown display is not representative for all selected notes
            displayBlankFragment();
        }
        toggleSelection(position);
        mSelectedNotes.put(position, note.getIdentification().getUid());

        return true;
    }

    private void toggleSelection(int position) {
        mAdapter.toggleSelection(position);
        int count = mAdapter.getSelectedItemCount();

        if (count == 0) {
            mActionMode.finish();
        } else {
            mActionMode.setTitle(String.valueOf(count));
            mActionMode.invalidate();
        }
    }

    public void select(final Note note,final boolean sameSelection) {
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
                            setDetailFragment(note, sameSelection);
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

    public Context getContext(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            return super.getContext();
        }
        return activity;
    }

    private class ActionModeCallback implements ActionMode.Callback {
        @SuppressWarnings("unused")
        private final String TAG = ActionModeCallback.class.getSimpleName();

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.row_note_context, menu);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                activity.getWindow().setStatusBarColor(ContextCompat.getColor(getContext(), R.color.theme_actionmode_dark));
                activity.getWindow().setNavigationBarColor(ContextCompat.getColor(getContext(), R.color.theme_actionmode));
            }
            isInActionMode = true;
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            List<Integer> items = mAdapter.getSelectedItems();
            switch (item.getItemId()) {
                case R.id.delete_menu_context:
                    deleteNotes(items);
                    mode.finish();
                    break;
                case R.id.edit_tag_menu_context:
                    editTags(items);
                    mode.finish();
                    break;
                case R.id.colorpicker_context:
                    chooseColor(items);
                    mode.finish();
                    break;
                case R.id.move_context:
                    moveNotes(items);
                    mode.finish();
            }
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mAdapter.clearSelection();
            mActionMode = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                activity.getWindow().setStatusBarColor(ContextCompat.getColor(getContext(), R.color.theme_default_primary_dark));
                activity.getWindow().setNavigationBarColor(ContextCompat.getColor(getContext(), R.color.md_black_1000));
            }
            isInActionMode = false;
        }
    }

    void deleteNotes(final List<Integer> items) {
        if (items != null) {
            final String account = activeAccountRepository.getActiveAccount().getAccount();
            final String rootFolder = activeAccountRepository.getActiveAccount().getRootFolder();

            final ArrayList<Note> notes = new ArrayList<Note>();
            for (int position : items) {
                final String uid = mSelectedNotes.get(position);
                final Note note = notesRepository.getByUIDWithoutDescription(account, rootFolder, uid);
                notes.add(note);
            }
            mAdapter.deleteNotes(notes);
            setListState();
            mSelectedNotes.clear();

            if (mSnackbarDelete != null) {
                mSnackbarDelete.dismiss();
            }

            mSnackbarDelete = Snackbar.make(activity.findViewById(R.id.coordinator_overview), R.string.snackbar_delete_message, Snackbar.LENGTH_LONG)
                    .setCallback(new Snackbar.Callback() {
                        @Override
                        public void onDismissed(Snackbar snackbar, int event) {
                            switch(event) {
                        /* If undo button pressed */
                                case Snackbar.Callback.DISMISS_EVENT_ACTION:
                                    mAdapter.clearNotes();
                                    mAdapter.addNotes(notesRepository.getAll(account, rootFolder, Utils.getNoteSorting(getActivity())));
                                    setListState();
                                    break;
                                default:
                                    for (Note note : notes) {
                                        if (note != null) {
                                            Notebook book = checkModificationPermissionInCurrentBook(account, rootFolder,
                                                    note.getIdentification().getUid());
                                            if (book == null) continue;
                                            notesRepository.delete(account, rootFolder, note);
                                        }
                                    }
                                    reloadData();
                                    Utils.setSelectedTagName(activity,null);
                                    Utils.setSelectedNotebookName(activity, null);
                                    if (tabletMode) {
                                        displayBlankFragment();
                                    }
                                    break;
                            }
                        }
                    }).setAction(R.string.snackbar_undo_delete, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                    /* Nothing */
                        }
                    });

            mSnackbarDelete.show();
        }
    }

    void editTags(final List<Integer> items) {
        if (items != null) {
            final String account = activeAccountRepository.getActiveAccount().getAccount();
            final String rootFolder = activeAccountRepository.getActiveAccount().getRootFolder();

            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle(R.string.dialog_change_tags);

            Map<String,Tag> allTags = new HashMap<>();
            allTags.putAll(tagRepository.getAllAsMap(account, rootFolder));
            final Set<String> tagNames = allTags.keySet();
            final String[] tagArr = tagNames.toArray(new String[tagNames.size()]);

            Arrays.sort(tagArr);

            final boolean[] selectionArr = new boolean[tagArr.length];
            final Map<Integer, Integer> selectedItems = new HashMap<Integer, Integer>();
            final Set<String> selectedTags = new LinkedHashSet<>();

            for (int position : items) {
                final String uid = mSelectedNotes.get(position);
                final Note note = notesRepository.getByUID(account, rootFolder, uid);

                if (note != null) {
                    for (Tag tag : note.getCategories()) {
                        selectedTags.add(tag.getName());
                    }

                    for(int i = 0; i < tagArr.length; i++){
                        if(selectedTags.contains(tagArr[i])){
                            selectionArr[i] = true;
                            selectedItems.put(i, i);
                        }
                    }
                }
            }
            builder.setMultiChoiceItems(tagArr, selectionArr,
                    new DialogInterface.OnMultiChoiceClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int indexSelected,
                                            boolean isChecked) {
                            if (isChecked) {
                                // If the user checked the item, add it to the selected items
                                selectedItems.put(indexSelected, indexSelected);
                            } else if (selectedItems.containsKey(indexSelected)) {
                                // Else, if the item is already in the array, remove it
                                selectedItems.remove(indexSelected);
                            }
                        }
                    })
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            selectedTags.clear();
                            for (Map.Entry<Integer, Integer> item : selectedItems.entrySet()) {
                                selectedTags.add(tagArr[item.getKey()]);
                            }

                            NoteTagRepository noteTagRepository = new NoteTagRepository(activity);
                            for (int position : items) {
                                final String uid = mSelectedNotes.get(position);
                                final Note note = notesRepository.getByUID(account, rootFolder, uid);
                                if (note != null) {
                                    noteTagRepository.delete(account, rootFolder, uid);
                                    for (String tag : selectedTags) {
                                        noteTagRepository.insert(account, rootFolder, uid, tag);
                                    }
                                    updateModificationDate(note, account, rootFolder);
                                }
                            }
                            mSelectedNotes.clear();
                            Utils.setSelectedTagName(activity,null);
                            Utils.setSelectedNotebookName(activity, null);
                            reloadData();
                        }
                    })
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            // nothing

                        }
                    });

            builder.show();
        }
    }

    void chooseColor(final List<Integer> items) {
        if (items != null) {
            final int initialColor = Color.WHITE;

            AmbilWarnaDialog dialog = new AmbilWarnaDialog(activity, initialColor, true, new AmbilWarnaDialog.OnAmbilWarnaListener() {
                @Override
                public void onOk(AmbilWarnaDialog dialog, int color) {
                    setColor(items, Colors.getColor(String.format("#%06X", (0xFFFFFF & color))));
                }

                @Override
                public void onRemove(AmbilWarnaDialog dialog) {
                    setColor(items, null);
                }

                @Override
                public void onCancel(AmbilWarnaDialog dialog) {
                    // do nothing
                }
            });
            dialog.show();
        }
    }

    void setColor(final List<Integer> items, org.kore.kolab.notes.Color color) {
        for (int position : items) {
            final String account = activeAccountRepository.getActiveAccount().getAccount();
            final String rootFolder = activeAccountRepository.getActiveAccount().getRootFolder();

            final String uid = mSelectedNotes.get(position);
            final Note note = notesRepository.getByUID(account, rootFolder, uid);

            if (note != null) {
                Notebook book = checkModificationPermissionInCurrentBook(account, rootFolder, uid);
                if (book == null) continue;

                note.setColor(color);

                notesRepository.update(account, rootFolder, note, book.getIdentification().getUid());
                updateModificationDate(note, account, rootFolder);
            }

        }
        mSelectedNotes.clear();
        Utils.setSelectedTagName(activity, null);
        Utils.setSelectedNotebookName(activity,null);
        reloadData();
    }

    @Nullable
    private Notebook checkModificationPermissionInCurrentBook(String account, String rootFolder, String uid) {
        Notebook book = notebookRepository.getByUID(account, rootFolder, notesRepository
                .getUIDofNotebook(account, rootFolder, uid));

        if (book.isShared()) {
            if (!((SharedNotebook) book).isNoteModificationAllowed()) {
                Toast.makeText(activity, R.string.no_change_permissions, Toast.LENGTH_LONG).show();
                return null;
            }
        }
        return book;
    }

    void moveNotes(final List<Integer> items) {
        if (items != null) {
            final ActiveAccount activeAccount = activeAccountRepository.getActiveAccount();
            final String account = activeAccount.getAccount();
            final String rootFolder = activeAccount.getRootFolder();

            final int[] position = {-1};
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle(R.string.move_note);

            List<Notebook> books = notebookRepository.getAll(account, rootFolder);
            String[] booksSummary = new String[books.size()];
            for (int i = 0; i < books.size(); i++) {
                booksSummary[i] = books.get(i).getSummary();
            }

            final ArrayAdapter<String> adapter = new ArrayAdapter<String>(activity, android.R.layout.simple_list_item_single_choice, booksSummary);

            builder.setSingleChoiceItems(adapter, -1, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    position[0] = which;
                }
            }).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (position[0] != -1) {
                        String notebookName = adapter.getItem(position[0]);
                        Notebook book = notebookRepository.getBySummary(account, rootFolder, notebookName);
                        for (Integer position : items) {
                            final String uid = mSelectedNotes.get(position);
                            final Note note = notesRepository.getByUID(account, rootFolder, uid);
                            if (note != null) {
                                if(Utils.checkNotebookPermissions(activity,activeAccount,note,book)){
                                    Toast.makeText(activity, R.string.no_change_permissions, Toast.LENGTH_LONG).show();
                                    continue;
                                }

                                notesRepository.update(account, rootFolder, note, book.getIdentification().getUid());
                                updateModificationDate(note, account, rootFolder);
                            }
                        }
                        mSelectedNotes.clear();
                        Utils.setSelectedTagName(activity,null);
                        Utils.setSelectedNotebookName(activity, null);
                        reloadData();
                    }
                }
            }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                /* Nothing */
                }
            });

            builder.show();
        }
    }

    void updateModificationDate(Note note, final String account, final String rootFolder) {
        note.getAuditInformation().setLastModificationDate(System.currentTimeMillis());

        Notebook book = notebookRepository.getByUID(account, rootFolder, notesRepository
                        .getUIDofNotebook(account, rootFolder, note.getIdentification().getUid()));
        notesRepository.update(account, rootFolder, note, book.getIdentification().getUid());
    }

    public void openDrawer(){
        mDrawer.openDrawer();
    }

    public void displayBlankFragment(){
        Log.d("displayBlankFragment", "tabletMode:" + tabletMode);
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

    public void preventBlankDisplaying(){
        this.preventBlankDisplaying = true;

        if(mAdapter != null && mRecyclerView != null){
            mAdapter.restoreElevation(mRecyclerView);
        }
    }

    @Override
    public void onResume(){
        super.onResume();

        // Resume the search view with the last keyword
        if(mSearchView != null) {
            mSearchView.post(new Runnable() {
                @Override
                public void run() {
                    mSearchView.setQuery(mSearchKeyWord, true);
                }
            });
        }

        toolbar.setNavigationIcon(R.drawable.drawer_icon);
        toolbar.setBackgroundColor(getResources().getColor(R.color.theme_default_primary));
        Utils.setToolbarTextAndIconColor(activity, toolbar,true);
        //displayBlankFragment();

        Intent startIntent = getActivity().getIntent();
        String email = startIntent.getStringExtra(Utils.INTENT_ACCOUNT_EMAIL);
        String rootFolder = startIntent.getStringExtra(Utils.INTENT_ACCOUNT_ROOT_FOLDER);
        //if called from the widget
        String notebookName = startIntent.getStringExtra(Utils.SELECTED_NOTEBOOK_NAME);
        String tagName = startIntent.getStringExtra(Utils.SELECTED_TAG_NAME);

        ActiveAccount activeAccount;
        if(email != null && rootFolder != null) {
            activeAccount = activeAccountRepository.switchAccount(email,rootFolder);

            //remove the values because if one selects an other account and then goes into detail an then back, the values will be present, in phone mode
            startIntent.removeExtra(Utils.INTENT_ACCOUNT_EMAIL);
            startIntent.removeExtra(Utils.INTENT_ACCOUNT_ROOT_FOLDER);
        }else{
            activeAccount = activeAccountRepository.getActiveAccount();
        }

        //if called from the widget
        if(notebookName != null){
            Utils.setSelectedNotebookName(activity, notebookName);
            Utils.setSelectedTagName(activity,null);
        }else if(tagName != null){
            Utils.setSelectedNotebookName(activity, null);
            Utils.setSelectedTagName(activity,tagName);
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

        if(selectedNotebookName != null) {
            Notebook nb = notebookRepository.getBySummary(activeAccount.getAccount(), activeAccount.getRootFolder(), selectedNotebookName);

            //GitHub Issue 31
            if (nb != null) {
                notebookName = nb.getIdentification().getUid();
            }
        }

        //Refresh the loaded data because it could be that something changed, after coming back from detail activity
        new AccountChangeThread(activeAccount,notebookName).run();
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
            String selectedTagName = Utils.getSelectedTagName(activity);
            if(resetDrawerSelection || (notebookUID == null && selectedTagName == null)){
                if(resetDrawerSelection){
                    Utils.setSelectedNotebookName(activity, null);
                    Utils.setSelectedTagName(activity, null);
                }
                notes = notesRepository.getAll(activeAccount.getAccount(),activeAccount.getRootFolder(),Utils.getNoteSorting(getActivity()));
            }else if(selectedTagName != null){
                notes = notetagRepository.getNotesWith(activeAccount.getAccount(), activeAccount.getRootFolder(), selectedTagName, Utils.getNoteSorting(activity));
            }else{
                notes = notesRepository.getFromNotebook(activeAccount.getAccount(),activeAccount.getRootFolder(),notebookUID,Utils.getNoteSorting(getActivity()));
            }

            Map<String,Tag> tags = tagRepository.getAllAsMap(activeAccount.getAccount(), activeAccount.getRootFolder());
            List<Notebook> notebooks = notebookRepository.getAll(activeAccount.getAccount(),activeAccount.getRootFolder());

            if(preventBlankDisplaying) {
                preventBlankDisplaying = false;
            } else if(getFragmentManager().findFragmentById(R.id.details_fragment) == null) {
                displayBlankFragment();
            }

            getActivity().runOnUiThread(new ReloadDataThread(notebooks, notes, tags));
        }
    }



    public class ReloadDataThread extends Thread{
        private final List<Notebook> notebooks;
        private final List<Note> notes;
        private final Map<String,Tag> tags;

        ReloadDataThread(List<Notebook> notebooks, List<Note> notes, Map<String,Tag> tags) {
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

        // Create the search view
        mSearchView = (SearchView) MenuItemCompat.getActionView(menu.findItem(R.id
            .action_search));
        setUpSearchView(mSearchView);
    }

    /**
     * Set up the search view with OnQueryTextListener()
     *
     * @param searchView the search view which need to be set up
     */
    private void setUpSearchView(final SearchView searchView) {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchNotes(query);
                // Submit the search will hide the keyboard
                searchView.clearFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // Apply the filter when the text is changing
                searchNotes(newText);
                return true;
            }
        });
        searchView.setQueryHint(getString(R.string.dialog_input_text_search_hint));
    }

    /**
     * This function searches all the note that fit the key word, at the moment, the query only
     * apply for the note summary, but it can be expanded to filter more. The notes from all
     * notebooks which matched the query will be update to the view for the user.
     *
     * @param keyWord input keyword to apply for the search
     */
    private void searchNotes(String keyWord) {
        ActiveAccount activeAccount = activeAccountRepository.getActiveAccount();
        List<Note> notes = notesRepository.searchNotes(activeAccount
                .getAccount(),
            activeAccount.getRootFolder(), keyWord, Utils.getNoteSorting(activity));

        // Update the search view with the result notes
        mAdapter.clearNotes();
        mAdapter.addNotes(notes);

        // Save the last keyword for restoring
        mSearchKeyWord = keyWord;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            menu.findItem(R.id.export_menu).setVisible(false);
            menu.findItem(R.id.import_menu).setVisible(false);
        }
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
            case R.id.tag_list:
                Intent i = new Intent(activity, TagListActivity.class);
                startActivityForResult(i, TAG_LIST_ACTIVITY_RESULT_CODE);
                break;
           case R.id.settings_menu:
                Intent settingsIntent = new Intent(activity,SettingsActivity.class);
                startActivity(settingsIntent);
                break;
            case R.id.import_menu:
                importNotebook();
                break;
            case R.id.export_menu:
                exportNotebooks();
                break;
            default:
                activity.dispatchMenuEvent(item);
                break;
        }
        return true;
    }

    private void importNotebook(){
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);

        intent.addCategory(Intent.CATEGORY_OPENABLE);

        intent.setType("application/zip");

        startActivityForResult(intent, Utils.READ_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {

        if (requestCode == Utils.READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (resultData != null) {

                try{
                    Toast.makeText(getActivity(), getActivity().getResources().getString(R.string.importing), Toast.LENGTH_SHORT).show();
                    Uri uri = resultData.getData();
                    String path = uri.getPath();

                    ActiveAccount activeAccount = activeAccountRepository.getActiveAccount();

                    Cursor cursor = getActivity().getContentResolver().query(uri, null, null, null, null, null);

                    String notebookName;

                    if (cursor != null && cursor.moveToFirst()) {
                        notebookName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));

                        notebookName = withouFileEnding(notebookName);
                    } else {

                        notebookName = withouFileEnding(path.substring(path.lastIndexOf("/") + 1));
                    }
					if (cursor != null) {
						cursor.close();
					}

                    InputStream inputStream = getActivity().getContentResolver().openInputStream(uri);

                    new ImportNotebook(getActivity(),inputStream).execute(activeAccount.getAccount(),activeAccount.getRootFolder(),notebookName);

                }catch (FileNotFoundException e){
                    Log.e("result", e.getMessage(), e);
                    NotificationManager notificationManager = (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);

                    final Notification notification = new NotificationCompat.Builder(getActivity())
                            .setSmallIcon(R.drawable.ic_kolabnotes_breeze)
                            .setContentTitle(getActivity().getResources().getString(R.string.export_canceled))
                            .setStyle(new NotificationCompat.BigTextStyle().bigText(e.getMessage()))
                            .setAutoCancel(true).build();

                    notificationManager.notify(Utils.WRITE_REQUEST_CODE, notification);
                }
            }
        }else if(requestCode == Utils.WRITE_REQUEST_CODE && resultCode == Activity.RESULT_OK){
            try {
                Uri uri = resultData.getData();
                String path = uri.getPath();

                ActiveAccount activeAccount = activeAccountRepository.getActiveAccount();

                Cursor cursor = getActivity().getContentResolver().query(uri, null, null, null, null, null);

                String notebookName;

                if (cursor != null && cursor.moveToFirst()) {
                    notebookName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));

                    notebookName = withouFileEnding(notebookName);
                } else {

                    notebookName = withouFileEnding(path.substring(path.lastIndexOf("/") + 1));
                }
				if (cursor != null) {
					cursor.close();
				}

                ParcelFileDescriptor pfd = getActivity().getContentResolver().openFileDescriptor(uri, "w");
                FileOutputStream fileOutputStream = new FileOutputStream(pfd.getFileDescriptor());

                new ExportNotebook(getActivity(), uri, fileOutputStream).execute(activeAccount.getAccount(), activeAccount.getRootFolder(), notebookName);
            }catch (FileNotFoundException e){
                Log.e("result", e.getMessage(), e);
                NotificationManager notificationManager = (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);

                final Notification notification = new NotificationCompat.Builder(getActivity())
                        .setSmallIcon(R.drawable.ic_kolabnotes_breeze)
                        .setContentTitle(getActivity().getResources().getString(R.string.export_canceled))
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(e.getMessage()))
                        .setAutoCancel(true).build();

                notificationManager.notify(Utils.WRITE_REQUEST_CODE, notification);
            }
        }
    }

    @NonNull
    private String withouFileEnding(String notebookName) {
        //if there already existed a file with that name
        if(notebookName.lastIndexOf('(') > 0){
            notebookName = notebookName.substring(0, notebookName.lastIndexOf('('));
        }else if (notebookName.endsWith(".zip") || notebookName.endsWith(".ZIP")) {
            notebookName = notebookName.substring(0, notebookName.length() - 4);
        }
        return notebookName;
    }

    class ImportNotebook extends AsyncTask<String, Void, String>{

        private final Context context;
        private final InputStream pathToZip;

        ImportNotebook(Context context, InputStream zip){
            this.context = context;
            this.pathToZip = zip;
        }


        @Override
        protected String doInBackground(String... params) {
            try {
                Log.d("import", Arrays.toString(params));
                LocalNotesRepository repo = new LocalNotesRepository(new KolabNotesParserV3(), "tmp");


                Notebook notebook = repo.importNotebook(params[2],new KolabNotesParserV3(), pathToZip);

                Notebook bySummary = notebookRepository.getBySummary(params[0], params[1], notebook.getSummary());
                if(bySummary == null){
                    notebookRepository.insert(params[0],params[1], notebook);
                    bySummary = notebook;
                }

                for(Note note : notebook.getNotes()){
                    Note byUID = notesRepository.getByUID(params[0], params[1], note.getIdentification().getUid());

                    if(byUID == null){
                        notesRepository.insert(params[0],params[1],note, bySummary.getIdentification().getUid());
                    }
                }

                return notebook.getSummary();
            } catch (Exception e) {
                Log.e("import", e.getMessage(),e);
                cancel(false);
                return params[0] +"/" + params[1] +"/"+ params[2] +"/"+e.getMessage();
            }
        }

        @Override
        protected void onCancelled(String s) {
            super.onCancelled(s);
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            final Notification notification = new NotificationCompat.Builder(context)
                    .setSmallIcon(R.drawable.ic_kolabnotes_breeze)
                    .setContentTitle(context.getResources().getString(R.string.import_canceled))
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(s))
                    .setAutoCancel(true).build();

            notificationManager.notify(0, notification);
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            final Notification notification = new NotificationCompat.Builder(context)
                    .setSmallIcon(R.drawable.ic_kolabnotes_breeze)
                    .setContentTitle(context.getResources().getString(R.string.imported))
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(s))
                    .setAutoCancel(true).build();

            notificationManager.notify(0, notification);

            reloadData();
        }
    }

    private void exportNotebooks(){

        Toast.makeText(getActivity(), getActivity().getResources().getString(R.string.exporting), Toast.LENGTH_SHORT).show();
        int selection = mDrawer.getCurrentSelection();
        final IDrawerItem drawerItem = mDrawer.getDrawerItems().get(selection);
        String tag = drawerItem.getTag() == null || drawerItem.getTag().toString().trim().length() == 0 ? null : drawerItem.getTag().toString();
        ActiveAccount activeAccount = activeAccountRepository.getActiveAccount();
        if(tag == null || !tag.equals("NOTEBOOK")){
            List<Notebook> all = notebookRepository.getAll(activeAccount.getAccount(), activeAccount.getRootFolder());

            for(Notebook book : all){

                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);

                intent.addCategory(Intent.CATEGORY_OPENABLE);

                intent.setType("application/zip");
                intent.putExtra(Intent.EXTRA_TITLE, book.getSummary());
                startActivityForResult(intent, Utils.WRITE_REQUEST_CODE);
            }
        }else{
            BaseDrawerItem base = (BaseDrawerItem)drawerItem;

            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);

            intent.addCategory(Intent.CATEGORY_OPENABLE);

            intent.setType("application/zip");
            intent.putExtra(Intent.EXTRA_TITLE, base.getName()+".zip");
            startActivityForResult(intent, Utils.WRITE_REQUEST_CODE);
        }
    }

    class ExportNotebook extends AsyncTask<String, Void, String>{

        private final Context context;
        private final OutputStream pathToZIP;
        private final Uri fileUri;
        private final Random random;

        ExportNotebook(Context context, Uri fileUri, OutputStream pathToZIP){
            this.context = context;
            this.pathToZIP = pathToZIP;
            random = new Random();
            this.fileUri = fileUri;
        }


        @Override
        protected String doInBackground(String... params) {
            try {
                Log.d("export", Arrays.toString(params));
                Notebook notebook = notebookRepository.getBySummary(params[0], params[1], params[2]);
                List<Note> fromNotebook = notesRepository.getFromNotebookWithDescriptionLoaded(params[0], params[1], notebook.getIdentification().getUid(), new NoteSorting());

                for(Note note : fromNotebook){
                    notebook.addNote(note);
                }

                LocalNotesRepository repository = new LocalNotesRepository(new KolabNotesParserV3(), "tmp");


                repository.exportNotebook(notebook, new KolabNotesParserV3(), pathToZIP);

                Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                intent.setData(fileUri);
                context.sendBroadcast(intent);

                return fileUri.toString();
            } catch (Exception e) {
                Log.e("export",e.getMessage(),e);
                cancel(false);
                return params[0] +"/" + params[1] +"/"+ params[2] +"/"+  e.getMessage();
            }
        }

        @Override
        protected void onCancelled(String s) {
            super.onCancelled(s);
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            final Notification notification = new NotificationCompat.Builder(context)
                    .setSmallIcon(R.drawable.ic_kolabnotes_breeze)
                    .setContentTitle(context.getResources().getString(R.string.export_canceled))
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(s))
                    .setAutoCancel(true).build();

            notificationManager.notify(random.nextInt(), notification);
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            final Notification notification = new NotificationCompat.Builder(context)
                    .setSmallIcon(R.drawable.ic_kolabnotes_breeze)
                    .setContentTitle(context.getResources().getString(R.string.exported))
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(s))
                    .setAutoCancel(true).build();

            notificationManager.notify(random.nextInt(), notification);
            reloadData();
        }
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

                orderDrawerItems(tagRepository.getAllAsMap(activeAccount.getAccount(),activeAccount.getRootFolder()), mDrawer, null);
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

    public Drawer getDrawer(){
        return mDrawer;
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

                String summary = notebook.getSummary();

                if(notebook.isShared()){
                    summary = ((SharedNotebook)notebook).getShortName();
                }

                Utils.setSelectedNotebookName(activity, summary);
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

            Intent startIntent = getActivity().getIntent();
            String email = startIntent.getStringExtra(Utils.INTENT_ACCOUNT_EMAIL);
            String rootFolder = startIntent.getStringExtra(Utils.INTENT_ACCOUNT_ROOT_FOLDER);

            ActiveAccount activeAccount;
            if(email != null && rootFolder != null){
                activeAccount = activeAccountRepository.switchAccount(email,rootFolder);
            }else{
                activeAccount = activeAccountRepository.getActiveAccount();
            }

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

    private SecondaryDrawerItem createTagItem(Tag tag){
        final SecondaryDrawerItem item = new SecondaryDrawerItem().withName(tag.getName()).withTag("TAG");
        item.withTextColorRes(R.color.abc_primary_text_material_light);
        Drawable circle = new ColorCircleDrawable(Color.WHITE, R.color.theme_selected_notes);
        if(tag.getColor() != null){
            final int color = Color.parseColor(tag.getColor().getHexcode());
            circle.setColorFilter(color, PorterDuff.Mode.MULTIPLY);
            //item.setTextColor(color);
            //item.withBadgeStyle(new BadgeStyle(color,color).withTextColor(color));
        }
        item.setIcon(circle);

        return item;
    }

    final SecondaryDrawerItem createNotebookForDrawer(Notebook notebook){
        String summary = notebook.getSummary();

        SecondaryDrawerItem drawerItem = new SecondaryDrawerItem();

        if(notebook.isShared()){
            SharedNotebook shared =((SharedNotebook) notebook);
            summary = shared.getShortName();

            if(shared.isNoteCreationAllowed() && !shared.isNoteModificationAllowed()){
                drawerItem.withBadgeBackgroundResource(R.drawable.ic_note_add_black_24dp).withBadge("   ");
            }else if(!shared.isNoteCreationAllowed() && !shared.isNoteModificationAllowed()){
                drawerItem.withBadgeBackgroundResource(R.drawable.ic_lock_black_24dp).withBadge("   ");
            }else if(!shared.isNoteCreationAllowed() && shared.isNoteModificationAllowed()){
                drawerItem.withBadgeBackgroundResource(R.drawable.ic_create_black_24dp).withBadge("   ");
            }

        }

        drawerItem.withName(summary).withTag("NOTEBOOK");

        return drawerItem;
    }

    final synchronized void reloadData(List<Notebook> notebooks, List<Note> notes, Map<String,Tag> tags){
        mDrawer.getDrawerItems().clear();

        addDrawerStandardItems(mDrawer);
        //Query the tags
        for (Tag tag : tags.values()) {
            mDrawer.getDrawerItems().add(createTagItem(tag));
        }

        //Query the notebooks
        for (Notebook notebook : notebooks) {

            mDrawer.getDrawerItems().add(createNotebookForDrawer(notebook));
        }

        orderDrawerItems(tags, mDrawer);

        if(mAdapter == null){
            final ActiveAccount activeAccount = activeAccountRepository.getActiveAccount();
            mAdapter = new NoteAdapter(new ArrayList<Note>(), R.layout.row_note_overview, activity, this, attachmentRepository.getNoteIDsWithAttachments(activeAccount.getAccount(),activeAccount.getRootFolder()));
        }else{
            final ActiveAccount activeAccount = activeAccountRepository.getActiveAccount();
            mAdapter.setNotesWithAttachment(attachmentRepository.getNoteIDsWithAttachments(activeAccount.getAccount(),activeAccount.getRootFolder()));
        }



        mAdapter.clearNotes();
        if(notes.size() == 0){
            mAdapter.notifyDataSetChanged();
        }else {
            mAdapter.addNotes(notes);
        }
        setListState();
    }

    private void setListState() {
        if (mAdapter != null) {
            if (mAdapter.isEmpty()) {
                mRecyclerView.setVisibility(View.GONE);
                mEmptyView.setVisibility(View.VISIBLE);
            } else {
                mRecyclerView.setVisibility(View.VISIBLE);
                mEmptyView.setVisibility(View.GONE);
            }
        }
    }


    final void reloadData(){
        ActiveAccount activeAccount = activeAccountRepository.getActiveAccount();
        final List<Note> notes = notesRepository.getAll(activeAccount.getAccount(), activeAccount.getRootFolder(), Utils.getNoteSorting(getActivity()));
        final List<Notebook> notebooks = notebookRepository.getAll(activeAccount.getAccount(), activeAccount.getRootFolder());
        final Map<String,Tag> tags = tagRepository.getAllAsMap(activeAccount.getAccount(), activeAccount.getRootFolder());
        reloadData(notebooks, notes, tags);
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
                Map<String,Tag> tags = tagRepository.getAllAsMap(activeAccount.getAccount(), activeAccount.getRootFolder());

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
                mDrawer.addItem(createNotebookForDrawer(nb));

                orderDrawerItems(tagRepository.getAllAsMap(activeAccount.getAccount(),activeAccount.getRootFolder()), mDrawer, value);
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

    void orderDrawerItems(Map<String,Tag> allTags, Drawer drawer){
        orderDrawerItems(allTags,drawer,null);
    }

    class Orderer implements Runnable{
        private final Drawer drawer;
        private final String selectionName;
        private final Map<String, Tag> allTags;

        Orderer(Map<String, Tag> allTags,Drawer drawer, String selectionName) {
            this.drawer = drawer;
            this.selectionName = selectionName;
            this.allTags = allTags;
        }

        private Orderer(Map<String, Tag> allTags,Drawer drawer) {
            this.drawer = drawer;
            this.selectionName = null;
            this.allTags = allTags;
        }

        @Override
        public void run() {
            ActiveAccount activeAccount = activeAccountRepository.getActiveAccount();
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
                if(selection < 0){
                    allnotesSelected = false;
                }
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

                drawer.getDrawerItems().add(createTagItem(allTags.get(tag)));

                idx++;
                if(!notebookSelected && !allnotesSelected && tag.equals(selected)){
                    selection = idx;
                }
            }

            drawer.getDrawerItems().add(new DividerDrawerItem());

            drawer.getDrawerItems().add(new PrimaryDrawerItem().withName(getResources().getString(R.string.drawer_item_notebooks)).withTag("HEADING_NOTEBOOK").withEnabled(false).withDisabledTextColor(getResources().getColor(R.color.material_drawer_secondary_text)).withIcon(R.drawable.ic_action_collection));

            idx = idx+2;
            if(notebookSelected){
                selection = idx;
            }
            BaseDrawerItem selectedItem = null;
            for(String notebook : notebooks){
                BaseDrawerItem item = createNotebookForDrawer(notebookRepository.getBySummary(activeAccount.getAccount(),activeAccount.getRootFolder(),notebook));
                item.withTextColorRes(R.color.abc_primary_text_material_light);
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
            }else {

                //fallback: if the notebook heading or tag heading is selected, select all notes
                final IDrawerItem fallbackCheck = drawer.getDrawerItems().get(selection);
                if (fallbackCheck != null) {
                    Object itemName = fallbackCheck.getTag();
                    if(itemName == null || itemName.toString().startsWith("HEADING")){
                        selection = 1;
                    }
                }
            }

            drawer.setSelection(selection);
            drawerItemClickedListener.changeNoteSelection(selectedItem);
        }
    }

    void orderDrawerItems(Map<String,Tag> allTags, Drawer drawer, String selectionName){
        getActivity().runOnUiThread(new Orderer(allTags,drawer, selectionName));
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
                String folder = ((ProfileDrawerItem)profile).getTag().toString();
                changed = !activeAccount.getAccount().equalsIgnoreCase(profile.getEmail()) || !activeAccount.getRootFolder().equalsIgnoreCase(folder);
                account = profile.getEmail();
                rootFolder = folder;
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
        drawer.getDrawerItems().add(new PrimaryDrawerItem().withName(getResources().getString(R.string.drawer_item_tags)).withTag("HEADING_TAG").withEnabled(false).withDisabledTextColor(getResources().getColor(R.color.material_drawer_secondary_text)).withIcon(R.drawable.ic_action_labels));

    }
}
