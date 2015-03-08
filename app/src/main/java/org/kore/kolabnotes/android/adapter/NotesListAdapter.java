package org.kore.kolabnotes.android.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;

import org.kore.kolab.notes.Note;

import java.util.List;

/**
 * Created by koni on 08.03.15.
 */
public class NotesListAdapter<T extends Note> extends ArrayAdapter<T> {

    public NotesListAdapter(Context context, int resource, int textViewResourceId, List<T> objects) {
        super(context, resource, textViewResourceId, objects);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView text = new TextView(getContext());

        T item = getItem(position);
        text.setText(item.getSummary());

        return text;
    }
}
