package com.postnikoff.consense.prefs;

import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import com.postnikoff.consense.R;
import com.postnikoff.consense.helper.AssetsPropertyReader;
import com.postnikoff.consense.helper.Constants;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Properties;

public class SettingsActivity extends PreferenceActivity {

    private static final String LOGTAG = SettingsActivity.class.getName();

    private static final String KEY_INSTALLED_APPS  = "pref_installed_apps";
    private static final String KEY_ACTIVITY        = "pref_activity_index";
    private static final String KEY_MUSIC           = "pref_music";

    private static final String URL_USER_SET_ACCESS      = "/user/setaccess";

    private static final int ACTIVITY_CAT_ID    = 5;
    private static final int APPS_CAT_ID        = 6;
    private static final int MUSIC_CAT_ID       = 1;

    @Override
    public void onBuildHeaders(List<PreferenceActivity.Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }


    protected boolean isValidFragment(String fragmentName) {
        if (GeneralPreferenceFragment.class.getName().equals(fragmentName)) {
            return true;
        } else if (NotificationPreferenceFragment.class.getName().equals(fragmentName)) {
            return true;
        } else if (DataSyncPreferenceFragment.class.getName().equals(fragmentName)) {
            return true;
        }
        return false;
    }

    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);

            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class GeneralPreferenceFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

        // Properties handler
        private AssetsPropertyReader propertyReader;
        private Properties properties;
        private SharedPreferences settings;


        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            // read properties file
            propertyReader  = new AssetsPropertyReader(this.getActivity());
            properties      = propertyReader.getProperties("app.properties");
            settings = getActivity().getSharedPreferences(Constants.SHARED_PREFERENCES_FILE, MODE_PRIVATE);
            addPreferencesFromResource(R.xml.pref_general);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

            AccessRuleTask task = new AccessRuleTask();

            if (key.equals(KEY_ACTIVITY)) {
                ListPreference preference = (ListPreference) findPreference(key);
                preference.setSummary(preference.getEntry());

                // update access rule on server
                task.execute(settings.getInt("userId", 0), ACTIVITY_CAT_ID, Integer.valueOf(sharedPreferences.getString(key, "")));
            } else if (key.equals(KEY_INSTALLED_APPS)) {
                ListPreference preference = (ListPreference) findPreference(key);
                preference.setSummary(preference.getEntry());

                // update access rule on server
                task.execute(settings.getInt("userId", 0), APPS_CAT_ID, Integer.valueOf(sharedPreferences.getString(key, "")));
            } else if (key.equals(KEY_MUSIC)) {
                ListPreference preference = (ListPreference) findPreference(key);
                preference.setSummary(preference.getEntry());

                // update access rule on server
                task.execute(settings.getInt("userId", 0), MUSIC_CAT_ID, Integer.valueOf(sharedPreferences.getString(key, "")));
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onPause() {
            super.onPause();
            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        }

        private class AccessRuleTask extends AsyncTask<Integer, Void, Integer> {

            public AccessRuleTask() {
            }

            @Override
            protected Integer doInBackground(Integer... params) {

                String requestParams = "userId="+params[0]+"&categoryId="+ params[1]+"&lvl="+params[2];
                Log.d(LOGTAG, requestParams);
                try {

                    // Connect to service
                    URL url = new URL(properties.getProperty("server.url") + URL_USER_SET_ACCESS);
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    con.setRequestMethod("POST");
                    con.setReadTimeout(5000);
                    con.setConnectTimeout(7000);
                    con.setDoOutput(true);

                    // Send request
                    OutputStreamWriter writer = new OutputStreamWriter(con.getOutputStream());
                    writer.write(requestParams);
                    writer.flush();

                    // Retrieve response
                /*StringBuilder sb        = new StringBuilder();
                BufferedReader reader   = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }*/

                    int response = con.getResponseCode();

                    // Release resources
                    writer.close();
                    con.disconnect();

                    return response;
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Integer result) {

                if (result != null && result == HttpURLConnection.HTTP_OK) {
                    Log.i(SettingsActivity.class.getName(), "Access rule successfully modified");
                } else {
                    Log.i(SettingsActivity.class.getName(), "Access rule could not be modified");
                }
            }
        }

    }

    /**
     * This fragment shows notification preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class NotificationPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_notification);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference("notifications_new_message_ringtone"));
        }

    }

    /**
     * This fragment shows data and sync preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class DataSyncPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_data_sync);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference("sync_frequency"));
        }

    }

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }


}
