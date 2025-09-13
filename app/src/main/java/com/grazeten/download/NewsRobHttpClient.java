package com.grazeten.download;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import android.content.Context;
import android.util.Log;

import com.grazeten.NewsRob;
import com.grazeten.PL;

import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class NewsRobHttpClient
{

  private static final String BOT_USER_AGENT     = "NewsRob (http://newsrob.com) Bot gzip";

  private static final String USER_AGENT_KEY     = "User-Agent";

  private static final String TAG                = NewsRobHttpClient.class.getSimpleName();

  private static final String PRETEND_USER_AGENT = "Mozilla/5.0 (Linux; U; Android 2.0; en-us; Milestone Build/SHOLS_U2_01.03.1) AppleWebKit/530.17 (KHTML, like Gecko) Version/4.0 Mobile Safari/530.17";
  // "Mozilla/5.0 (iPhone; U; CPU iPhone 2_1 like Mac OS X; en)"
  // + " AppleWebKit/528.5+ (KHTML, like Gecko) Version/3.1.2"
  // + " Mobile/5F136 Safari/525.20.1";
  private static final String USER_AGENT         = PRETEND_USER_AGENT + " " + "NewsRob (http://newsrob.com) gzip";
  private final OkHttpClient    delegate;
  private RuntimeException    mLeakedException   = new IllegalStateException("NewsRobHttpClient created and never closed");

  private static Boolean      countingEnabled;
  private Context             context;

  public static NewsRobHttpClient newInstance(Context ctx)
  {
    return newInstance(true, ctx);
  }

  public static NewsRobHttpClient newInstance(boolean followRedirects, Context ctx)
  {
    OkHttpClient.Builder builder = new OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .followRedirects(followRedirects)
        .followSslRedirects(followRedirects)
        .connectionPool(new ConnectionPool(5, 5, TimeUnit.MINUTES));

    OkHttpClient client = builder.build();
    return new NewsRobHttpClient(client, ctx);
  }

  private NewsRobHttpClient(OkHttpClient client, Context ctx)
  {
    this.context = ctx.getApplicationContext();
    this.delegate = client;
  }

  // Static factory methods for easier Apache HttpClient compatibility
  public static NewsRobHttpRequest createHttpGet(String url)
  {
    try {
      return NewsRobHttpRequest.createGet(url);
    } catch (java.net.URISyntaxException e) {
      throw new RuntimeException("Invalid URL: " + url, e);
    }
  }

  public static NewsRobHttpRequest createHttpPost(String url)
  {
    try {
      return NewsRobHttpRequest.createPost(url);
    } catch (java.net.URISyntaxException e) {
      throw new RuntimeException("Invalid URL: " + url, e);
    }
  }

  @Override
  protected void finalize() throws Throwable
  {
    super.finalize();
    if (mLeakedException != null)
    {
      Log.e(TAG, "Leak found", mLeakedException);
      mLeakedException = null;
    }
  }

  /**
   * Release resources associated with this client. You must call this, or significant resources (sockets and memory) may be leaked.
   */
  public void close()
  {
    if (mLeakedException != null)
    {
      // OkHttp manages connections internally, no explicit shutdown needed
      mLeakedException = null;
    }
  }

  public NewsRobHttpResponse executeZipped(NewsRobHttpRequest req) throws IOException
  {
    modifyRequestToAcceptGzipResponse(req);
    return this.execute(req);
  }

  public NewsRobHttpResponse execute(NewsRobHttpRequest request) throws IOException
  {
    maintainUserAgent(request);

    Request okHttpRequest = request.toOkHttpRequest();
    Response response = delegate.newCall(okHttpRequest).execute();
    NewsRobHttpResponse resp = new NewsRobHttpResponse(response);
    outputResponseDebugInfo(request, resp);
    return resp;
  }

  private void maintainUserAgent(NewsRobHttpRequest request)
  {
    if (request.getURI().getHost().equals("t.co"))
      request.setHeader(USER_AGENT_KEY, BOT_USER_AGENT);

  }

  private void outputResponseDebugInfo(NewsRobHttpRequest request, NewsRobHttpResponse resp) throws IOException
  {
    String status = "-> HTTP STATUS: " + resp.getStatusCode();
    status += " " + resp.getStatusCode() + " " + resp.getMessage();
    ResponseBody body = resp.getOkHttpResponse().body();
    status += " length=" + (body != null ? body.contentLength() : -1);
    if ("1".equals(NewsRob.getDebugProperties(context).getProperty("printCurls", "0")))
    {
      PL.log("Curl= " + NewsRobHttpClient.toCurl(request) + status, context);
    }
    else
    {
      if (NewsRob.isDebuggingEnabled(context))
        PL.log("NewsRobHttpClient: " + request.getURI() + status, context);
    }
    if (NewsRob.isDebuggingEnabled(context) && resp.getStatusCode() >= 400)
    {
      PL.log("Status " + resp.getStatusCode() + " for " + request.getURI() + ":", context);

      StringBuilder headers = new StringBuilder("  request headers=");
      for (String name : request.getHeaders().keySet())
      {
        headers.append("    " + name + "=" + request.getHeaders().get(name) + "\n");
      }

      if (false)
        PL.log(headers.toString(), context);
      headers = new StringBuilder("  response headers=");
      for (String name : resp.getOkHttpResponse().headers().names())
      {
        headers.append("    " + name + "=" + resp.getOkHttpResponse().headers().get(name) + "\n");
      }
      PL.log(headers.toString(), context);

      if ("1".equals(NewsRob.getDebugProperties(context).getProperty("dumpPayload", "0")) && body != null)
        PL.log("  Payload=" + body.string(), context);
    }
  }



  /**
   * Generates a cURL command equivalent to the given request.
   */
  public static String toCurl(NewsRobHttpRequest request) throws IOException
  {
    StringBuilder builder = new StringBuilder();

    builder.append("curl ");

    for (String name : request.getHeaders().keySet())
    {
      builder.append("--header \"");
      builder.append(name).append(": ").append(request.getHeaders().get(name));
      builder.append("\" ");
    }

    URI uri = request.getURI();

    builder.append("\"");
    builder.append(uri);
    builder.append("\"");

    if (request.hasBody() && !uri.toString().contains("ClientLogin"))
    {
      String body = request.getBodyAsString();
      if (body != null && body.length() < (8192 * 4))
      {
        builder.append(" --data-ascii \"").append(body).append("\"");
      }
      else
      {
        builder.append(" [TOO MUCH DATA TO INCLUDE]");
      }
    }

    return builder.toString();
  }

  /**
   * Modifies a request to indicate to the server that we would like a gzipped response. (Uses the "Accept-Encoding" HTTP header.)
   * 
   * @param request
   *          the request to modify
   * @see #getUngzippedContent
   */
  public static void modifyRequestToAcceptGzipResponse(NewsRobHttpRequest request)
  {
    request.addHeader("Accept-Encoding", "gzip");
  }

  /**
   * Gets the input stream from a response entity. If the entity is gzipped then this will get a stream over the uncompressed data.
   * 
   * @param response
   *          the response whose content should be read
   * @return the input stream to read from
   * @throws IOException
   */
  public static InputStream getUngzippedContent(NewsRobHttpResponse response, Context context) throws IOException
  {
    ResponseBody body = response.getOkHttpResponse().body();
    if (body == null) return null;
    
    InputStream responseStream = body.byteStream();
    if (isCountingEnabled(context))
      responseStream = new CountingInputStream(responseStream, "OUTER", context);
    if (responseStream == null)
      return responseStream;
    String contentEncoding = response.getOkHttpResponse().header("Content-Encoding");
    if (contentEncoding == null)
      return responseStream;
    if (contentEncoding.contains("gzip"))
      responseStream = new GZIPInputStream(responseStream);
    if (isCountingEnabled(context))
      responseStream = new CountingInputStream(responseStream, "INNER ", context);
    return responseStream;
  }

  // Compatibility method that takes HttpEntity and returns InputStream
  public static InputStream getUngzippedContent(HttpEntity entity, Context context) throws IOException
  {
    if (entity == null || entity.getResponseBody() == null) return null;
    
    InputStream responseStream = entity.getContent();
    if (isCountingEnabled(context))
      responseStream = new CountingInputStream(responseStream, "OUTER", context);
    if (responseStream == null)
      return responseStream;
    
    String contentEncoding = entity.getContentType();
    if (contentEncoding != null && contentEncoding.contains("gzip"))
      responseStream = new GZIPInputStream(responseStream);
    if (isCountingEnabled(context))
      responseStream = new CountingInputStream(responseStream, "INNER ", context);
    return responseStream;
  }

  private static boolean isCountingEnabled(Context context)
  {
    if (countingEnabled == null)
      countingEnabled = "1".equals(NewsRob.getDebugProperties(context).getProperty("countBytesTransferred", "0"));
    return countingEnabled;
  }

}







class CountingInputStream extends FilterInputStream
{

  private long    countedBytes;
  private String  label;
  private long    started;
  private Context context;

  public CountingInputStream(InputStream is, String label, Context context)
  {
    super(is);
    this.label = label;
    started = System.currentTimeMillis();
    this.context = context;
  }

  @Override
  public int read(byte[] buffer, int offset, int count) throws IOException
  {
    int readBytesCount = super.read(buffer, offset, count);
    if (readBytesCount > 0)
      countedBytes += readBytesCount;
    return readBytesCount;
  }

  @Override
  public void close() throws IOException
  {
    super.close();
    PL.log(String.format("-------- [%s] transferred: %8.3f KB in %8.3f seconds.", label, (countedBytes / 1024.0),
        ((System.currentTimeMillis() - started) / 1000.0)), context);
  }

}
