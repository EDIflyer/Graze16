package com.grazeten;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.grazeten.activities.AbstractNewsRobListActivity;
import com.grazeten.activities.ArticleListActivity;
import com.grazeten.activities.FeedListActivity;
import com.grazeten.activities.SettingsActivity;
import com.grazeten.activities.UIHelper;

public class DashboardListActivity extends AbstractNewsRobListActivity
{

  static final String      TAG                               = DashboardListActivity.class.getSimpleName();

  private static final int DIALOG_MARK_ALL_AS_READ     = 200;
  private static final int DIALOG_SHOW_LICENSE               = 201;
  private static final int DIALOG_SHOW_REINSTALL_NEWSROB     = 202;

  public static Dialog createShowReinstallDialog(final EntryManager entryManager, final Activity enclosingActivity)
  {
    DialogInterface.OnClickListener negativeListener = new DialogInterface.OnClickListener()
    {
      public void onClick(DialogInterface dialog, int which)
      {
        entryManager.maintainLastTimeProposedReinstall();
        if (which == DialogInterface.BUTTON1)
        {

          final Intent viewIntent = new Intent(Intent.ACTION_VIEW);
          viewIntent.setData(Uri.parse("market://details?id=" + EntryManager.LEGACY_PACKAGE_NAME));
          enclosingActivity.startActivity(viewIntent);
        }

      }
    };

    return new AlertDialog.Builder(enclosingActivity).setIcon(android.R.drawable.ic_dialog_info)
        .setMessage(enclosingActivity.getResources().getText(R.string.newsrob_three_to_six_text)).setTitle(R.string.newsrob_three_to_six)
        .setPositiveButton("Install", negativeListener).setNegativeButton("Later", negativeListener).create();
  }



  SimpleCursorAdapter sca;

  private DBQuery     dbQuery;

  int                 counter;

  @Override
  protected Cursor createCursorFromQuery(DBQuery dbq)
  {
    return getEntryManager().getDashboardContentCursor(dbq);
  }

  @Override
  protected DBQuery getDbQuery()
  {
    return dbQuery;
  }

  @Override
  public String getDefaultControlPanelTitle()
  {
    String pro = getEntryManager().isProVersion() ? " Pro" : "";
    return getResources().getString(R.string.app_name) + pro;
  }

  @Override
  public String getDefaultStatusBarTitle()
  {
    String appName = getResources().getString(R.string.app_name);
    return String.format("%s %s %s", appName, getEntryManager().getMyVersionName(), getLastSyncTimeAsString());
  }

  private String getLastSyncTimeAsString()
  {
    StringBuilder lastSynced = new StringBuilder();

    boolean lastSyncComplete = getSharedPreferences().getBoolean(EntryManager.SETTINGS_LAST_SYNC_COMPLETE, false);
    long lastSyncTime = getEntryManager().getLastSyncTime();

    if (lastSyncTime > 0)
    {
      lastSynced.append("\nlast sync - ");
      lastSynced.append(getTimeDistance(lastSyncTime));
      lastSynced.append(lastSyncComplete ? "" : " - incomplete");
    }
    return lastSynced.toString();
  }

  private String getTimeDistance(long t)
  {

    float diff = (System.currentTimeMillis() - t) / 1000 / 60;
    if (diff < 1)
    {
      return "less than a minute ago";
    }
    else if (diff < 5)
    {
      return "a couple of minutes ago";
    }
    else if (diff < 50)
    {
      return "ca. " + ((int) diff) + " minutes ago";
    }
    else
    {
      diff /= 60;
      if (diff < 1.2)
      {
        return "ca. an hour ago";
      }
      else if (diff < 2)
      {
        return "less than two hours ago";
      }
      else if (diff < 20)
      {
        return "ca. " + ((int) diff) + " hours ago";
      }
      else
      {
        diff /= 24;
        if (diff < 1.2)
        {
          return "a day ago";
        }
        else if (diff < 4)
        {
          return ((int) diff) + " days ago";
        }
        else if (diff < 6)
        {
          return "too long ago";
        }
        else
        {
          return "when dinosaurs were walking the earth";
        }
      }
    }
  }

  @Override
  protected CharSequence getToastMessage()
  {
    return getResources().getString(R.string.app_name) + " " + getEntryManager().getMyVersionName() + getLastSyncTimeAsString();
  }

  private void initialize(Intent i)
  {
    dbQuery = UIHelper.createDBQueryFromIntentExtras(getEntryManager(), i);

    Cursor c = getEntryManager().getDashboardContentCursor(dbQuery);
    startManagingCursor(c);
    
    android.util.Log.d(TAG, "Dashboard cursor count: " + (c != null ? c.getCount() : "null"));
    if (c != null && c.getCount() > 0) {
      android.util.Log.d(TAG, "Cursor has " + c.getCount() + " items");
      
      // Force hide empty view when we have dashboard items
      View emptyView = findViewById(android.R.id.empty);
      if (emptyView != null) {
        android.util.Log.d(TAG, "Dashboard: Hiding empty view - we have " + c.getCount() + " items");
        emptyView.setVisibility(View.GONE);
      }
      getListView().setVisibility(View.VISIBLE);
    }

    final int readIndicator = getEntryManager().isLightColorSchemeSelected() ? R.drawable.read_indicator : R.drawable.read_indicator_dark;
    sca = new SimpleCursorAdapter(this, R.layout.dashboard_list_row, c, new String[] { "_id", "frequency", "sum_unread_freq" }, new int[] {
        R.id.item_title, R.id.item_count, R.id.unread });
    sca.setViewBinder(new SimpleCursorAdapter.ViewBinder()
    {

      public boolean setViewValue(View view, final Cursor cursor, int columnIndex)
      {
        if (columnIndex == 2)
        {
          TextView tv = (TextView) view;
          boolean containsUnread = cursor.getInt(2) > 0;
          tv.setBackgroundResource(containsUnread ? readIndicator : R.drawable.read_indicator_invisible);
          return true;
        }
        else if (columnIndex == 1)
        {
          final View itemCount = view;

          View.OnClickListener itemCountClickListener = new View.OnClickListener()
          {

            @Override
            public void onClick(View v)
            {
              final String stringValueOfTag = v.getTag(R.id.view_position).toString();
              final int position = Integer.valueOf(stringValueOfTag);
              onListItemClick(getListView(), v, position, -99l);
              final Drawable d = getResources().getDrawable(
                  entryManager.isLightColorSchemeSelected() ? R.drawable.count_list_item : R.drawable.count_list_item_dark);
              LayoutParams lp = v.getLayoutParams();
              lp.height = v.getHeight();
              v.setLayoutParams(lp);
              v.setBackgroundDrawable(d);

            }
          };

          view.setTag(R.id.view_position, cursor.getPosition());
          itemCount.setOnClickListener(itemCountClickListener);

        }

        return false;
      }
    });

    setListAdapter(sca);

    // Ensure ListView is clickable
    getListView().setClickable(true);
    getListView().setOnItemClickListener((parent, view, position, id) -> {
      android.util.Log.d(TAG, "ListView onItemClick triggered: position=" + position + ", id=" + id);
      onListItemClick(getListView(), view, position, id);
    });
    
    // Force hide empty view if we have data
    if (c != null && c.getCount() > 0) {
      android.util.Log.d(TAG, "Hiding empty view - we have " + c.getCount() + " items");
      View emptyView = findViewById(android.R.id.empty);
      if (emptyView != null) {
        emptyView.setVisibility(View.GONE);
      }
      // Ensure ListView is visible
      getListView().setVisibility(View.VISIBLE);
    } else {
      android.util.Log.d(TAG, "No data in cursor, keeping empty view visible");
    }

    if (!getEntryManager().isLicenseAccepted())
    {
      showDialog(DIALOG_SHOW_LICENSE);
    }
    else
    {



      // Skip this activity when now labels are displayed
      if ((sca.getCount() == 1) && i.getBooleanExtra("skip", true))
      {

        Intent intent = new Intent(this, FeedListActivity.class);
        UIHelper.addExtrasFromDBQuery(intent, dbQuery);
        startActivity(intent);
        if (!isTaskRoot())
        {
          finish();
        }
      }
    }

  }

  public void modelUpdated(String atomId)
  {
  }

  @Override
  protected boolean onContextItemSelected(MenuItem item, int selectedPosition)
  {
    String label = null;
    int ord = -99;
    try
    {
      Cursor c = (Cursor) sca.getItem(selectedPosition);
      label = c.getString(0);
      ord = c.getInt(3);
    }
    catch (CursorIndexOutOfBoundsException cioobe)
    {
      // label stays null
    }
    if (label == null)
    {
      return false;
    }

    if (item.getItemId() == MENU_ITEM_MARK_ALL_READ_ID)
    {
      boolean showOnlyNotes = (ord == -7) && "notes".equals(label);
      Long feedId = null;
      if (showOnlyNotes)
      {
        feedId = getEntryManager().findNotesFeedId();
        label = null;
      }
      DBQuery dbq = getDbQuery();
      instantiateMarkAllReadDialog(label, feedId, dbq.getStartDate(), dbq.getDateLimit(), dbq.isSortOrderAscending(), dbq.getLimit());
    }

    // getEntryManager().requestMarkAllAsRead(label, null, 0, handler);
    return true;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    try {
      android.util.Log.d(TAG, "DashboardListActivity.onCreate() - starting");
      super.onCreate(savedInstanceState);
      android.util.Log.d(TAG, "DashboardListActivity.onCreate() - super.onCreate completed");
      
      setContentView(R.layout.dashboard_list);
      android.util.Log.d(TAG, "DashboardListActivity.onCreate() - setContentView completed");

      initialize(getIntent());
      android.util.Log.d(TAG, "DashboardListActivity.onCreate() - initialize completed");

    } catch (Exception e) {
      android.util.Log.e(TAG, "Fatal error in DashboardListActivity.onCreate()", e);
      throw e;
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    // Refresh the list when returning to this activity (e.g., after sync)
    initialize(getIntent());
  }

  @Override
  protected void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo, int selectedPosition)
  {

    Cursor c = (Cursor) sca.getItem(selectedPosition);
    String label = c.getString(0);
    int ord = c.getInt(3);

    menu.setHeaderTitle(label);
    menu.add(0, MENU_ITEM_MARK_ALL_READ_ID, 0, R.string.menu_item_mark_all_read);

    DBQuery dbq = new DBQuery(getDbQuery());
    dbq.setShouldHideReadItemsWithoutUpdatingThePreference(true);

    boolean showOnlyNotes = (ord == -7) && "notes".equals(label);
    if (showOnlyNotes)
    {
      dbq.setFilterFeedId(getEntryManager().findNotesFeedId());
    }
    else
    {
      dbq.setFilterLabel(label);
    }

    if (!getEntryManager().isMarkAllReadPossible(dbq))
    {
      menu.getItem(0).setEnabled(false);
    }

  }

  @Override
  protected Dialog onCreateDialog(int id)
  {
    if (DIALOG_SHOW_LICENSE == id)
    {
      DialogInterface.OnClickListener negativeListener = new DialogInterface.OnClickListener()
      {
        public void onClick(DialogInterface dialog, int which)
        {
          if (which == DialogInterface.BUTTON1)
          {
            getEntryManager().acceptLicense();
            DashboardListActivity.this.startActivity(new Intent().setClass(DashboardListActivity.this, SettingsActivity.class));
          }
          else
          {
            finish();
          }
        }
      };

      return new AlertDialog.Builder(this).setIcon(android.R.drawable.ic_dialog_info).setMessage(R.string.license_text)
          .setTitle(R.string.license).setPositiveButton(android.R.string.ok, negativeListener)
          .setNegativeButton(android.R.string.cancel, negativeListener).create();

    }
    else if (DIALOG_SHOW_REINSTALL_NEWSROB == id)
    {
      return createShowReinstallDialog(getEntryManager(), this);
    }

    return super.onCreateDialog(id);
  }

  /**
   * id is not used at the moment, except when it contains -99 it will directly go to the article list
   */
  protected void onListItemClick(ListView l, View v, int position, long id)
  {
    android.util.Log.d(TAG, "onListItemClick: position=" + position + ", id=" + id);

    final boolean goToArticleList = id == -99l;

    Cursor c = (Cursor) getListView().getAdapter().getItem(position);

    String labelName = c.getString(0);
    int frequency = c.getInt(1);

    int ord = c.getInt(3);
    
    android.util.Log.d(TAG, "Clicked item: labelName=" + labelName + ", frequency=" + frequency + ", ord=" + ord);

    boolean showOnlyNotes = (ord == -7) && "notes".equals(labelName);
    Long feedId = null;
    if (showOnlyNotes)
    {
      feedId = getEntryManager().findNotesFeedId();
      labelName = null;
    }

    DBQuery dbq = new DBQuery(getDbQuery());
    dbq.setFilterLabel(labelName);
    dbq.setFilterFeedId(feedId);

    if (showOnlyNotes || goToArticleList)
    {

      Intent intent = new Intent(this, ArticleListActivity.class);
      UIHelper.addExtrasFromDBQuery(intent, dbq);
      startActivity(intent);

    }
    else
    {

      if (frequency == 1)
      {
        startShowEntryActivityForPosition(0, dbq);
      }
      else
      {
        // Intent intent = new Intent(this, EntryListActivity.class);
        Intent intent = new Intent(this, FeedListActivity.class);
        UIHelper.addExtrasFromDBQuery(intent, dbq);
        startActivity(intent);
      }
    }
  }

  @Override
  protected void onNewIntent(Intent intent)
  {
    super.onNewIntent(intent);
    initialize(intent);
  }

  @Override
  protected void onPostCreate(Bundle savedInstanceState)
  {
    super.onPostCreate(savedInstanceState);
    hideSortOrderToggle();

  }

}
