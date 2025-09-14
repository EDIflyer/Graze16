package com.graze16.oldreader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UpdateArticlesRequest
{
  public enum MarkType
  {
    READ, UNREAD;
  }

  private static final String         MARK_READ   = "user/-/state/com.google/read";
  private static final String         MARK_UNREAD = "user/-/state/com.google/read";
  private static final String         READ_ID     = "a";
  private static final String         UNREAD_ID   = "r";

  private Map<String, String>         fields      = new HashMap<>();
  private List<String>                ids         = new ArrayList<String>();

  public UpdateArticlesRequest(MarkType markType) throws IOException
  {
    super();

    if (markType == null)
    {
      throw new IOException("Mark type must be set");
    }

    switch (markType)
    {
      case READ:
        addField(READ_ID, MARK_READ);
        break;
      case UNREAD:
        addField(UNREAD_ID, MARK_UNREAD);
        break;
    }
  }

  public void addField(String name, String value)
  {
    if (name == null)
    {
      throw new NullPointerException("name");
    }
    if (value == null)
    {
      throw new NullPointerException("value");
    }
    fields.put(name, value);
  }

  public void addId(String id)
  {
    ids.add(id);
    addField("i", id);
  }

  public Map<String, String> getFields()
  {
    return fields;
  }

  public List<String> getIds()
  {
    return ids;
  }
}
