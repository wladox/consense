package com.postnikoff.consense.geo;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.AsyncTask;
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

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.postnikoff.consense.R;
import com.postnikoff.consense.adapter.GeofenceAdapter;
import com.postnikoff.consense.model.MyGeofence;
import com.postnikoff.consense.network.UserListActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class GeofenceListActivity extends Activity implements ResultCallback<Status>,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{

    private static final String LOGTAG = GeofenceListActivity.class.getName();

    private List<Geofence>  mGeofenceList;
    private PendingIntent   mGeofencePendingIntent;
    private GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_geofence_list);

        mGeofenceList           = new ArrayList<>();
        mGeofencePendingIntent  = null;

        // retrieve geofences from server response stored in the intent
        String geofences = getIntent().getStringExtra("geofences");
        List<MyGeofence> geofenceList = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(geofences);
            for(int i = 0; i < array.length(); i++) {
                JSONObject jsonObject = array.getJSONObject(i);
                MyGeofence geofence = new MyGeofence();
                geofence.setGeofenceId(jsonObject.getInt("geofenceId"));
                geofence.setName(jsonObject.getString("name"));
                geofence.setLatitude(jsonObject.getDouble("latitude"));
                geofence.setLongitude(jsonObject.getDouble("longitude"));
                geofence.setRadius(jsonObject.getInt("radius"));
                geofence.setDuration(jsonObject.getInt("duration"));
                geofenceList.add(geofence);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }


        // Display the list with geofences
        ArrayAdapter<MyGeofence> adapter = new GeofenceAdapter(this, R.id.geofence_listview, geofenceList);
        ListView listView = (ListView) findViewById(R.id.geofence_listview);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                MyGeofence g = (MyGeofence) parent.getItemAtPosition(position);
                mGeofenceList.add(new Geofence.Builder()
                                .setRequestId(String.valueOf(view.getId()))
                                .setCircularRegion(
                                        g.getLatitude(),
                                        g.getLongitude(),
                                        g.getRadius()
                                )
                                .setExpirationDuration(g.getDuration())
                                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                                .build()
                );
                Toast.makeText(GeofenceListActivity.this, "item clicked with id: " + view.getId() + ", " + g.getLatitude() + ", " + g.getLongitude(), Toast.LENGTH_LONG).show();
                Log.i(LOGTAG, "geofence list contains " + mGeofenceList.size());
                //loadUsers(view.getId());
            }
        });

        TextView geofenceView = (TextView) findViewById(R.id.geofences_view);
        geofenceView.setText("Geofences close to you");

        buildGoogleApiClient();
        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
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
        /*LoadUsersTask userTask = new LoadUsersTask(geofenceId);
        userTask.execute();*/
    }

    public void startGeofencing(MenuItem item) {
        if (mGeofenceList.size() > 0) {
            LocationServices.GeofencingApi.addGeofences(
                    mGoogleApiClient,
                    getGeofencingRequest(),
                    getGeofencePendingIntent()
            ).setResultCallback(GeofenceListActivity.this);
            Log.d(GeofenceListActivity.LOGTAG, "Geofencing started");
        } else {
            Toast.makeText(this, "you didn't select at least one geofence", Toast.LENGTH_LONG).show();
        }

    }

    public void removeGeofencing(MenuItem item) {
        if (!mGoogleApiClient.isConnected()) {
            Toast.makeText(this, getString(R.string.not_connected), Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            // Remove geofences.
            LocationServices.GeofencingApi.removeGeofences(
                    mGoogleApiClient,
                    // This is the same pending intent that was used in addGeofences().
                    getGeofencePendingIntent()
            ).setResultCallback(this); // Result processed in onResult().
        } catch (SecurityException securityException) {
            // Catch exception generated if the app does not use ACCESS_FINE_LOCATION permission.
            logSecurityException(securityException);
        }
    }

    private void logSecurityException(SecurityException securityException) {
        Log.e(LOGTAG, "Invalid location permission. " +
                "You need to use ACCESS_FINE_LOCATION with geofences", securityException);
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofences(mGeofenceList);
        return builder.build();
    }

    private PendingIntent getGeofencePendingIntent() {
        //Reuse intent if we already have it
        if (mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }

        Intent intent = new Intent(this, GeofenceTransitionsIntentService.class);
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public void onResult(Status status) {
        Log.i(LOGTAG, "Result received: " + status.getStatusMessage());

    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.i(LOGTAG, "Play services GEOFENCING API connected");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(LOGTAG, "Play services GEOFENCING API connection suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(LOGTAG, "Play services GEOFENCING API connection failed");
    }

}
