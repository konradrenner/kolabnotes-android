package org.kore.kolabnotes.android.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import androidx.core.content.ContextCompat;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;;
import android.widget.TextView;

import org.kore.kolab.notes.Tag;
import org.kore.kolabnotes.android.R;
import org.kore.kolabnotes.android.Utils;

import java.text.DateFormat;
import java.util.Collections;
import java.util.List;

/**
 * Created by yaroslav on 10.01.16.
 */
public class TagAdapter extends SelectableAdapter<TagAdapter.ViewHolder>{

    DateFormat dateFormatter;
    Context context;
    private ViewHolder.ClickListener clickListener;
    private int rowLayout;
    List<Tag> tags;
    private int COLOR_SELECTED_NOTE;

    public TagAdapter(List<Tag> tags, Context context, int rowLayout, ViewHolder.ClickListener clickListener) {
        this.dateFormatter = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        this.context = context;
        this.rowLayout = rowLayout;
        this.clickListener = clickListener;
        Collections.sort(tags);
        this.tags = tags;
        COLOR_SELECTED_NOTE = ContextCompat.getColor(context, R.color.theme_selected_notes);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(rowLayout, parent, false);

        return new ViewHolder(v, clickListener, tags);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        boolean isSelected = SelectableAdapter.getSelectedItems().contains(position) ? true : false;
        final Tag tag = tags.get(position);
        holder.tagName.setText(tag.getName());
        holder.tagCreatedDate.setText(String.format("%s: %s", context.getResources().getString(R.string.tag_creationDate), dateFormatter.format(tag.getAuditInformation().getCreationDate())));
        holder.tagModificationDate.setText(String.format("%s: %s", context.getResources().getString(R.string.tag_modificationDate), dateFormatter.format(tag.getAuditInformation().getLastModificationDate())));
        Drawable circle = ContextCompat.getDrawable(context, R.drawable.tag_list_default_circle);
        if (tag.getColor() != null) {
            circle.setColorFilter(Color.parseColor(tag.getColor().getHexcode()), PorterDuff.Mode.MULTIPLY);
        }
        holder.tagColor.setBackground(circle);
        Utils.setElevation(holder.tagColor, 3);

        if (isSelected) {
            holder.tagColor.setClickable(false);
            holder.tagName.setBackgroundColor(COLOR_SELECTED_NOTE);
            holder.tagCreatedDate.setBackgroundColor(COLOR_SELECTED_NOTE);
            holder.tagModificationDate.setBackgroundColor(COLOR_SELECTED_NOTE);
            holder.tagHolder.setBackgroundColor(COLOR_SELECTED_NOTE);
        } else {
            holder.tagColor.setClickable(true);
            holder.tagName.setBackgroundColor(Color.TRANSPARENT);
            holder.tagCreatedDate.setBackgroundColor(Color.TRANSPARENT);
            holder.tagModificationDate.setBackgroundColor(Color.TRANSPARENT);
            holder.tagHolder.setBackground(ContextCompat.getDrawable(context, R.drawable.item_round));
        }
    }

    public void clearTags() {
        int size = this.tags.size();
        if (size > 0) {
            for (int i = 0; i < size; i++) {
                tags.remove(0);
            }

            this.notifyItemRangeRemoved(0, size);
        }
    }

    public void deleteTags(List<Tag> tags) {
        this.tags.removeAll(tags);
        this.notifyDataSetChanged();
    }

    public void addTags(List<Tag> tags) {
        this.tags.addAll(tags);
        Collections.sort(this.tags);
        this.notifyItemRangeInserted(0, tags.size() - 1);
    }

    @Override
    public int getItemCount() {
        return tags == null ? 0 : tags.size();
    }

    public boolean isEmpty() {
        return tags.isEmpty() ? true : false;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {

        TextView tagName;
        TextView tagCreatedDate;
        TextView tagModificationDate;
        View tagColor;
        CardView tagHolder;
        private ClickListener listener;
        private List<Tag> tags;


        public ViewHolder(View itemView, final ClickListener listener, final List<Tag> tags) {
            super(itemView);
            this.tags = tags;
            this.listener = listener;

            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);

            tagName = (TextView) itemView.findViewById(R.id.tagName);
            tagCreatedDate = (TextView) itemView.findViewById(R.id.tagCreatedDate);
            tagModificationDate = (TextView) itemView.findViewById(R.id.tagModificationDate);
            tagColor = itemView.findViewById(R.id.tagColor);
            tagColor.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onColorPickerClicked(getAdapterPosition(), tags.get(getAdapterPosition()));
                    }
                }
            });
            tagHolder = (CardView) itemView;
        }

        @Override
        public void onClick(View v) {
            if (listener != null) {
                listener.onItemClicked(getAdapterPosition(), tags.get(getAdapterPosition()));
            }
        }

        @Override
        public boolean onLongClick(View v) {
            if (listener != null) {
                return listener.onItemLongClicked(getAdapterPosition(), tags.get(getAdapterPosition()));
            }
            return false;
        }

        public interface ClickListener {
            void onColorPickerClicked(int position, Tag tag);
            void onItemClicked(int position, Tag tag);
            boolean onItemLongClicked(int position, Tag tag);
        }
    }
}
