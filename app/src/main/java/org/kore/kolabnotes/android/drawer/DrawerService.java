package org.kore.kolabnotes.android.drawer;

import android.content.Context;
import android.graphics.Color;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;

import org.kore.kolab.notes.Notebook;
import org.kore.kolab.notes.SharedNotebook;
import org.kore.kolab.notes.Tag;
import org.kore.kolabnotes.android.R;

import java.util.Collection;

/**
 * Created by koni on 22.05.17.
 */

public class DrawerService {

    private final NavigationView view;
    private final DrawerLayout layout;

    public DrawerService(NavigationView view, DrawerLayout layout) {
        this.view = view;
        this.layout = layout;
    }

    public void overrideNotebooks(OnDrawerSelectionChangedListener listener, Collection<Notebook> notebooks){
        final Menu overview = view.getMenu();
        final SubMenu notebookSubmenu = overview.findItem(R.id.navigation_notebooks).getSubMenu();

        notebookSubmenu.clear();

        for(Notebook book : notebooks){
            addNotebook(view.getContext(), notebookSubmenu, listener, book);
        }
    }

    public void overrideTags(OnDrawerSelectionChangedListener listener, Collection<Tag> tags){
        final Menu overview = view.getMenu();
        final SubMenu tagSubmenu = overview.findItem(R.id.navigation_tags).getSubMenu();

        tagSubmenu.clear();

        for(Tag tag : tags){
            addTag(tagSubmenu, listener, tag);
        }
    }

    public MenuItem addTag(OnDrawerSelectionChangedListener listener, Tag tag){
        return addTag(view.getMenu().findItem(R.id.navigation_tags).getSubMenu(), listener, tag);
    }

    private MenuItem addTag(SubMenu tagMenu, OnDrawerSelectionChangedListener listener, Tag tag){
        SpannableString spannable = new SpannableString(tag.getName());
        if(tag.getColor() != null) {
            spannable.setSpan(new ForegroundColorSpan(Color.parseColor(tag.getColor().getHexcode())), 0, spannable.length(), 0);
        }
        final MenuItem newTagEntry = tagMenu.add(spannable);
        newTagEntry.setOnMenuItemClickListener(new OnTagItemClickedListener(listener, layout));
        newTagEntry.setCheckable(true);
        return newTagEntry;
    }

    public MenuItem addNotebook(OnDrawerSelectionChangedListener listener, Notebook notebook){
        return addNotebook(view.getContext(), view.getMenu().findItem(R.id.navigation_notebooks).getSubMenu(), listener, notebook);
    }

    private MenuItem addNotebook(Context context, SubMenu notebookMenu, OnDrawerSelectionChangedListener listener, Notebook notebook){
        final MenuItem newNotebookEntry = notebookMenu.add(notebook.getSummary());
        setNotebookPermissionIcon(context, notebook, newNotebookEntry);
        newNotebookEntry.setOnMenuItemClickListener(new OnNotebookItemClickedListener(listener, layout));
        newNotebookEntry.setCheckable(true);
        return newNotebookEntry;
    }

    private void setNotebookPermissionIcon(Context context, Notebook book, MenuItem newNotebookEntry){
        if(book.isShared()){
            SharedNotebook shared = (SharedNotebook) book;

            if(shared.isNoteCreationAllowed() && !shared.isNoteModificationAllowed()){
                newNotebookEntry.setIcon(context.getResources().getDrawable(R.drawable.ic_note_add_black_24dp));
            }else if(!shared.isNoteCreationAllowed() && !shared.isNoteModificationAllowed()){
                newNotebookEntry.setIcon(context.getResources().getDrawable(R.drawable.ic_lock_black_24dp));
            }else if(!shared.isNoteCreationAllowed() && shared.isNoteModificationAllowed()){
                newNotebookEntry.setIcon(context.getResources().getDrawable(R.drawable.ic_create_black_24dp));
            }
        }
    }
}
