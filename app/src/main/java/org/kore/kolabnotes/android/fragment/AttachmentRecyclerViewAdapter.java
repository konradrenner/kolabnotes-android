package org.kore.kolabnotes.android.fragment;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.kore.kolab.notes.Attachment;
import org.kore.kolabnotes.android.R;

import java.util.List;

/**
 * {@link RecyclerView.Adapter} that can display a {@link Attachment} and makes a call to the
 * specified {@link AttachmentFragment.OnListFragmentInteractionListener}.
 */
public class AttachmentRecyclerViewAdapter extends RecyclerView.Adapter<AttachmentRecyclerViewAdapter.ViewHolder> {

    private final List<Attachment> mValues;
    private final AttachmentFragment.OnListFragmentInteractionListener mListener;

    public AttachmentRecyclerViewAdapter(List<Attachment> items, AttachmentFragment.OnListFragmentInteractionListener listener) {
        mValues = items;
        mListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_attachment_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mItem = mValues.get(position);
        holder.mfilesize.setText(mValues.get(position).getData().length);
        holder.mfilename.setText(mValues.get(position).getFileName());

        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mListener) {
                    // Notify the active callbacks interface (the activity, if the
                    // fragment is attached to one) that an item has been selected.
                    mListener.onListFragmentInteraction(holder.mItem);
                }
            }
        });
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
        public Attachment mItem;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mfilesize = (TextView) view.findViewById(R.id.filesize);
            mfilename = (TextView) view.findViewById(R.id.filename);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mfilename.getText() + "'";
        }
    }
}
