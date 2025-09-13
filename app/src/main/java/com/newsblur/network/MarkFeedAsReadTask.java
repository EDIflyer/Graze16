package com.newsblur.network;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class MarkFeedAsReadTask
{
  private final APIManager apiManager;
  private final ExecutorService executorService;
  private final Handler mainHandler;

  public MarkFeedAsReadTask(final Context context, final APIManager apiManager)
  {
    this.apiManager = apiManager;
    this.executorService = Executors.newSingleThreadExecutor();
    this.mainHandler = new Handler(Looper.getMainLooper());
  }

  public void execute(String... id)
  {
    executorService.execute(() -> {
      Boolean result = apiManager.markFeedAsRead(id);
      mainHandler.post(() -> onPostExecute(result));
    });
  }

  protected abstract void onPostExecute(Boolean result);

  public void shutdown()
  {
    if (executorService != null && !executorService.isShutdown())
    {
      executorService.shutdown();
    }
  }
}
