package org.kore.kolabnotes.android.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
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

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.ViewHolder> {

    private List<Note> notes;
    private int rowLayout;
    private Context context;
    private NoteSelectedListener listener;
    private DateFormat dateFormatter;

    private List<ViewHolder> views;

    public NoteAdapter(List<Note> notes, int rowLayout, Context context, NoteSelectedListener listener) {
        this.notes = notes;
        this.rowLayout = rowLayout;
        this.context = context;
        this.listener = listener;
        this.dateFormatter = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        views = new ArrayList<>(notes.size());
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

        return new ViewHolder(v);
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
            textView.setTextColor(useLightColor ? Color.WHITE : Color.BLACK);
            viewHolder.categories.addView(textView);
        }else{
            TextView textView = new TextView(context);
            textView.setText(context.getResources().getString(R.string.tags));
            textView.setTextColor(useLightColor ? Color.WHITE : Color.BLACK);
            viewHolder.categories.addView(textView);

            ArrayList<Tag> sorted = new ArrayList<>(note.getCategories());

            Collections.sort(sorted);

            for(Tag tag : sorted){
                if(tag.getColor() == null){
                    TextView tagTextView = new TextView(context);
                    tagTextView.setText(tag.getName());
                    tagTextView.setTextColor(useLightColor ? Color.WHITE : Color.BLACK);
                    final Drawable drawable = context.getResources().getDrawable(R.drawable.color_background_with_dashedborder).mutate();

                    int backgroundColor = note.getColor() == null ? Color.WHITE : Color.parseColor(note.getColor().getHexcode());

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

        }else{
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
        Utils.setElevation(viewHolder.cardView,5);

        viewHolder.itemView.setOnClickListener(new ClickListener(i));

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
                    Utils.setElevation(recyclerView.getChildAt(i),5);
                }
            }
            Utils.setElevation(v,30);
            listener.onSelect(notes.get(index), same);
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
        LinearLayout categories;
        CardView cardView;

        public ViewHolder(View itemView) {
            super(itemView);
            name = (TextView) itemView.findViewById(R.id.noteSummary);
            classification = (TextView) itemView.findViewById(R.id.classification);
            createdDate = (TextView) itemView.findViewById(R.id.createdDate);
            modificationDate = (TextView) itemView.findViewById(R.id.modificationDate);
            categories = (LinearLayout) itemView.findViewById(R.id.categories);
            cardView = (CardView)itemView;
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
            classification.setVisibility(View.VISIBLE);
            categories.setVisibility(View.VISIBLE);
        }
    }

    public interface NoteSelectedListener{
        void onSelect(Note note, boolean sameSelection);
    }
}
