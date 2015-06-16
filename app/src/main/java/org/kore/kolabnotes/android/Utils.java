package org.kore.kolabnotes.android;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Outline;
import android.os.Build;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.mikepenz.materialdrawer.model.ProfileDrawerItem;

import org.kore.kolab.notes.Note;
import org.kore.kolab.notes.Tag;
import org.kore.kolabnotes.android.content.AccountIdentifier;
import org.kore.kolabnotes.android.security.AuthenticatorActivity;

import java.util.Objects;

public class Utils {

    public static final String INTENT_ACCOUNT_EMAIL = "intent_account_email";
    public static final String INTENT_ACCOUNT_ROOT_FOLDER = "intent_account_rootfolder";
    public static final String NOTE_UID = "note_uid";
    public static final String NOTEBOOK_UID = "notebook_uid";

    /*
    public static void configureWindowEnterExitTransition(Window w) {
        Explode ex = new Explode();
        ex.setInterpolator(new PathInterpolator(0.4f, 0, 1, 1));
        w.setExitTransition(ex);
        w.setEnterTransition(ex);
    }
    */

    public static void configureFab(View fabButton) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            fabButton.setOutlineProvider(new ViewOutlineProvider() {
                @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                @Override
                public void getOutline(View view, Outline outline) {
                    int fabSize = view.getContext().getResources().getDimensionPixelSize(R.dimen.fab_size);
                    outline.setOval(0, 0, fabSize, fabSize);
                }
            });
        } else {
            ((ImageButton) fabButton).setScaleType(ImageView.ScaleType.FIT_CENTER);
        }
    }
    
    public static final String getNameOfActiveAccount(Context context, String pemail, String prootFolder){
        AccountManager accountManager = AccountManager.get(context);
        Account[] accounts = accountManager.getAccountsByType(AuthenticatorActivity.ARG_ACCOUNT_TYPE);

        for(int i=0;i<accounts.length;i++) {
            String email = accountManager.getUserData(accounts[i],AuthenticatorActivity.KEY_EMAIL);
            String name = accountManager.getUserData(accounts[i],AuthenticatorActivity.KEY_ACCOUNT_NAME);
            String rootFolder = accountManager.getUserData(accounts[i],AuthenticatorActivity.KEY_ROOT_FOLDER);

            if(pemail.equals(email) && prootFolder.equals(rootFolder)){
                return name;
            }
        }

        return context.getResources().getString(R.string.drawer_account_local);
    }

    public static final AccountIdentifier getAccountIdentifierWithName(Context context, String account){
        String rootFolder = "Notes";
        String email = "local";
        if(!account.equals("local")) {
            AccountManager accountManager = AccountManager.get(context);
            Account[] accounts = AccountManager.get(context).getAccountsByType(AuthenticatorActivity.ARG_ACCOUNT_TYPE);

            for (Account acc : accounts) {
                if(account.equals(accountManager.getUserData(acc, AuthenticatorActivity.KEY_ACCOUNT_NAME))){
                    email = accountManager.getUserData(acc, AuthenticatorActivity.KEY_EMAIL);
                    rootFolder = accountManager.getUserData(acc, AuthenticatorActivity.KEY_ROOT_FOLDER);
                }
            }
        }

        return new AccountIdentifier(email,rootFolder);
    }

    /**
     * Creates a exact copy of an note
     *
     * @param source
     * @return Note
     */
    public static final Note copy(Note source){
        Note note = new Note(source.getIdentification(),source.getAuditInformation(),source.getClassification(),source.getSummary());
        note.setDescription(source.getDescription());
        note.setColor(source.getColor());
        note.setAttachment(source.getAttachment());
        note.addCategories(source.getCategories().toArray(new Tag[source.getCategories().size()]));

        return note;
    }

    public static final boolean differentMutableData(Note one, Note two){
        if(!Objects.equals(one.getClassification(),two.getClassification())){
            return true;
        }
        if(!Objects.equals(one.getColor(),two.getColor())){
            return true;
        }
        if(!Objects.equals(one.getDescription(),two.getDescription())){
            return true;
        }
        if(!Objects.equals(one.getAttachment(),two.getAttachment())){
            return true;
        }
        if(!Objects.equals(one.getSummary(),two.getSummary())){
            return true;
        }
        if(one.getCategories().size() != two.getCategories().size() || !one.getCategories().containsAll(two.getCategories())){
            return true;
        }

        return false;
    }
}
