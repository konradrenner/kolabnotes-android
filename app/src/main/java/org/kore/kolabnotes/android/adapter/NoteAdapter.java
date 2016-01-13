package org.kore.kolabnotes.android.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.kore.kolab.notes.Note;
import org.kore.kolab.notes.Tag;
import org.kore.kolabnotes.android.NoteSortingComparator;
import org.kore.kolabnotes.android.R;
import org.kore.kolabnotes.android.Utils;

import java.lang.reflect.Array;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NoteAdapter extends SelectableAdapter<NoteAdapter.ViewHolder> {

    private List<Note> notes;
    private int rowLayout;
    private Context context;
    private ViewHolder.ClickListener clickListener;
    private DateFormat dateFormatter;
    private int COLOR_SELECTED_NOTE;

    private List<ViewHolder> views;

    public NoteAdapter(List<Note> notes, int rowLayout, Context context, ViewHolder.ClickListener clickListener) {
        this.notes = notes;
        this.rowLayout = rowLayout;
        this.context = context;
        this.clickListener = clickListener;
        this.dateFormatter = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        views = new ArrayList<>(notes.size());
        COLOR_SELECTED_NOTE = ContextCompat.getColor(context, R.color.theme_selected_notes);
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
        Collections.sort(this.notes, new NoteSortingComparator(Utils.getNoteSorting(context)));
        this.notifyItemRangeInserted(0, notes.size() - 1);
    }

    @Override
    public ViewHolder onCreateViewHolder(final ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(rowLayout, viewGroup, false);

        return new ViewHolder(v, clickListener, notes);
    }


    public void setMetainformationVisible(boolean value){
        for(ViewHolder holder : this.views){
            if(value){
                holder.showMetainformation();
            }else{
                holder.hideMetainformation();
            }
        }

        notifyDataSetChanged();
    }

    public void setCharacteristicsVisible(boolean value){
        for(ViewHolder holder : this.views){
            if(value){
                holder.showCharacteristics();
            }else{
                holder.hideCharacteristics();
            }
        }

        notifyDataSetChanged();
    }

    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, int i) {
        boolean isSelected = SelectableAdapter.getSelectedItems().contains(i) ? true : false;
        final Note note = notes.get(i);
        viewHolder.name.setText(note.getSummary());
        viewHolder.classification.setText(context.getResources().getString(R.string.classification)+": "+note.getClassification());
        viewHolder.createdDate.setText(context.getResources().getString(R.string.creationDate)+": "+ dateFormatter.format(note.getAuditInformation().getCreationDate()));
        viewHolder.modificationDate.setText(context.getResources().getString(R.string.modificationDate)+": "+dateFormatter.format(note.getAuditInformation().getLastModificationDate()));
        viewHolder.categories.removeAllViews();

        boolean useLightColor = Utils.useLightTextColor(context, note.getColor());

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(12, 0, 0, 0);

        if(note.getCategories().isEmpty()){
            TextView textView = new TextView(context);
            textView.setText(context.getResources().getString(R.string.notags));
            if (isSelected) {
                textView.setTextColor(Color.BLACK);
            } else {
                textView.setTextColor(useLightColor ? Color.WHITE : Color.BLACK);
            }
            viewHolder.categories.addView(textView);
        }else{
            TextView textView = new TextView(context);
            textView.setText(context.getResources().getString(R.string.tags));
            if (isSelected) {
                textView.setTextColor(Color.BLACK);
            } else {
                textView.setTextColor(useLightColor ? Color.WHITE : Color.BLACK);
            }
            viewHolder.categories.addView(textView);

            ArrayList<Tag> sorted = new ArrayList<>(note.getCategories());

            Collections.sort(sorted);

            for(Tag tag : sorted){
                if(tag.getColor() == null){
                    TextView tagTextView = new TextView(context);
                    tagTextView.setText(tag.getName());
                    int backgroundColor;
                    if (isSelected) {
                        backgroundColor = COLOR_SELECTED_NOTE;
                    } else {
                        backgroundColor = note.getColor() == null ? Color.WHITE : Color.parseColor(note.getColor().getHexcode());
                    }
                    tagTextView.setTextColor(useLightColor ? Color.WHITE : Color.BLACK);
                    final Drawable drawable = context.getResources().getDrawable(R.drawable.color_background_with_dashedborder).mutate();

                    drawable.setColorFilter(backgroundColor, PorterDuff.Mode.MULTIPLY);
                    tagTextView.setBackground(drawable);
                    tagTextView.setLayoutParams(params);

                    viewHolder.categories.addView(tagTextView);
                }else{
                    boolean useLight = Utils.useLightTextColor(context, tag.getColor());

                    TextView tagTextView = new TextView(context);
                    tagTextView.setText(tag.getName());
                    tagTextView.setTextColor(useLight ? Color.WHITE : Color.BLACK);
                    final Drawable drawable = context.getResources().getDrawable(R.drawable.color_background_with_border).mutate();
                    drawable.setColorFilter(Color.parseColor(tag.getColor().getHexcode()), PorterDuff.Mode.MULTIPLY);
                    tagTextView.setBackground(drawable);
                    tagTextView.setLayoutParams(params);

                    viewHolder.categories.addView(tagTextView);
                }
            }
        }

        /* If note selected */
        if (isSelected) {
            viewHolder.cardView.setCardBackgroundColor(COLOR_SELECTED_NOTE);
            viewHolder.name.setBackgroundColor(COLOR_SELECTED_NOTE);
            viewHolder.classification.setBackgroundColor(COLOR_SELECTED_NOTE);
            viewHolder.createdDate.setBackgroundColor(COLOR_SELECTED_NOTE);
            viewHolder.modificationDate.setBackgroundColor(COLOR_SELECTED_NOTE);
            viewHolder.categories.setBackgroundColor(COLOR_SELECTED_NOTE);

            viewHolder.name.setTextColor(Color.BLACK);
            viewHolder.classification.setTextColor(Color.BLACK);
            viewHolder.createdDate.setTextColor(Color.BLACK);
            viewHolder.modificationDate.setTextColor(Color.BLACK);
        } else {
            if(note != null && note.getColor() != null){
                viewHolder.cardView.setCardBackgroundColor(Color.parseColor(note.getColor().getHexcode()));
                viewHolder.name.setBackgroundColor(Color.parseColor(note.getColor().getHexcode()));
                viewHolder.classification.setBackgroundColor(Color.parseColor(note.getColor().getHexcode()));
                viewHolder.createdDate.setBackgroundColor(Color.parseColor(note.getColor().getHexcode()));
                viewHolder.modificationDate.setBackgroundColor(Color.parseColor(note.getColor().getHexcode()));
                viewHolder.categories.setBackgroundColor(Color.parseColor(note.getColor().getHexcode()));

            /*
            * Text color depending on background color:
            * If spectrum from cyan to red and saturation greater than or equal to 0.5 - text is white.
            * If spectrum is not included in these borders or brightness greater than or equal to 0.8 - text is black.
            */
                if (useLightColor) {
                    viewHolder.name.setTextColor(Color.WHITE);
                    viewHolder.classification.setTextColor(Color.WHITE);
                    viewHolder.createdDate.setTextColor(Color.WHITE);
                    viewHolder.modificationDate.setTextColor(Color.WHITE);
                } else {
                    viewHolder.name.setTextColor(Color.BLACK);
                    viewHolder.classification.setTextColor(Color.GRAY);
                    viewHolder.createdDate.setTextColor(Color.GRAY);
                    viewHolder.modificationDate.setTextColor(Color.GRAY);
                }

            } else {
                viewHolder.cardView.setCardBackgroundColor(Color.WHITE);
                viewHolder.name.setBackgroundColor(Color.WHITE);
                viewHolder.classification.setBackgroundColor(Color.WHITE);
                viewHolder.createdDate.setBackgroundColor(Color.WHITE);
                viewHolder.modificationDate.setBackgroundColor(Color.WHITE);
                viewHolder.categories.setBackgroundColor(Color.WHITE);

                viewHolder.name.setTextColor(Color.BLACK);
                viewHolder.classification.setTextColor(Color.GRAY);
                viewHolder.createdDate.setTextColor(Color.GRAY);
                viewHolder.modificationDate.setTextColor(Color.GRAY);
            }
        }
        Utils.setElevation(viewHolder.cardView, 5);

        if(Utils.getShowMetainformation(context)){
            viewHolder.showMetainformation();
        }else{
            viewHolder.hideMetainformation();
        }

        if(Utils.getShowCharacteristics(context)){
            viewHolder.showCharacteristics();
        }else{
            viewHolder.hideCharacteristics();
        }
    }

    @Override
    public int getItemCount() {
        return notes == null ? 0 : notes.size();
    }

    public boolean isEmpty() {
        return notes.isEmpty() ? true : false;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener{
        TextView name;
        TextView classification;
        TextView createdDate;
        TextView modificationDate;
        LinearLayout categories;
        CardView cardView;
        private ClickListener listener;
        private List<Note> notes;

        public ViewHolder(View itemView, ClickListener listener, List<Note> notes) {
            super(itemView);

            this.notes = notes;
            this.listener = listener;

            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);

            name = (TextView) itemView.findViewById(R.id.noteSummary);
            classification = (TextView) itemView.findViewById(R.id.classification);
            createdDate = (TextView) itemView.findViewById(R.id.createdDate);
            modificationDate = (TextView) itemView.findViewById(R.id.modificationDate);
            categories = (LinearLayout) itemView.findViewById(R.id.categories);
            cardView = (CardView)itemView;
        }

        @Override
        public void onClick(View v) {
            if (listener != null) {
                ViewParent parent = v.getParent();
                if(parent instanceof RecyclerView){
                    RecyclerView recyclerView = (RecyclerView)parent;
                    for(int i=0; i < recyclerView.getChildCount(); i++){
                        Utils.setElevation(recyclerView.getChildAt(i),5);
                        if(i == getAdapterPosition()){
                            Utils.setElevation(recyclerView.getChildAt(i),30);
                        }
                    }
                }
                listener.onItemClicked(getAdapterPosition(), notes.get(getAdapterPosition()));
            }
        }

        @Override
        public boolean onLongClick(View v) {
            if (listener != null) {
                ViewParent parent = v.getParent();
                if(parent instanceof RecyclerView){
                    RecyclerView recyclerView = (RecyclerView)parent;
                    for(int i=0; i < recyclerView.getChildCount(); i++){
                        Utils.setElevation(recyclerView.getChildAt(i),5);
                    }
                }

                return listener.onItemLongClicked(getAdapterPosition(), notes.get(getAdapterPosition()));
            }
            return false;
        }

        public interface ClickListener {
            void onItemClicked(int position, Note note);
            boolean onItemLongClicked(int position, Note note);
        }

        void hideMetainformation(){
            createdDate.setVisibility(View.GONE);
            modificationDate.setVisibility(View.GONE);
        }

        void showMetainformation(){
            createdDate.setVisibility(View.VISIBLE);
            modificationDate.setVisibility(View.VISIBLE);
        }

        void hideCharacteristics(){
            classification.setVisibility(View.GONE);
            categories.setVisibility(View.GONE);
        }

        void showCharacteristics(){
            //issue #85
            classification.setVisibility(View.GONE);
            categories.setVisibility(View.VISIBLE);
        }
    }
}
