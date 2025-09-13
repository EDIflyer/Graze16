package com.grazeten.download;

// Compatibility class for Apache HttpClient StatusLine API
public class StatusLine
{
  private final int statusCode;
  private final String reasonPhrase;

  public StatusLine(int statusCode, String reasonPhrase)
  {
    this.statusCode = statusCode;
    this.reasonPhrase = reasonPhrase;
  }

  public int getStatusCode()
  {
    return statusCode;
  }

  public String getReasonPhrase()
  {
    return reasonPhrase;
  }

  @Override
  public String toString()
  {
    return statusCode + " " + reasonPhrase;
  }
}