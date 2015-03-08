package org.kore.kolabnotes.android;

import android.app.Activity;
import android.app.Fragment;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import org.kore.kolab.notes.AccountInformation;
import org.kore.kolab.notes.Note;
import org.kore.kolab.notes.Notebook;
import org.kore.kolab.notes.NotesRepository;
import org.kore.kolab.notes.local.LocalNotesRepository;
import org.kore.kolab.notes.v3.KolabNotesParserV3;
import org.kore.kolabnotes.android.async.NotesLoaderTask;

import java.util.HashMap;
import java.util.Map;


public class MainActivity extends Activity implements NotesFragment.OnNotesFragmentInteractionListener, NotebooksFragment.OnNotebookFragmentInteractionListener, NoteFragment.OnNoteFragmentInteractionListener{

    private final static Map<String, NotesRepository> REPO_CACHE = new HashMap<>();
    private String selectedNotebook;
    private String selectedNote;

    static{
        //At the moment there are just notebooks supported which are in the Notes folder
        REPO_CACHE.put("Notes",new LocalNotesRepository(new KolabNotesParserV3(),"Notes"));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        /*if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new NotebooksFragment())
                    .add(R.id.container, new NotesFragment())
                    .add(R.id.container, new NoteFragment())
                    .commit();
        }*/
        //new NotesLoaderTask().execute();
        NotesRepository notes = REPO_CACHE.get("Notes");
        Notebook notebook = notes.createNotebook("Book1", "Book1");
        Note note = notebook.createNote("Note1", "Note1");
        note.setDescription("This is note 1");
        note = notebook.createNote("Note2", "Note2");
        note.setDescription("This is note 2");

        notebook = notes.createNotebook("Book2", "Book2");
        note = notebook.createNote("Note21", "Note21");
        note.setDescription("Hallo");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }



        return super.onOptionsItemSelected(item);
    }

    public static NotesRepository getRepository(String rootFolder){
        return REPO_CACHE.get(rootFolder);
    }

    @Override
    public void onNoteFragmentInteraction(Uri uri) {
        //Nothing at the moment
    }

    @Override
    public void onNotebookFragmentInteraction(String id) {
        selectedNotebook = id;
    }

    @Override
    public void onNotesFragmentInteraction(String id) {
        selectedNote = id;
    }

    public String getSelectedNotebook() {
        return selectedNotebook;
    }

    public String getSelectedNote() {
        return selectedNote;
    }
}
