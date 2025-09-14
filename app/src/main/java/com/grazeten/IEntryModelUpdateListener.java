package com.graze16;

import com.graze16.jobs.ModelUpdateResult;

public interface IEntryModelUpdateListener
{

  void modelUpdateStarted(boolean fastSyncOnly);

  void modelUpdated();

  void modelUpdated(String atomId);

  void modelUpdateFinished(ModelUpdateResult result);

  void statusUpdated();

}
