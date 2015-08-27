package com.postnikoff.consense.geo;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.Geofence;
import com.postnikoff.consense.R;
import com.postnikoff.consense.adapter.GeofenceAdapter;
import com.postnikoff.consense.adapter.UserFeatureAdapter;
import com.postnikoff.consense.model.UserFeature;
import com.postnikoff.consense.network.UserListActivity;

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
import java.util.ArrayList;
import java.util.List;

public class GeofenceListActivity extends Activity {

    private static final String LOGTAG = GeofenceListActivity.class.getName();
    private static final String URI = "http://192.168.0.109:8080/Consense/geofence/";
    private static final String URI_REQUEST_USERS = "/users";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_geofence_list);

        String geofences = getIntent().getStringExtra("geofences");
        List<MyGeofence> geofenceList = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(geofences);
            for(int i = 0; i < array.length(); i++) {
                JSONObject jsonObject = array.getJSONObject(i);
                MyGeofence geofence = new MyGeofence();
                geofence.setGeofenceId(jsonObject.getInt("geofenceId"));
                geofence.setName(jsonObject.getString("name"));
                geofenceList.add(geofence);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }


        ArrayAdapter<MyGeofence> adapter = new GeofenceAdapter(this, R.id.geofence_listview, geofenceList);
        ListView listView = (ListView) findViewById(R.id.geofence_listview);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(GeofenceListActivity.this, "item clicked with id: " + view.getId(), Toast.LENGTH_LONG).show();
                loadUsers(view.getId());
            }
        });

        TextView geofenceView = (TextView) findViewById(R.id.geofences_view);
        geofenceView.setText("Geofences close to you");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_geofence_list, menu);
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

    private void loadUsers(Integer geofenceId) {
        LoadUsersTask userTask = new LoadUsersTask(geofenceId);
        userTask.execute();
    }

    private class LoadUsersTask extends AsyncTask<Integer, Void, String> {

        private Integer geofenceId;

        public LoadUsersTask(Integer geofenceId) {
            this.geofenceId = geofenceId;
        }

        @Override
        protected String doInBackground(Integer... params) {

            try {

                URL url = new URL(URI + geofenceId + URI_REQUEST_USERS);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");
                con.setReadTimeout(10000);
                con.setConnectTimeout(12000);

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
            if (result != null && result != "") {
                Intent intent = new Intent(GeofenceListActivity.this, UserListActivity.class);
                intent.putExtra("userlist", result);
                startActivity(intent);
            } else {
                //mPasswordView.setError(getString(R.string.error_incorrect_password));
                //mPasswordView.requestFocus();
                Toast.makeText(GeofenceListActivity.this, "Userlist could not be retrieved", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
