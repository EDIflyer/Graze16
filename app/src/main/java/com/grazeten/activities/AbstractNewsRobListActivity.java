package com.graze16.activities;

import android.app.AlertDialog;
import android.app.Dialog;
import androidx.appcompat.app.AppCompatActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import com.graze16.DashboardListActivity;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.animation.ObjectAnimator;
import android.widget.AbsoluteLayout;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;
import android.widget.ListView;

import com.graze16.BackendProvider;
import com.graze16.DBQuery;
import com.graze16.DashboardListActivity;
import com.graze16.EntryManager;
import com.graze16.IEntryModelUpdateListener;
import com.graze16.NewsRob;
import com.graze16.PL;
import com.graze16.R;
import com.graze16.SyncInterfaceFactory;
import com.graze16.jobs.ClearModelSucceeded;
import com.graze16.jobs.EntryUpdateSucceeded;
import com.graze16.jobs.Job;
import com.graze16.jobs.ModelUpdateResult;
import com.graze16.jobs.SynchronizeModelSucceeded;
import com.graze16.storage.SdCardStorageAdapter;
import com.graze16.util.FlurryUtil;
import com.graze16.util.GoogleAdsUtil;
import com.graze16.util.SDK11Helper;
import com.graze16.util.Timing;
import com.graze16.util.U;

public abstract class AbstractNewsRobListActivity extends AppCompatActivity
    implements IEntryModelUpdateListener, View.OnLongClickListener
{
  private static final String TAG                         = AbstractNewsRobListActivity.class.getSimpleName();

  protected ListView listView;

  protected ListView getListView()
  {
    if (listView == null)
    {
      listView = findViewById(android.R.id.list);
    }
    return listView;
  }

  protected void setListAdapter(CursorAdapter adapter)
  {
    getListView().setAdapter(adapter);
  }

  private static final int    MENU_ITEM_REFRESH_ID        = 1;
  private static final int    MENU_ITEM_SETTINGS_ID       = 2;
  private static final int    MENU_ITEM_CLEAR_CACHE_ID    = 3;

  private static final int    MENU_ITEM_LOGOUT_ID         = 4;
  private static final int    MENU_ITEM_HIDE_ID           = 5;

  private static final int    MENU_ITEM_CANCEL_ID         = 6;

  protected static final int  MENU_ITEM_MARK_ALL_READ_ID  = 7;
  protected static final int  MENU_ITEM_MANAGE_FEED_ID    = 8;
  protected static final int  MENU_ITEM_SUBSCRIBE_FEED_ID = 15;

  protected static final int  MENU_ITEM_SEARCH_ID         = 9;

  protected static final int  MENU_ITEM_FAQ_ID            = 10;
  protected static final int  MENU_ITEM_TOGGLE_THEME_ID   = 11;
  protected static final int  MENU_ITEM_SORT_ID           = 17;

  protected static final int  MENU_ITEM_SHOW_FILTER_ID    = 18;

  private static final int    DIALOG_SD_CARD_ADVISABLE_ID = 0;
  private static final int    DIALOG_CLEAR_CACHE          = 1;
  private static final int    DIALOG_SHOW_FILTER_INFO_ID  = 2;

  Handler                     handler                     = new Handler();

  protected EntryManager      entryManager;

  private Runnable            refreshUIRunnable;

  private int                 positionOfSelectedItemOnLongPress;

  private ImageButton refreshButton;
  private ImageButton markAllReadButton;
  private ImageButton showHideButton;
  private ImageButton toggleOrderButton;
  private ImageButton customSyncButton;

  private int                 currentTheme;
  private String              currentActionBarLocation;
  
  private Menu                currentMenu;
  private ObjectAnimator      syncIconAnimator;

  private View                progressIndicator;
  private ProgressBar         progressBar;
  private String             lastSyncResult = "";
  private TextView            progressDescription;
  private LinearLayout progressContainer;

  private GoogleAdsUtil       googleAdsUtil;

  protected void activateProgressIndicator()
  {
    Log.d("SYNC_DEBUG", "activateProgressIndicator called");
    if (shouldActionBarBeHidden())
    {
      PL.log("Action bar is hidden, returning", this);
      return;
    }

    // Based on user requirements: ONLY animate the action bar refresh icon,
    // do NOT show center-screen progress indicators
    
    // Hide all center-screen progress indicators
    if (progressIndicator != null)
    {
      progressIndicator.setVisibility(View.GONE);
    }
    
    View dataUpdateProgress = findViewById(R.id.data_update_progress);
    if (dataUpdateProgress != null)
    {
      dataUpdateProgress.setVisibility(View.GONE);
    }

    // Hide progress container
    if (progressContainer != null)
    {
      progressContainer.setVisibility(View.INVISIBLE);
    }

    // ONLY animate the sync menu icon in the action bar
    animateSyncMenuItem(true);

    // Update progress information for when progress container becomes visible
    String status = "Background Operation In Progress";
    Job runningJob = entryManager.getCurrentRunningJob();

    if (runningJob != null)
    {
      status = runningJob.getJobDescription();
      if (runningJob.isProgressMeassurable())
      {
        int[] progress = runningJob.getProgress();
        int currentArticle = progress[0];
        int allArticles = progress[1];
        if (progressBar != null) {
          progressBar.setMax(allArticles);
          progressBar.setProgress(currentArticle);
          progressBar.setIndeterminate(false);
        }
        status = runningJob.getJobDescription() + " (" + currentArticle + "/" + allArticles + ")";
      }
      else
      {
        if (progressBar != null) {
          progressBar.setMax(0);
          progressBar.setProgress(0);
          progressBar.setIndeterminate(true);
        }
      }
    }
    else
    {
      if (progressBar != null) {
        progressBar.setMax(0);
        progressBar.setProgress(0);
        progressBar.setIndeterminate(true);
      }
    }
    
    if (progressDescription != null) {
      progressDescription.setText(status);
    }

    if (runningJob != null)
    {
      status = runningJob.getJobDescription();
      if (runningJob.isProgressMeassurable())
      {
        int[] progress = runningJob.getProgress();
        int currentArticle = progress[0];
        int allArticles = progress[1];
        // Don't show center-screen progress bar - only action bar animation
        // progressBar.setMax(allArticles);
        // progressBar.setProgress(currentArticle);
        // progressBar.setIndeterminate(false);
        // progressBar.setVisibility(View.VISIBLE);
        status = runningJob.getJobDescription() + " (" + currentArticle + "/" + allArticles + ")";
      }
      else
      {
        // Don't configure center-screen progress bar
        // progressBar.setMax(0);
        // progressBar.setProgress(0);
        // progressBar.setIndeterminate(true);
      }

    }
    else
    {
      // Don't configure center-screen progress bar
      // progressBar.setMax(0);
      // progressBar.setProgress(0);
      // progressBar.setIndeterminate(true);
    }
    
    // Don't hide progress bar and description - they're needed for the progress container
    // The progressBar and progressDescription are the same elements used in the progress container
    
    // progressDescription.setText(status); -- This is now set above

    /*
     * 
     * Job runningJob = getEntryManager().getCurrentRunningJob(); if (runningJob == null) { setTitle(getDefaultStatusBarTitle()); return; }
     * 
     * if (runningJob.isProgressMeassurable()) { int[] progress = runningJob.getProgress(); int currentArticle = progress[0]; int allArticles = progress[1];
     * setTitle(runningJob.getJobDescription() + " (" + currentArticle + "/" + allArticles + ")"); } else { setTitle(runningJob.getJobDescription()); }
     */
  }

  private void checkIfSDCardAccessible()
  {
    if (entryManager.getStorageAdapter() instanceof SdCardStorageAdapter)
    {
      if (isTaskRoot() && !entryManager.getStorageAdapter().canWrite())
      {
        showDialog(DIALOG_SD_CARD_ADVISABLE_ID);
      }
    }

  }

  abstract protected Cursor createCursorFromQuery(DBQuery dbq);

  protected void deactivateProgressIndicator()
  {
    PL.log("AbstractNewsRobList.deactivateProgressIndicator(" + progressIndicator + ")", this);
    
    if (!shouldActionBarBeHidden())
    {
      if (progressIndicator != null)
      {
        progressIndicator.setVisibility(View.GONE);
      }
      else
      {
        // Hide alternative progress indicators
        View dataUpdateProgress = findViewById(R.id.data_update_progress);
        if (dataUpdateProgress != null)
        {
          dataUpdateProgress.setVisibility(View.INVISIBLE);
        }
      }
    }

    // Stop animating the sync menu icon
    animateSyncMenuItem(false);

    // Don't automatically hide progress container - let user control it
    // Only hide when sync actually completes (handled in modelUpdateFinished)
    
    setTitle(getDefaultStatusBarTitle());
    // Don't call hideProgressBar() here - keep it visible until user dismisses or sync completes
  }

  abstract protected DBQuery getDbQuery();

  public abstract String getDefaultControlPanelTitle();

  public abstract String getDefaultStatusBarTitle();

  protected EntryManager getEntryManager()
  {
    if (entryManager == null)
    {
      entryManager = EntryManager.getInstance(this);
    }
    return entryManager;
  }

  protected SharedPreferences getSharedPreferences()
  {
    return getEntryManager().getSharedPreferences();
  }

  protected abstract CharSequence getToastMessage();

  void hideDataUpdateProgressMonitor()
  {
    if (true)
    {
      return;
    }
    runOnUiThread(new Runnable()
    {

      @Override
      public void run()
      {
        findViewById(R.id.data_update_progress).setVisibility(View.INVISIBLE);
      }
    });

  }

  protected void hideProgressBar()
  {
    if (shouldActionBarBeHidden())
    {
      return;
    }

    PL.log("AbstractNewsRobList.hideProgressBar(" + progressContainer + ")", this);
    if (progressContainer == null)
    {
      return;
    }
    PL.log("AbstractNewsRobList.hideProgressBar2(" + progressContainer + ") visibility=" + progressContainer.getVisibility(), this);
    if (progressContainer.getVisibility() == View.INVISIBLE)
    {
      return;
    }

    Animation outAnimation = AnimationUtils.loadAnimation(this, R.anim.push_up_out);
    progressContainer.startAnimation(outAnimation);
    progressContainer.setVisibility(View.INVISIBLE);

    PL.log("AbstractNewsRobList.hideProgressBar3(" + progressContainer + ") visibility=" + progressContainer.getVisibility(), this);
  }

  protected void hideSortOrderToggle()
  {
    if (shouldActionBarBeHidden())
    {
      return;
    }
    if (toggleOrderButton != null) {
      toggleOrderButton.setVisibility(View.GONE);
    }
  }

  protected void instantiateMarkAllReadDialog()
  {
    instantiateMarkAllReadDialog(getDbQuery());
  }

  protected void instantiateMarkAllReadDialog(DBQuery dbq)
  {
    instantiateMarkAllReadDialog(dbq.getFilterLabel(), dbq.getFilterFeedId(), dbq.getStartDate(), dbq.getDateLimit(),
        dbq.isSortOrderAscending(), dbq.getLimit());
  }

  protected void instantiateMarkAllReadDialog(final String filterLabel, final Long filterFeedId, final long startDate, final long dateLimit,
      boolean sortDateAscending, int limit)
  {

    final DBQuery dbq = new DBQuery(getEntryManager(), filterLabel, filterFeedId);
    dbq.setStartDate(startDate);
    dbq.setDateLimit(dateLimit);
    dbq.setSortOrderAscending(sortDateAscending);
    dbq.setLimit(limit);
    dbq.setShouldHideReadItemsWithoutUpdatingThePreference(true);

    final long noOfArticlesMarkedAsRead = getEntryManager().getMarkAllReadCount(dbq);
    final Runnable action = new Runnable()
    {
      public void run()
      {
        Toast.makeText(AbstractNewsRobListActivity.this,
            noOfArticlesMarkedAsRead + " article" + (noOfArticlesMarkedAsRead > 1 ? "s" : "") + " marked as read.", Toast.LENGTH_LONG).show();
        if (getEntryManager().shouldHideReadItems())
        {
          getListView().setSelection(0);
        }
        getEntryManager().requestMarkAllAsRead(dbq);
      }
    };

    if (noOfArticlesMarkedAsRead >= getEntryManager().getMarkAllReadConfirmationDialogThreshold()) {
      boolean pl = noOfArticlesMarkedAsRead > 1;
      String message = "Mark " + (pl ? "all " : "") + noOfArticlesMarkedAsRead + " article" + (pl ? "s" : "") + " read?";
      showConfirmationDialog(message, action);
    }
    else {
      action.run();
    }
  }

  public void modelUpdated()
  {
    final String className = getClass().getSimpleName();

    new Thread(new Runnable()
    {

      @Override
      public void run()
      {
        showDataUpdateProgressMonitor();
        try
        {
          Timing t = new Timing(className + "::modelUpdated", AbstractNewsRobListActivity.this);
          Cursor newCursor = createCursorFromQuery(getDbQuery());

          // force the cursor to be loaded,
          // so that this needn't be done on the UI thread
          newCursor.moveToFirst();
          newCursor(newCursor, (CursorAdapter) getListView().getAdapter());
          runOnUiThread(refreshUIRunnable);
          t.stop();
        }
        finally
        {
          hideDataUpdateProgressMonitor();
        }
      }

    }).start();

    runOnUiThread(refreshUIRunnable);
  }

  public void modelUpdateFinished(final ModelUpdateResult result)
  {
    Log.d("SYNC_DEBUG", "modelUpdateFinished called");
    handler.post(new Runnable()
    {

      public void run()
      {
        updateButtons();
        deactivateProgressIndicator();
        
        // Don't automatically hide progress bar when sync completes
        // User must manually dismiss it by clicking the sync button again
        // hideProgressBar();

        if (result instanceof SynchronizeModelSucceeded)
        {
          SynchronizeModelSucceeded succeeded = (SynchronizeModelSucceeded) result;

          if (succeeded.getNoOfEntriesUpdated() > 0)
          {
            lastSyncResult = "Sync complete - " + succeeded.getNoOfEntriesUpdated() + " articles updated";
            refreshUI();
            Toast.makeText(AbstractNewsRobListActivity.this, 
                lastSyncResult,
                Toast.LENGTH_SHORT).show();
            // Toast.makeText(AbstractNewsRobListActivity.this,
            // succeeded.getMessage(),
            // Toast.LENGTH_LONG).show(); // I18N
          }
          else
          {
            lastSyncResult = "Sync complete - no new articles";
            Toast.makeText(AbstractNewsRobListActivity.this, 
                lastSyncResult,
                Toast.LENGTH_SHORT).show();
          }
        }
        else if (result instanceof ClearModelSucceeded)
        {
          ClearModelSucceeded succeeded = (ClearModelSucceeded) result;
          // Toast.makeText(AbstractNewsRobListActivity.this,
          // succeeded.getMessage(),
          // Toast.LENGTH_LONG).show(); // I18N
          if (succeeded.noOfEntriesDeleted > 0)
          {
            refreshUI();
          }

        }
        else if (result instanceof EntryUpdateSucceeded)
        {
          refreshUI();
        }
        else
        {
          refreshUI();
          Toast.makeText(AbstractNewsRobListActivity.this, result.getMessage(), Toast.LENGTH_LONG).show(); // I18N
        }
      }
    });
  }

  public void modelUpdateStarted(boolean fastSyncOnly)
  {
    Log.d("SYNC_DEBUG", "modelUpdateStarted called with fastSyncOnly=" + fastSyncOnly);
    runOnUiThread(refreshUIRunnable);
    
    // Ensure progress container is updated when sync starts
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        updateProgressContainer();
      }
    });

    if (!fastSyncOnly && EntryManager.ACTION_BAR_TOP.equals(entryManager.getActionBarLocation())
        && (View.VISIBLE != progressContainer.getVisibility()) && !entryManager.hasProgressReportBeenOpened())
    {
      refreshButton.postDelayed(new Runnable()
      {
        @Override
        public void run()
        {
          Animation inAnimation = AnimationUtils.loadAnimation(AbstractNewsRobListActivity.this, R.anim.bouncing);

          final ImageView arrow = findViewById(R.id.bouncing_arrow);
          final View hint = findViewById(R.id.show_open_progress_hint);

          if (arrow != null)
          {

            arrow.setImageResource(R.drawable.gen_arrow_up); // Cupcake
            inAnimation.setAnimationListener(new Animation.AnimationListener()
            {
              @Override
              public void onAnimationEnd(Animation animation)
              {
                arrow.postDelayed(new Runnable()
                {
                  @Override
                  public void run()
                  {
                    arrow.setVisibility(View.INVISIBLE);
                    hint.setVisibility(View.INVISIBLE);
                  }
                }, 6000);

              }

              @Override
              public void onAnimationRepeat(Animation animation)
              {
              }

              @Override
              public void onAnimationStart(Animation animation)
              {

                int[] loc = new int[2];
                refreshButton.getLocationOnScreen(loc);

                int y = (loc[0] + (refreshButton.getWidth() / 2)) - (arrow.getWidth() / 2);

                AbsoluteLayout.LayoutParams lp = new AbsoluteLayout.LayoutParams(
                    AbsoluteLayout.LayoutParams.WRAP_CONTENT, AbsoluteLayout.LayoutParams.WRAP_CONTENT, y, 0);
                arrow.setLayoutParams(lp);
                // findViewById(R.id.list_parent).requestLayout();

                arrow.setVisibility(View.VISIBLE);
                hint.setVisibility(View.VISIBLE);

              }
            });

            arrow.startAnimation(inAnimation);
          }
        }
      }, 2500);
    }
  }

  /** This will run on the UI Thread */
  protected void newCursor(final Cursor newCursor, final CursorAdapter adapter)
  {
    runOnUiThread(new Runnable()
    {
      @Override
      public void run()
      {
        Timing t = new Timing(this.getClass().getSimpleName() + "::newCursor()", AbstractNewsRobListActivity.this);

        Timing t2 = new Timing("CursorAdapter::changeCursor", AbstractNewsRobListActivity.this);
        Cursor existingCursor = adapter.getCursor();
        adapter.changeCursor(newCursor);
        t2.stop();
        startManagingCursor(newCursor);

        if (existingCursor != null)
        {
          stopManagingCursor(existingCursor);
          existingCursor.close();
        }
        t.stop();
      }
    });
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig)
  {
    super.onConfigurationChanged(newConfig);
    // AdUtil.onConfigurationChanged(this, newConfig);
    googleAdsUtil.showAds(this);
    refreshUI();
  }

  @Override
  public boolean onContextItemSelected(MenuItem item)
  {
    if ((positionOfSelectedItemOnLongPress > -1) && onContextItemSelected(item, positionOfSelectedItemOnLongPress))
    {
      return true;
    }
    return super.onContextItemSelected(item);
  }

  protected abstract boolean onContextItemSelected(MenuItem item, int positionOfSelectedItemOnLongPress);

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    Log.d("SYNC_DEBUG", "AbstractNewsRobListActivity.onCreate() called");
    getEntryManager();

    boolean isHwAccelerationEnabled = EntryManager.getInstance(this).isHardwareAccelerationListsEnabled();
    PL.log("User set hw accel to enabled=" + isHwAccelerationEnabled + " (activity: " + this + ").", this);

    if (isHwAccelerationEnabled)
    {
      SDK11Helper.enableHWAccelerationForActivity(this);
    }

    setTheme(getEntryManager().getCurrentThemeResourceId());
    requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
    requestWindowFeature(Window.FEATURE_NO_TITLE);

    googleAdsUtil = new GoogleAdsUtil(entryManager);

    /*
     * WindowManager.LayoutParams lp = new WindowManager.LayoutParams(); lp.copyFrom(getWindow().getAttributes()); lp.format = PixelFormat.RGBA_8888;
     * getWindow().setAttributes(lp);
     */

    // setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);

    super.onCreate(savedInstanceState);

    /*
     * if ("com.graze16.VIEW".equals(getIntent().getAction())) { Intent i = new Intent(this, ShowArticleActivity.class); i.putExtra("atomId",
     * getIntent().getDataString()); startActivity(i); finish(); } else {
     */

    refreshUIRunnable = new Runnable()
    {
      public void run()
      {
        refreshUI();
      }
    };
    getEntryManager().updateLastUsed();
    if (NewsRob.isDebuggingEnabled(this)) {
      PL.log("onCreate called on " + getClass().getSimpleName(), this);
    }
  }

  @Override
  public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
  {
    if ((menuInfo != null) && (menuInfo instanceof AdapterView.AdapterContextMenuInfo))
    {
      AdapterView.AdapterContextMenuInfo mi = (AdapterView.AdapterContextMenuInfo) menuInfo;
      positionOfSelectedItemOnLongPress = mi.position;
    }
    if (positionOfSelectedItemOnLongPress > -1)
    {
      onCreateContextMenu(menu, v, menuInfo, positionOfSelectedItemOnLongPress);
    }
    super.onCreateContextMenu(menu, v, menuInfo);
  }

  protected abstract void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo, int positionOfSelectedItemOnLongPress);

  @Override
  protected Dialog onCreateDialog(int id)
  {
    switch (id)
    {
      case DIALOG_SD_CARD_ADVISABLE_ID:
        return new AlertDialog.Builder(this).setIcon(android.R.drawable.ic_dialog_alert).setMessage(R.string.advise_sdcard_read_only)
            .setTitle(R.string.advise_sdcard_read_only_title).setPositiveButton(android.R.string.ok, null).create();

      case DIALOG_CLEAR_CACHE:
        ProgressDialog pd = new ProgressDialog(this);
        pd.setTitle(R.string.clear_cache_dialog_title);
        pd.setMessage(U.t(this, R.string.clear_cache_dialog_message));
        pd.setIndeterminate(true);
        return pd;

      case DIALOG_SHOW_FILTER_INFO_ID:
        return new AlertDialog.Builder(this).setIcon(android.R.drawable.ic_dialog_alert).setMessage(getDbQuery().toString())
            .setTitle("Current Filter").setPositiveButton(android.R.string.ok, null).create();
    }

    return super.onCreateDialog(id);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu)
  {
    super.onCreateOptionsMenu(menu);
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.menu_main, menu);
    
    // Store menu reference for sync icon animation
    currentMenu = menu;
    
    // Set the correct initial icon for show/hide toggle
    MenuItem showHideItem = menu.findItem(R.id.menu_show_hide);
    if (showHideItem != null) {
      boolean shouldHideReadItems = getDbQuery().shouldHideReadItems();
      showHideItem.setIcon(shouldHideReadItems ? R.drawable.ic_dot_white_32dp : R.drawable.ic_circle_white_32dp);
    }
    
    return true;

    // if the action bar is not shown the actions that would go into the bar
    // need to be shown in the menu
//    if (EntryManager.ACTION_BAR_GONE.equals(getEntryManager().getActionBarLocation()))
//    {
//      menu.add(0, MENU_ITEM_REFRESH_ID, 0, R.string.menu_refresh).setIcon(android.R.drawable.ic_menu_rotate).setShortcut('1', 'r');
//
//      menu.add(0, MENU_ITEM_CANCEL_ID, 0, R.string.menu_cancel).setIcon(android.R.drawable.ic_menu_close_clear_cancel);
//
//      menu.add(0, MENU_ITEM_MARK_ALL_READ_ID, 0, R.string.menu_item_mark_all_read).setShortcut('3', 'm')
//          .setIcon(android.R.drawable.ic_menu_agenda);
//
//      menu.add(0, MENU_ITEM_HIDE_ID, 0, "").setShortcut('4', 'h');
//
//      menu.add(0, MENU_ITEM_SORT_ID, 0, "").setShortcut('5', 'o').setIcon(android.R.drawable.ic_menu_sort_by_size);
//    }
//
//    /*
//     * menu.add(0, MENU_ITEM_SEARCH_ID, 0, R.string.menu_item_search) .setIcon(android.R.drawable.ic_search_category_default)
//     * .setAlphabeticShortcut(SearchManager.MENU_KEY);
//     */
//
//    Intent intent = new Intent();
//    intent.setClass(this, SubscribeFeedActivity.class);
//
//    menu.add(0, MENU_ITEM_SUBSCRIBE_FEED_ID, 0, "Subscribe Feed").setIcon(android.R.drawable.ic_menu_add).setShortcut('4', 'f')
//        .setIntent(intent);
//
//    menu.add(0, MENU_ITEM_TOGGLE_THEME_ID, 0, R.string.menu_toggle_theme).setIcon(android.R.drawable.ic_menu_slideshow).setShortcut('6', 't');
//
//    menu.add(0, MENU_ITEM_SETTINGS_ID, 0, R.string.menu_settings).setIcon(android.R.drawable.ic_menu_preferences).setShortcut('7', 's');
//
//    menu.add(0, MENU_ITEM_CLEAR_CACHE_ID, 0, R.string.menu_clear_cache).setIcon(android.R.drawable.ic_menu_delete).setShortcut('8', 'c');
//    menu.add(0, MENU_ITEM_LOGOUT_ID, 0, R.string.menu_logout).setIcon(android.R.drawable.ic_lock_power_off).setShortcut('9', 'l')
//        .setTitleCondensed("Logout");
//    if (NewsRob.isDebuggingEnabled(this))
//    {
//      menu.add(0, MENU_ITEM_SHOW_FILTER_ID, 99, "Show Filter (Debug)").setIcon(android.R.drawable.ic_menu_info_details).setShortcut('8', 'c');
//    }

    // Uri uri = Uri.parse("http://bit.ly/nrfaq");
    // // Uri uri =
    // //
    // Uri.parse("http://groups.google.com/group/newsrob/web/frequently-asked-questions");
    // menu.add(0, MENU_ITEM_FAQ_ID, 0,
    // R.string.menu_faq).setIcon(android.R.drawable.ic_menu_help)
    // .setTitleCondensed("FAQ").setIntent(new Intent(Intent.ACTION_VIEW,
    // uri));

    // if (entryManager.canNewsRobProBeBought()) {
    // final Intent viewIntent = new Intent(Intent.ACTION_VIEW);
    // viewIntent.setData(Uri.parse("market://details?id=" +
    // EntryManager.PRO_PACKAGE_NAME));
    // menu.add(0, ArticleViewHelper.MENU_ITEM_BUY_NEWSROB_PRO, 30,
    // "Buy NewsRob Pro!").setIntent(viewIntent)
    // .setTitleCondensed("Buy").setIcon(R.drawable.gen_auto_app_icon);
    // }

//    return result;
  }

  public boolean onLongClick(View v)
  {
    try
    {
      positionOfSelectedItemOnLongPress = getListView().getPositionForView(v);
      if (positionOfSelectedItemOnLongPress > -1)
      {
        openContextMenu(getListView());
      }
      return true;
    }
    catch (NullPointerException npe)
    {
    }
    return false;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item)
  {
    if (item.getItemId() == android.R.id.home) {
        // Handle home/logo button click - navigate to dashboard only if not already there
        Log.d("HomeNavigation", "Home button pressed from " + this.getClass().getSimpleName());
        if (!(this instanceof DashboardListActivity)) {
            Log.d("HomeNavigation", "Not on Dashboard, navigating to DashboardListActivity");
            Intent intent = new Intent(this, DashboardListActivity.class);
            // Prevent auto-skip to feeds - stay on dashboard home view
            intent.putExtra("skip", false);
            // Remove task-switching flags to behave like regular navigation
            startActivity(intent);
            finish(); // Close current activity to prevent back stack issues
        } else {
            Log.d("HomeNavigation", "Already on Dashboard, doing nothing");
        }
        return true;
    } else if (item.getItemId() == R.id.menu_sync) {
        if (getEntryManager().isModelCurrentlyUpdated()) {
            // During sync, clicking should toggle sync progress details
            toggleProgressBarVisibility();
        } else {
            // Start sync
            requestRefresh();
            Toast.makeText(this, "Syncing articles...", Toast.LENGTH_SHORT).show();
        }
        return true;
    } else if (item.getItemId() == R.id.menu_mark_all_read) {
        // Trigger mark all read functionality
        instantiateMarkAllReadDialog();
        return true;
    } else if (item.getItemId() == R.id.menu_show_hide) {
        // Trigger show/hide functionality
        requestToggleHideItems();
        String toastText = getDbQuery().shouldHideReadItems() ? "Unread Articles only" : "All Articles";
        Toast.makeText(this, toastText, Toast.LENGTH_SHORT).show();
        // Update the icon based on current state
        boolean shouldHideReadItems = getDbQuery().shouldHideReadItems();
        item.setIcon(shouldHideReadItems ? R.drawable.ic_dot_white_32dp : R.drawable.ic_circle_white_32dp);
        return true;
    } else if (item.getItemId() == R.id.menu_subscribe_feed) {
        Intent intent = new Intent().setClass(this, SubscribeFeedActivity.class);
        startActivity(intent);
        return true;
    } else if (item.getItemId() == R.id.menu_toggle_theme) {
        getEntryManager().toggleTheme();
        reopenIfThemeOrActionBarLocationChanged();
        return true;
    } else if (item.getItemId() == R.id.menu_settings) {
        if (false) {
          Intent intent1 = new Intent();
          intent1.setClassName("com.graze16", "com.graze16.activities.ArticleListActivity");
          intent1.putExtra("FEED_URL", "xxx"); // http://www.spiegel.de/schlagzeilen/index.rss
          startActivity(intent1);
          return true;
        }
        else {
          Intent i1 = new Intent().setClass(this, SettingsActivity.class);
          startActivity(i1);
          return true;
        }
    } else if (item.getItemId() == R.id.menu_clear_cache) {
        showConfirmationDialog("Clear the Cache?", new Runnable() {
          @Override
          public void run() {
            getEntryManager().requestClearCache(handler);
          }
        });
        return true;
    } else if (item.getItemId() == R.id.menu_logout) {
        showConfirmationDialog("Logout and Clear Cache?", new Runnable() {
          public void run() {
            Log.d(TAG, "Logging out ...");
            getEntryManager().logout();
            getEntryManager().requestClearCache(handler);
          }
        });
        return true;
    } else {
        return super.onOptionsItemSelected(item);
    }
  }

  @Override
  protected void onPause()
  {
    hideProgressBar();
    getEntryManager().removeListener(this);

    if (false)
    {
      googleAdsUtil.hideAds(this);
      UIHelper.pauseWebViews(this);
    }
    super.onPause();

  }

  @Override
  protected void onPostCreate(Bundle savedInstanceState)
  {
    super.onPostCreate(savedInstanceState);
    Toolbar toolbar = findViewById(R.id.activity_actionbar);
    setSupportActionBar(toolbar);
    getSupportActionBar().setTitle("Graze16");
    getSupportActionBar().setHomeAsUpIndicator(R.drawable.gen_logo_32dp);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    toolbar.setTitleTextColor(Color.WHITE);
    // Set gradient background
    boolean isLightTheme = getEntryManager().isLightColorSchemeSelected();
    toolbar.setBackgroundResource(isLightTheme ? R.drawable.list_header_background : R.drawable.list_header_background_dark);

    //    if (!shouldActionBarBeHidden())
    //    {
    //        findViewById(R.id.control_panel_stub).setVisibility(View.VISIBLE);
    // setupButtons(); // Using menu buttons instead of custom view
    //    }

    //    final ViewGroup parent = findViewById(R.id.ad_parent);
    //    final View controlPanel = findViewById(R.id.control_panel);
    //    final View statusBar = findViewById(R.id.status_bar);
    //    if (EntryManager.ACTION_BAR_BOTTOM.equals(getEntryManager().getActionBarLocation())) {
    //      // put the toolbar at the bottom
    //      parent.removeView(controlPanel);
    //      parent.addView(controlPanel);
    //      // show the status bar
    //      statusBar.setVisibility(View.VISIBLE);
    //    }
    //    else if (shouldActionBarBeHidden()) {
    //      // show the status bar
    //      statusBar.setVisibility(View.VISIBLE);
    //      if (controlPanel != null) {
    //        controlPanel.setVisibility(View.GONE);
    //      }
    //    }

    //    if (!shouldActionBarBeHidden()) {
    progressIndicator = findViewById(R.id.background_progress);
    if (progressIndicator != null) {
      progressIndicator.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          toggleProgressBarVisibility();
          progressIndicator.postInvalidateDelayed(150);
        }
      });
    }
    progressBar = findViewById(R.id.progress_bar);
    progressDescription = findViewById(R.id.status_text);
    progressContainer = findViewById(R.id.progress_container);
//  }
    getListView().setOnCreateContextMenuListener(this);
    signalBackgroundDataIsTurnedOffOrInAirplaneMode();
    getEntryManager().showReleaseNotes();
    currentTheme = getEntryManager().getCurrentThemeResourceId();
    currentActionBarLocation = getEntryManager().getActionBarLocation();

    Drawable d = getResources().getDrawable(R.drawable.progress_small_white);
    ProgressBar statusProgressBar = findViewById(R.id.progress_status_bar);
    if (statusProgressBar != null) {
      statusProgressBar.setIndeterminateDrawable(d);
    }
    TextView controlPanelText = findViewById(R.id.control_panel_text);
    if (controlPanelText != null) {
      controlPanelText.setText(getDefaultControlPanelTitle());
      controlPanelText.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          Toast.makeText(AbstractNewsRobListActivity.this, getToastMessage(), Toast.LENGTH_LONG).show();
        }
      });
    }//    View newsRobLogo = findViewById(R.id.newsrob_logo);
//    if (newsRobLogo != null) {
//      newsRobLogo.setOnClickListener(new View.OnClickListener()
//      {
//        @Override
//        public void onClick(View v)
//        {
//          Toast.makeText(AbstractNewsRobListActivity.this, "-> Home", Toast.LENGTH_SHORT).show();
//          Intent i = new Intent(AbstractNewsRobListActivity.this, DashboardListActivity.class);
//          i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//          i.putExtra("skip", false);
//          finish();
//          startActivity(i);
//        }
//      });
//    }

    googleAdsUtil.showAds(this);
    if (getIntent().hasExtra("showProgress") && getIntent().getBooleanExtra("showProgress", false)) {
      showProgressBar();
    }
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu)
  {
    // Ensure menu reference is available for sync animation
    currentMenu = menu;
//    boolean canRefresh = getEntryManager().canRefresh();
//
//    if (EntryManager.ACTION_BAR_GONE.equals(getEntryManager().getActionBarLocation()))
//    {
//      menu.findItem(MENU_ITEM_REFRESH_ID).setEnabled(canRefresh);
//      menu.findItem(MENU_ITEM_MARK_ALL_READ_ID).setEnabled(shouldMarkAllReadButtonBeEnabled());
//
//      MenuItem hideMenuItem = menu.findItem(MENU_ITEM_HIDE_ID);
//      menu.findItem(MENU_ITEM_CANCEL_ID).setEnabled(!getEntryManager().isCancelRequested() && getEntryManager().isModelCurrentlyUpdated());
//
//      if (getDbQuery().shouldHideReadItems())
//      {
//        hideMenuItem.setTitle("Show Read Articles");
//        hideMenuItem.setTitleCondensed("Show Read Articles");
//        hideMenuItem.setIcon(android.R.drawable.ic_lock_silent_mode_off);
//      }
//      else
//      {
//        hideMenuItem.setTitle("Hide Read Articles");
//        hideMenuItem.setTitleCondensed("Hide Read Articles"); // I18N
//        hideMenuItem.setIcon(android.R.drawable.ic_lock_silent_mode);
//      }
//
//      MenuItem sortMenuItem = menu.findItem(MENU_ITEM_SORT_ID);
//      if (getDbQuery().isSortOrderAscending())
//      {
//        sortMenuItem.setTitle("Newest first");
//      }
//      else
//      {
//        sortMenuItem.setTitle("Oldest first");
//      }
//    }
//
//    menu.findItem(MENU_ITEM_CLEAR_CACHE_ID).setEnabled(canRefresh);
//    menu.findItem(MENU_ITEM_LOGOUT_ID).setEnabled(!getEntryManager().needsSession() && canRefresh);

    return super.onPrepareOptionsMenu(menu);
  }

  @Override
  protected void onResume()
  {
    Log.d("SYNC_DEBUG", "AbstractNewsRobListActivity.onResume() called");
    NewsRob.lastActivity = this;

    super.onResume();

    if (false)
    {
      UIHelper.resumeWebViews(this);
    }

    Log.d("SYNC_DEBUG", "Registering EntryManager listener");
    getEntryManager().addListener(this);
    if (!reopenIfThemeOrActionBarLocationChanged())
    {
      getDbQuery().updateShouldHideReadItems();
      modelUpdated();

      refreshUI(); // LATER Maybe I should maintain and check lastModified
      // and conditionally do refreshUI();

      checkIfSDCardAccessible();
      // AdUtil.publishAd(this);

      googleAdsUtil.showAds(this);
    }
  }

  @Override
  protected void onStart()
  {
    super.onStart();
    FlurryUtil.onStart(this);
  }

  @Override
  protected void onStop()
  {
    super.onStop();
    FlurryUtil.onStop(this);
  }

  protected void refreshProgressBar()
  {
    if ((entryManager.getCurrentRunningJob() != null) || entryManager.isModelCurrentlyUpdated())
    {
      activateProgressIndicator();
    }
    else
    {
      deactivateProgressIndicator();
    }
  }

  void refreshUI()
  {
    // ((BaseAdapter) getListView().getAdapter()).notifyDataSetChanged(); // TODO what
    // is this good for?
    updateButtons();
    updateControlPanelTitle();
    refreshProgressBar();
    
    // Update progress container if it's visible
    updateProgressContainer();
  }

  private boolean reopenIfThemeOrActionBarLocationChanged()
  {
    if ((currentTheme != getEntryManager().getCurrentThemeResourceId())
        || !getEntryManager().getActionBarLocation().equals(currentActionBarLocation))
    {
      finish();
      PL.log("AbstractNewsRobListActivity. Change detected that makes a relaunch necessary.", this);
      startActivity(getIntent());
      return true;
    }
    return false;
  }

  protected void requestRefresh()
  {
    if (NewsRob.isDebuggingEnabled(this))
    {
      PL.log("ANRLA: User requested refresh manually.", this);
    }

    // Show progress immediately when user taps refresh
    activateProgressIndicator();

    if (getEntryManager().needsSession())
    {
      BackendProvider provider = SyncInterfaceFactory.getSyncInterface(this);
      provider.startLogin(this, getApplicationContext());
    }
    else
    {
      getEntryManager().requestSynchronization(false);
    }
  }

  protected void requestToggleHideItems()
  {
    getDbQuery().toggleHideItems();
    modelUpdated();
  }

  protected void requestToggleSortOrder()
  {
    getDbQuery().toggleSortOrder();
    modelUpdated();
  }

  protected void requestUploadOnlyRefresh()
  {
    if (NewsRob.isDebuggingEnabled(this))
    {
      PL.log("ANRLA: User requested upload refresh manually.", this);
    }

    if (getEntryManager().needsSession())
    {
      BackendProvider provider = SyncInterfaceFactory.getSyncInterface(this);
      provider.startLogin(this, getApplicationContext());
    }
    else
    {
      getEntryManager().requestSynchronization(true);
    }
  }

  private void setupButtons()
  {
    if (shouldActionBarBeHidden()) {
      return;
    }

    // In Progress Cancel Sync Button
//    Button cancelSyncButton = findViewById(R.id.cancel_sync);
//    if (cancelSyncButton != null) {
//      cancelSyncButton.setBackgroundResource(R.drawable.custom_button);
//      cancelSyncButton.setOnClickListener(new View.OnClickListener()
//      {
//        @Override
//        public void onClick(View v)
//        {
//          entryManager.cancel();
//        }
//      });
//    }

    refreshButton = findViewById(R.id.refresh);
    if (refreshButton == null) {
      return;
    }
    refreshButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v)
      {
        if ("Refresh".equals(v.getTag())) {
          requestRefresh();
          Toast.makeText(AbstractNewsRobListActivity.this, "Refresh", Toast.LENGTH_SHORT).show();
        }
      }
    });
    refreshButton.setLongClickable(true);
    refreshButton.setOnLongClickListener(new View.OnLongClickListener() {
      @Override
      public boolean onLongClick(View v) {
        if ("Refresh".equals(v.getTag())) {
          requestUploadOnlyRefresh();
          Toast.makeText(AbstractNewsRobListActivity.this, "Upload Only Refresh", Toast.LENGTH_SHORT).show();
        }
        return true;
      }
    });

    showHideButton = findViewById(R.id.show_hide_button);
    if (showHideButton != null) {
      showHideButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          requestToggleHideItems();
          String toastText = getDbQuery().shouldHideReadItems() ? "Unread Articles only" : "All Articles";
          Toast.makeText(AbstractNewsRobListActivity.this, toastText, Toast.LENGTH_SHORT).show();
        }
      });
    }

    markAllReadButton = findViewById(R.id.mark_all_read_button);
    if (markAllReadButton != null) {
      markAllReadButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          AbstractNewsRobListActivity.this.instantiateMarkAllReadDialog();
        }
      });
    }

    toggleOrderButton = findViewById(R.id.toggle_order_button);
    if (toggleOrderButton != null) {
      toggleOrderButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          requestToggleSortOrder();
          String toastText = getDbQuery().isSortOrderAscending() ? "Oldest first" : "Newest first";
          Toast.makeText(AbstractNewsRobListActivity.this, toastText, Toast.LENGTH_SHORT).show();
        }
      });
    }
    updateButtons();
  }

  private boolean shouldActionBarBeHidden()
  {
    String actionBarLocation = getEntryManager().getActionBarLocation();
    boolean hidden = EntryManager.ACTION_BAR_GONE.equals(actionBarLocation);
    Log.d("SYNC_DEBUG", "Action bar location=" + actionBarLocation + ", hidden=" + hidden);
    return hidden;
  }

  protected boolean shouldMarkAllReadButtonBeEnabled()
  {
    DBQuery dbq = new DBQuery(getDbQuery());
    dbq.setShouldHideReadItemsWithoutUpdatingThePreference(true);
    return getEntryManager().isMarkAllReadPossible(dbq);
  }

  protected boolean shouldRefreshButtonBeEnabled()
  {
    return getEntryManager().canRefresh();
  }

  void showConfirmationDialog(String message, final Runnable action)
  {
    AlertDialog.Builder builder = new AlertDialog.Builder(this)
        .setTitle("Please Confirm!")
        .setMessage(message)
        .setIcon(android.R.drawable.ic_dialog_alert)
        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            action.run();
          }
        })
        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            dialog.cancel();
          }
        });
    final AlertDialog dialog = builder.create();
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      dialog.setOnShowListener(new DialogInterface.OnShowListener() {
        @Override
        public void onShow(DialogInterface d)
        {
          Button posButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
          Button negButton = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
          LinearLayout.LayoutParams posParams = (LinearLayout.LayoutParams) posButton.getLayoutParams();
          posParams.weight = 1;
          posParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
          LinearLayout.LayoutParams negParams = (LinearLayout.LayoutParams) negButton.getLayoutParams();
          negParams.weight = 1;
          negParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
          posButton.setLayoutParams(posParams);
          negButton.setLayoutParams(negParams);
        }
      });
    }
    dialog.show();
  }

  void showDataUpdateProgressMonitor()
  {
    if (true)
    {
      return;
    }
    runOnUiThread(new Runnable()
    {

      @Override
      public void run()
      {
        ProgressBar pb = (ProgressBar) findViewById(R.id.data_update_progress);
        pb.setVisibility(View.VISIBLE);
      }
    });

  }

  protected void showProgressBar()
  {
    Log.d("SYNC_DEBUG", "showProgressBar() called, shouldActionBarBeHidden=" + shouldActionBarBeHidden());
    if (shouldActionBarBeHidden())
    {
      Log.d("SYNC_DEBUG", "Action bar is hidden, not showing progress bar");
      return;
    }

    getEntryManager().updateProgressReportBeenOpened();

    Log.d("SYNC_DEBUG", "Making progress container visible and starting animation");
    
    // Set visibility first
    progressContainer.setVisibility(View.VISIBLE);
    
    // Force layout measurement by requesting layout
    progressContainer.requestLayout();
    
    // Post a runnable to ensure layout is complete before animation
    progressContainer.post(new Runnable() {
      @Override
      public void run() {
        // Debug layout positioning after layout is complete
        int[] location = new int[2];
        progressContainer.getLocationOnScreen(location);
        Log.d("SYNC_DEBUG", "Progress container screen position after layout: x=" + location[0] + ", y=" + location[1]);
        Log.d("SYNC_DEBUG", "Progress container dimensions after layout: width=" + progressContainer.getWidth() + ", height=" + progressContainer.getHeight());
        
        // Bring to front to ensure it's above other views
        progressContainer.bringToFront();
        
        // Start animation after layout is complete
        Animation inAnimation = AnimationUtils.loadAnimation(AbstractNewsRobListActivity.this, R.anim.push_up_in);
        progressContainer.startAnimation(inAnimation);
        
        Log.d("SYNC_DEBUG", "Animation started after layout completion");
      }
    });
    
    Log.d("SYNC_DEBUG", "Progress container visibility=" + progressContainer.getVisibility());
    
    // Force immediate update of the progress container with current sync status
    updateProgressContainer();
    
    // Also force a UI refresh to ensure latest job info is displayed
    refreshUI();
  }
  
  private void updateProgressContainer()
  {
    if (progressContainer == null || progressContainer.getVisibility() != View.VISIBLE) {
      return;
    }
    
    String status = "Background Operation In Progress";
    Job runningJob = entryManager.getCurrentRunningJob();
    
    // Try both approaches: direct findViewById on container and global fields
    ProgressBar containerProgressBar = progressContainer.findViewById(R.id.progress_bar);
    TextView containerStatusText = progressContainer.findViewById(R.id.status_text);
    
    // Also use global fields as backup (they should point to same views)
    if (containerProgressBar == null) {
      containerProgressBar = progressBar;
    }
    if (containerStatusText == null) {
      containerStatusText = progressDescription;
    }
    
    // Check if we're sync is actually running
    boolean isModelUpdating = entryManager.isModelCurrentlyUpdated();
    
    if (runningJob != null)
    {
      String jobDesc = runningJob.getJobDescription();
      if (jobDesc != null && !jobDesc.isEmpty()) {
        status = jobDesc;
      }
      
      if (runningJob.isProgressMeassurable())
      {
        int[] progress = runningJob.getProgress();
        int currentArticle = progress[0];
        int allArticles = progress[1];
        
        if (containerProgressBar != null) {
          containerProgressBar.setMax(allArticles);
          containerProgressBar.setProgress(currentArticle);
          containerProgressBar.setIndeterminate(false);
          containerProgressBar.setVisibility(View.VISIBLE);
        }
        
        status = jobDesc + " (" + currentArticle + "/" + allArticles + ")";
      }
      else
      {
        if (containerProgressBar != null) {
          containerProgressBar.setMax(0);
          containerProgressBar.setProgress(0);
          containerProgressBar.setIndeterminate(true);
          containerProgressBar.setVisibility(View.VISIBLE);
        }
      }
    }
    else if (isModelUpdating)
    {
      // Even without a specific job, show sync progress
      status = "Syncing articles...";
      if (containerProgressBar != null) {
        containerProgressBar.setMax(0);
        containerProgressBar.setProgress(0);
        containerProgressBar.setIndeterminate(true);
        containerProgressBar.setVisibility(View.VISIBLE);
      }
    }
    else
    {
      // No sync running - show completion status but keep progress bar visible
      status = lastSyncResult.isEmpty() ? "Ready for sync" : lastSyncResult;
      if (containerProgressBar != null) {
        if (lastSyncResult.isEmpty()) {
          // No sync has run yet - hide the progress bar
          containerProgressBar.setVisibility(View.INVISIBLE);
        } else {
          // Show a completed progress bar instead of hiding it
          containerProgressBar.setMax(100);
          containerProgressBar.setProgress(100);
          containerProgressBar.setIndeterminate(false);
          containerProgressBar.setVisibility(View.VISIBLE);
        }
      }
    }
    
    if (containerStatusText != null) {
      containerStatusText.setText(status);
    }
  }

  /**
   * Signal to the user that background data is turned off, if s/he has auto-sync enabled and this is the entry point (task root) Also signal if Airplane mode
   * is on.
   */
  private void signalBackgroundDataIsTurnedOffOrInAirplaneMode()
  {
    if (getEntryManager().isAutoSyncEnabled())
    {
      long minutesSinceLastSignalling = (System.currentTimeMillis() - getEntryManager().getLastShownThatSyncIsNotPossible()) / 1000 / 60;
      if (minutesSinceLastSignalling > 60)
      {
        String message = null;

        if (getEntryManager().isInAirplaneMode())
        {
          message = "You are in Airplane mode.\nSyncing and downloading of articles won't work.";
        }
        else if (!getEntryManager().isBackgroundDataEnabled())
        {
          message = "The 'Background Data' setting is turned off.\nNo auto-sync will occur. You can sync manually though.";
        }
        else if (!getEntryManager().isSystemwideSyncEnabled())
        {
          message = "The Android 'Auto-Sync' setting is turned off.\nNo auto-sync will occur. You can sync manually though.";
        }

        if (message != null)
        {
          Toast.makeText(this, message, Toast.LENGTH_LONG).show();
          getEntryManager().updateLastShownThatSyncIsNotPossible();
        }
      }
    }
  }

  protected void startShowEntryActivityForPosition(int position, DBQuery dbq)
  {
    Intent i = new Intent(AbstractNewsRobListActivity.this, ShowArticleActivity.class);
    i.putExtra(UIHelper.EXTRA_KEY_POSITION, position);
    UIHelper.addExtrasFromDBQuery(i, dbq);
    startActivity(i);
  }

  public void statusUpdated()
  {
    runOnUiThread(new Runnable()
    {
      public void run()
      {
        refreshProgressBar();
        updateButtons();
      }
    });
  }

  protected void toggleProgressBarVisibility()
  {
    Log.d("SYNC_DEBUG", "toggleProgressBarVisibility called");
    if (progressContainer.getVisibility() == View.VISIBLE)
    {
      Log.d("SYNC_DEBUG", "Progress bar is visible, hiding it");
      hideProgressBar();
    }
    else
    {
      Log.d("SYNC_DEBUG", "Progress bar is hidden, showing it");
      showProgressBar();
    }
  }
  
  private void animateSyncMenuItem(boolean animate)
  {
    Log.d("SYNC_DEBUG", "animateSyncMenuItem called with animate=" + animate);
    if (currentMenu == null) {
      Log.d("SYNC_DEBUG", "currentMenu is null, trying to force menu creation");
      // Try to force menu creation by calling invalidateOptionsMenu
      runOnUiThread(new Runnable() {
        @Override
        public void run() {
          invalidateOptionsMenu();
        }
      });
      return;
    }
    
    MenuItem syncItem = currentMenu.findItem(R.id.menu_sync);
    if (syncItem == null) {
      Log.d("SYNC_DEBUG", "syncItem is null");
      return;
    }
    
    Log.d("SYNC_DEBUG", "Found syncItem, proceeding with animation=" + animate);
    
    if (animate) {
      // Create a custom ImageButton to replace the menu item - like the original refreshButton
      if (customSyncButton == null) {
        customSyncButton = new ImageButton(this);
        customSyncButton.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        customSyncButton.setScaleType(ImageButton.ScaleType.CENTER);
        
        // Set size to match menu item (48dp for proper touch target)
        int size = (int) (48 * getResources().getDisplayMetrics().density); // 48dp in pixels
        android.view.ViewGroup.LayoutParams params = new android.view.ViewGroup.LayoutParams(size, size);
        customSyncButton.setLayoutParams(params);
        
        // Add padding to center the icon properly
        int padding = (int) (8 * getResources().getDisplayMetrics().density); // 8dp padding
        customSyncButton.setPadding(padding, padding, padding, padding);
        
        // Add click listener to toggle progress bar during sync
        customSyncButton.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            Log.d("SYNC_DEBUG", "Custom sync button clicked during animation");
            // Toggle progress bar visibility when user taps during sync
            toggleProgressBarVisibility();
          }
        });
      }
      
      // Use proper spinner drawable based on theme (sized to match sync icon)
      boolean isLightTheme = getEntryManager().isLightColorSchemeSelected();
      int spinnerDrawable = isLightTheme ? R.drawable.progress_sync_black : R.drawable.progress_sync_white;
      customSyncButton.setImageResource(spinnerDrawable);
      
      // Replace the menu item with our custom animated button
      syncItem.setActionView(customSyncButton);
      
      // The animated-rotate drawable will handle the smooth spinning automatically
      // No need for manual ObjectAnimator - this eliminates the jumpiness
      
    } else {
      // Stop animation and restore normal menu item
      // No manual animation cleanup needed since animated-rotate handles it
      
      // Remove custom action view and restore normal menu item
      syncItem.setActionView(null);
      syncItem.setEnabled(true);
    }
  }

  protected void updateButtons()
  {
    if (EntryManager.ACTION_BAR_GONE.equals(getEntryManager().getActionBarLocation()))
    {
      ProgressBar progressStatusBar = (ProgressBar) findViewById(R.id.progress_status_bar);

      progressStatusBar.setVisibility(entryManager.isModelCurrentlyUpdated() ? View.VISIBLE : View.INVISIBLE);
    }

    if (shouldActionBarBeHidden()) {
      return;
    }
    if (refreshButton == null) {
      setupButtons();
    }
    if (refreshButton == null) {
      return;
    }

    ImageButton cancelButton = findViewById(R.id.cancel_sync);
    if (cancelButton != null) {
      cancelButton.setVisibility(entryManager.isModelCurrentlyUpdated() && !entryManager.isCancelRequested() ? View.VISIBLE : View.INVISIBLE);
      if (cancelButton.getVisibility() == View.VISIBLE) {
        cancelButton.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            entryManager.cancel();
          }
        });
      }
    }

    if (refreshButton != null) {
      refreshButton.setTag("Refresh");
      refreshButton.setImageResource(R.drawable.gen_toolbar_icon_sync);
      refreshButton.setEnabled(shouldRefreshButtonBeEnabled());
      refreshButton.setFocusable(refreshButton.isEnabled());
    }

    DBQuery dbq = getDbQuery();
    boolean shouldHideReadItems = dbq.shouldHideReadItems();
    if (showHideButton != null) {
      showHideButton.setImageResource(shouldHideReadItems ? R.drawable.ic_dot_white_32dp : R.drawable.ic_circle_white_32dp);
    }
    if (toggleOrderButton != null) {
      toggleOrderButton.setImageResource(getDbQuery().isSortOrderAscending() ? R.drawable.ic_arrow_up_white_32dp
          : R.drawable.ic_arrow_down_white_32dp);
    }
    if (markAllReadButton != null) {
      markAllReadButton.setEnabled(shouldMarkAllReadButtonBeEnabled());
      markAllReadButton.setFocusable(markAllReadButton.isEnabled());
    }
  }

  private void updateControlPanelTitle()
  {
    TextView controlPanelTitle = (TextView) findViewById(R.id.control_panel_title);
    controlPanelTitle.setText(getDefaultStatusBarTitle());
  }

}
