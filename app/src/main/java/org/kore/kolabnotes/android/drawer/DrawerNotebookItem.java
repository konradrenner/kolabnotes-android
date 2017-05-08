package org.kore.kolabnotes.android.drawer;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;

import org.kore.kolab.notes.Notebook;
import org.kore.kolab.notes.SharedNotebook;
import org.kore.kolabnotes.android.R;

import java.util.Optional;

/**
 * Created by koni on 08.05.17.
 */

public class DrawerNotebookItem extends AbstractDrawerItem{
    private final Notebook notebook;

    public DrawerNotebookItem(Notebook notebook) {
        super(notebook.getIdentification().getUid(), notebook.getSummary());
        this.notebook = notebook;
    }

    public Notebook getNotebook() {
        return notebook;
    }

    public Drawable getPermessionIcon(Context context){
        if(notebook.isShared()){
            SharedNotebook shared =((SharedNotebook) notebook);

            if(shared.isNoteCreationAllowed() && !shared.isNoteModificationAllowed()){
                return context.getResources().getDrawable(R.drawable.ic_note_add_black_24dp);
            }else if(!shared.isNoteCreationAllowed() && !shared.isNoteModificationAllowed()){
                return context.getResources().getDrawable(R.drawable.ic_lock_black_24dp);
            }else if(!shared.isNoteCreationAllowed() && shared.isNoteModificationAllowed()){
                return context.getResources().getDrawable(R.drawable.ic_create_black_24dp);
            }
        }
        return new ColorDrawable(Color.TRANSPARENT);
    }

    @Override
    public String toString() {
        return "DrawerNotebookItem{" +
                super.toString() +
                "notebook=" + notebook +
                '}';
    }
}
