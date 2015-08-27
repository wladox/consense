package com.postnikoff.consense;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.intel.context.Sensing;
import com.intel.context.error.ContextError;
import com.intel.context.exception.ContextProviderException;
import com.intel.context.item.ContextType;
import com.intel.context.item.Item;
import com.intel.context.item.LocationCurrent;
import com.intel.context.option.activity.ActivityOptionBuilder;
import com.intel.context.option.activity.Mode;
import com.intel.context.option.activity.ReportType;
import com.intel.context.sensing.ContextTypeListener;
import com.intel.context.sensing.InitCallback;
import com.postnikoff.consense.adapter.UserFeatureAdapter;
import com.postnikoff.consense.db.ContextDataSource;
import com.postnikoff.consense.geo.GeofenceListActivity;
import com.postnikoff.consense.model.UserFeature;
import com.postnikoff.consense.sensing.ActivityRecognitionApplication;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
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
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class UserProfileActivity extends Activity
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener{

    private static final String TAG     = UserProfileActivity.class.getName();
    private static final String URI     = "http://192.168.0.109:8080/Consense/context/add";
    private static final String URI_GEO = "http://192.168.0.109:8080/Consense/geofence/neighbours";

    private static final String REQUESTING_LOCATION_UPDATES_KEY = "requesting_location_updates";
    private static final String LAST_UPDATED_TIME_STRING_KEY    = "last_update_time";
    private static final String LOCATION_KEY                    = "last_location";

    private TextView mLatitudeTextView;
    private TextView mLongitudeTextView;
    private TextView mLastUpdateTimeTextView;

    private static ContextTypeListener mConsenseContextListener;
    private Sensing mSensing;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private LocationRequest mLocationRequest;
    private String mLastUpdateTime;

    private SharedPreferences settings;
    private ContextDataSource dataSource;

    private boolean mRequestingLocationUpdates = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_proile);

        updateValuesFromBundle(savedInstanceState);

        settings = getPreferences(MODE_PRIVATE);

        mLatitudeTextView = (TextView) findViewById(R.id.latitudeText);
        mLongitudeTextView = (TextView) findViewById(R.id.longitudeText);
        mLastUpdateTimeTextView = (TextView) findViewById(R.id.update_time);
        //getActionBar().show();

        //store userfeatures onPause!!!
        String userfeatures = getIntent().getStringExtra("userfeatures");
        ArrayList<UserFeature> features = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(userfeatures);
            for(int i = 0; i < array.length(); i++) {
                JSONObject jsonObject = array.getJSONObject(i);
                UserFeature userFeature = new UserFeature();
                userFeature.setFeatureId(jsonObject.getInt("featureId"));
                userFeature.setFeatureName(jsonObject.getString("featureName"));
                userFeature.setCategoryId(jsonObject.getInt("categoryId"));
                userFeature.setCategoryName(jsonObject.getString("categoryName"));
                features.add(userFeature);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        dataSource = new ContextDataSource(this);

        ArrayAdapter<UserFeature> adapter = new UserFeatureAdapter(this, features);
        ListView listView = (ListView) findViewById(R.id.feature_list);
        listView.setAdapter(adapter);

        mSensing = ActivityRecognitionApplication.getInstance().getmSensing();
        mConsenseContextListener = ActivityRecognitionApplication.getInstance().getmActivityRecognitionListener();

        buildGoogleApiClient();
        //getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    public void startDaemon(MenuItem v) {
        mSensing.start(new InitCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(getApplicationContext(), "Context sensing daemon started", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(ContextError contextError) {
                Toast.makeText(getApplicationContext(), "Error: " + contextError.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    public void enableSensing(MenuItem v) {
        ActivityOptionBuilder activityRecognitionSettings = new ActivityOptionBuilder();
        activityRecognitionSettings.setMode(Mode.NORMAL);
        activityRecognitionSettings.setReportType(ReportType.FREQUENCY);

        try {
            mSensing.enableSensing(ContextType.ACTIVITY_RECOGNITION, activityRecognitionSettings.toBundle());
            mSensing.addContextTypeListener(ContextType.ACTIVITY_RECOGNITION, mConsenseContextListener);

            mSensing.enableSensing(ContextType.LOCATION, null);
            mSensing.addContextTypeListener(ContextType.LOCATION, mConsenseContextListener);

            mSensing.enableSensing(ContextType.INSTALLED_APPS, null);
            mSensing.addContextTypeListener(ContextType.INSTALLED_APPS, mConsenseContextListener);


        } catch (ContextProviderException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "Error enabling context type: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e(TAG, "Error enabling context type " + e.getMessage());
        }
    }

    public void disableSensing(MenuItem v) {
        try {
            mSensing.removeContextTypeListener(mConsenseContextListener);
            mSensing.disableSensing(ContextType.ACTIVITY_RECOGNITION);
            mSensing.disableSensing(ContextType.LOCATION);
            mSensing.disableSensing(ContextType.INSTALLED_APPS);
        } catch (ContextProviderException e) {
            e.printStackTrace();
        }
    }

    public void stopDaemon(MenuItem v) {
        mSensing.stop();
    }

    public void showGeofenceList(MenuItem v) {
        GeofenceLoadTask task = new GeofenceLoadTask(mLastLocation.getLatitude(), mLastLocation.getLongitude());
        task.execute();
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(UserProfileActivity.this)
                .addConnectionCallbacks(UserProfileActivity.this)
                .addOnConnectionFailedListener(UserProfileActivity.this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_user_proile, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(UserProfileActivity.this, SettingsActivity.class);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.i(TAG, "Location services connected");
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {
            Log.i(TAG, "last known location retrieved");

            mLatitudeTextView.setText(String.valueOf(mLastLocation.getLatitude()));
            mLongitudeTextView.setText(String.valueOf(mLastLocation.getLongitude()));
        } else {
            Log.i(TAG, "Could not retrieve last known location");
        }

        if (mRequestingLocationUpdates) {
            createLocationRequest();
            startLocationUpdates();
        }

    }

    @Override
    public void onConnectionSuspended(int i) {

        Log.i(TAG, "Connection to Play Services suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

        Log.i(TAG, "Connection to Play Services failed");
    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
        mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
        refreshDisplay();
    }

    private void refreshDisplay() {
        mLatitudeTextView.setText(String.valueOf(mLastLocation.getLatitude()));
        mLongitudeTextView.setText(String.valueOf(mLastLocation.getLongitude()));
        mLastUpdateTimeTextView.setText(mLastUpdateTime);
    }

    private class ContextUpdater extends AsyncTask<Item, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Item... params) {

            Item item = params[0];

            JSONObject object = new JSONObject();
            try {
                object.put("type", "location");
                JSONArray itemParams = new JSONArray();
                if (item instanceof LocationCurrent) {
                    LocationCurrent locationCurrent = (LocationCurrent) item;
                    JSONObject param1 = new JSONObject();
                    param1.put("name", "latitude");
                    param1.put("value", locationCurrent.getLocation().getLatitude());

                    JSONObject param2 = new JSONObject();
                    param2.put("name", "longitude");
                    param2.put("value", locationCurrent.getLocation().getLongitude());

                    JSONObject param3 = new JSONObject();
                    param3.put("name", "accuracy");
                    param3.put("value", locationCurrent.getAccuracy());

                    itemParams.put(param1);
                    itemParams.put(param2);
                    itemParams.put(param3);
                    object.put("params", itemParams);
                    object.put("created", "2015-08-11");
                }
                Log.d("UserProfileActivity", "Location update: " + object.toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }


            try {
                URL url = new URL(URI);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("POST");
                con.setConnectTimeout(10000);
                con.setRequestProperty("content-type", "application/json");
                con.setDoOutput(true);

                OutputStreamWriter writer = new OutputStreamWriter(con.getOutputStream());
                writer.write(object.toString());
                writer.flush();


                //StringBuilder sb = new StringBuilder();
//                BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));

                /*String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }*/

                int response = con.getResponseCode();
                Log.d("UserProfileActivity" , "Response code: " + response);

                writer.close();
                con.disconnect();


                //if (response == 200)
               //     return sb.toString();


            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                Toast.makeText(UserProfileActivity.this, "location state updated", Toast.LENGTH_LONG).show();
            }
        }
    }

    public void setPreference(View view) {
        SharedPreferences.Editor editor = settings.edit();

        // retrieve text from text view and put it in a string object

    }

    protected void startLocationUpdates() {
        if (mLocationRequest == null) {
            createLocationRequest();
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @Override
    protected void onResume() {
        super.onResume();
        dataSource.open();
        mGoogleApiClient.connect();
        if (mGoogleApiClient.isConnected() && !mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        dataSource.close();
        stopLocationUpdates();
    }

    private void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    private void createData() {

        //create an item and store it using datasource method "create"
        //don't forget logging

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(REQUESTING_LOCATION_UPDATES_KEY, mRequestingLocationUpdates);
        outState.putParcelable(LOCATION_KEY, mLastLocation);
        outState.putString(LAST_UPDATED_TIME_STRING_KEY, mLastUpdateTime);
        super.onSaveInstanceState(outState);
    }

    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {

        }
    }

    public class GeofenceLoadTask extends AsyncTask<String, Void, String> {

        private String latitude;
        private String longitude;

        public GeofenceLoadTask(double latitude, double longitude) {
            this.latitude = String.valueOf(latitude);
            this.longitude = String.valueOf(longitude);
        }

        @Override
        protected String doInBackground(String... params) {

            String p = "lat="+latitude+"&long="+longitude;

            try {

                URL url = new URL(URI_GEO);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("POST");
                con.setReadTimeout(10000);
                con.setConnectTimeout(12000);
                con.setDoOutput(true);

                OutputStreamWriter writer = new OutputStreamWriter(con.getOutputStream());
                writer.write(p);
                writer.flush();

                StringBuilder sb = new StringBuilder();
                BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));

                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }

                //int response = con.getResponseCode();

                writer.close();
                con.disconnect();

                return sb.toString();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return "";
        }

        @Override
        protected void onPostExecute(String result) {

            if (result != null && result != "") {
                Intent intent = new Intent(UserProfileActivity.this, GeofenceListActivity.class);
                intent.putExtra("geofences", result);
                startActivity(intent);
            } else {
                //mPasswordView.setError(getString(R.string.error_incorrect_password));
                //mPasswordView.requestFocus();
                Toast.makeText(UserProfileActivity.this, "Geofences could not be retrieved", Toast.LENGTH_SHORT).show();
            }
        }
    }

}


