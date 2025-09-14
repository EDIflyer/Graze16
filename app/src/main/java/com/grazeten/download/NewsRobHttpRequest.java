package com.graze16.download;

import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okio.Buffer;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

// Wrapper class for OkHttp Request to maintain compatibility with Apache HttpClient API
public class NewsRobHttpRequest
{
  private final Request.Builder builder;
  private final URI uri;
  private final Map<String, String> headers;
  private RequestBody body;
  private String method;

  public NewsRobHttpRequest(String method, URI uri)
  {
    this.method = method;
    this.uri = uri;
    this.builder = new Request.Builder().url(uri.toString());
    this.headers = new HashMap<>();
    
    // Set default user agent
    String userAgent = "Mozilla/5.0 (Linux; U; Android 2.0; en-us; Milestone Build/SHOLS_U2_01.03.1) AppleWebKit/530.17 (KHTML, like Gecko) Version/4.0 Mobile Safari/530.17 NewsRob (http://newsrob.com) gzip";
    setHeader("User-Agent", userAgent);
  }

  // Convenience constructors to match Apache HttpClient
  public static NewsRobHttpRequest createGet(String url) throws java.net.URISyntaxException
  {
    return new NewsRobHttpRequest("GET", new URI(url));
  }

  public static NewsRobHttpRequest createPost(String url) throws java.net.URISyntaxException
  {
    return new NewsRobHttpRequest("POST", new URI(url));
  }

  public void setHeader(String name, String value)
  {
    headers.put(name, value);
    builder.header(name, value);
  }

  public void addHeader(String name, String value)
  {
    headers.put(name, value);
    builder.addHeader(name, value);
  }

  public void removeHeaders(String name)
  {
    headers.remove(name);
    builder.removeHeader(name);
  }

  public Map<String, String> getHeaders()
  {
    return headers;
  }

  public URI getURI()
  {
    return uri;
  }

  public void setEntity(RequestBody body)
  {
    this.body = body;
  }

  public void setBody(RequestBody body)
  {
    this.body = body;
  }

  // Helper method to set form parameters - missing method from EntriesRetriever
  public void setFormParameters(Map<String, String> formData)
  {
    FormBody.Builder formBuilder = new FormBody.Builder();
    for (Map.Entry<String, String> entry : formData.entrySet())
    {
      formBuilder.add(entry.getKey(), entry.getValue());
    }
    this.body = formBuilder.build();
  }

  // Legacy method name for compatibility
  public void setFormData(Map<String, String> formData)
  {
    setFormParameters(formData);
  }

  public boolean hasBody()
  {
    return body != null;
  }

  public String getBodyAsString() throws IOException
  {
    if (body == null) return null;
    Buffer buffer = new Buffer();
    body.writeTo(buffer);
    return buffer.readUtf8();
  }

  public Request toOkHttpRequest()
  {
    Request.Builder finalBuilder = builder;
    if (body != null)
    {
      finalBuilder = finalBuilder.method(method, body);
    }
    else
    {
      finalBuilder = finalBuilder.method(method, null);
    }
    return finalBuilder.build();
  }
}