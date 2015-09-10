package org.kore.kolabnotes.android.fragment;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import org.kore.kolabnotes.android.R;
import org.kore.kolabnotes.android.Utils;
import org.kore.kolabnotes.android.content.AccountIdentifier;
import org.kore.kolabnotes.android.content.ActiveAccount;
import org.kore.kolabnotes.android.content.ActiveAccountRepository;
import org.kore.kolabnotes.android.security.AuthenticatorActivity;

import java.util.Arrays;

/**
 * Created by koni on 10.09.15.
 */
public class ChooseAccountDialogFragment extends DialogFragment implements View.OnClickListener{

    private ActiveAccountRepository activeAccountRepository;

    private Spinner accountSpinner;
    private Button selectButton;
    private Button cancelButton;
    private AccountManager mAccountManager;
    private String localAccountName;

    public ChooseAccountDialogFragment() {
        // Empty constructor required for DialogFragment
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_choose_account, container);

        activeAccountRepository = new ActiveAccountRepository(getActivity());

        accountSpinner = (Spinner) view.findViewById(R.id.spinner_account);
        selectButton = (Button) view.findViewById(R.id.select_button);
        cancelButton = (Button) view.findViewById(R.id.cancel_button);
        selectButton.setOnClickListener(this);
        cancelButton.setOnClickListener(this);
        localAccountName = getResources().getString(R.string.drawer_account_local);
        getDialog().setTitle(R.string.account_choose_title);

        mAccountManager = AccountManager.get(getActivity());

        initAccountSpinner();
        return view;
    }

    @Override
    public void onClick(View view) {
        if(view.getId() == R.id.select_button) {
            OnAccountChooseListener listener = (OnAccountChooseListener) getActivity();

            String name = accountSpinner.getSelectedItem().toString();

            final AccountIdentifier selectedAccount = Utils.getAccountIdentifierWithName(getActivity(), name);

            activeAccountRepository.switchAccount(selectedAccount.getAccount(), selectedAccount.getRootFolder());

            listener.onAccountElected(name, selectedAccount);
        }
        this.dismiss();
    }

    void initAccountSpinner(){
        Account[] accounts = mAccountManager.getAccountsByType(AuthenticatorActivity.ARG_ACCOUNT_TYPE);

        String[] accountNames = new String[accounts.length+1];

        accountNames[0] = localAccountName;
        int selection = 0;

        final ActiveAccount activeAccount = activeAccountRepository.getActiveAccount();

        for(int i=0; i< accounts.length;i++){
            accountNames[i+1] = mAccountManager.getUserData(accounts[i],AuthenticatorActivity.KEY_ACCOUNT_NAME);

            String folder = mAccountManager.getUserData(accounts[i],AuthenticatorActivity.KEY_ROOT_FOLDER);
            String email = mAccountManager.getUserData(accounts[i],AuthenticatorActivity.KEY_EMAIL);

            if(activeAccount.getAccount().equals(email) && activeAccount.getRootFolder().equals(folder)){
                selection = i+1;
            }
        }

        Arrays.sort(accountNames, 1, accountNames.length);

        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(getActivity(),R.layout.widget_config_spinner_item,accountNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        accountSpinner.setAdapter(adapter);
        accountSpinner.setSelection(selection);
    }
}
