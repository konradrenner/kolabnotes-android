package org.kore.kolabnotes.android.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.kore.kolab.notes.Attachment;
import org.kore.kolabnotes.android.R;
import org.kore.kolabnotes.android.content.ActiveAccount;
import org.kore.kolabnotes.android.content.ActiveAccountRepository;
import org.kore.kolabnotes.android.content.AttachmentRepository;

/**
 * A fragment representing a list of Items.
 * <p>
 * Activities containing this fragment MUST implement the {@link OnListFragmentInteractionListener}
 * interface.
 */
public class AttachmentFragment extends Fragment {
    private OnListFragmentInteractionListener mListener;

    private ActiveAccountRepository activeAccountRepository;
    private AttachmentRepository attachmentRepository;
    private String noteUID;

    private RecyclerView recyclerView;
    private TextView noAttachmentView;
    private AttachmentRecyclerViewAdapter adapter;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public AttachmentFragment() {
    }

    @SuppressWarnings("unused")
    public static AttachmentFragment newInstance(String noteUID) {
        AttachmentFragment fragment = new AttachmentFragment();
        Bundle args = new Bundle();
        args.putString("noteUID",noteUID);
        fragment.setArguments(args);
        fragment.noteUID = noteUID;
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_attachment_list, container, false);
        View recycler = view.findViewById(R.id.attachment_list);

        // Set the adapter
        if (recycler instanceof RecyclerView) {
            recyclerView = (RecyclerView) recycler;
            ActiveAccount activeAccount = activeAccountRepository.getActiveAccount();
            adapter = new AttachmentRecyclerViewAdapter(attachmentRepository.getAllForNote(activeAccount.getAccount(),activeAccount.getRootFolder(),noteUID,false), mListener);
            recyclerView.setAdapter(adapter);
        }

        noAttachmentView = (TextView) view.findViewById(R.id.empty_view_attachment);

        setListState();
        return view;
    }

    private void setListState() {
        if (adapter != null) {
            if (adapter.isEmpty()) {
                recyclerView.setVisibility(View.GONE);
                noAttachmentView.setVisibility(View.VISIBLE);
            } else {
                recyclerView.setVisibility(View.VISIBLE);
                noAttachmentView.setVisibility(View.GONE);
            }
        }
    }

    public void setNoteUID(String uid){
        this.noteUID = uid;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnListFragmentInteractionListener) {
            mListener = (OnListFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnListFragmentInteractionListener");
        }

        activeAccountRepository = new ActiveAccountRepository(context);
        attachmentRepository = new AttachmentRepository(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
        activeAccountRepository= null;
        attachmentRepository = null;
    }

    public void onBackPressed(){
        mListener.fragmentFinished(new Intent(), OnFragmentCallback.ResultCode.BACK);
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnListFragmentInteractionListener {
        void onListFragmentInteraction(Attachment item);
        void fragmentFinished(Intent resultIntent, OnFragmentCallback.ResultCode code);
    }
}
