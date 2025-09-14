package com.graze16.download;

import okhttp3.Response;
import okhttp3.ResponseBody;
import java.io.IOException;

// Wrapper class for OkHttp Response to maintain compatibility with Apache HttpClient API  
public class NewsRobHttpResponse
{
  private final Response response;

  public NewsRobHttpResponse(Response response)
  {
    this.response = response;
  }

  public Response getOkHttpResponse()
  {
    return response;
  }

  public int getStatusCode()
  {
    return response.code();
  }

  public String getMessage()
  {
    return response.message();
  }

  // Compatibility method to match Apache HttpClient API
  public StatusLine getStatusLine()
  {
    return new StatusLine(response.code(), response.message());
  }

  // Get response body as entity for compatibility
  public HttpEntity getEntity()
  {
    return new HttpEntity(response.body());
  }

  // Missing method from EntriesRetriever usage
  public String getEntityContent() throws IOException
  {
    ResponseBody body = response.body();
    if (body != null)
    {
      return body.string();
    }
    return null;
  }

  // Missing method from EntriesRetriever usage  
  public String getHeader(String name)
  {
    return response.header(name);
  }

  // Additional compatibility methods
  public String getFirstHeader(String name)
  {
    return response.header(name);
  }

  public boolean isSuccessful()
  {
    return response.isSuccessful();
  }

  public void close()
  {
    if (response.body() != null)
    {
      response.body().close();
    }
  }
}