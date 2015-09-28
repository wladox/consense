package com.postnikoff.consense;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.ResultReceiver;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.intel.context.Sensing;
import com.intel.context.error.ContextError;
import com.intel.context.exception.ContextProviderException;
import com.intel.context.item.ContextType;
import com.intel.context.option.activity.ActivityOptionBuilder;
import com.intel.context.option.activity.Mode;
import com.intel.context.option.activity.ReportType;
import com.intel.context.option.audio.AudioOptionBuilder;
import com.intel.context.sensing.ContextTypeListener;
import com.intel.context.sensing.InitCallback;
import com.postnikoff.consense.adapter.UserFeatureAdapter;
import com.postnikoff.consense.db.ContextDataSource;
import com.postnikoff.consense.geo.FetchAddressIntentService;
import com.postnikoff.consense.geo.GeofenceTransitionsIntentService;
import com.postnikoff.consense.helper.AssetsPropertyReader;
import com.postnikoff.consense.helper.Constants;
import com.postnikoff.consense.model.MyGeofence;
import com.postnikoff.consense.model.UserFeature;
import com.postnikoff.consense.network.UserListActivity;
import com.postnikoff.consense.prefs.HeadersActivity;
import com.postnikoff.consense.sensing.ContextSensingApplication;
import com.postnikoff.consense.upload.ImageUploadActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

public class UserProfileActivity extends Activity
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        LocationListener, ResultCallback<Status> {

    private static final String TAG     = UserProfileActivity.class.getName();
    private static final String URI_CONTEXT_SET     = "/context/set";
    private static final String URI_GEO_NEIGHBOURS = "/geofence/neighbours";
    private static final String URI_USER_IMAGE = "/user/image";

    private static final String REQUESTING_LOCATION_UPDATES_KEY = "requesting_location_updates";
    private static final String LAST_UPDATED_TIME_STRING_KEY    = "last_update_time";
    private static final String LOCATION_KEY                    = "last_location";

    // Camera activity request codes
    private static final int CAMERA_CAPTURE_IMAGE_REQUEST_CODE = 100;
    public static final int MEDIA_TYPE_IMAGE = 1;

    private Uri fileUri;

    private static ContextTypeListener mConsenseContextListener;

    // GUI components
    private TextView mUsernameTextView;
    private TextView mLatitudeTextView;
    private TextView mLongitudeTextView;
    private TextView mLastUpdateTimeTextView;
    private TextView mAddressTextView;
    private ImageView mUserImageView;

    // Sensing + Geofencing components
    private List<Geofence>  mGeofenceList;
    private Sensing         mSensing;
    private GoogleApiClient mGoogleApiClient;
    private Location        mLastLocation;
    private LocationRequest mLocationRequest;
    private String          mLastUpdateTime;
    private boolean         mRequestingLocationUpdates = true;
    private String          mAddress;

    private PendingIntent   mGeofencePendingIntent;

    // Geocoding service
   private AddressResultReceiver mResultReceiver;


    // Properties handler
    private AssetsPropertyReader    propertyReader;
    private Properties              properties;

    // Preferences handler
    private SharedPreferences settings;

    // SQLite
    private ContextDataSource dataSource;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);
        updateValuesFromBundle(savedInstanceState);

        mAddress = "";
        mResultReceiver = new AddressResultReceiver(new Handler());

        // reserve a list for geofences
        mGeofenceList           = new ArrayList<>();
        mGeofencePendingIntent  = null;

        // Context Sensing
        mSensing        = ContextSensingApplication.getInstance().getmSensing();
        mConsenseContextListener = ContextSensingApplication.getInstance().getmActivityRecognitionListener();

        // establish database connection
        dataSource      = new ContextDataSource(this);

        // open shared Preferences
        settings        = getSharedPreferences(Constants.SHARED_PREFERENCES_FILE,MODE_PRIVATE);

        // read properties file
        propertyReader  = new AssetsPropertyReader(getBaseContext());
        properties      = propertyReader.getProperties("app.properties");

        buildGoogleApiClient();

        mUsernameTextView       = (TextView) findViewById(R.id.usernameView);

        // labels for location information
        mLatitudeTextView       = (TextView) findViewById(R.id.latitudeText);
        mLongitudeTextView      = (TextView) findViewById(R.id.longitudeText);
        mLastUpdateTimeTextView = (TextView) findViewById(R.id.update_time);
        mAddressTextView        = (TextView) findViewById(R.id.addressView);
        mUserImageView          = (ImageView) findViewById(R.id.userImage);
        mUserImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isDeviceSupportCamera()) {
                    Toast.makeText(getApplicationContext(),
                            "Sorry! Your device doesn't support camera",
                            Toast.LENGTH_LONG).show();
                }
                captureImage();
            }
        });
        
        // feature list of a user
        ArrayList<UserFeature> features = new ArrayList<>();
        String userdata = getIntent().getStringExtra("userdata");

        if (userdata != null) {
            SharedPreferences.Editor editor = settings.edit();
            editor.putString("features", userdata);
            editor.commit();
            getUserFeatures(features, userdata);

        } else {
            getUserFeatures(features, settings.getString("features", null));
        }

        // populate user feature list
        ArrayAdapter<UserFeature> adapter = new UserFeatureAdapter(this, features);
        ListView listView = (ListView) findViewById(R.id.feature_list);
        listView.setAdapter(adapter);

        ImageLoaderTask imageLoaderTask = new ImageLoaderTask();
        imageLoaderTask.execute();
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

    private boolean isDeviceSupportCamera() {
        if (getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA))
            return true;
        else
            return false;
    }

    private void captureImage() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        fileUri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
        // start the image capture Intent
        startActivityForResult(intent, CAMERA_CAPTURE_IMAGE_REQUEST_CODE);
    }

    private void launchUploadActivity(boolean isImage){
        Intent i = new Intent(UserProfileActivity.this, ImageUploadActivity.class);
        i.putExtra("filePath", fileUri.getPath());
        i.putExtra("isImage", isImage);
        startActivity(i);
    }

    /**
     * Creating file uri to store image/video
     */
    public Uri getOutputMediaFileUri(int type) {
        return Uri.fromFile(getOutputMediaFile(type));
    }

    /**
     * returning image / video
     */
    private static File getOutputMediaFile(int type) {

        // External sdcard location
        File mediaStorageDir = new File(
                Environment
                        .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                Constants.IMAGE_DIRECTORY_NAME);

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d(TAG, "Oops! Failed create "
                        + Constants.IMAGE_DIRECTORY_NAME + " directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss",
                Locale.getDefault()).format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator
                    + "IMG_" + timeStamp + ".jpg");
        } else {
            return null;
        }

        return mediaFile;
    }

    private void updateContextOnServer() {

        dataSource.open();
        String userContext = dataSource.findAllAsJSON();
        dataSource.close();

        // Send context to the server DB
        ContextUpdateTask contextUpdater = new ContextUpdateTask();
        contextUpdater.execute(userContext);
    }

    private void getUserFeatures(ArrayList<UserFeature> features, String userdata) {
        try {
            JSONObject object = new JSONObject(userdata);

            if (settings.getString("userId", null) == null) {
                SharedPreferences.Editor editor = settings.edit();
                editor.putString("userId", object.getString("userId"));
                Log.d(TAG, "userId = " + object.getString("userId"));
                editor.commit();
            }

            mUsernameTextView.setText(object.getString("username"));

            JSONArray array = new JSONArray(object.getString("features"));
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

    // Menu item click
    public void enableSensing(MenuItem v) {
        ActivityOptionBuilder activityRecognitionSettings = new ActivityOptionBuilder();
        activityRecognitionSettings.setMode(Mode.NORMAL);
        activityRecognitionSettings.setReportType(ReportType.FREQUENCY);

        AudioOptionBuilder audioOptionBuilder = new AudioOptionBuilder();
        audioOptionBuilder.setMode(com.intel.context.option.audio.Mode.ON_CHANGE);

        try {
            // activity recognition
            mSensing.enableSensing(ContextType.ACTIVITY_RECOGNITION, activityRecognitionSettings.toBundle());
            mSensing.addContextTypeListener(ContextType.ACTIVITY_RECOGNITION, mConsenseContextListener);

            // location current
            mSensing.enableSensing(ContextType.LOCATION, null);
            mSensing.addContextTypeListener(ContextType.LOCATION, mConsenseContextListener);

            // installed apps
            mSensing.enableSensing(ContextType.INSTALLED_APPS, null);
            mSensing.addContextTypeListener(ContextType.INSTALLED_APPS, mConsenseContextListener);

            // audio classification
            mSensing.enableSensing(ContextType.AUDIO, audioOptionBuilder.toBundle());
            mSensing.addContextTypeListener(ContextType.AUDIO, mConsenseContextListener);

            mSensing.enableSensing(ContextType.MUSIC, null);
            mSensing.addContextTypeListener(ContextType.MUSIC, mConsenseContextListener);

            // pedometer
            mSensing.enableSensing(ContextType.PEDOMETER, null);
            mSensing.addContextTypeListener(ContextType.PEDOMETER, mConsenseContextListener);

        } catch (ContextProviderException e) {
            Log.e(TAG, "Error enabling context type " + e.getMessage());
        }
    }

    public void disableSensing(MenuItem v) {
        try {
            mSensing.removeContextTypeListener(mConsenseContextListener);
            mSensing.disableSensing(ContextType.ACTIVITY_RECOGNITION);
            mSensing.disableSensing(ContextType.LOCATION);
            mSensing.disableSensing(ContextType.INSTALLED_APPS);
            mSensing.disableSensing(ContextType.AUDIO);
            mSensing.disableSensing(ContextType.MUSIC);
            mSensing.disableSensing(ContextType.PEDOMETER);
        } catch (ContextProviderException e) {
            Log.e(TAG, "Error during disabling of sensing occured " + e.getMessage());
        }
    }

    public void stopDaemon(MenuItem v) {
        mSensing.stop();
    }

    public void logout(MenuItem v) {

        dataSource.close();
        stopLocationUpdates();
        finish();

    }

    // Menu item click
    public void showUsers(MenuItem v) {
        Intent intent = new Intent(UserProfileActivity.this, UserListActivity.class);
        startActivity(intent);
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
        getMenuInflater().inflate(R.menu.menu_user_profile, menu);
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
            Intent intent = new Intent(UserProfileActivity.this, HeadersActivity.class);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.i(TAG, "Location services connected");
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {
            updateGeofenceList();
            Log.i(TAG, "last known location retrieved");
            mLatitudeTextView.setText(String.valueOf(mLastLocation.getLatitude()));
            mLongitudeTextView.setText(String.valueOf(mLastLocation.getLongitude()));
            Log.d(TAG, "last known location: lat= " + mLastLocation.getLatitude() + ", long="
                    +mLastLocation.getLongitude() + ",speed=" + mLastLocation.getSpeed());
            if (!Geocoder.isPresent()) {
                Toast.makeText(this, R.string.no_geocoder_available, Toast.LENGTH_LONG).show();
                return;
            } else {
                startGeocodingService();
            }
        } else {
            Log.i(TAG, "Could not retrieve last known location");
        }

        if (mRequestingLocationUpdates) {
            createLocationRequest();
            startLocationUpdates();
        }

    }

    private void populateGeofenceList(String geofenceList) {
        JSONArray array = null;
        List<MyGeofence> geofences = null;
        try {
            array = new JSONArray(geofenceList);
            geofences = new ArrayList<>();
            for(int i = 0; i < array.length(); i++) {
                JSONObject jsonObject = array.getJSONObject(i);
                MyGeofence geofence = new MyGeofence();
                geofence.setGeofenceId(jsonObject.getInt("geofenceId"));
                geofence.setName(jsonObject.getString("name"));
                geofence.setLatitude(jsonObject.getDouble("latitude"));
                geofence.setLongitude(jsonObject.getDouble("longitude"));
                geofence.setRadius(jsonObject.getInt("radius"));
                geofence.setDuration(jsonObject.getInt("duration"));
                geofences.add(geofence);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        ArrayList<Geofence> geofencesToAdd      = new ArrayList<>();
        ArrayList<Geofence> geofencesToRemove   = new ArrayList<>();
        ArrayList<String>   geofenceIdsToRemove = new ArrayList<>();

        boolean exist;
        for (MyGeofence g : geofences) {
            exist = false;
            for (Geofence geofence : mGeofenceList) {
                if (geofence.getRequestId().equals(g.getGeofenceId().toString())) {
                    exist = true;
                }
            }
            if (!exist)
                geofencesToAdd.add(createGeofence(g));
        }

        if (geofencesToAdd.size() > 0 && mGoogleApiClient.isConnected()) {
            mGeofenceList.addAll(geofencesToAdd);
            LocationServices.GeofencingApi.addGeofences(
                    mGoogleApiClient,
                    getGeofencingRequest(geofencesToAdd),
                    getGeofencePendingIntent())
                    .setResultCallback(UserProfileActivity.this);
            Log.d(TAG, "Geofencing started");
        } else {
            Log.d(TAG, "Nothing to be added or Play Services are not available");
        }

        for (Geofence geofence : mGeofenceList) {
            exist = false;
            for (MyGeofence g : geofences) {
                if (geofence.getRequestId().equals(g.getGeofenceId().toString())) {
                    exist = true;
                }
            }
            if (!exist) {
                geofenceIdsToRemove.add(geofence.getRequestId());
                geofencesToRemove.add(geofence);
            }
        }

        if (geofenceIdsToRemove.size() > 0 && mGoogleApiClient.isConnected()) {
            mGeofenceList.removeAll(geofencesToRemove);
            LocationServices.GeofencingApi.removeGeofences(
                    mGoogleApiClient,
                    geofenceIdsToRemove)
                    .setResultCallback(UserProfileActivity.this);
            Log.d(TAG, geofenceIdsToRemove.size() +" geofences has been removed");
        } else {
            Log.d(TAG, "Nothing to be removed or Play Services are not available");
        }

    }

    @NonNull
    private Geofence createGeofence(MyGeofence g) {
        return new Geofence.Builder()
                        .setRequestId(String.valueOf(g.getGeofenceId()))
                        .setCircularRegion(
                                g.getLatitude(),
                                g.getLongitude(),
                                g.getRadius()
                        )
                        .setExpirationDuration(g.getDuration())
                        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                        .build();
    }

    private void updateGeofenceList() {
        GeofenceLoadTask task = new GeofenceLoadTask(mLastLocation.getLatitude(), mLastLocation.getLongitude());
        task.execute();
    }

    public void startGeofencing() {
        if (mGeofenceList.size() > 0) {
            LocationServices.GeofencingApi.addGeofences(
                    mGoogleApiClient,
                    getGeofencingRequest(mGeofenceList),
                    getGeofencePendingIntent()
            ).setResultCallback(UserProfileActivity.this);
            Log.d(TAG, "Geofencing started");
        } else {
            Toast.makeText(this, "you didn't select at least one geofence", Toast.LENGTH_LONG).show();
        }

    }

    public void removeGeofencing() {
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
            ).setResultCallback(UserProfileActivity.this); // Result processed in onResult().
        } catch (SecurityException securityException) {
            // Catch exception generated if the app does not use ACCESS_FINE_LOCATION permission.
            logSecurityException(securityException);
        }
    }

    private void logSecurityException(SecurityException securityException) {
        Log.e(TAG, "Invalid location permission. " +
                "You need to use ACCESS_FINE_LOCATION with geofences", securityException);
    }

    private GeofencingRequest getGeofencingRequest(List<Geofence> geofenceList) {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofences(geofenceList);
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
        updateGeofenceList();
        Log.d(TAG, "Location update received: speed=" + mLastLocation.getSpeed() + ", latitude=" + mLastLocation.getLatitude() + ", longitude=" + mLastLocation.getLongitude());
        mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
        startGeocodingService();
        refreshDisplay();
    }

    protected void startLocationUpdates() {
        if (mLocationRequest == null) {
            createLocationRequest();
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    // location update interval must be adaptable depending on user's speed and presence inside of a geofence
    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    protected void startGeocodingService() {
        Intent intent = new Intent(this, FetchAddressIntentService.class);
        intent.putExtra(Constants.RECEIVER, mResultReceiver);
        intent.putExtra(Constants.LOCATION_DATA_EXTRA, mLastLocation);
        startService(intent);
    }

    private void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    private void refreshDisplay() {
        mLatitudeTextView.setText(String.valueOf(mLastLocation.getLatitude()));
        mLongitudeTextView.setText(String.valueOf(mLastLocation.getLongitude()));
        mLastUpdateTimeTextView.setText(mLastUpdateTime);
        mAddressTextView.setText(mAddress);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelable("file_uri", fileUri);
        outState.putBoolean(REQUESTING_LOCATION_UPDATES_KEY, mRequestingLocationUpdates);
        outState.putParcelable(LOCATION_KEY, mLastLocation);
        outState.putString(LAST_UPDATED_TIME_STRING_KEY, mLastUpdateTime);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        fileUri = savedInstanceState.getParcelable("file_uri");
    }

    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {

        }
    }


    /**
     * Receiving activity result method will be called after closing the camera
     * */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CAMERA_CAPTURE_IMAGE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                launchUploadActivity(true);
            } else if(resultCode == RESULT_CANCELED) {
                showToast("User cancelled image capture");
            } else {
                showToast("Sorry! Failed to capture image");
            }
        }
    }

    @Override
    public void onResult(Status status) {
        Log.i(TAG, "Result received: " + status.getStatusMessage());
        Log.i(TAG, "Result received: " + status.getStatusCode());
    }

    private class AddressResultReceiver extends ResultReceiver {

        /**
         * Create a new ResultReceive to receive results.  Your
         * {@link #onReceiveResult} method will be called from the thread running
         * <var>handler</var> if given, or from an arbitrary thread if null.
         *
         * @param handler
         */
        public AddressResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            mAddress = resultData.getString(Constants.RESULT_DATA_KEY);
            refreshDisplay();
            if (resultCode == Constants.SUCCESS_RESULT) {
                showToast(getString(R.string.address_found));
            }
        }
    }

    private void showToast(String message) {
        Toast.makeText(UserProfileActivity.this, message, Toast.LENGTH_LONG).show();
    }

    private class ContextUpdateTask extends AsyncTask<String, Void, Boolean> {

        public ContextUpdateTask() {}

        @Override
        protected Boolean doInBackground(String... params) {

            Log.i(TAG, "context updater on process");

            String json = params[0];

            String postArgs = "userId=3&context="+json;
            /*JSONObject object = new JSONObject();
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
            }*/


            try {

                URL url = new URL(properties.getProperty("server.url") + URI_CONTEXT_SET);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("POST");
                con.setReadTimeout(10000);
                con.setConnectTimeout(10000);
                //con.setRequestProperty("content-type", "application/json");
                con.setDoOutput(true);

                OutputStreamWriter writer = new OutputStreamWriter(con.getOutputStream());
                writer.write(postArgs);
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
                Toast.makeText(UserProfileActivity.this, "context successfully updated", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(UserProfileActivity.this, "context NOT updated", Toast.LENGTH_LONG).show();
            }
        }
    }

    private class GeofenceLoadTask extends AsyncTask<String, Void, String> {

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

                // Connect to service
                URL url = new URL(properties.getProperty("server.url") + URI_GEO_NEIGHBOURS);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("POST");
                con.setReadTimeout(10000);
                con.setConnectTimeout(12000);
                con.setDoOutput(true);

                // Send request
                OutputStreamWriter writer = new OutputStreamWriter(con.getOutputStream());
                Log.i("GeofenceTask", "request: " + p);
                writer.write(p);
                writer.flush();

                // Retrieve response
                StringBuilder sb        = new StringBuilder();
                BufferedReader reader   = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }

                //int response = con.getResponseCode();

                // Release resources
                writer.close();
                con.disconnect();

                Log.i("GeofenceTask", "response: \n" +sb.toString());

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

            if (result != null && !result.equals("")) {
                populateGeofenceList(result);
            } else {
                Toast.makeText(UserProfileActivity.this, "Geofences could not be retrieved", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class ImageLoaderTask extends AsyncTask<String, Void, Bitmap> {

        private String userId;

        public ImageLoaderTask() {

        }

        @Override
        protected void onPreExecute() {
            userId = settings.getString("userId", "");
        }

        @Override
        protected Bitmap doInBackground(String... params) {

            try {

                // Connect to service
                URL url = new URL(properties.getProperty("server.url") + URI_USER_IMAGE);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("POST");
                con.setReadTimeout(10000);
                con.setConnectTimeout(12000);
                con.setDoOutput(true);

                // Send request
                OutputStreamWriter writer = new OutputStreamWriter(con.getOutputStream());
                writer.write("userId="+userId);
                writer.flush();

                // Retrieve response
                InputStream is  = con.getInputStream();
                Bitmap bitmap   = BitmapFactory.decodeStream(is);
                is.close();

                // Release resources
                writer.close();
                con.disconnect();


                return bitmap;
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap != null) {
                mUserImageView.setImageBitmap(bitmap);
                refreshDisplay();
            } else {
                Log.e(TAG, "User image not available");
            }
        }
    }
}


