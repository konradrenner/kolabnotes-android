package org.kore.kolabnotes.android;

import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import org.kore.kolabnotes.android.fragment.DrawEditorFragment;
import org.kore.kolabnotes.android.fragment.OnFragmentCallback;

/**
 * Created by yaroslav on 25.03.16.
 */

public class DrawEditorActivity extends AppCompatActivity implements OnFragmentCallback {
    public static final String TAG_RETURN_BITMAP = "ReturnBitmap";

    private DrawEditorFragment drawEditorFragment;
    private Toolbar toolbar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_draweditor);

        toolbar = (Toolbar) findViewById(R.id.toolbar_draweditor);
        if (toolbar != null) {
            toolbar.setTitle(R.string.draweditor_title);
        }

        drawEditorFragment = (DrawEditorFragment) getFragmentManager().findFragmentById(R.id.draweditor_fragment);
    }

    @Override
    public void fragmentFinished(Intent resultIntent, ResultCode code) {
        if(ResultCode.OK == code) {
            setResult(RESULT_OK, resultIntent);
        }else{
            setResult(RESULT_CANCELED, resultIntent);
        }
        finish();
    }

    @Override
    public void fileSelected() {
        /* Nothing */
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        drawEditorFragment.onBackPressed();
    }
}
