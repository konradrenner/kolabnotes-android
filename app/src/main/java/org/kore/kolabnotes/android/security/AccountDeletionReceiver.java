package org.kore.kolabnotes.android.security;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.kore.kolabnotes.android.content.AccountIdentifier;
import org.kore.kolabnotes.android.content.ActiveAccountRepository;
import org.kore.kolabnotes.android.content.AttachmentRepository;
import org.kore.kolabnotes.android.content.ModificationRepository;
import org.kore.kolabnotes.android.content.NoteRepository;
import org.kore.kolabnotes.android.content.NoteTagRepository;
import org.kore.kolabnotes.android.content.TagRepository;

import java.util.LinkedHashSet;
import java.util.Set;

public class AccountDeletionReceiver extends BroadcastReceiver {
    public AccountDeletionReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final NoteTagRepository noteTagRepository = new NoteTagRepository(context);
        final TagRepository tagRepository = new TagRepository(context);
        final NoteRepository noteRepository = new NoteRepository(context);
        final ModificationRepository modificationRepository = new ModificationRepository(context);
        final AttachmentRepository attachmentRepository = new AttachmentRepository(context);
        final ActiveAccountRepository activeAccountRepository = new ActiveAccountRepository(context);
        final AccountManager accountManager = AccountManager.get(context);
        final Account[] accounts = accountManager.getAccounts();

        final Set<AccountIdentifier> allAccounts = activeAccountRepository.getAllAccounts();
        Set<AccountIdentifier> accountsForDeletion = new LinkedHashSet<>(allAccounts);

        for(Account account : accounts){
            String email = accountManager.getUserData(account, AuthenticatorActivity.KEY_EMAIL);
            String rootFolder = accountManager.getUserData(account,AuthenticatorActivity.KEY_ROOT_FOLDER);

            accountsForDeletion.remove(new AccountIdentifier(email,rootFolder));
        }
        accountsForDeletion.remove(new AccountIdentifier("local","Notes"));

        for(AccountIdentifier identifier : accountsForDeletion){
            String email = identifier.getAccount();
            String rootFolder = identifier.getRootFolder();
            activeAccountRepository.deleteAccount(identifier.getAccount(),identifier.getRootFolder());

            noteRepository.cleanAccount(email,rootFolder);
            noteTagRepository.cleanAccount(email,rootFolder);
            tagRepository.cleanAccount(email,rootFolder);
            attachmentRepository.cleanAccount(email, rootFolder);
            modificationRepository.cleanAccount(email,rootFolder);
        }
    }
}
