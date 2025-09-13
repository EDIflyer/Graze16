package com.grazeten.download;

import okhttp3.ResponseBody;
import java.io.IOException;
import java.io.InputStream;

// Compatibility class for Apache HttpClient HttpEntity API
public class HttpEntity
{
  private final ResponseBody responseBody;

  public HttpEntity(ResponseBody responseBody)
  {
    this.responseBody = responseBody;
  }

  public InputStream getContent() throws IOException
  {
    return responseBody != null ? responseBody.byteStream() : null;
  }

  public long getContentLength()
  {
    return responseBody != null ? responseBody.contentLength() : -1;
  }

  public void consumeContent()
  {
    // OkHttp handles this automatically
    if (responseBody != null)
    {
      responseBody.close();
    }
  }

  public String getContentType()
  {
    if (responseBody != null && responseBody.contentType() != null)
    {
      return responseBody.contentType().toString();
    }
    return null;
  }

  public ResponseBody getResponseBody()
  {
    return responseBody;
  }
}