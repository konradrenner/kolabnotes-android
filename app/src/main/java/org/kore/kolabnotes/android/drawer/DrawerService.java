package org.kore.kolabnotes.android.drawer;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;

import org.kore.kolab.notes.Note;
import org.kore.kolab.notes.Notebook;
import org.kore.kolab.notes.SharedNotebook;
import org.kore.kolab.notes.Tag;
import org.kore.kolabnotes.android.R;

import java.util.Collection;

/**
 * Created by koni on 22.05.17.
 */

public class DrawerService {

    public void overrideNotebooks(NavigationView view, Collection<Notebook> notebooks){
        final Menu overview = view.getMenu();
        final SubMenu notebookSubmenu = overview.findItem(R.id.navigation_notebooks).getSubMenu();

        notebookSubmenu.clear();

        for(Notebook book : notebooks){
            addNotebook(view.getContext(), notebookSubmenu, book);
        }
    }

    public void overrideTags(NavigationView view, Collection<Tag> tags){
        final Menu overview = view.getMenu();
        final SubMenu tagSubmenu = overview.findItem(R.id.navigation_tags).getSubMenu();

        tagSubmenu.clear();

        for(Tag tag : tags){
            addTag(tagSubmenu, tag);
        }
    }

    public void addTag(NavigationView view, Tag tag){
        addTag(view.getMenu().findItem(R.id.navigation_tags).getSubMenu(), tag);
    }

    private void addTag(SubMenu tagMenu, Tag tag){
        SpannableString spannable = new SpannableString(tag.getName());
        if(tag.getColor() != null) {
            spannable.setSpan(new ForegroundColorSpan(Color.parseColor(tag.getColor().getHexcode())), 0, spannable.length(), 0);
        }
        final MenuItem newTagEntry = tagMenu.add(spannable);
    }

    public void addNotebook(NavigationView view, Notebook notebook){
        addNotebook(view.getContext(), view.getMenu().findItem(R.id.navigation_notebooks).getSubMenu(), notebook);
    }

    private void addNotebook(Context context, SubMenu notebookMenu, Notebook notebook){
        final MenuItem newNotebookEntry = notebookMenu.add(notebook.getSummary());
        newNotebookEntry.setIcon(getNotebookPermissionIcon(context, notebook));
    }

    @NonNull
    private Drawable getNotebookPermissionIcon(Context context, Notebook book){
        if(book.isShared()){
            SharedNotebook shared = (SharedNotebook) book;

            if(shared.isNoteCreationAllowed() && !shared.isNoteModificationAllowed()){
                return context.getResources().getDrawable(R.drawable.ic_note_add_black_24dp);
            }else if(!shared.isNoteCreationAllowed() && !shared.isNoteModificationAllowed()){
                return context.getResources().getDrawable(R.drawable.ic_lock_black_24dp);
            }else if(!shared.isNoteCreationAllowed() && shared.isNoteModificationAllowed()){
                return context.getResources().getDrawable(R.drawable.ic_create_black_24dp);
            }
        }
        final ShapeDrawable shapeDrawable = new ShapeDrawable();
        shapeDrawable.setAlpha(0);
        return shapeDrawable;
    }
}
