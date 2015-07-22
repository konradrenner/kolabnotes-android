package org.kore.kolabnotes.android;

import org.kore.kolab.notes.Note;
import org.kore.kolabnotes.android.content.NoteSorting;

import java.util.Comparator;

/**
 * Created by koni on 22.07.15.
 */
public class NoteSortingComparator implements Comparator<Note> {

    private final NoteSorting sorting;

    public NoteSortingComparator(NoteSorting sorting) {
        this.sorting = sorting;
    }

    @Override
    public int compare(Note note1, Note note2) {
        return Utils.SortingColumns.valueOf(sorting.getColumnName()).compare(note1,note2,sorting.getDirection());
    }
}
