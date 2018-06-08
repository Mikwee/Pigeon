package com.mikwee.timebrowser.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.mikwee.timebrowser.R;
import com.mikwee.timebrowser.activities.SmbActivity;

public class LoginFragment extends Fragment {
    public final static String TAG = LoginFragment.class.getSimpleName();

    private final String regex = "%5y3hg35g_è-è_";
    private final String sharedPrefsName = "savedMachines";


    //Views
    private EditText username;
    private TextInputEditText password;
    private Button loginBtn;
    private ProgressBar loading;
    private CheckBox remember;

    //some other variables
    private String savedUsername;
    private String savedPassword;
    private String mac;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_login, container, false);
        initViews(v);

        //Look for saved username or password
        getSavedUsernamePassword();

        //Set saved values if not null
        if (savedUsername != null)
            username.setText(savedUsername);
        if (savedPassword != null)
            password.setText(savedPassword);

        return v;
    }

    //Initialize layout views
    private void initViews(View v) {
        TextView hostTitle = (TextView) v.findViewById(R.id.host_title);
        TextView ipLabel = (TextView) v.findViewById(R.id.ip);
        username = (EditText) v.findViewById(R.id.username_input);
        password = (TextInputEditText) v.findViewById(R.id.password_input);
        loginBtn = (Button) v.findViewById(R.id.login_button);
        loading = (ProgressBar) v.findViewById(R.id.loading_login);
        remember = (CheckBox) v.findViewById(R.id.cb_remember);


        if (getArguments() != null) {
            ipLabel.setText(getArguments().getString("ip", "---.---.---.---"));
            hostTitle.setText("\"" + getArguments().getString("hostName", "------") + "\"");
            mac = getArguments().getString("mac");
        }

        loginBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                //show loading progressbar
                showLoading();

                //get username and password
                String u = username.getText().toString();
                String p = password.getText().toString();

                //check if inserted username and password are valid
                if (inputValid(u, p)) {

                    //remember is true?
                    boolean save = remember.isChecked();
                    //values aren't already saved ?
                    boolean upFirstTime = (savedUsername == null) || (savedPassword == null);

                    boolean newValues = false;
                    if (!upFirstTime)
                        newValues = !(savedUsername.equals(u) && savedPassword.equals(p));

                    if (save && (upFirstTime || newValues)) {
                        saveUsernamePassword(u, p);
                    }

                    //Proceed
                    SmbActivity ma = (SmbActivity) getActivity();
                    ma.tryLogin(u, p);

                } else {
                    hideLoading();
                    Toast.makeText(getContext(), getString(R.string.login_empty_up), Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    //Reads shared preferences to find username and password
    private void getSavedUsernamePassword() {

        if (mac == null) {
            Log.e(TAG, "error: MAC PASSED IS NULL");
            return;
        }

        //get saved machines shared prefs
        SharedPreferences prefs = getContext().getSharedPreferences(sharedPrefsName, Context.MODE_PRIVATE);
        String usernameAndPassword = prefs.getString(mac, null);

        //If mac does not exist in shared prefs
        if (usernameAndPassword == null)
            return;

        String[] upArr = usernameAndPassword.split(regex);

        if (upArr.length == 2) {
            savedUsername = upArr[0];
            savedPassword = upArr[1];
        } else {
            Log.e(TAG, "Arguments not equal to 2, wrong Shared prefs string");
        }


    }

    //Replace authenticate button with loading
    private void showLoading() {
        loginBtn.setVisibility(View.GONE);
        loading.setVisibility(View.VISIBLE);

        View view = getActivity().getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    //first check to see if input username and password are valid
    private boolean inputValid(String u, String p) {
        //remove all blanks todo  more accurate check
        u.replace(" ", "");

        //if size greater than 0
        return u.length() > 0 && p.length() > 0;
    }

    //Replace loading with authenticate
    public void hideLoading() {
        loading.setVisibility(View.GONE);
        loginBtn.setVisibility(View.VISIBLE);
    }

    //Write username and password to shared prefs
    private void saveUsernamePassword(String u, String p) {
        //Get shared preferences
        SharedPreferences prefs = getContext().getSharedPreferences(sharedPrefsName, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        //Create string and save
        String up = u + regex + p;
        editor.putString(mac, up);
        editor.apply();
    }

}
