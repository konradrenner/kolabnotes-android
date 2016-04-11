package org.kore.kolabnotes.android;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import org.kore.kolab.notes.Attachment;
import org.kore.kolabnotes.android.fragment.AttachmentFragment;
import org.kore.kolabnotes.android.fragment.OnFragmentCallback;

/**
 * Created by konradrenner on 11.04.2016
 */

public class AttachmentActivity extends AppCompatActivity implements AttachmentFragment.OnListFragmentInteractionListener {

    private AttachmentFragment attachmentFragment;
    private Toolbar toolbar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attachments);

        Intent startIntent = getIntent();
        String uid = startIntent.getStringExtra(Utils.NOTE_UID);

        toolbar = (Toolbar) findViewById(R.id.toolbar_attachments);
        if (toolbar != null) {
            toolbar.setTitle(R.string.attachment_title);
        }

        attachmentFragment = (AttachmentFragment) getSupportFragmentManager().findFragmentById(R.id.attachment_fragment);
        attachmentFragment.setNoteUID(uid);
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
    public void onListFragmentInteraction(Attachment item) {
        //nothing at the moment
    }
}
