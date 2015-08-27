package com.postnikoff.consense.sensing;

import android.app.Application;
import android.location.Location;
import android.util.Log;
import android.widget.Toast;

import com.intel.context.Sensing;
import com.intel.context.error.ContextError;
import com.intel.context.item.ActivityRecognition;
import com.intel.context.item.AppsInstalled;
import com.intel.context.item.Item;
import com.intel.context.item.LocationCurrent;
import com.intel.context.item.activityrecognition.ActivityName;
import com.intel.context.item.activityrecognition.PhysicalActivity;
import com.intel.context.item.appsinstalled.ApplicationStatus;
import com.intel.context.item.installedapplication.InstalledApplicationInfo;
import com.intel.context.sensing.ContextTypeListener;
import com.intel.context.sensing.SensingEvent;
import com.intel.context.sensing.SensingStatusListener;
import com.postnikoff.consense.db.ContextDataSource;
import com.postnikoff.consense.model.ContextParam;
import com.postnikoff.consense.model.ContextState;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by CodeX on 06.08.2015.
 */
public class ActivityRecognitionApplication extends Application {

    private static ActivityRecognitionApplication application;
    private static ContextTypeListener mActivityRecognitionListener;
    public Sensing mSensing;

    private ContextDataSource dataSource;

    public Sensing getmSensing() {
        return mSensing;
    }

    public ContextTypeListener getmActivityRecognitionListener() {
        return mActivityRecognitionListener;
    }

    @Override
    public void onCreate() {
        application = this;
        mSensing = new Sensing(getApplicationContext(), new MySensingListener());
        mActivityRecognitionListener = new ConsenseContextTypeListener();
        super.onCreate();
    }

    public static ActivityRecognitionApplication getInstance() {
        if (application == null)
            application = new ActivityRecognitionApplication();
        return application;
    }

    private class MySensingListener implements SensingStatusListener {

        private final String TAG = MySensingListener.class.getName();

        MySensingListener() {}

        @Override
        public void onEvent(SensingEvent sensingEvent) {
            Log.i(TAG, "Event: " + sensingEvent.getDescription());
        }

        @Override
        public void onFail(ContextError contextError) {
            Log.e(TAG, "Context Sensing error: " + contextError.getMessage());
        }
    }

    private class ConsenseContextTypeListener implements ContextTypeListener {

        private final String TAG = ConsenseContextTypeListener.class.getName();

        @Override
        public void onReceive(Item item) {
            // Activity recognition item received
            if (item instanceof ActivityRecognition) {
                ActivityRecognition activityRecognition = (ActivityRecognition) item;
                //List<PhysicalActivity> physicalActivityList = activityRecognition.getActivities();
                PhysicalActivity physicalActivity = activityRecognition.getMostProbableActivity();
                ActivityName activityName = physicalActivity.getActivity();

                //store in DB
                long timestamp      = activityRecognition.getTimestamp();
                String contextType  = activityRecognition.getContextType();
                String actName      = activityName.name();
                int probability     = activityRecognition.getProbability(activityName);

                ContextState newState = new ContextState(timestamp, contextType);
                List<ContextParam> params = new ArrayList<>();
                ContextParam param1 = new ContextParam("activityName", "string", actName);
                ContextParam param2 = new ContextParam("probability", "int", new Integer(probability).toString());
                params.add(param1);
                params.add(param2);
                newState.setParams(params);

                newState = storeContextState(newState);
                Log.i(TAG, "New context state item has been stored in DB: " + newState.getId());

            } else if (item instanceof LocationCurrent) {

                LocationCurrent location = (LocationCurrent) item;
                Location l = location.getLocation();

                String  activity = location.getActivity();
                long    timestamp = location.getTimestamp();
                String contextType = location.getContextType();
                float   accuracy = l.getAccuracy();
                double  lat      = l.getLatitude();
                double  lon      = l.getLongitude();
                long    time     = l.getTime();
                float   speed    = l.getSpeed();

                ContextState contextState = new ContextState(timestamp, contextType);
                List<ContextParam> params = new ArrayList<>();
                ContextParam param1 = new ContextParam("accuracy", "float", new Float(accuracy).toString());
                ContextParam param2 = new ContextParam("lat", "double", new Double(lat).toString());
                ContextParam param3 = new ContextParam("long", "double", new Double(lon).toString());
                ContextParam param4 = new ContextParam("speed", "float", new Float(speed).toString());
                params.add(param1);
                params.add(param2);
                params.add(param3);
                params.add(param4);
                contextState.setParams(params);

                contextState = storeContextState(contextState);

                Log.i(TAG, "New Location State has been stored in DB: " + contextState.getId());
            } else if (item instanceof AppsInstalled) {
                AppsInstalled appsInstalled = (AppsInstalled) item;
                String  contextType = appsInstalled.getContextType();
                long    timestamp   = appsInstalled.getTimestamp();

                ContextState contextState = new ContextState(timestamp, contextType);
                contextState.setParams(new ArrayList<ContextParam>());

                List<InstalledApplicationInfo> apps = appsInstalled.getInstalledApplications();
                for (InstalledApplicationInfo app : apps) {
                    String appName      = app.getAppName();
                    String installDate  = app.getInstallationDate();
                    ApplicationStatus status = app.getStatus();
                    String appStatus    = status.name();

                    contextState.addParam(new ContextParam("appName", "string", appName));
                    contextState.addParam(new ContextParam("installDate", "string", installDate));
                    contextState.addParam(new ContextParam("appStatus", "string", appStatus));
                }

                Log.i(TAG, "New installed apps State: ");

            }
            else {
                Log.i(TAG, "Invalid state type: " + item.getContextType());
            }
        }

        private ContextState storeContextState(ContextState newState) {
            dataSource = new ContextDataSource(getApplicationContext());
            dataSource.open();
            newState = dataSource.create(newState);
            dataSource.close();
            return newState;
        }

        @Override
        public void onError(ContextError contextError) {
            Toast.makeText(getApplicationContext(), "Listener Status: " + contextError.getMessage(), Toast.LENGTH_LONG).show();
            Log.e(TAG, "Error: " + contextError.getMessage());
        }
    }
}
