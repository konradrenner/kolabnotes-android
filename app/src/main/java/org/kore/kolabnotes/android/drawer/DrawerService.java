package org.kore.kolabnotes.android.drawer;

import android.content.Context;
import android.graphics.Color;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;

import org.kore.kolab.notes.Notebook;
import org.kore.kolab.notes.SharedNotebook;
import org.kore.kolab.notes.Tag;
import org.kore.kolabnotes.android.R;
import org.kore.kolabnotes.android.Utils;

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

    public void setNotesFromAccountClickListener(final OnDrawerSelectionChangedListener listener){
        final Menu menu = view.getMenu();
        menu.findItem(R.id.all_notes).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                listener.allNotesSelected();
                layout.closeDrawer(Gravity.LEFT);
                return true;
            }
        });

        menu.findItem(R.id.all_notes_from_account).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                listener.allNotesFromAccountSelected();
                layout.closeDrawer(Gravity.LEFT);
                return true;
            }
        });
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

    public void deleteNotebook(String notebookName){
        final SubMenu notebookSubmenu = view.getMenu().findItem(R.id.navigation_notebooks).getSubMenu();

        for(int i=0; i<notebookSubmenu.size(); i++){
            final int currentId = notebookSubmenu.getItem(i).getItemId();
            final String actualTitle = notebookSubmenu.getItem(i).getTitle().toString();
            if(actualTitle.equals(notebookName)){
                notebookSubmenu.removeItem(currentId);
            }
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
        final MenuItem newTagEntry = tagMenu.add(Menu.NONE, tagMenu.size(), Menu.NONE, spannable);
        newTagEntry.setOnMenuItemClickListener(new OnTagItemClickedListener(listener, layout));
        return newTagEntry;
    }

    public MenuItem addNotebook(OnDrawerSelectionChangedListener listener, Notebook notebook){
        return addNotebook(view.getContext(), view.getMenu().findItem(R.id.navigation_notebooks).getSubMenu(), listener, notebook);
    }

    private MenuItem addNotebook(Context context, SubMenu notebookMenu, OnDrawerSelectionChangedListener listener, Notebook notebook){
        final MenuItem newNotebookEntry = notebookMenu.add(Menu.NONE, notebookMenu.size(), Menu.NONE, notebook.getSummary());
        setNotebookPermissionIcon(context, notebook, newNotebookEntry);
        newNotebookEntry.setOnMenuItemClickListener(new OnNotebookItemClickedListener(listener, layout));
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
