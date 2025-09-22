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
        
        @Override
        public void onResume() {
            super.onResume();
            // Ensure the action bar title is set correctly when returning to main settings
            SettingsActivity activity = (SettingsActivity) getActivity();
            if (activity != null && activity.getSupportActionBar() != null) {
                activity.getSupportActionBar().setTitle(R.string.settings_title);
            }
        }
        
        private void setupCategoryClickListeners() {
            Preference syncPref = findPreference("sync_category");
            if (syncPref != null) {
                syncPref.setOnPreferenceClickListener(preference -> {
                    navigateToFragment(new SyncSettingsFragment(), "Synchronization");
                    return true;
                });
            }
            
            Preference cachePref = findPreference("cache_category");
            if (cachePref != null) {
                cachePref.setOnPreferenceClickListener(preference -> {
                    navigateToFragment(new CacheSettingsFragment(), "Local Cache");
                    return true;
                });
            }
            
            Preference uiPref = findPreference("ui_category");
            if (uiPref != null) {
                uiPref.setOnPreferenceClickListener(preference -> {
                    navigateToFragment(new UISettingsFragment(), "User Interface");
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
            
            Preference experimentalPref = findPreference("experimental_category");
            if (experimentalPref != null) {
                experimentalPref.setOnPreferenceClickListener(preference -> {
                    navigateToFragment(new ExperimentalSettingsFragment(), "Work in Progress (Exper.)");
                    return true;
                });
            }
            
            Preference aboutPref = findPreference("about_category");
            if (aboutPref != null) {
                aboutPref.setOnPreferenceClickListener(preference -> {
                    navigateToFragment(new AboutSettingsFragment(), "About");
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
                return "Version " + pInfo.versionName + " (" + com.graze16.BuildConfig.GIT_COMMIT_HASH + ")";
            } catch (PackageManager.NameNotFoundException e) {
                return "Version information unavailable";
            }
        }
        
        private void showLicenseDialog() {
            String licenseText = "MIT License\n\n" +
                "Copyright (c) 2020 M. Kamp, T. Tabbal, nayfield, ediflyer et al\n\n" +
                "Permission is hereby granted, free of charge, to any person obtaining a copy " +
                "of this software and associated documentation files (the \"Software\"), to deal " +
                "in the Software without restriction, including without limitation the rights " +
                "to use, copy, modify, merge, publish, distribute, sublicense, and/or sell " +
                "copies of the Software, and to permit persons to whom the Software is " +
                "furnished to do so, subject to the following conditions:\n\n" +
                "The above copyright notice and this permission notice shall be included in all " +
                "copies or substantial portions of the Software.\n\n" +
                "THE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR " +
                "IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, " +
                "FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE " +
                "AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER " +
                "LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, " +
                "OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE " +
                "SOFTWARE.";
            
            new AlertDialog.Builder(getActivity())
                .setTitle("License")
                .setMessage(licenseText)
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

    // Sync Settings Fragment
    public static class SyncSettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.sync_settings, rootKey);
            updateListPreferenceSummaries();
        }
        
        private void updateListPreferenceSummaries() {
            updateListPreferenceSummary("settings_service_provider");
            updateListPreferenceSummary("settings_sync_type");
            updateListPreferenceSummary("settings_automatic_refreshing_enabled2");
            updateListPreferenceSummary("settings_automatic_refresh_interval");
            updateListPreferenceSummary("storage_asset_download");
            updateListPreferenceSummary("settings_global_download_pref");
        }
        
        private void updateListPreferenceSummary(String key) {
            androidx.preference.ListPreference pref = (androidx.preference.ListPreference) findPreference(key);
            if (pref != null) {
                CharSequence entry = pref.getEntry();
                if (entry != null) {
                    pref.setSummary(entry);
                }
                pref.setOnPreferenceChangeListener((preference, newValue) -> {
                    androidx.preference.ListPreference listPref = (androidx.preference.ListPreference) preference;
                    CharSequence[] entries = listPref.getEntries();
                    CharSequence[] entryValues = listPref.getEntryValues();
                    
                    if (entries != null && entryValues != null) {
                        int index = listPref.findIndexOfValue(newValue.toString());
                        if (index >= 0 && index < entries.length) {
                            listPref.setSummary(entries[index]);
                        }
                    }
                    return true;
                });
            }
        }
    }

    // Cache Settings Fragment
    public static class CacheSettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.cache_settings, rootKey);
            updateListPreferenceSummaries();
        }
        
        private void updateListPreferenceSummaries() {
            updateListPreferenceSummary("settings_storage_provider");
            updateListPreferenceSummary("settings_entry_manager_entries_capacity");
            updateListPreferenceSummary("settings_keep_starred");
            updateListPreferenceSummary("settings_keep_shared");
            updateListPreferenceSummary("settings_keep_notes");
        }
        
        private void updateListPreferenceSummary(String key) {
            androidx.preference.ListPreference pref = (androidx.preference.ListPreference) findPreference(key);
            if (pref != null) {
                CharSequence entry = pref.getEntry();
                if (entry != null) {
                    pref.setSummary(entry);
                }
                pref.setOnPreferenceChangeListener((preference, newValue) -> {
                    androidx.preference.ListPreference listPref = (androidx.preference.ListPreference) preference;
                    CharSequence[] entries = listPref.getEntries();
                    CharSequence[] entryValues = listPref.getEntryValues();
                    
                    if (entries != null && entryValues != null) {
                        int index = listPref.findIndexOfValue(newValue.toString());
                        if (index >= 0 && index < entries.length) {
                            listPref.setSummary(entries[index]);
                        }
                    }
                    return true;
                });
            }
        }
    }

    // UI Settings Fragment
    public static class UISettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.ui_settings, rootKey);
        }
    }

    // Experimental Settings Fragment
    public static class ExperimentalSettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.experimental_settings, rootKey);
        }
    }
}
