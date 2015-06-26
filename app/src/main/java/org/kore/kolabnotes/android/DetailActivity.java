package org.kore.kolabnotes.android;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import org.kore.kolabnotes.android.fragment.DetailFragment;
import org.kore.kolabnotes.android.fragment.OnFragmentFinished;

public class DetailActivity extends AppCompatActivity implements OnFragmentFinished{

    private DetailFragment detailFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        detailFragment = (DetailFragment)getFragmentManager().findFragmentById(R.id.detail_fragment);

        Intent startIntent = getIntent();
        String uid = startIntent.getStringExtra(Utils.NOTE_UID);
        String notebook = startIntent.getStringExtra(Utils.NOTEBOOK_UID);

        detailFragment.setStartNotebook(notebook);
        detailFragment.setStartUid(uid);
    }

    @Override
    public void fragmentFinished(Intent resultIntent, ResultCode code) {
        if(ResultCode.OK == code){
            setResult(RESULT_OK,resultIntent);
        }else{
            setResult(RESULT_CANCELED,resultIntent);
        }
        finish();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        //outState.putParcelable("appInfo", appInfo.getComponentName());
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
       detailFragment.onBackPressed();
    }

}
