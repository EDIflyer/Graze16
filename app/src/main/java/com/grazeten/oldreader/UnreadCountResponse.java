package com.graze16.oldreader;

import java.util.List;

public class UnreadCountResponse
{
  public class UnreadCount
  {
    Integer count;
    String  id;
    Long    newestItemTimestampUsec;
  }

  Integer           max;
  List<UnreadCount> unreadcounts;
}
