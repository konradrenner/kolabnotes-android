package org.kore.kolabnotes.android.adapter;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.TextView;

import org.kore.kolab.notes.Note;
import org.kore.kolab.notes.Tag;
import org.kore.kolabnotes.android.MainActivity;
import org.kore.kolabnotes.android.R;

import java.text.DateFormat;
import java.util.Collections;
import java.util.List;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.ViewHolder> {

    private List<Note> notes;
    private int rowLayout;
    private Context context;
    private NoteSelectedListener listener;
    private DateFormat dateFormatter;

    public NoteAdapter(List<Note> notes, int rowLayout, Context context, NoteSelectedListener listener) {
        this.notes = notes;
        this.rowLayout = rowLayout;
        this.context = context;
        this.listener = listener;
        this.dateFormatter = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
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
        Collections.sort(this.notes);
        this.notifyItemRangeInserted(0, notes.size() - 1);
    }

    @Override
    public ViewHolder onCreateViewHolder(final ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(rowLayout, viewGroup, false);

        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, int i) {
        final Note note = notes.get(i);
        viewHolder.name.setText(note.getSummary());
        viewHolder.classification.setText(context.getResources().getString(R.string.classification)+": "+note.getClassification());
        viewHolder.createdDate.setText(context.getResources().getString(R.string.creationDate)+": "+ dateFormatter.format(note.getAuditInformation().getCreationDate()));
        viewHolder.modificationDate.setText(context.getResources().getString(R.string.modificationDate)+": "+dateFormatter.format(note.getAuditInformation().getLastModificationDate()));
        StringBuilder tags = new StringBuilder();
        for(Tag tag : note.getCategories()){
            tags.append(tag.getName());
            tags.append(", ");
        }
        if(tags.length() > 0) {
            viewHolder.categories.setText(context.getResources().getString(R.string.tags)+": "+tags.substring(0, tags.length() - 2));
        }else{
            viewHolder.categories.setText(context.getResources().getString(R.string.notags));
        }

        if(note != null && note.getColor() != null){
            viewHolder.cardView.setCardBackgroundColor(Color.parseColor(note.getColor().getHexcode()));
        }else{
            viewHolder.cardView.setCardBackgroundColor(Color.WHITE);
        }
        viewHolder.cardView.setElevation(5);


        viewHolder.itemView.setOnClickListener(new ClickListener(i));
    }

    class ClickListener implements View.OnClickListener{
        public int index;

        public ClickListener(int index) {
            this.index = index;
        }

        @Override
        public void onClick(View v) {
            boolean same = false;
            ViewParent parent = v.getParent();
            if(parent instanceof RecyclerView){
                RecyclerView recyclerView = (RecyclerView)parent;
                for(int i=0; i < recyclerView.getChildCount(); i++){
                    recyclerView.getChildAt(i).setElevation(5);
                }
            }
            v.setElevation(30);
            listener.onSelect(notes.get(index),same);
        }
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

    public interface NoteSelectedListener{
        void onSelect(Note note, boolean sameSelection);
    }
}
