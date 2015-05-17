package org.kore.kolabnotes.android.adapter;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.support.v7.widget.CardView;
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
        final Note note = notes.get(i);

        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, int i) {
        final Note note = notes.get(i);
        viewHolder.name.setText(note.getSummary());
        viewHolder.classification.setText(mAct.getResources().getString(R.string.classification)+": "+note.getClassification());
        viewHolder.createdDate.setText(mAct.getResources().getString(R.string.creationDate)+": "+note.getAuditInformation().getCreationDate());
        viewHolder.modificationDate.setText(mAct.getResources().getString(R.string.modificationDate)+": "+note.getAuditInformation().getLastModificationDate());
        StringBuilder tags = new StringBuilder();
        for(String tag : note.getCategories()){
            tags.append(tag);
            tags.append(", ");
        }
        if(tags.length() > 0) {
            viewHolder.categories.setText(mAct.getResources().getString(R.string.tags)+": "+tags.substring(0, tags.length() - 2));
        }else{
            viewHolder.categories.setText(mAct.getResources().getString(R.string.notags));
        }

        if(note != null && note.getColor() != null){
            viewHolder.cardView.setCardBackgroundColor(Color.parseColor(note.getColor().getHexcode()));
        }else{
            viewHolder.cardView.setCardBackgroundColor(Color.WHITE);
        }


        viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAct.animateActivity(note);
            }
        });
    }

    @Override
    public int getItemCount() {
        return notes == null ? 0 : notes.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        TextView classification;
        TextView createdDate;
        TextView modificationDate;
        TextView categories;
        CardView cardView;

        public ViewHolder(View itemView) {
            super(itemView);
            name = (TextView) itemView.findViewById(R.id.noteSummary);
            classification = (TextView) itemView.findViewById(R.id.classification);
            createdDate = (TextView) itemView.findViewById(R.id.createdDate);
            modificationDate = (TextView) itemView.findViewById(R.id.modificationDate);
            categories = (TextView) itemView.findViewById(R.id.categories);
            cardView = (CardView)itemView;
        }

    }
}
