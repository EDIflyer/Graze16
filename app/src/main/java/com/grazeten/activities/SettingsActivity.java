package com.graze16.activities;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;

import com.graze16.DashboardListActivity;
import com.graze16.EntryManager;
import com.graze16.IEntryModelUpdateListener;
import com.graze16.R;
import com.graze16.jobs.ModelUpdateResult;
import com.graze16.preference.ListPreference;
import com.graze16.util.SDKVersionUtil;

public class SettingsActivity extends PreferenceActivity implements IEntryModelUpdateListener
{

  private Handler handler = new Handler();

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    final EntryManager em = EntryManager.getInstance(this);

    addPreferencesFromResource(R.xml.settings);

    getPreferenceScreen().setOnPreferenceChangeListener(em);

    if (SDKVersionUtil.getVersion() < 8)
      disableSetting(em, EntryManager.SETTINGS_PLUGINS, "Froyo+");

    if (SDKVersionUtil.getVersion() < 11)
    {
      disableSetting(em, EntryManager.SETTINGS_HW_ACCEL_LISTS_ENABLED, "HC+ only");
      disableSetting(em, EntryManager.SETTINGS_HW_ACCEL_ADV_ENABLED, "HC+ only");
    }

    if (em.shouldHWZoomControlsBeDisabled())
    {
      Preference pref = getPreferenceScreen().findPreference(EntryManager.SETTINGS_HOVERING_ZOOM_CONTROLS_ENABLED);
      if (pref != null)
      {
        pref.setEnabled(false);
        if (pref.getSummary() != null)
          pref.setSummary("Disabled until HTC fixes a bug that hurts this function. Sorry.");
      }
    }

    if (em.shouldSyncInProgressNotificationBeDisabled())
    {
      Preference pref = getPreferenceScreen().findPreference(EntryManager.SETTINGS_SYNC_IN_PROGRESS_NOTIFICATION);
      if (pref != null)
      {
        pref.setEnabled(false);
        if (pref.getSummary() != null)
          pref.setSummary("Disabled until HTC/Dell fixes a bug that hurts this function. Sorry.");
      }
    }

    if (em.shouldActionBarLocationOnlyAllowGone())
    {
      ListPreference pref = (ListPreference) getPreferenceScreen().findPreference(EntryManager.SETTINGS_UI_ACTION_BAR_LOCATION);
      if (pref != null)
      {
        pref.setEnabled(false);
        CharSequence[] seq = pref.getEntries();
        CharSequence[] newSeq = new CharSequence[] { seq[2] };
        pref.setEntries(newSeq);

        getPreferenceScreen().removePreference(pref);
      }
    }

    // Add click listeners for About section preferences
    Preference aboutLicensePref = findPreference("about_license_preference");
    if (aboutLicensePref != null) {
      aboutLicensePref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
          showLicenseDialog();
          return true;
        }
      });
    }
    
    Preference aboutGithubPref = findPreference("about_github_preference");
    if (aboutGithubPref != null) {
      aboutGithubPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
          openGithubRepository();
          return true;
        }
      });
    }

  }

  private void disableSetting(EntryManager em, String keyOfPref)
  {
    disableSetting(em, keyOfPref, "PRO");
  }

  private void disableSetting(EntryManager em, String keyOfPref, String reason)
  {
    Preference pref = getPreferenceScreen().findPreference(keyOfPref);
    if (pref != null)
    {
      pref.setEnabled(false);
      if (pref.getTitle() != null)
        pref.setTitle(pref.getTitle() + " (" + reason + ")");
    }
  }

  @Override
  protected void onResume()
  {
    super.onResume();
    EntryManager em = EntryManager.getInstance(this);
    manageModelRelatedSettingsState(em.isModelCurrentlyUpdated());
    em.addListener(this);
  }

  @Override
  protected void onPause()
  {
    EntryManager.getInstance(this).removeListener(this);
    super.onPause();
  }

  public void modelUpdateFinished(ModelUpdateResult result)
  {
    manageModelRelatedSettingsState(false);
  }

  public void modelUpdateStarted(boolean fastSyncOnly)
  {
    manageModelRelatedSettingsState(true);
  }

  public void modelUpdated()
  {
  }

  private void manageModelRelatedSettingsState(final boolean newState)
  {
    handler.post(new Runnable()
    {
      public void run()
      {
        Preference storageProviderPref = getPreferenceManager().findPreference(EntryManager.SETTINGS_STORAGE_PROVIDER_KEY);
        storageProviderPref.setEnabled(!newState);
      }
    });
  }

  public void statusUpdated()
  {
  }

  public void modelUpdated(String atomId)
  {

  }

  private void showLicenseDialog()
  {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle(R.string.license)
           .setMessage(R.string.license_text)
           .setIcon(android.R.drawable.ic_dialog_info)
           .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
             public void onClick(DialogInterface dialog, int which) {
               dialog.dismiss();
             }
           });
    builder.create().show();
  }

  private void openGithubRepository()
  {
    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/EDIflyer/Graze16"));
    startActivity(browserIntent);
  }
}
