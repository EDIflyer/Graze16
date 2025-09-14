package com.graze16.activities;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.WindowInsetsController;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.graze16.EntryManager;
import com.graze16.IEntryModelUpdateListener;
import com.graze16.R;
import com.graze16.jobs.ModelUpdateResult;

public class SettingsActivity extends AppCompatActivity implements IEntryModelUpdateListener {
    
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        
        // Set up action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.settings_title);
        }

        // Load the main settings fragment
        if (findViewById(R.id.settings_container) != null) {
            if (savedInstanceState == null) {
                getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings_container, new MainSettingsFragment())
                    .commit();
            }
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        EntryManager em = EntryManager.getInstance(this);
        em.addListener(this);
    }

    @Override
    protected void onPause() {
        EntryManager.getInstance(this).removeListener(this);
        super.onPause();
    }

    public void modelUpdateFinished(ModelUpdateResult result) {
        // TODO: Update fragments if needed
    }

    public void modelUpdateStarted(boolean fastSyncOnly) {
        // TODO: Update fragments if needed
    }

    public void modelUpdated() {
    }

    public void statusUpdated() {
    }

    public void modelUpdated(String atomId) {
    }

    // Main Settings Fragment
    public static class MainSettingsFragment extends PreferenceFragmentCompat {
        
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.main_settings, rootKey);
            setupCategoryClickListeners();
        }
        
        private void setupCategoryClickListeners() {
            Preference aboutPref = findPreference("about_category");
            if (aboutPref != null) {
                aboutPref.setOnPreferenceClickListener(preference -> {
                    navigateToFragment(new AboutSettingsFragment(), "About");
                    return true;
                });
            }
            
            Preference notificationsPref = findPreference("notifications_category");
            if (notificationsPref != null) {
                notificationsPref.setOnPreferenceClickListener(preference -> {
                    navigateToFragment(new NotificationsSettingsFragment(), "Notifications");
                    return true;
                });
            }
        }
        
        private void navigateToFragment(PreferenceFragmentCompat fragment, String title) {
            SettingsActivity activity = (SettingsActivity) getActivity();
            if (activity != null) {
                if (activity.getSupportActionBar() != null) {
                    activity.getSupportActionBar().setTitle(title);
                }
                activity.getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings_container, fragment)
                    .addToBackStack(null)
                    .commit();
            }
        }
    }

    // About Settings Fragment
    public static class AboutSettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.about_settings, rootKey);
            
            Preference versionPref = findPreference("about_version_preference");
            if (versionPref != null) {
                String versionInfo = getVersionInfo();
                versionPref.setSummary(versionInfo);
            }
            
            Preference licensePref = findPreference("about_license_preference");
            if (licensePref != null) {
                licensePref.setOnPreferenceClickListener(preference -> {
                    showLicenseDialog();
                    return true;
                });
            }
            
            Preference githubPref = findPreference("about_github_preference");
            if (githubPref != null) {
                githubPref.setOnPreferenceClickListener(preference -> {
                    openGithubRepository();
                    return true;
                });
            }
        }
        
        private String getVersionInfo() {
            try {
                PackageManager pm = getActivity().getPackageManager();
                PackageInfo pInfo = pm.getPackageInfo(getActivity().getPackageName(), 0);
                return "Version " + pInfo.versionName + " (" + pInfo.versionCode + ")";
            } catch (PackageManager.NameNotFoundException e) {
                return "Version information unavailable";
            }
        }
        
        private void showLicenseDialog() {
            new AlertDialog.Builder(getActivity())
                .setTitle("License")
                .setMessage("This app is licensed under the Apache License 2.0")
                .setPositiveButton("OK", null)
                .show();
        }
        
        private void openGithubRepository() {
            Intent intent = new Intent(Intent.ACTION_VIEW, 
                Uri.parse("https://github.com/EDIflyer/Graze16"));
            startActivity(intent);
        }
    }

    // Notifications Settings Fragment
    public static class NotificationsSettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.notifications_settings, rootKey);
        }
    }
}
