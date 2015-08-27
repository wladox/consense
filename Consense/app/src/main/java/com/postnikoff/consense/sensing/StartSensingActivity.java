package com.postnikoff.consense.sensing;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.intel.context.Sensing;
import com.intel.context.error.ContextError;
import com.intel.context.exception.ContextProviderException;
import com.intel.context.item.ContextType;
import com.intel.context.option.activity.ActivityOptionBuilder;
import com.intel.context.option.activity.Mode;
import com.intel.context.option.activity.ReportType;
import com.intel.context.sensing.ContextTypeListener;
import com.intel.context.sensing.InitCallback;
import com.postnikoff.consense.R;

public class StartSensingActivity extends Activity {

    private static String TAG = StartSensingActivity.class.getName();
    private Sensing mSensing;
    private ContextTypeListener mActivityRecognitionListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_sensing);

        mSensing = ActivityRecognitionApplication.getInstance().getmSensing();
        mActivityRecognitionListener = ActivityRecognitionApplication.getInstance().getmActivityRecognitionListener();
    }

    public void startDaemon(View v) {
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

    public void enableSensing(View v) {
        ActivityOptionBuilder settings = new ActivityOptionBuilder();
        settings.setMode(Mode.NORMAL);
        settings.setReportType(ReportType.FREQUENCY);

        try {
            mSensing.enableSensing(ContextType.ACTIVITY_RECOGNITION, settings.toBundle());
            mSensing.addContextTypeListener(ContextType.ACTIVITY_RECOGNITION, mActivityRecognitionListener);

            mSensing.enableSensing(ContextType.LOCATION, null);
            mSensing.addContextTypeListener(ContextType.LOCATION, mActivityRecognitionListener);

            mSensing.enableSensing(ContextType.INSTALLED_APPS, null);
            mSensing.addContextTypeListener(ContextType.INSTALLED_APPS, mActivityRecognitionListener);

            mSensing.enableSensing(ContextType.PEDOMETER, null);
            mSensing.addContextTypeListener(ContextType.PEDOMETER, mActivityRecognitionListener);

        } catch (ContextProviderException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "Error enabling context type: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e(TAG, "Error enabling context type " + e.getMessage());
        }
    }

    public void disableSensing(View v) {
        try {
            mSensing.removeContextTypeListener(mActivityRecognitionListener);
            mSensing.disableSensing(ContextType.ACTIVITY_RECOGNITION);
            mSensing.disableSensing(ContextType.LOCATION);
            mSensing.disableSensing(ContextType.INSTALLED_APPS);
            mSensing.disableSensing(ContextType.PEDOMETER);
        } catch (ContextProviderException e) {
            e.printStackTrace();
        }
    }

    public void stopDaemon(View v) {
        mSensing.stop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_start_sensing, menu);
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
}
