package org.kore.kolabnotes.android.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.kore.kolab.notes.Note;
import org.kore.kolabnotes.android.MainPhoneActivity;
import org.kore.kolabnotes.android.R;

import java.util.List;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.ViewHolder> {

    private List<Note> notes;
    private int rowLayout;
    private MainPhoneActivity mAct;

    public NoteAdapter(List<Note> notes, int rowLayout, MainPhoneActivity act) {
        this.notes = notes;
        this.rowLayout = rowLayout;
        this.mAct = act;
    }


    public void clearNotes() {
        int size = this.notes.size();
        if (size > 0) {
            for (int i = 0; i < size; i++) {
                notes.remove(0);
            }

            this.notifyItemRangeRemoved(0, size);
        }
    }

    public void addNotes(List<Note> notes) {
        this.notes.addAll(notes);
        this.notifyItemRangeInserted(0, notes.size() - 1);
    }

    @Override
    public ViewHolder onCreateViewHolder(final ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(rowLayout, viewGroup, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, int i) {
        final Note appInfo = notes.get(i);
        viewHolder.name.setText(appInfo.getSummary());
        //viewHolder.image.setImageDrawable(appInfo.getIcon());

        viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAct.animateActivity(appInfo, viewHolder.image);
            }
        });
    }

    @Override
    public int getItemCount() {
        return notes == null ? 0 : notes.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView name;
        public ImageView image;

        public ViewHolder(View itemView) {
            super(itemView);
            name = (TextView) itemView.findViewById(R.id.countryName);
            image = (ImageView) itemView.findViewById(R.id.countryImage);
        }

    }
}
