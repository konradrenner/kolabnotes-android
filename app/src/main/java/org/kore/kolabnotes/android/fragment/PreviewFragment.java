package org.kore.kolabnotes.android.fragment;

import android.content.ContentResolver;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import org.kore.kolab.notes.Attachment;
import org.kore.kolabnotes.android.R;
import org.kore.kolabnotes.android.content.ActiveAccount;
import org.kore.kolabnotes.android.content.ActiveAccountRepository;
import org.kore.kolabnotes.android.content.AttachmentRepository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 *
 *@author Konrad Renner
 */
public class PreviewFragment extends Fragment implements MediaPlayer.OnPreparedListener, MediaController.MediaPlayerControl{
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
    private ImageView imageView;
    private LinearLayout musicView;
    private VideoView videoView;
    private TextView emptyView;
    private TextView nowPlayingView;

    private MediaPlayer mediaPlayer;
    private MediaController mediaController;

    private Handler handler = new Handler();

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
        musicView = (LinearLayout) getActivity().findViewById(R.id.main_audio_view);
        videoView = (VideoView) getActivity().findViewById(R.id.preview_video);
        emptyView = (TextView) getActivity().findViewById(R.id.empty_view_preview);
        imageView = (ImageView) getActivity().findViewById(R.id.preview_picture);
        nowPlayingView = (TextView) getActivity().findViewById(R.id.now_playing_text);

        musicView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mediaController.show(0);
                return true;
            }
        });

        final ActiveAccount activeAccount = this.accountRepository.getActiveAccount();
        //final Attachment attachment = this.attachmentRepository.getAttachmentWithAttachmentID(activeAccount.getAccount(), activeAccount.getRootFolder(), noteUID, attachmentID);
        displayPreview(activeAccount, noteUID, null);
    }

    public void displayPreview(ActiveAccount account, String noteUID, Attachment attachment){

        webView.setVisibility(View.INVISIBLE);
        textView.setVisibility(View.INVISIBLE);
        musicView.setVisibility(View.INVISIBLE);
        videoView.setVisibility(View.INVISIBLE);
        imageView.setVisibility(View.INVISIBLE);

        if(attachment == null){
            emptyView.setVisibility(View.VISIBLE);
            return;
        }else{
            emptyView.setVisibility(View.INVISIBLE);
        }

        if(attachment.getMimeType().startsWith("text/html")){
            displayHTML(account, noteUID, attachment);
        }else if(attachment.getMimeType().startsWith("text/")){
            displayText(account, noteUID,attachment);
        }else if(attachment.getMimeType().startsWith("audio/")){
            displayAudio(account, noteUID,attachment);
        }else if(attachment.getMimeType().startsWith("video/")){
            displayVideo(account, noteUID,attachment);
        }else if(attachment.getMimeType().startsWith("image/")){
            displayImage(account, noteUID, attachment);
        }
    }

    void displayHTML(ActiveAccount account, String noteUID,Attachment attachment){
        webView.setVisibility(View.VISIBLE);

        webView.loadUrl(attachmentRepository.getUriFromAttachment(account.getAccount(), account.getRootFolder(), noteUID, attachment).toString());
    }

    void displayImage(ActiveAccount account, String noteUID,Attachment attachment){
        imageView.setVisibility(View.VISIBLE);

        imageView.setImageURI(attachmentRepository.getUriFromAttachment(account.getAccount(), account.getRootFolder(), noteUID, attachment));
    }

    void displayText(ActiveAccount account, String noteUID,Attachment attachment){
        textView.setVisibility(View.VISIBLE);

        if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
            ContentResolver contentResolver = getActivity().getContentResolver();
            Uri uri = attachmentRepository.getUriFromAttachment(account.getAccount(), account.getRootFolder(), noteUID, attachment);


            try(BufferedReader reader = new BufferedReader(new InputStreamReader(contentResolver.openInputStream(uri)))){

                StringBuilder text = new StringBuilder();
                String  line;
                while((line =reader.readLine()) != null){
                    text.append(line);
                    text.append(System.lineSeparator());
                }

                textView.setText(text);
            } catch (IOException e) {
                Log.e("displayText","Exception while opening file:",e);
            }
        }
    }

    void displayAudio(ActiveAccount account, String noteUID,Attachment attachment){
        musicView.setVisibility(View.VISIBLE);

        try {
            nowPlayingView.setText(attachment.getFileName());

            mediaPlayer = new MediaPlayer();
            mediaPlayer.setOnPreparedListener(this);

            mediaController = new MediaController(getActivity());

            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setDataSource(getActivity(), attachmentRepository.getUriFromAttachment(account.getAccount(), account.getRootFolder(), noteUID, attachment));
            mediaPlayer.prepare();

            mediaController.show(0);
        }catch (IOException e){
            Toast.makeText(getActivity(), R.string.attachment_not_previewable, Toast.LENGTH_LONG).show();
        }

    }

    void displayVideo(ActiveAccount account, String noteUID,Attachment attachment){
        videoView.setVisibility(View.VISIBLE);

        MediaController mediaController = new MediaController(getActivity());
        mediaController.setAnchorView(videoView);

        videoView.setMediaController(mediaController);

        videoView.setVideoURI(attachmentRepository.getUriFromAttachment(account.getAccount(), account.getRootFolder(), noteUID, attachment));

        mediaController.show(0);
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
        if(mimeType.startsWith("image/")){
            return true;
        }

        return false;
    }


    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onStop() {
        super.onStop();
        if(mediaController != null) {
            mediaController.hide();
        }
        if(mediaPlayer != null){
            mediaPlayer.stop();
            mediaPlayer.release();
        }
    }

    @Override
    public int getAudioSessionId() {
        return 42;
    }

    //--MediaPlayerControl methods----------------------------------------------------
    public void start() {
        mediaPlayer.start();
    }

    public void pause() {
        mediaPlayer.pause();
    }

    public int getDuration() {
        return mediaPlayer.getDuration();
    }

    public int getCurrentPosition() {
        return mediaPlayer.getCurrentPosition();
    }

    public void seekTo(int i) {
        mediaPlayer.seekTo(i);
    }

    public boolean isPlaying() {
        return mediaPlayer.isPlaying();
    }

    public int getBufferPercentage() {
        return 0;
    }

    public boolean canPause() {
        return true;
    }

    public boolean canSeekBackward() {
        return true;
    }

    public boolean canSeekForward() {
        return true;
    }
    //--------------------------------------------------------------------------------

    public void onPrepared(MediaPlayer mediaPlayer) {
        mediaController.setMediaPlayer(this);
        mediaController.setAnchorView(musicView);

        handler.post(new Runnable() {
            public void run() {
                mediaController.setEnabled(true);
                mediaController.show();
            }
        });
    }
}
