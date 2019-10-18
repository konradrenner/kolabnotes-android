package org.kore.kolabnotes.android;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import org.kore.kolab.notes.Attachment;
import org.kore.kolabnotes.android.content.ActiveAccountRepository;
import org.kore.kolabnotes.android.fragment.AttachmentFragment;
import org.kore.kolabnotes.android.fragment.OnFragmentCallback;
import org.kore.kolabnotes.android.fragment.PreviewFragment;

/**
 * Created by konradrenner on 11.04.2016
 */

public class AttachmentActivity extends AppCompatActivity implements AttachmentFragment.OnListFragmentInteractionListener {

    private AttachmentFragment attachmentFragment;
    private Toolbar toolbar;
    private PreviewFragment previewFragment;
    private ActiveAccountRepository accountRepository;

    private String noteUID;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attachments);

        Intent startIntent = getIntent();
        noteUID = startIntent.getStringExtra(Utils.NOTE_UID);

        toolbar = (Toolbar) findViewById(R.id.toolbar_attachments);
        if (toolbar != null) {
            toolbar.setTitle(R.string.attachment_title);
        }

        attachmentFragment = (AttachmentFragment) getFragmentManager().findFragmentById(R.id.attachment_fragment);
        attachmentFragment.setNoteUID(noteUID);

        accountRepository = new ActiveAccountRepository(this);

        if (findViewById(R.id.preview_fragment) != null) {

            if (savedInstanceState != null) {
                return;
            }

            previewFragment = PreviewFragment.newInstance(noteUID,null);

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.preview_fragment, previewFragment).commit();
        }
    }

    @Override
    public void fragmentFinished(Intent resultIntent, OnFragmentCallback.ResultCode code) {
        if(OnFragmentCallback.ResultCode.OK == code) {
            setResult(RESULT_OK, resultIntent);
        }else{
            setResult(RESULT_CANCELED, resultIntent);
        }
        finish();
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        attachmentFragment.onBackPressed();
    }

    @Override
    public void onListFragmentInteraction(AttachmentFragment.OnListFragmentInteractionListener.Operation operation, Attachment item) {
        if(Operation.DELETE == operation){
            attachmentFragment.deleteAttachment(item);
        }else if(Operation.SELECT == operation){
            attachmentFragment.shareFile(item);
        }else if(Operation.PREVIEW == operation){
            previewFragment.displayPreview(accountRepository.getActiveAccount(),noteUID,item);
        }
    }
}
