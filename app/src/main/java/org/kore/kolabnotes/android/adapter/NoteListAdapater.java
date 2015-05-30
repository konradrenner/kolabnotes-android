package org.kore.kolabnotes.android.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.kore.kolab.notes.Note;
import org.kore.kolabnotes.android.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by koni on 30.05.15.
 */
public class NoteListAdapater extends ArrayAdapter<Note> {

    private final ArrayList<Note> notes;

    public NoteListAdapater(Context context, int resource, List<Note> objects) {
        super(context, resource);
        notes = new ArrayList<>(objects);
    }

    @Override
    public Note getItem(int position) {
        return notes.get(position);
    }

    @Override
    public int getPosition(Note item) {
        for(int i=0; i<notes.size();i++){
            if(item.equals(notes.get(i))){
                return i;
            }
        }
        return -1;
    }

    @Override
    public int getCount() {
        return notes.size();
    }

    @Override
    public void sort(Comparator<? super Note> comparator) {
        Collections.sort(notes,comparator);
    }

    @Override
    public void insert(Note object, int index) {
        notes.add(index,object);
    }

    @Override
    public void addAll(Note... items) {
        notes.addAll(Arrays.asList(items));
    }

    @Override
    public void addAll(Collection<? extends Note> collection) {
        notes.addAll(collection);
    }

    @Override
    public void add(Note object) {
        notes.add(object);
    }

    @Override
    public void remove(Note object) {
        notes.remove(object);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Note note = notes.get(position);

        TextView textView = (TextView) convertView.findViewById(R.id.list_note_row_summary);
        textView.setText(note.getSummary());

        return convertView;
    }
}
