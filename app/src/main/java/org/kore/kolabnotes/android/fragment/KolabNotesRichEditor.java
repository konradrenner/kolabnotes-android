package org.kore.kolabnotes.android.fragment;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import jp.wasabeef.richeditor.RichEditor;

/**
 * This class is just here to fix issue 22 and because it looks like wasabeef is not activly developing the richeditor any more.
 * Have a look at these issues/pull requests: https://github.com/wasabeef/richeditor-android/pull/123/commits/c225299d443af660582877d232a27bcee14c2f70
 * https://github.com/wasabeef/richeditor-android/issues/155
 * https://github.com/wasabeef/richeditor-android/pull/145/commits/a71100298878f18bbe06cf1b2aac07bcadf838db
 *
 * @deprecated delete class and use a fixed version of wasabeef richeditor (if he will be active any day in the future again)
 */
public class KolabNotesRichEditor extends RichEditor{


    public KolabNotesRichEditor(Context context) {
        super(context);
    }

    public KolabNotesRichEditor(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public KolabNotesRichEditor(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected EditorWebViewClient createWebviewClient() {
        return new KolabNotesEditorWebViewClient();
    }

   protected class KolabNotesEditorWebViewClient extends RichEditor.EditorWebViewClient{
       @Override
       public boolean shouldOverrideUrlLoading(WebView view, String url) {
           String decode = Uri.decode(url);

           //really really dirty hack to fix issue 174
           decode = decode.replaceAll("\\+","&plus;");

           return super.shouldOverrideUrlLoading(view, decode);
       }
   }
}
