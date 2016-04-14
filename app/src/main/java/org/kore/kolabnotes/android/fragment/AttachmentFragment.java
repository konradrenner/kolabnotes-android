package org.kore.kolabnotes.android.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.kore.kolab.notes.Attachment;
import org.kore.kolabnotes.android.R;
import org.kore.kolabnotes.android.Utils;
import org.kore.kolabnotes.android.adapter.AttachmentRecyclerViewAdapter;
import org.kore.kolabnotes.android.content.ActiveAccount;
import org.kore.kolabnotes.android.content.ActiveAccountRepository;
import org.kore.kolabnotes.android.content.AttachmentRepository;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

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
    private View topView;
    private Toolbar toolbar;
    private AppCompatActivity activity;
    private FloatingActionButton fab;

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
        topView = inflater.inflate(R.layout.fragment_attachment_list, container, false);
        return topView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar_attachments);
        activity.setSupportActionBar(toolbar);
        if(activity.getSupportActionBar() != null){
            activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        // Handle Back Navigation
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        fab = (FloatingActionButton)topView.findViewById(R.id.fab_button);
        fab.setOnClickListener(new NewAttachmentListener());

        View recycler = topView.findViewById(R.id.attachment_list);

        // Set the adapter
        if (recycler instanceof RecyclerView) {
            recyclerView = (RecyclerView) recycler;
            ActiveAccount activeAccount = activeAccountRepository.getActiveAccount();
            adapter = new AttachmentRecyclerViewAdapter(attachmentRepository.getAllForNote(activeAccount.getAccount(), activeAccount.getRootFolder(),noteUID,false), activity, mListener);
            recyclerView.setAdapter(adapter);
            recyclerView.setHasFixedSize(true);
        }

        noAttachmentView = (TextView) topView.findViewById(R.id.empty_view_attachment);

        setListState();
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

        this.activity = (AppCompatActivity)context;

        activeAccountRepository = new ActiveAccountRepository(context);
        attachmentRepository = new AttachmentRepository(context);
    }

    public void deleteAttachment(Attachment attachment){
        ActiveAccount activeAccount = this.activeAccountRepository.getActiveAccount();
        this.attachmentRepository.delete(activeAccount.getAccount(),activeAccount.getRootFolder(),this.noteUID,attachment);
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

    class NewAttachmentListener implements View.OnClickListener{
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);

            intent.addCategory(Intent.CATEGORY_OPENABLE);

            intent.setType("*/*");

            startActivityForResult(intent, Utils.READ_REQUEST_CODE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {
        if (requestCode == Utils.READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (resultData != null) {

                Uri uri = resultData.getData();
                String path = uri.getPath();

                ContentResolver contentResolver = activity.getContentResolver();
                Cursor cursor = contentResolver.query(uri, null, null, null, null, null);

                String fileName;

                if (cursor != null && cursor.moveToFirst()) {
                    fileName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                } else {
                    fileName = path.substring(path.lastIndexOf("/") + 1);
                }

                String mimeType = contentResolver.getType(uri);

                try {
                    InputStream inputStream = contentResolver.openInputStream(uri);
                    byte[] buffer = new byte[1024];

                    ByteArrayOutputStream output = new ByteArrayOutputStream();
                    int bytes;
                    while ((bytes = inputStream.read(buffer)) != -1) {
                        output.write(buffer, 0, bytes);
                    }

                    byte[] data = output.toByteArray();

                    inputStream.close();
                    output.close();

                    final Attachment attachment = new Attachment(fileName,mimeType);
                    attachment.setData(data);

                    ActiveAccount activeAccount = activeAccountRepository.getActiveAccount();
                    attachmentRepository.insert(activeAccount.getAccount(),activeAccount.getRootFolder(),noteUID,attachment);

                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            adapter.addAttachment(attachment);
                        }
                    });

                } catch (FileNotFoundException e) {
                    Log.e("attach", "File not found", e);
                } catch (IOException e) {
                    Log.e("attach", "IO Exception", e);
                }
            }
        }
    }

    public void shareFile(Attachment attachment){
        ActiveAccount activeAccount = this.activeAccountRepository.getActiveAccount();
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, this.attachmentRepository.getUriFromAttachment(activeAccount.getAccount(),activeAccount.getRootFolder(), this.noteUID, attachment));
        shareIntent.setType(attachment.getMimeType());
        startActivity(Intent.createChooser(shareIntent, getResources().getText(R.string.send_to)));
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
        enum Operation{NEW, DELETE, PREVIEW, SELECT}
        void onListFragmentInteraction(Operation op, Attachment item);
        void fragmentFinished(Intent resultIntent, OnFragmentCallback.ResultCode code);
    }
}
