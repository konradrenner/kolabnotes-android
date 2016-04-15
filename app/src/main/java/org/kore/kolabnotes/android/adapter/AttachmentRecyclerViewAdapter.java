package org.kore.kolabnotes.android.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import org.kore.kolab.notes.Attachment;
import org.kore.kolab.notes.Note;
import org.kore.kolabnotes.android.NoteSortingComparator;
import org.kore.kolabnotes.android.R;
import org.kore.kolabnotes.android.Utils;
import org.kore.kolabnotes.android.fragment.AttachmentFragment;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * {@link RecyclerView.Adapter} that can display a {@link Attachment} and makes a call to the
 * specified {@link AttachmentFragment.OnListFragmentInteractionListener}.
 */
public class AttachmentRecyclerViewAdapter extends RecyclerView.Adapter<AttachmentRecyclerViewAdapter.ViewHolder> {

    private final List<Attachment> mValues;
    private final AttachmentFragment.OnListFragmentInteractionListener mListener;
    private final Context context;

    public AttachmentRecyclerViewAdapter(List<Attachment> items,Context context, AttachmentFragment.OnListFragmentInteractionListener listener) {
        mValues = items;
        mListener = listener;
        this.context = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_attachment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mItem = mValues.get(position);
        holder.mfilesize.setText(holder.mItem.getData() == null ? "0" : Integer.toString(holder.mItem.getData().length));
        holder.mfilename.setText(holder.mItem.getFileName());
        holder.mMimeType.setText(holder.mItem.getMimeType());

        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mListener) {
                    mListener.onListFragmentInteraction(AttachmentFragment.OnListFragmentInteractionListener.Operation.SELECT, holder.mItem);
                }
            }
        });

        holder.mDeleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteAttachment(holder.mItem);
                mListener.onListFragmentInteraction(AttachmentFragment.OnListFragmentInteractionListener.Operation.DELETE, holder.mItem);
            }
        });

        if(Utils.isTablet(context.getResources())){
            //TODO check if mimetype is possible to preview
            holder.mPreviewButton.setVisibility(View.VISIBLE);
        }
    }

    public void deleteAttachment(Attachment attachment) {
        this.mValues.remove(attachment);
        this.notifyDataSetChanged();
    }

    public void addAttachment(Attachment attachment) {
        this.mValues.add(attachment);
        Collections.sort(this.mValues, new Comparator<Attachment>() {
            @Override
            public int compare(Attachment lhs, Attachment rhs) {
                return lhs.getFileName().compareTo(rhs.getFileName());
            }
        });
        this.notifyItemInserted(this.mValues.indexOf(attachment));
    }

    public boolean isEmpty(){
        return mValues.isEmpty();
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final TextView mfilesize;
        public final TextView mfilename;
        public final TextView mMimeType;
        public final ImageButton mDeleteButton;
        public final ImageButton mPreviewButton;

        public Attachment mItem;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mfilesize = (TextView) view.findViewById(R.id.filesize);
            mfilename = (TextView) view.findViewById(R.id.filename);
            mMimeType = (TextView) view.findViewById(R.id.mimetype);
            mDeleteButton = (ImageButton) view.findViewById(R.id.deleteButton);
            mPreviewButton = (ImageButton) view.findViewById(R.id.previewButton);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mfilename.getText() + "'";
        }
    }
}
