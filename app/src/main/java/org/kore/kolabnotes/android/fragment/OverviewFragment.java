package org.kore.kolabnotes.android.fragment;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
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
import android.support.v4.view.GravityCompat;
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
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.kore.kolab.notes.AuditInformation;
import org.kore.kolab.notes.Colors;
import org.kore.kolab.notes.Identification;
import org.kore.kolab.notes.Note;
import org.kore.kolab.notes.Notebook;
import org.kore.kolab.notes.SharedNotebook;
import org.kore.kolab.notes.Tag;
import org.kore.kolab.notes.local.LocalNotesRepository;
import org.kore.kolab.notes.v3.KolabNotesParserV3;
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
import org.kore.kolabnotes.android.drawer.DrawerAccountsService;
import org.kore.kolabnotes.android.drawer.DrawerService;
import org.kore.kolabnotes.android.drawer.OnDrawerSelectionChangedListener;
import org.kore.kolabnotes.android.security.AuthenticatorActivity;
import org.kore.kolabnotes.android.setting.SettingsActivity;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
public class OverviewFragment extends Fragment implements NoteAdapter.ViewHolder.ClickListener, OnAccountSwitchedListener, OnDrawerSelectionChangedListener {

    public static final int DETAIL_ACTIVITY_RESULT_CODE = 1;
    public static final int TAG_LIST_ACTIVITY_RESULT_CODE = 1;
    private static final String TAG_ACTION_MODE = "ActionMode";
    private static final String TAG_SELECTED_NOTES = "SelectedNotes";
    private static final String TAG_SELECTABLE_ADAPTER = "SelectableAdapter";


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
    private DrawerAccountsService mDrawerAccountsService;

    private NoteRepository notesRepository;
    private NotebookRepository notebookRepository;
    private TagRepository tagRepository;
    private NoteTagRepository notetagRepository;
    private ActiveAccountRepository activeAccountRepository;
    private AttachmentRepository attachmentRepository;
    private ModificationRepository modificationRepository;
    private Toolbar toolbar;

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
        activity.getSupportActionBar().setHomeButtonEnabled(true);

        tabletMode = Utils.isTablet(getResources());

        setHasOptionsMenu(true);

        mAccountManager = AccountManager.get(activity);
        Account[] accounts = mAccountManager.getAccountsByType(AuthenticatorActivity.ARG_ACCOUNT_TYPE);

        Set<AccountIdentifier> allAccounts = activeAccountRepository.getAllAccounts();

        if(allAccounts.size() == 0){
            allAccounts = activeAccountRepository.initAccounts();
        }

        final ActiveAccount activeAccount = activeAccountRepository.getActiveAccount();
        mDrawerAccountsService = new DrawerAccountsService(activity.getNavigationView());

        //For accounts cleanup
        Set<AccountIdentifier> accountsForDeletion = new LinkedHashSet<>(allAccounts);
        accountsForDeletion.remove(new AccountIdentifier("local","Notes"));

        for(int i=0;i<accounts.length;i++) {
            String email = mAccountManager.getUserData(accounts[i],AuthenticatorActivity.KEY_EMAIL);
            String rootFolder = mAccountManager.getUserData(accounts[i],AuthenticatorActivity.KEY_ROOT_FOLDER);
            accountsForDeletion.remove(new AccountIdentifier(email,rootFolder));
        }

        cleanupAccounts(accountsForDeletion);


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
                        String folder = mAccountManager.getUserData(acc, AuthenticatorActivity.KEY_ROOT_FOLDER);
                        if (activeAccount.getAccount().equalsIgnoreCase(email) && activeAccount.getRootFolder().equals(folder)) {
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
                            //TODO set drawer selection to all notes from account
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

    @Override
    public void onAccountSwitched(String name, AccountIdentifier accountIdentifier) {
        mDrawerAccountsService.changeSelectedAccount(name, accountIdentifier.getAccount());
        mDrawerAccountsService.displayNavigation();
        activity.setTitle(name);
        reloadData();
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

        final String nameOfActiveAccount = Utils.getNameOfActiveAccount(activity, activeAccount.getAccount(), activeAccount.getRootFolder());
        mDrawerAccountsService.changeSelectedAccount(nameOfActiveAccount, activeAccount.getAccount());

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
                        mDrawerAccountsService.changeSelectedAccount(name, activeAccount.getAccount());
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
            case android.R.id.home:
                activity.getDrawerLayout().openDrawer(GravityCompat.START);
                break;
            case R.id.create_notebook_menu:
                AlertDialog newNBDialog = createNotebookDialog();
                newNBDialog.show();
                break;
            case R.id.delete_notebook_menu:
                AlertDialog deleteNBDialog = deleteNotebookDialog();

                final String selectedNotebookName = Utils.getSelectedNotebookName(activity);
                if(selectedNotebookName == null){
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
        ActiveAccount activeAccount = activeAccountRepository.getActiveAccount();
        if(/*tag == null || !tag.equals("NOTEBOOK")*/true){
            List<Notebook> all = notebookRepository.getAll(activeAccount.getAccount(), activeAccount.getRootFolder());

            for(Notebook book : all){

                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);

                intent.addCategory(Intent.CATEGORY_OPENABLE);

                intent.setType("application/zip");
                intent.putExtra(Intent.EXTRA_TITLE, book.getSummary());
                startActivityForResult(intent, Utils.WRITE_REQUEST_CODE);
            }
        }else{
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);

            intent.addCategory(Intent.CATEGORY_OPENABLE);

            intent.setType("application/zip");
            //TODO get notebook name from drawer
            String notebookName = null;
            intent.putExtra(Intent.EXTRA_TITLE, notebookName+".zip");
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
                ActiveAccount activeAccount = activeAccountRepository.getActiveAccount();

                final String selectedNotebookName = Utils.getSelectedNotebookName(activity);
                Notebook book = null;
                if(selectedNotebookName != null) {
                    book = notebookRepository.getBySummary(activeAccount.getAccount(), activeAccount.getRootFolder(), selectedNotebookName);
                }

                notebookRepository.delete(activeAccount.getAccount(), activeAccount.getRootFolder(), book);

                new DrawerService(activity.getNavigationView(), activity.getDrawerLayout()).deleteNotebook(book.getSummary());
                Utils.setSelectedNotebookName(activity, null);
                Utils.setSelectedTagName(activity,null);
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

            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mDrawerAccountsService.overrideAccounts(activity, mAccountManager.getAccountsByType(AuthenticatorActivity.ARG_ACCOUNT_TYPE), mAccountManager, activity.getDrawerLayout());
                }
            });

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

    final synchronized void reloadData(List<Notebook> notebooks, List<Note> notes, Map<String,Tag> tags){
        DrawerService drawerService = new DrawerService(activity.getNavigationView(), activity.getDrawerLayout());
        if(notebooks != null) {
            drawerService.overrideNotebooks(this, notebooks);
        }
        if(tags != null) {
            drawerService.overrideTags(this, tags.values());
        }

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

    public void showAccountChooseDialog() {
        FragmentManager fm = getFragmentManager();
        ChooseAccountDialogFragment chooseAccountDialog = new ChooseAccountDialogFragment();
        chooseAccountDialog.show(fm, "fragment_choose_account");
    }

    @Override
    public void notebookSelected(String notebookName) {
        ActiveAccount activeAccount = activeAccountRepository.getActiveAccount();
        final String account = activeAccount.getAccount();
        final String rootFolder = activeAccount.getRootFolder();
        final Notebook notebook = notebookRepository.getBySummary(account, rootFolder, notebookName);
        final List<Note> notes = notesRepository.getFromNotebook(account, rootFolder, notebook.getIdentification().getUid(), Utils.getNoteSorting(getActivity()));
        reloadData(null, notes, null);
    }

    @Override
    public void tagSelected(String tagName) {
        ActiveAccount activeAccount = activeAccountRepository.getActiveAccount();
        final List<Note> notes = notetagRepository.getNotesWith(activeAccount.getAccount(), activeAccount.getRootFolder(), tagName, Utils.getNoteSorting(getActivity()));
        reloadData(null, notes, null);
    }

    @Override
    public void allNotesFromAccountSelected() {
        ActiveAccount activeAccount = activeAccountRepository.getActiveAccount();
        final List<Note> notes = notesRepository.getAll(activeAccount.getAccount(), activeAccount.getRootFolder(), Utils.getNoteSorting(getActivity()));
        reloadData(null, notes, null);
    }

    @Override
    public void allNotesSelected() {
        ActiveAccount activeAccount = activeAccountRepository.getActiveAccount();
        final List<Note> notes = notesRepository.getAll(Utils.getNoteSorting(getActivity()));
        reloadData(null, notes, null);
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
                DrawerService service = new DrawerService(activity.getNavigationView(), activity.getDrawerLayout());
                service.addNotebook(OverviewFragment.this, nb);
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


}
