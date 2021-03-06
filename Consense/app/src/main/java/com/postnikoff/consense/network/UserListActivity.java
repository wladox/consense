package com.postnikoff.consense.network;

import android.app.ListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.postnikoff.consense.R;
import com.postnikoff.consense.adapter.UserListAdapter;
import com.postnikoff.consense.helper.AssetsPropertyReader;
import com.postnikoff.consense.helper.Constants;
import com.postnikoff.consense.model.User;
import com.postnikoff.consense.model.UserFeature;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

public class UserListActivity extends ListActivity {

    private static final String URI = "/geofence/";
    private static final String URI_USERS = "/users";

    private AssetsPropertyReader propertyReader;
    private Properties properties;

    private SharedPreferences settings;

    private List<User> mUserList;
    private ArrayAdapter<User> mUserArrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_list);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        propertyReader = new AssetsPropertyReader(getApplicationContext());
        properties = propertyReader.getProperties("app.properties");

        settings = getSharedPreferences(Constants.SHARED_PREFERENCES_FILE, MODE_PRIVATE);

        mUserList= new ArrayList<>();
        mUserArrayAdapter = new UserListAdapter(this, mUserList);
        setListAdapter(mUserArrayAdapter);

        LoadUsersTask loadUsersTask = new LoadUsersTask(settings.getInt("userId", 0));
        loadUsersTask.execute();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        User selectedItem = (User) getListView().getItemAtPosition(position);

        Toast.makeText(UserListActivity.this, "you clicked " + selectedItem.getUsername() + " with id: "
                + selectedItem.getUserId(), Toast.LENGTH_SHORT).show();
    }

    private void getUsers(String userList) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        try {
            JSONArray array = new JSONArray(userList);
            for(int i = 0; i < array.length(); i++) {
                JSONObject jsonObject = array.getJSONObject(i);
                User user = new User();
                user.setUserId(jsonObject.getInt("userId"));
                user.setUsername(jsonObject.getString("username"));
                user.setEmail(jsonObject.getString("email"));
                user.setName(jsonObject.getString("name"));
                user.setSurname(jsonObject.getString("surname"));
                try {
                    user.setBirthday(sdf.parse(jsonObject.getString("birthday")));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                mUserList.add(user);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private class LoadUsersTask extends AsyncTask<Integer, Void, String> {

        private int userId;

        public LoadUsersTask(int userId) {
            this.userId = userId;
        }

        @Override
        protected String doInBackground(Integer... params) {

            try {

                URL url = new URL(properties.getProperty("server.url") + URI + URI_USERS);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");
                con.setReadTimeout(10000);
                con.setConnectTimeout(12000);
                con.setDoOutput(true);
                con.setDoInput(true);

                OutputStreamWriter writer = new OutputStreamWriter(con.getOutputStream());
                writer.write("userId="+userId);
                writer.flush();

                BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));

                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }

                //int response = con.getResponseCode();

                con.disconnect();
                return sb.toString();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null && !result.equals("")) {
               /* Intent intent = new Intent(GeofenceListActivity.this, UserListActivity.class);
                intent.putExtra("userlist", result);
                startActivity(intent);*/
                getUsers(result);
                mUserArrayAdapter.notifyDataSetChanged();
            } else {
                //mPasswordView.setError(getString(R.string.error_incorrect_password));
                //mPasswordView.requestFocus();
                Toast.makeText(UserListActivity.this, "Userlist could not be retrieved", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
