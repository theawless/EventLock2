package com.gobbledygook.theawless.eventlock2;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;
import android.provider.CalendarContract;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;

public class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final int CALENDAR_READ_REQUEST_CODE = 0;
    private static final String TAG = "SETTINGS_FRAGMENT";
    private EventNotificationManager eventNotificationManager = new EventNotificationManager();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getPreferenceManager().setSharedPreferencesName(getString(R.string.preferences));
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        addPreferencesFromResource(R.xml.preferences);
        checkAndRequestPermission();
    }

    @Override
    public void onStart() {
        super.onStart();
        Activity activity = getActivity();
        activity.startService(new Intent(activity, SchedulingService.class));
        activity.startService(new Intent(activity, LockscreenSchedulingService.class));
    }

    @Override
    public void onDestroy() {
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getString(R.string.to_key))) {
            String keyVal = sharedPreferences.getString(key, "");
            if (keyVal.isEmpty()) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(key, "0");
                editor.apply();
            }
        }
        Intent intent = new Intent(getActivity(), SchedulingService.class);
        getActivity().startService(intent);
        if (sharedPreferences.getBoolean(getString(R.string.always_key), Boolean.parseBoolean(getString(R.string.always_default)))) {
            eventNotificationManager.show(getActivity());
        }
    }

    void checkAndRequestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && getActivity().checkSelfPermission(Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_CALENDAR}, CALENDAR_READ_REQUEST_CODE);
        } else {
            createMultiSelectCalendars();
        }
    }

    @SuppressWarnings("MissingPermission")
    private void createMultiSelectCalendars() {
        ContentResolver resolver = getActivity().getContentResolver();
        Uri calendarUri = CalendarContract.Calendars.CONTENT_URI;
        String[] projection = new String[]{CalendarContract.Calendars._ID, CalendarContract.Calendars.CALENDAR_DISPLAY_NAME};
        Cursor cursor = resolver.query(calendarUri, projection, null, null, null);
        final ArrayList<String> calendarNames = new ArrayList<>();
        final ArrayList<String> calendarIds = new ArrayList<>();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                calendarNames.add(cursor.getString(1));
                calendarIds.add(Integer.valueOf(cursor.getInt(0)).toString());
            }
            cursor.close();
        }
        ListPreference selectedCalendarPref = (ListPreference) findPreference(getString(R.string.selected_calendars_key));
        selectedCalendarPref.setEntries(calendarNames.toArray(new CharSequence[calendarNames.size()]));
        selectedCalendarPref.setEntryValues(calendarIds.toArray(new CharSequence[calendarNames.size()]));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == CALENDAR_READ_REQUEST_CODE) {
            if (grantResults.length <= 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getActivity(), R.string.read_permission_error, Toast.LENGTH_SHORT).show();
                getActivity().finish();
            } else {
                createMultiSelectCalendars();
            }
        } else {
            Log.wtf(TAG, "Weird request code!");
        }
    }
}
