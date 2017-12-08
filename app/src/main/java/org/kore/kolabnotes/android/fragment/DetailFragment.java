package org.kore.kolabnotes.android.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintJob;
import android.print.PrintManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.kore.kolab.notes.AuditInformation;
import org.kore.kolab.notes.Colors;
import org.kore.kolab.notes.Identification;
import org.kore.kolab.notes.Note;
import org.kore.kolab.notes.Notebook;
import org.kore.kolab.notes.SharedNotebook;
import org.kore.kolab.notes.Tag;
import org.kore.kolabnotes.android.AccountChooserActivity;
import org.kore.kolabnotes.android.AttachmentActivity;
import org.kore.kolabnotes.android.DrawEditorActivity;
import org.kore.kolabnotes.android.R;
import org.kore.kolabnotes.android.Utils;
import org.kore.kolabnotes.android.content.AccountIdentifier;
import org.kore.kolabnotes.android.content.ActiveAccount;
import org.kore.kolabnotes.android.content.ActiveAccountRepository;
import org.kore.kolabnotes.android.content.AttachmentRepository;
import org.kore.kolabnotes.android.content.NoteRepository;
import org.kore.kolabnotes.android.content.NoteTagRepository;
import org.kore.kolabnotes.android.content.NotebookRepository;
import org.kore.kolabnotes.android.content.TagRepository;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import jp.wasabeef.richeditor.RichEditor;
import yuku.ambilwarna.AmbilWarnaDialog;

/**
 * Fragment for displaying and editing the details of a note
 */
public class DetailFragment extends Fragment implements OnAccountSwitchedListener {
    public static final int DRAWEDITOR_ACTIVITY_RESULT_CODE = 1;
    public static final int ATTACHMENT_ACTIVITY_RESULT_CODE = 2;

    private final static String HTMLSTART = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0//EN\" \"http://www.w3.org/TR/REC-html40/strict.dtd\">" +
            "<html><head><meta name=\"kolabnotes-richtext\" content=\"1\" /><meta http-equiv=\"Content-Type\" /></head><body>";

    private final static String HTMLEND = "</body></html>";

    private static final String EDITOR = "editor";

    private NotebookRepository notebookRepository;
    private NoteRepository noteRepository;
    private NoteTagRepository noteTagRepository;
    private TagRepository tagRepository;
    private ActiveAccountRepository activeAccountRepository;

    private Toolbar toolbar;

    private Note note = null;

    private Note.Classification selectedClassification;

    private org.kore.kolab.notes.Color selectedColor;

    private Set<String> selectedTags = new LinkedHashSet<>();

    private Map<String,Tag> allTags = new HashMap<>();

    //Given notebook is set, if a notebook uid was in the start intent,
    //intialNotebook ist the notebook-UID which is selected after setSpinnerSelection was called
    private String givenNotebook;
    private String intialNotebookName;
    private boolean isNewNote;

    //These two elements will be set by the activity or factory method when fragment is created
    private String startUid;
    private String startNotebook;

    private RichEditor editor;

    private EditText editText;
    
    private AppCompatActivity activity;

    private boolean isDescriptionDirty = false;
    private boolean accountGotChanged = false;

    private String uuidForCreation;

    //This map contains inline images in its base form, sadly the android webview destroys the correct form
    private Map<String,String> base64Images = new HashMap<>();

    public static DetailFragment newInstance(String noteUid, String notebook){
        DetailFragment f = new DetailFragment();
        f.setStartUid(noteUid);
        f.setStartNotebook(notebook);
        Bundle args = new Bundle();
        f.setArguments(args);
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_detail,
                container,
                false);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        this.activity = (AppCompatActivity)activity;

        notebookRepository = new NotebookRepository(activity);
        noteRepository = new NoteRepository(activity);
        noteTagRepository = new NoteTagRepository(activity);
        tagRepository = new TagRepository(activity);
        activeAccountRepository = new ActiveAccountRepository(activity);

        ((OnFragmentCallback)activity).fragementAttached(this);
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);
        activity.setSupportActionBar(toolbar);
        if(activity.getSupportActionBar() != null){
            activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        setHasOptionsMenu(true);

        boolean useRicheditor = Utils.getUseRicheditor(activity);

        if(useRicheditor) {
            editor = (RichEditor) activity.findViewById(R.id.detail_description);
            editor.setVisibility(View.VISIBLE);
            editor.setBackgroundColor(Color.TRANSPARENT);
            editor.setEditorHeight(300);
            editor.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View view, boolean b) {
                    final View bar = activity.findViewById(R.id.editor_bar);
                    final int visibility = bar.getVisibility();
                    if (visibility == View.GONE) {
                        bar.setVisibility(View.VISIBLE);
                    } else {
                        bar.setVisibility(View.GONE
                        );
                    }
                }
            });
            initEditor();
        }else{
            editText = (EditText) activity.findViewById(R.id.detail_description_plain);
            editText.setVisibility(View.VISIBLE);
            editText.setMovementMethod(LinkMovementMethod.getInstance());
        }

        // Handle Back Navigation :D
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DetailFragment.this.onBackPressed();
            }
        });

        Intent startIntent = activity.getIntent();
        String uid = startUid;
        String notebook = startNotebook;
        String accountEmail = startIntent.getStringExtra(Utils.INTENT_ACCOUNT_EMAIL);
        String rootFolder = startIntent.getStringExtra(Utils.INTENT_ACCOUNT_ROOT_FOLDER);

        String action = startIntent.getAction();

        if (Intent.ACTION_SEND.equals(action)) {
            final String type = startIntent.getType();

            if(type != null && type.startsWith("image")){
                loadImageFromIntent(startIntent);
            }else{
                takeTextFromIntent(startIntent);
            }
        }

        Log.d("onCreate", "accountEmail:" + accountEmail);
        Log.d("onCreate","rootFolder:"+rootFolder);
        Log.d("onCreate","notebook-uid:"+notebook);

        ActiveAccount activeAccount;
        if(accountEmail != null && rootFolder != null){
            activeAccount = activeAccountRepository.switchAccount(accountEmail,rootFolder);
        }else{
            activeAccount = activeAccountRepository.getActiveAccount();
        }

        toolbar.setTitle(Utils.getNameOfActiveAccount(activity, activeAccount.getAccount(), activeAccount.getRootFolder()));

        if(uid != null) {

            //if a note was selected from the "all notes" overview
            final AccountIdentifier accountFromNote = noteRepository.getAccountFromNote(uid);
            if(!activeAccount.getAccount().equals(accountFromNote.getAccount()) || !activeAccount.getRootFolder().equals(accountFromNote.getRootFolder())){
                activeAccount = activeAccountRepository.switchAccount(accountFromNote.getAccount(),accountFromNote.getRootFolder());
            }

            initSpinner();

            note = noteRepository.getByUID(activeAccount.getAccount(), activeAccount.getRootFolder(), uid);

            //Maybe the note got deleted (sync happend after a click on a note was done) => Issues 34 on GitHub
            if (note == null) {
                Toast.makeText(activity, R.string.note_not_found, Toast.LENGTH_LONG).show();
            } else {
                EditText summary = (EditText) activity.findViewById(R.id.detail_summary);
                summary.setText(note.getSummary());

                String desc = note.getDescription();
                if(!TextUtils.isEmpty(desc)) {
                    String updatedDesc = initImageMap(note.getDescription());
                    setHtml(updatedDesc);
                    note.setDescription(updatedDesc);
                }

                selectedClassification = note.getClassification();
                for (Tag tag : note.getCategories()) {
                    selectedTags.add(tag.getName());
                }

                selectedColor = note.getColor();

                if(notebook == null){
                    notebook = noteRepository.getUIDofNotebook(activeAccount.getAccount(), activeAccount.getRootFolder(), uid);
                }
            }
        }else{
            initSpinner();
            isNewNote = true;
        }

        allTags.putAll(tagRepository.getAllAsMap(activeAccount.getAccount(), activeAccount.getRootFolder()));
        setNotebook(activeAccount, notebook, startNotebook != null);
        intialNotebookName = getNotebookSpinnerSelectionName();

        if (savedInstanceState != null && savedInstanceState.getString(EDITOR) != null) {
            /* Restoring saved data into editor */
            String descriptionValue = initImageMap(savedInstanceState.getString(EDITOR));
            setHtml(descriptionValue);
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            activity.findViewById(R.id.action_insert_image).setVisibility(View.GONE);
        }
    }

    private void takeTextFromIntent(Intent startIntent) {
        CharSequence description = startIntent.getCharSequenceExtra(Intent.EXTRA_TEXT);
        CharSequence hdescription = startIntent.getCharSequenceExtra(Intent.EXTRA_HTML_TEXT);
        String summary = startIntent.getStringExtra(Intent.EXTRA_SUBJECT);

        if(!TextUtils.isEmpty(hdescription)) {
            String updatedDesc = initImageMap(hdescription.toString());
            setHtml(updatedDesc);
        }else if(!TextUtils.isEmpty(description)) {

            String updatedDesc = initImageMap(description.toString());
            setHtml(updatedDesc);
        }

        if(!TextUtils.isEmpty(summary)) {
            EditText esummary = (EditText) activity.findViewById(R.id.detail_summary);
            esummary.setText(summary);

        }
    }

    private String getUUIDForCreation(){
        if(uuidForCreation == null){
            uuidForCreation = UUID.randomUUID().toString();
        }
        return  uuidForCreation;
    }

    void setToolbarColor(){
        boolean lightText = true;
        if (selectedColor != null) {
            toolbar.setBackgroundColor(Color.parseColor(selectedColor.getHexcode()));
            lightText = Utils.useLightTextColor(activity, selectedColor);
        }else{
            toolbar.setBackgroundColor(getResources().getColor(R.color.theme_default_primary));
        }

        Utils.setToolbarTextAndIconColor(activity, toolbar, lightText);
    }

    void setHtml(String text){
        final String stripped = stripBody(text);
        if(editor != null){
            editor.setHtml(stripped);
        }else{
            Spanned fromHtml = Html.fromHtml(stripped);
            editText.setText(fromHtml, TextView.BufferType.SPANNABLE);
        }
    }

    String stripBody(String html){
        return Utils.getHtmlBodyText(html);
    }

    public void setStartUid(String startUid) {
        this.startUid = startUid;
    }

    public void setStartNotebook(String startNotebook) {
        this.startNotebook = startNotebook;
    }

    public Note getNote(){
        return note;
    }

    void setNotebook(ActiveAccount activeAccount,String uid, boolean setGivenNotebook){
        if(uid != null) {
            //GitHub Issue 37
            Notebook notebook = notebookRepository.getByUID(activeAccount.getAccount(), activeAccount.getRootFolder(), uid);
            if(notebook != null) {

                String summary = notebook.getSummary();

                if(notebook.isShared()){
                    SharedNotebook shared = (SharedNotebook)notebook;
                    summary = shared.getShortName();

                    if(!shared.isNoteCreationAllowed() && !shared.isNoteModificationAllowed()){
                        Toast.makeText(activity, R.string.no_write_permissions, Toast.LENGTH_LONG).show();
                    }else if(shared.isNoteCreationAllowed() && !shared.isNoteModificationAllowed()){
                        if(note != null) {
                            Toast.makeText(activity, R.string.no_change_permissions, Toast.LENGTH_LONG).show();
                        }
                    }else if(!shared.isNoteCreationAllowed() && shared.isNoteModificationAllowed()){
                        Toast.makeText(activity, R.string.no_create_permissions, Toast.LENGTH_LONG).show();
                    }
                }

                String notebookSummary = summary;
                setSpinnerSelection(notebookSummary);
                if(setGivenNotebook){
                    givenNotebook = notebookSummary;
                }
            }else{
                Spinner spinner = (Spinner) activity.findViewById(R.id.spinner_notebook);
                spinner.setSelection(0);
            }
        }
    }

    void initEditor(){
        editor.setOnTextChangeListener(new RichEditor.OnTextChangeListener() {
            @Override
            public void onTextChange(String s) {
                DetailFragment.this.isDescriptionDirty = true;
            }
        });

        activity.findViewById(R.id.action_undo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editor.undo();
            }
        });

        activity.findViewById(R.id.action_redo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editor.redo();
            }
        });

        activity.findViewById(R.id.action_bold).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editor.setBold();
            }
        });

        activity.findViewById(R.id.action_italic).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editor.setItalic();
            }
        });


        activity.findViewById(R.id.action_subscript).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editor.setSubscript();
            }
        });

        activity.findViewById(R.id.action_superscript).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editor.setSuperscript();
            }
        });

        activity.findViewById(R.id.action_strikethrough).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editor.setStrikeThrough();
            }
        });

        activity.findViewById(R.id.action_underline).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editor.setUnderline();
            }
        });

        activity.findViewById(R.id.action_heading1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editor.setHeading(1);
            }
        });

        activity.findViewById(R.id.action_heading2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editor.setHeading(2);
            }
        });

        activity.findViewById(R.id.action_heading3).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editor.setHeading(3);
            }
        });

        activity.findViewById(R.id.action_heading4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editor.setHeading(4);
            }
        });

        activity.findViewById(R.id.action_heading5).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editor.setHeading(5);
            }
        });

        activity.findViewById(R.id.action_heading6).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editor.setHeading(6);
            }
        });

        activity.findViewById(R.id.action_txt_color).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                AmbilWarnaDialog dialog = new AmbilWarnaDialog(activity, Color.BLACK, false, new AmbilWarnaDialog.OnAmbilWarnaListener() {
                    @Override
                    public void onOk(AmbilWarnaDialog dialog, int color) {
                        editor.setTextColor(color);
                    }

                    @Override
                    public void onRemove(AmbilWarnaDialog dialog) {
                        // do nothing
                    }

                    @Override
                    public void onCancel(AmbilWarnaDialog dialog) {
                        // do nothing
                    }
                });
                dialog.show();
            }
        });

        activity.findViewById(R.id.action_bg_color).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AmbilWarnaDialog dialog = new AmbilWarnaDialog(activity, Color.WHITE, false, new AmbilWarnaDialog.OnAmbilWarnaListener() {
                    @Override
                    public void onOk(AmbilWarnaDialog dialog, int color) {
                        editor.setTextBackgroundColor(color == Color.WHITE ? Color.TRANSPARENT : color);
                    }

                    @Override
                    public void onRemove(AmbilWarnaDialog dialog) {
                        // do nothing
                    }

                    @Override
                    public void onCancel(AmbilWarnaDialog dialog) {
                        // do nothing
                    }
                });
                dialog.show();
            }
        });

        activity.findViewById(R.id.action_indent).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editor.setIndent();
            }
        });

        activity.findViewById(R.id.action_outdent).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editor.setOutdent();
            }
        });

        activity.findViewById(R.id.action_align_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editor.setAlignLeft();
            }
        });

        activity.findViewById(R.id.action_align_center).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editor.setAlignCenter();
            }
        });

        activity.findViewById(R.id.action_align_right).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editor.setAlignRight();
            }
        });

        activity.findViewById(R.id.action_blockquote).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editor.setBlockquote();
            }
        });
        activity.findViewById(R.id.action_bullets).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editor.setBullets();
            }
        });
        activity.findViewById(R.id.action_numbers).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editor.setNumbers();
            }
        });

        activity.findViewById(R.id.action_insert_image).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);

                intent.addCategory(Intent.CATEGORY_OPENABLE);

                intent.setType("image/*");

                startActivityForResult(intent, Utils.READ_REQUEST_CODE);
            }
        });

        activity.findViewById(R.id.action_insert_link).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);

                builder.setTitle(R.string.dialog_input_link);

                LayoutInflater inflater = activity.getLayoutInflater();
                final View view = inflater.inflate(R.layout.dialog_link_input, null);

                builder.setView(view);

                builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        TextView text = (TextView) view.findViewById(R.id.dialog_text_input_field);

                        CharSequence input = text.getText();
                        if (input == null || input.toString().trim().length() == 0) {
                            Toast.makeText(activity, R.string.error_field_required, Toast.LENGTH_SHORT).show();
                        } else {
                            editor.insertLink(input.toString(), input.toString());
                        }
                    }
                });
                builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //nothing
                    }
                });

                builder.show();
            }
        });

        activity.findViewById(R.id.action_insert_checkbox).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editor.insertTodo();
            }
        });

        activity.findViewById(R.id.action_draweditor).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(activity, DrawEditorActivity.class);
                startActivityForResult(i, DRAWEDITOR_ACTIVITY_RESULT_CODE);
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {

        if (requestCode == Utils.READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (resultData != null) {
                loadImageFromIntent(resultData);
            }
        } else if (requestCode == DRAWEDITOR_ACTIVITY_RESULT_CODE && resultCode == Activity.RESULT_OK) {
            if (resultData != null) {
                if (resultData.hasExtra(DrawEditorActivity.TAG_RETURN_BITMAP) &&
                        resultData.getByteArrayExtra(DrawEditorActivity.TAG_RETURN_BITMAP) != null) {
                    byte[] image = resultData.getByteArrayExtra(DrawEditorActivity.TAG_RETURN_BITMAP);
                    String prefix = "data:image/png;base64,";
                    String imageEncoded = prefix + Base64.encodeToString(image, Base64.NO_WRAP);

                    String alt = UUID.randomUUID().toString();

                    /* Set focus, as after rotate focus is lost and it's impossible to insert an image */
                    editor.focusEditor();
                    editor.insertImage(imageEncoded, alt);
                    editor.getScaleX();
                    putImage(alt, imageEncoded);
                    
                    if (activity instanceof OnFragmentCallback) {
                        ((OnFragmentCallback) activity).fileSelected();
                    }
                }
            }
        }else if(requestCode == ATTACHMENT_ACTIVITY_RESULT_CODE){
            if (activity instanceof OnFragmentCallback) {
                ((OnFragmentCallback) activity).fileSelected();
            }
        }
    }

    private void loadImageFromIntent(Intent resultData) {
        Uri uri = resultData.getData();
        if(uri != null){
            loadImageFromUri(uri);
        }else{
            //in case of getting image via share intent from other app
            final ClipData clipData = resultData.getClipData();
            for(int i=0;i<clipData.getItemCount();i++){
                loadImageFromUri(clipData.getItemAt(i).getUri());
            }
        }
    }

    private void loadImageFromUri(Uri uri) {
        String path = uri.getPath();

        try {
            Bitmap immagex = BitmapFactory.decodeStream(activity.getContentResolver().openInputStream(uri));
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            String lowerPath = path.toLowerCase();
            String prefix;
            if (lowerPath.endsWith("png")) {
                immagex.compress(Bitmap.CompressFormat.PNG, 100, baos);
                prefix = "data:image/png;base64,";
            } else if (lowerPath.endsWith("webp")) {
                immagex.compress(Bitmap.CompressFormat.WEBP, 100, baos);
                prefix = "data:image/webp;base64,";
            } else {
                immagex.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                prefix = "data:image/jpeg;base64,";
            }

            byte[] b = baos.toByteArray();
            if(b.length < 900000) {
                String imageEncoded = prefix + Base64.encodeToString(b, Base64.NO_WRAP);

                String alt = path;

                //issue 125
                if (!editor.isFocused()) {
                /* Set focus, as after rotate focus is lost and it's impossible to insert an image */
                    editor.focusEditor();
                }
                editor.insertImage(imageEncoded, alt);
                putImage(alt, imageEncoded);
            }else{
                Toast.makeText(activity, R.string.image_too_big, Toast.LENGTH_LONG).show();
            }


            if (activity instanceof OnFragmentCallback) {
                ((OnFragmentCallback) activity).fileSelected();
            }
        }catch(IOException e){
            Log.e("onActivityResult",e.toString());
        }
    }

    String getNotebookSpinnerSelectionName(){
        Spinner spinner = (Spinner) activity.findViewById(R.id.spinner_notebook);

        if(spinner.getSelectedItem() == null){
            return null;
        }

        return spinner.getSelectedItem().toString();
    }

    void setSpinnerSelection(String notebookSummary){
        Spinner spinner = (Spinner) activity.findViewById(R.id.spinner_notebook);
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
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.detail_toolbar, menu);

        setToolbarColor();
    }

    String setShareIntentSubject(Intent shareIntent){
        EditText summary = (EditText) activity.findViewById(R.id.detail_summary);
        String ssummary = summary.getText().toString();
        if(!TextUtils.isEmpty(ssummary)) {
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, ssummary);
            return ssummary;
        }
        return null;
    }

    String setShareIntentDescription(Intent shareIntent){
        String descriptionValue = repairImages(getDescriptionFromView());
        if(descriptionValue != null) {
            if(!descriptionValue.startsWith("<!DOCTYPE HTML")) {
                descriptionValue = HTMLSTART + repairImages(getDescriptionFromView()) + HTMLEND;
            }

            shareIntent.putExtra(Intent.EXTRA_TEXT, Html.fromHtml(descriptionValue));
            shareIntent.putExtra(Intent.EXTRA_HTML_TEXT, descriptionValue);
            return descriptionValue;
        }
        return null;
    }

    public boolean shareNote(Intent shareIntent) {
        setShareIntentSubject(shareIntent);
        String descriptionValue = setShareIntentDescription(shareIntent);

        if(!TextUtils.isEmpty(descriptionValue)) {
            startActivity(Intent.createChooser(shareIntent, getResources().getText(R.string.send_to)));
        }else{
            Toast.makeText(activity, R.string.empty_note, Toast.LENGTH_LONG).show();
        }
        return false;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            menu.findItem(R.id.print).setVisible(false);
            menu.findItem(R.id.attachments).setVisible(false);
        }
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
            //case R.id.change_classification: issue 85
            //    editClassification();
            //    break;
            case R.id.colorpicker:
                chooseColor();
                break;
            case R.id.metainformation:
                showMetainformation();
                break;
            case R.id.share:
                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.setType("text/html");
                shareNote(shareIntent);
                break;
            case R.id.print:
                printNote();
                break;
            case R.id.attachments:
                showAttachments();
                break;
            case R.id.changeAccount:
                showAccountChooseDialog();
                break;
        }
        return true;
    }

    void chooseColor(){

        final int initialColor = selectedColor == null ? Color.WHITE : Color.parseColor(selectedColor.getHexcode());

        AmbilWarnaDialog dialog = new AmbilWarnaDialog(activity, initialColor, true, new AmbilWarnaDialog.OnAmbilWarnaListener() {
            @Override
            public void onOk(AmbilWarnaDialog dialog, int color) {
                selectedColor = Colors.getColor(String.format("#%06X", (0xFFFFFF & color)));
                setToolbarColor();
            }

            @Override
            public void onRemove(AmbilWarnaDialog dialog) {
                selectedColor = null;
                setToolbarColor();
            }

            @Override
            public void onCancel(AmbilWarnaDialog dialog) {
                // do nothing
            }
        });
        dialog.show();
    }

    void showMetainformation(){
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        builder.setTitle(R.string.title_metainformation);

        LayoutInflater inflater = activity.getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_metainformation, null);

        builder.setView(view);

        builder.setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                //just close dialog
            }
        });

        final DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.DEFAULT);

        final Timestamp now = new Timestamp(System.currentTimeMillis());
        final String productID = note == null ? "kolabnotes-android" : note.getIdentification().getProductId();
        final String uid = note == null ? "" : note.getIdentification().getUid();
        final Timestamp createdAt = note == null ? now : note.getAuditInformation().getCreationDate();
        final Timestamp modification = note == null ? now : note.getAuditInformation().getLastModificationDate();

        ((TextView) view.findViewById(R.id.createdDate)).setText(dateFormat.format(createdAt));
        ((TextView) view.findViewById(R.id.modificationDate)).setText(dateFormat.format(modification));
        ((TextView) view.findViewById(R.id.productID)).setText(productID);
        ((TextView) view.findViewById(R.id.UID)).setText(uid);

        builder.show();
    }

    void editClassification(){
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        builder.setTitle(R.string.dialog_change_classification);

        LayoutInflater inflater = activity.getLayoutInflater();
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


    void showAccountChooseDialog(){
        AccountChooserActivity chooser = (AccountChooserActivity)this.activity;
        chooser.showAccountChooseDialog();
    }

    @Override
    public void onAccountSwitched(String name, AccountIdentifier accountIdentifier) {
        resetSpinner();
        if(toolbar != null){
            toolbar.setTitle(name);
        }
        this.accountGotChanged = true;
        this.isNewNote = true;
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
                    DetailFragment.this.selectedClassification = Note.Classification.PUBLIC;
                    break;
                case R.id.radio_confidential:
                    DetailFragment.this.selectedClassification = Note.Classification.CONFIDENTIAL;
                    break;
                case R.id.radio_private:
                    DetailFragment.this.selectedClassification = Note.Classification.PRIVATE;
                    break;
            }
        }
    }

    void editTags(){
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.dialog_change_tags);

        final Set<String> tagNames = allTags.keySet();

        final String[] tagArr = tagNames.toArray(new String[tagNames.size()]);

        Arrays.sort(tagArr);

        final boolean[] selectionArr = new boolean[tagArr.length];

        final ArrayList<Integer> selectedItems=new ArrayList<Integer> ();

        for(int i=0;i<tagArr.length;i++){
            if(selectedTags.contains(tagArr[i])){
                selectionArr[i] = true;
                selectedItems.add(i);
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
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        selectedTags.clear();
                        for (int i = 0; i < selectedItems.size(); i++) {
                            selectedTags.add(tagArr[selectedItems.get(i)]);
                        }
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

    String getDescriptionFromView(){
        if(editor != null){
            final String html = editor.getHtml();
            if(TextUtils.isEmpty(html)){
                return null;
            }
            return html;
        }

        StringBuilder sb = new StringBuilder(HTMLSTART);
        sb.append(Html.toHtml(editText.getText()));
        sb.append(HTMLEND);
        return sb.toString();
    }

    private AlertDialog createNotebookDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        builder.setTitle(R.string.dialog_input_text_notebook);

        LayoutInflater inflater = activity.getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_text_input, null);

        builder.setView(view);

        builder.setPositiveButton(R.string.ok, new CreateNotebookButtonListener((EditText) view.findViewById(R.id.dialog_text_input_field)));
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //nothing at the moment
            }
        });
        return builder.create();
    }

    public class CreateNotebookButtonListener implements DialogInterface.OnClickListener{

        private final EditText textField;

        public CreateNotebookButtonListener(EditText textField) {
            this.textField = textField;
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

            Notebook nb = new Notebook(ident,audit, Note.Classification.PUBLIC, value);
            nb.setDescription(value);
            notebookRepository.insert(activeAccount.getAccount(), activeAccount.getRootFolder(), nb);

            initSpinner();

            setSpinnerSelection(value);
        }
    }


    void saveNote(){
        EditText summary = (EditText) activity.findViewById(R.id.detail_summary);

        Spinner spinner = (Spinner) activity.findViewById(R.id.spinner_notebook);

        if(spinner.getSelectedItem() == null){
            //Just possible if there is no notebook created
            AlertDialog notebookDialog = createNotebookDialog();

            notebookDialog.show();
        }else {

            if (TextUtils.isEmpty(summary.getText().toString())) {
                summary.setError(getString(R.string.error_field_required));
                summary.requestFocus();
                return;
            }

            String notebookName = getNotebookSpinnerSelectionName();

            String descriptionValue = repairImages(getDescriptionFromView());

            if(descriptionValue != null && !descriptionValue.startsWith("<!DOCTYPE HTML")){
                descriptionValue = HTMLSTART + repairImages(getDescriptionFromView()) + HTMLEND;
            }

            final ActiveAccount activeAccount = activeAccountRepository.getActiveAccount();
            if (note == null || accountGotChanged) {
                final String uuid = getUUIDForCreation();
                Identification ident = new Identification(uuid, "kolabnotes-android");
                Timestamp now = new Timestamp(System.currentTimeMillis());
                AuditInformation audit = new AuditInformation(now, now);

                note = new Note(ident, audit, selectedClassification == null ? Note.Classification.PUBLIC : selectedClassification, summary.getText().toString());
                note.setDescription(descriptionValue);
                note.setColor(selectedColor);

                Notebook book = notebookRepository.getBySummary(activeAccount.getAccount(), activeAccount.getRootFolder(), notebookName);

                if(book.isShared()){
                    if(!((SharedNotebook)book).isNoteCreationAllowed()){
                        Toast.makeText(activity, R.string.no_create_permissions, Toast.LENGTH_LONG).show();
                        return;
                    }
                }

                noteRepository.insert(activeAccount.getAccount(), activeAccount.getRootFolder(), note, book.getIdentification().getUid());
                noteTagRepository.delete(activeAccount.getAccount(), activeAccount.getRootFolder(), uuid);
                for (String tag : selectedTags) {
                    if(accountGotChanged){
                        //search for the selected tag, if it  not exists, create it
                        boolean existsTag = tagRepository.existsTagNameFor(activeAccount.getAccount(), activeAccount.getRootFolder(), tag);
                        if(!existsTag){
                            final Tag newTag = Tag.createNewTag(tag);
                            tagRepository.insert(activeAccount.getAccount(), activeAccount.getRootFolder(), newTag);
                        }
                    }
                    noteTagRepository.insert(activeAccount.getAccount(), activeAccount.getRootFolder(), uuid, tag);
                }
            } else {
                final String uuid = note.getIdentification().getUid();
                note.setSummary(summary.getText().toString());
                note.setDescription(descriptionValue);
                note.setClassification(selectedClassification);
                note.setColor(selectedColor);
                note.getAuditInformation().setLastModificationDate(System.currentTimeMillis());

                Notebook book = notebookRepository.getBySummary(activeAccount.getAccount(), activeAccount.getRootFolder(), notebookName);

                if (checkModificationPermissions(book)) return;

                noteRepository.update(activeAccount.getAccount(), activeAccount.getRootFolder(), note, book.getIdentification().getUid());

                noteTagRepository.delete(activeAccount.getAccount(), activeAccount.getRootFolder(), uuid);
                for (String tag : selectedTags) {
                    noteTagRepository.insert(activeAccount.getAccount(), activeAccount.getRootFolder(), uuid, tag);
                }

                String selectedNotebookName = Utils.getSelectedNotebookName(activity);

                String corrSumm = book.getSummary();

                if(book.isShared()){
                    corrSumm = ((SharedNotebook)book).getShortName();
                }

                if(selectedNotebookName != null && !selectedNotebookName.equals(corrSumm)){
                    Utils.setSelectedNotebookName(activity,corrSumm);
                }
            }

            Intent returnIntent = new Intent();
            if (isNewNote) {
                if(Utils.getSelectedNotebookName(activity) != null){
                    Utils.setSelectedNotebookName(activity,notebookName);
                }
            }
            isDescriptionDirty = false;

            Utils.updateWidgetsForChange(activity);

            ((OnFragmentCallback) activity).fragmentFinished(returnIntent, OnFragmentCallback.ResultCode.SAVED);
        }
    }

    private boolean checkModificationPermissions(Notebook book) {
        return Utils.checkNotebookPermissions(activity, activeAccountRepository.getActiveAccount(), note, book);
    }

    /**
     * Android WebView adds line terminators to inline images, these must be deleted
     * @param html
     */
    String repairImages(String html){
        //issue 127
        if(!Utils.getUseRicheditor(activity)){
            return html;
        }

        if(html == null || html.trim().length() == 0){
            return null;
        }

        final StringBuilder repaired = new StringBuilder(html);

        int start = 0;
        while((start = html.indexOf("<img src",start)) != -1){
            int withoutTag = start+10;
            int endOfImage = html.indexOf("\"",withoutTag);

            int startOfAltContent = endOfImage+7;
            int endOfAlt = html.indexOf("\"",startOfAltContent);

            String altContent = html.substring(startOfAltContent,endOfAlt);

            start = endOfAlt;

            repaired.replace(withoutTag, endOfImage,base64Images.get(altContent));
        }

        return repaired.toString();
    }

    String initImageMap(String description){
        int start = 0;
        StringBuilder withAlts = new StringBuilder(description);
        while((start = description.indexOf("<img src",start)) != -1){
            int withoutTag = start+10;
            int end = description.indexOf("\"",withoutTag);

            String image = description.substring(withoutTag,end);

            //check if the alt tag is present (will be used to identify an image)
            String possibleAlt = description.substring(end+2,end+5);
            if(possibleAlt.equals("alt")){
                int startAltContent = end+7;
                String altContent = description.substring(startAltContent,description.indexOf("\"",startAltContent));
                base64Images.put(altContent,image);
            }else{
                String uid = UUID.randomUUID().toString();
                base64Images.put(UUID.randomUUID().toString(),image);
                withAlts.insert(end+2,"alt=\""+uid+"\"");
            }
            start = end;
        }

        return withAlts.toString();
    }

    void putImage(String alt, String image){
        this.base64Images.put(alt,image);
    }

    void deleteNote(){
        if(note != null && !accountGotChanged){
            Notebook book = notebookRepository.getByUID(activeAccountRepository.getActiveAccount().getAccount(), activeAccountRepository.getActiveAccount().getRootFolder(), noteRepository.getUIDofNotebook(activeAccountRepository.getActiveAccount().getAccount(), activeAccountRepository.getActiveAccount().getRootFolder(), note.getIdentification().getUid()));

            if(book.isShared()){
                if(!((SharedNotebook)book).isNoteModificationAllowed()){
                    Toast.makeText(activity, R.string.no_change_permissions, Toast.LENGTH_LONG).show();
                    return;
                }
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(activity);

            builder.setTitle(R.string.dialog_delete_note);
            builder.setMessage(R.string.dialog_question_delete);
            builder.setPositiveButton(R.string.yes,new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ActiveAccount activeAccount = activeAccountRepository.getActiveAccount();
                    DetailFragment.this.noteRepository.delete( activeAccount.getAccount(), activeAccount.getRootFolder(),note);

                    new AttachmentRepository(activity).deleteForNote(activeAccount.getAccount(), activeAccount.getRootFolder(), note.getIdentification().getUid());

                    Utils.updateWidgetsForChange(activity.getApplication());

                    Intent returnIntent = new Intent();
                    returnIntent.putExtra("selectedNotebookName",givenNotebook);

                    ((OnFragmentCallback)activity).fragmentFinished(returnIntent, OnFragmentCallback.ResultCode.DELETED);
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

    public void resetSpinner(){
        Spinner spinner = initSpinner();
        spinner.setSelection(0);
    }

    Spinner initSpinner(){
        Spinner spinner = (Spinner) activity.findViewById(R.id.spinner_notebook);

        final ActiveAccount activeAccount = activeAccountRepository.getActiveAccount();

        List<Notebook> notebooks = notebookRepository.getAll(activeAccount.getAccount(), activeAccount.getRootFolder());

        String[] notebookArr = new String[notebooks.size()];

        for(int i=0; i<notebooks.size();i++){
            String summary = notebooks.get(i).getSummary();

            if(notebooks.get(i).isShared()){
                summary = ((SharedNotebook)notebooks.get(i)).getShortName();
            }

            notebookArr[i] = summary;
        }

        Arrays.sort(notebookArr);

        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(activity,R.layout.notebook_spinner_item,notebookArr);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        return spinner;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        //outState.putParcelable("appInfo", appInfo.getComponentName());
        String descriptionValue = repairImages(getDescriptionFromView());
        if (descriptionValue != null) {
            outState.putString(EDITOR, descriptionValue);
        }
    }

    public void onBackPressed() {
        if(note != null){
            boolean differences = checkDifferences();

            if(differences) {
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);

                builder.setTitle(R.string.dialog_cancel_warning);
                builder.setMessage(R.string.dialog_question_cancel);
                builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        goBack();
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
                goBack();
            }
        } else if (editor.getHtml() != null || !TextUtils.isEmpty(
                ((EditText) activity.findViewById(R.id.detail_summary)).getText().toString())) {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);

            builder.setTitle(R.string.dialog_cancel_warning);
            builder.setMessage(R.string.dialog_question_cancel);
            builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if(isNewNote){
                        final ActiveAccount activeAccount = activeAccountRepository.getActiveAccount();
                        new AttachmentRepository(activity).deleteForNote(activeAccount.getAccount(), activeAccount.getRootFolder(), getUUIDForCreation());
                    }
                    goBack();
                }
            });
            builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    /* Nothing */
                }
            });
            builder.show();
        } else {
            goBack();
        }
    }

    public boolean checkDifferences(){
        EditText summary = (EditText) activity.findViewById(R.id.detail_summary);

        Spinner spinner = (Spinner) activity.findViewById(R.id.spinner_notebook);

        boolean differences = false;
        if(summary != null && spinner != null && note != null){

            Note newNote = new Note(note.getIdentification(), note.getAuditInformation(),
                    selectedClassification == null ? Note.Classification.PUBLIC : selectedClassification,
                    summary.getText() == null ? null : summary.getText().toString());

            newNote.setDescription(repairImages(getDescriptionFromView()));
            newNote.setColor(selectedColor);

            Tag[] tags = new Tag[selectedTags.size()];
            int i = 0;
            for (String tag : selectedTags) {
                tags[i++] = allTags.get(tag);
            }

            newNote.addCategories(tags);

            String nb = spinner.getSelectedItem() == null ? null : spinner.getSelectedItem().toString();

            boolean nbSameNames = false;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                //NPE prevention
                nbSameNames = Objects.equals(nb,intialNotebookName);
            }else{
                nbSameNames = nb.equals(intialNotebookName);
            }
            differences = Utils.differentMutableData(note, newNote) || !nbSameNames || isDescriptionDirty;
        }
        return  differences;
    }

    private void printNote() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            PrintManager printManager = (PrintManager) activity.getSystemService(Context.PRINT_SERVICE);
            String jobName = getString(R.string.app_name) + " Document";
            PrintDocumentAdapter printAdapter = editor.createPrintDocumentAdapter();
            PrintJob printJob = printManager.print(jobName, printAdapter, new PrintAttributes.Builder().build());
        }
    }

    private void showAttachments() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Intent intent = new Intent(activity,AttachmentActivity.class);
            if(isNewNote){
                intent.putExtra(Utils.NOTE_UID, getUUIDForCreation());
            }else {
                intent.putExtra(Utils.NOTE_UID, note.getIdentification().getUid());
            }
            startActivityForResult(intent, ATTACHMENT_ACTIVITY_RESULT_CODE);
        }
    }

    private void goBack(){
        Intent returnIntent = new Intent();
        returnIntent.putExtra("selectedNotebookName",givenNotebook);

        ((OnFragmentCallback)activity).fragmentFinished(returnIntent, OnFragmentCallback.ResultCode.BACK);
    }
}
