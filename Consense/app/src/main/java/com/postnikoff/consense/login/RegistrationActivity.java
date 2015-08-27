package com.postnikoff.consense.login;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.postnikoff.consense.R;
import com.postnikoff.consense.UserProfileActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class RegistrationActivity extends Activity {

    private final static String URI = "http://192.168.0.98:8080/Consense/user/add";

    private AsyncTask<String, Void, Boolean> mAuthTask;

    private EditText usernameView;
    private EditText emailView;
    private EditText passView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        usernameView = (EditText) findViewById(R.id.register_usernickname);
        emailView = (EditText) findViewById(R.id.register_email);
        passView = (EditText) findViewById(R.id.register_pwd);

        Button registerButton = (Button) findViewById(R.id.registration_button);
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                register();
            }
        });

    }

    private void register() {
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        emailView.setError(null);
        passView.setError(null);

        // Store values at the time of the login attempt.
        String username = usernameView.getText().toString();
        String email = emailView.getText().toString();
        String password = passView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            passView.setError(getString(R.string.error_invalid_password));
            focusView = passView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            emailView.setError(getString(R.string.error_field_required));
            focusView = emailView;
            cancel = true;
        } else if (!isEmailValid(email)) {
            emailView.setError(getString(R.string.error_invalid_email));
            focusView = emailView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            mAuthTask = new UserRegistrationTask(username,email, password);
            mAuthTask.execute();
        }
    }

    private void showProgress(boolean b) {

    }

    private boolean isEmailValid(String email) {
        return email.contains("@");
    }

    private boolean isPasswordValid(String password) {
        return password.length() > 5;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_registration, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private String computeHash(String password) {
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("MD5");
            digest.update(password.getBytes());
            byte messageDigest[] = digest.digest();

            StringBuffer MD5Hash = new StringBuffer();
            for (int i = 0; i < messageDigest.length; i++) {
                String h = Integer.toHexString(0xFF & messageDigest[i]);
                while (h.length() < 2)
                    h = "0" + h;
                MD5Hash.append(h);
            }

            return  MD5Hash.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return password;
    }

    private class UserRegistrationTask extends AsyncTask<String, Void, Boolean> {

        private final String username;
        private final String mEmail;
        private final String mPassword;

        public UserRegistrationTask(String username, String email, String password) {
            this.username = username;
            this.mEmail = email;
            this.mPassword = computeHash(password);
        }

        @Override
        protected Boolean doInBackground(String... params) {

            JSONObject object = new JSONObject();
            try {
                object.put("userId", 0);
                object.put("username", username);
                object.put("email", mEmail);
                object.put("password", mPassword);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            try {
                URL url = new URL(URI);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("POST");
                con.setRequestProperty("content-type", "application/json");
                con.setDoOutput(true);

                OutputStreamWriter writer = new OutputStreamWriter(con.getOutputStream());
                writer.write(object.toString());
                writer.flush();

                /*StringBuilder sb = new StringBuilder();
                BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));*/
                int responseCode = con.getResponseCode();

                writer.close();
                con.disconnect();

                if (responseCode == 201)
                    return true;
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return false;
        }

        @Override
        protected void onPostExecute(Boolean result) {

            if (result) {
                Intent intent = new Intent(RegistrationActivity.this, UserProfileActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(RegistrationActivity.this, "Registration failed. See log file.", Toast.LENGTH_LONG).show();
            }

        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }
}
