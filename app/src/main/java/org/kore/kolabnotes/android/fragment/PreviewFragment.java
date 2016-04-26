package org.kore.kolabnotes.android.fragment;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.MediaController;
import android.widget.VideoView;

import org.kore.kolab.notes.Attachment;
import org.kore.kolabnotes.android.R;
import org.kore.kolabnotes.android.content.ActiveAccount;
import org.kore.kolabnotes.android.content.ActiveAccountRepository;
import org.kore.kolabnotes.android.content.AttachmentRepository;
/**
 *
 *@author Konrad Renner
 */
public class PreviewFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_NOTEUID = "noteUID";
    private static final String ARG_ATTACHMENTID = "attachmentID";

    // TODO: Rename and change types of parameters
    private String noteUID;
    private String attachmentID;
    private AttachmentRepository attachmentRepository;
    private ActiveAccountRepository accountRepository;
    private WebView webView;
    private EditText textView;
    private MediaController musicView;
    private VideoView videoView;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param noteUID Parameter 1.
     * @param attachmentID Parameter 2.
     * @return A new instance of fragment PreviewFragment.
     */
    public static PreviewFragment newInstance(String noteUID, String attachmentID) {
        PreviewFragment fragment = new PreviewFragment();
        Bundle args = new Bundle();
        args.putString(ARG_NOTEUID, noteUID);
        args.putString(ARG_ATTACHMENTID, attachmentID);
        fragment.setArguments(args);
        return fragment;
    }
    public PreviewFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            noteUID = getArguments().getString(ARG_NOTEUID);
            attachmentID = getArguments().getString(ARG_ATTACHMENTID);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_preview, container, false);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.attachmentRepository = new AttachmentRepository(context);
        this.accountRepository = new ActiveAccountRepository(context);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        webView = (WebView) getActivity().findViewById(R.id.preview_html);
        textView = (EditText) getActivity().findViewById(R.id.preview_text);
        musicView = (MediaController) getActivity().findViewById(R.id.preview_music);
        videoView = (VideoView) getActivity().findViewById(R.id.preview_video);

        final ActiveAccount activeAccount = this.accountRepository.getActiveAccount();
        final Attachment attachment = this.attachmentRepository.getAttachmentWithAttachmentID(activeAccount.getAccount(), activeAccount.getRootFolder(), noteUID, attachmentID);
        displayPreview(activeAccount, noteUID, attachment);
    }

    public void displayPreview(ActiveAccount account, String noteUID, Attachment attachment){

        webView.setVisibility(View.INVISIBLE);
        textView.setVisibility(View.INVISIBLE);
        musicView.setVisibility(View.INVISIBLE);
        videoView.setVisibility(View.INVISIBLE);

        /*if(attachment.getMimeType().startsWith("text/html")){
            displayHTML(attachment);
        }else if(attachment.getMimeType().startsWith("text/")){
            displayText(attachment);
        }else if(attachment.getMimeType().startsWith("audio/")){
            displayAudio(attachment);
        }else if(attachment.getMimeType().startsWith("video/")){
            displayVideo(attachment);
        }*/
    }

    void displayHTML(ActiveAccount account, String noteUID,Attachment attachment){
        webView.setVisibility(View.VISIBLE);
    }

    void displayText(ActiveAccount account, String noteUID,Attachment attachment){
        textView.setVisibility(View.VISIBLE);

    }

    void displayAudio(ActiveAccount account, String noteUID,Attachment attachment){
        musicView.setVisibility(View.VISIBLE);

    }

    void displayVideo(ActiveAccount account, String noteUID,Attachment attachment){
        videoView.setVisibility(View.VISIBLE);

        MediaController mediaController = new MediaController(getActivity());
        mediaController.setAnchorView(videoView);
        videoView.setMediaController(mediaController);

        //videoView.setVideoURI(attachmentRepository.getUriFromAttachment());
    }

    public static boolean previewableMimetype(String mimeType){
        if(mimeType.startsWith("text/")){
            return true;
        }
        if(mimeType.startsWith("audio/")){
            return true;
        }
        if(mimeType.startsWith("video/")){
            return true;
        }

        return false;
    }


    @Override
    public void onDetach() {
        super.onDetach();
    }
}
