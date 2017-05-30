package org.kore.kolabnotes.android.drawer;

/**
 * Created by koni on 30.05.17.
 */

public interface OnDrawerSelectionChangedListener {
    void notebookSelected(String notebookName);
    void tagSelected(String tagName);
    void allNotesSelected();
    void allNotesFromAccountSelected();
}
