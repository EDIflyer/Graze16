package com.grazeten.feedly;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.List;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.Response;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.grazeten.BackendProvider.AuthToken;
import com.grazeten.EntryManager;

public class FeedlyManager implements FeedlyKey
{
  private FeedlyApi     api = new Retrofit.Builder()
      .baseUrl(FeedlyApi.BASE_URL)
      .addConverterFactory(GsonConverterFactory.create())
      .build()
      .create(FeedlyApi.class);
  private final Context context;
  private String        userId;
  private String        accessCode;
  private String        currentToken;
  private String        refreshToken;
  private long          tokenExpire;

  public FeedlyManager(Context context)
  {
    this.context = context;

    EntryManager entryManager = EntryManager.getInstance(context);
    SharedPreferences preferences = entryManager.getSharedPreferences();

    accessCode = preferences.getString(FeedlyApi.SHPREF_KEY_ACCESS_CODE, null);
    userId = preferences.getString(FeedlyApi.SHPREF_KEY_FEEDLY_USER_ID, null);
    currentToken = preferences.getString(FeedlyApi.SHPREF_KEY_ACCESS_TOKEN, null);
    refreshToken = preferences.getString(FeedlyApi.SHPREF_KEY_REFRESH_TOKEN, null);
    tokenExpire = preferences.getLong(FeedlyApi.SHPREF_KEY_TOKEN_EXPIRE, -1);

    if ((currentToken == null) || entryManager.needsSession())
    {
      getAccessToken(accessCode);
    }
  }

  private String buildPinnedTag()
  {
    return "user/" + userId + "/tag/grazerss.pinned";
  }

  private String buildStarTag()
  {
    return "user/" + userId + "/tag/global.saved";
  }

  private String buildUnpinString(List<String> entryIds)
  {
    StringBuffer buf = new StringBuffer(URLEncoder.encode(buildPinnedTag()));
    buf.append("/");

    for (int i = 0; i < entryIds.size(); i++)
    {
      buf.append(URLEncoder.encode(entryIds.get(i)));

      if (i < (entryIds.size() - 1))
      {
        buf.append(",");
      }
    }

    return buf.toString();
  }

  private String buildUnstarString(List<String> entryIds)
  {
    StringBuffer buf = new StringBuffer(URLEncoder.encode(buildStarTag()));
    buf.append("/");

    for (int i = 0; i < entryIds.size(); i++)
    {
      buf.append(URLEncoder.encode(entryIds.get(i)));

      if (i < (entryIds.size() - 1))
      {
        buf.append(",");
      }
    }

    return buf.toString();
  }

  public boolean deleteSubscription(String feedId)
  {
    try {
      Response<okhttp3.ResponseBody> resp = api.deleteSubscription(getAuthHeader(), feedId).execute();
      return resp.isSuccessful();
    } catch (Exception e) {
      return false;
    }
  }

  public boolean getAccessToken(String authCode)
  {
    try {
      Response<ExchangeCodeResponse> response = api.getAccessToken(authCode, FeedlyApi.CLIENT_ID, CLIENT_SECRET, FeedlyApi.REDIRECT_URI, "",
          "authorization_code").execute();
      
      ExchangeCodeResponse resp = response.body();
      if ((resp != null) && (resp.access_token != null))
      {
        storeTokenResponse(resp);
        return true;
      }
    } catch (Exception e) {
      // Handle error
    }

    return false;
  }

  private String getAuthHeader()
  {
    return " OAuth " + currentToken;
  }

  public String getCategories()
  {
    try {
      Response<okhttp3.ResponseBody> resp = api.getCategories(getAuthHeader()).execute();
      return responseToString(resp);
    } catch (Exception e) {
      return null;
    }
  }

  public LatestRead getLatestRead(Long newerThan)
  {
    try {
      Response<LatestRead> response = api.getLatestRead(getAuthHeader(), newerThan).execute();
      return response.body();
    } catch (Exception e) {
      return null;
    }
  }

  public StreamIdsResponse getPinnedIds(boolean newestFirst, Long lastUpdate, Integer maxItems, String continuation)
  {
    try {
      String ranked = newestFirst ? "newest" : "oldest";
      Response<StreamIdsResponse> response = api.getStreamIds(getAuthHeader(), buildPinnedTag(), maxItems, ranked, true, lastUpdate, continuation).execute();
      return response.body();
    } catch (Exception e) {
      return null;
    }
  }

  public StreamContentResponse getSaved(boolean newestFirst, Long lastUpdate, Integer maxItems, String continuation)
  {
    try {
      String ranked = newestFirst ? "newest" : "oldest";
      Response<StreamContentResponse> response = api.getStreamContent(getAuthHeader(), buildStarTag(), maxItems, ranked, false, lastUpdate, continuation).execute();
      return response.body();
    } catch (Exception e) {
      return null;
    }
  }

  public List<Subscriptions> getSubscriptions()
  {
    try {
      Response<List<Subscriptions>> response = api.getSubscriptions(getAuthHeader()).execute();
      return response.body();
    } catch (Exception e) {
      return null;
    }
  }

  public StreamContentResponse getUnread(boolean newestFirst, Long lastUpdate, Integer maxItems, String continuation)
  {
    try {
      String ranked = newestFirst ? "newest" : "oldest";
      Response<StreamContentResponse> response = api.getStreamContent(getAuthHeader(), "user/" + userId + "/category/global.all", maxItems, ranked, true, lastUpdate, continuation).execute();
      return response.body();
    } catch (Exception e) {
      return null;
    }
  }

  public UnreadCountResponse getUnreadCounts()
  {
    try {
      Response<UnreadCountResponse> response = api.getUnreadCounts(getAuthHeader()).execute();
      return response.body();
    } catch (Exception e) {
      return null;
    }
  }

  public StreamContentResponse getUnreadGrazeRSSOnly(boolean newestFirst, Long lastUpdate, Integer maxItems, String continuation)
  {
    try {
      String ranked = newestFirst ? "newest" : "oldest";
      Response<StreamContentResponse> response = api.getStreamContent(getAuthHeader(), "user/" + userId + "/category/grazeten", maxItems, ranked, true, lastUpdate, continuation).execute();
      return response.body();
    } catch (Exception e) {
      return null;
    }
  }

  public boolean isTokenExpired()
  {
    return System.currentTimeMillis() > tokenExpire;
  }

  public boolean isTokenValid()
  {
    return (currentToken != null) && (isTokenExpired() == false);
  }

  public void logout()
  {
    EntryManager entryManager = EntryManager.getInstance(context);
    entryManager.clearAuthToken();
    SharedPreferences preferences = entryManager.getSharedPreferences();

    userId = null;
    currentToken = null;
    refreshToken = null;
    tokenExpire = -1;

    Editor e = preferences.edit();
    e.putString(FeedlyApi.SHPREF_KEY_FEEDLY_USER_ID, null);
    e.putString(FeedlyApi.SHPREF_KEY_ACCESS_TOKEN, null);
    e.putString(FeedlyApi.SHPREF_KEY_REFRESH_TOKEN, null);
    e.putLong(FeedlyApi.SHPREF_KEY_TOKEN_EXPIRE, -1);
    e.commit();
  }

  public boolean markItemsPinned(List<String> entryIds)
  {
    try {
      Response<okhttp3.ResponseBody> resp = api.tagItems(getAuthHeader(), buildPinnedTag(), new TagRequest(entryIds)).execute();
      return resp.isSuccessful();
    } catch (Exception e) {
      return false;
    }
  }

  public boolean markRead(List<String> ids)
  {
    try {
      MarkRequest data = new MarkRequest();
      data.action = "markAsRead";
      data.type = "entries";
      data.entryIds = ids;

      Response<okhttp3.ResponseBody> resp = api.markItems(getAuthHeader(), data).execute();

      return resp.isSuccessful();
    } catch (Exception e) {
      return false;
    }
  }

  public boolean markUnRead(List<String> ids)
  {
    try {
      MarkRequest data = new MarkRequest();
      data.action = "keepUnread";
      data.type = "entries";
      data.entryIds = ids;

      Response<okhttp3.ResponseBody> resp = api.markItems(getAuthHeader(), data).execute();

      return resp.isSuccessful();
    } catch (Exception e) {
      return false;
    }
  }

  public boolean refreshToken()
  {
    try {
      Response<ExchangeCodeResponse> response = api.refreshToken(refreshToken, FeedlyApi.CLIENT_ID, CLIENT_SECRET, "refresh_token").execute();
      ExchangeCodeResponse resp = response.body();

      if ((resp != null) && (resp.access_token != null))
      {
        storeTokenResponse(resp);
        return true;
      }
    } catch (Exception e) {
      // Handle error
    }

    return false;
  }

  private String responseToString(Response<okhttp3.ResponseBody> resp)
  {
    try
    {
      if (resp.body() != null)
      {
        return resp.body().string();
      }
      return null;
    }
    catch (IOException e)
    {
      return null;
    }
  }

  public SearchFeedsResponse searchFeeds(String searchString)
  {
    try {
      Response<SearchFeedsResponse> response = api.searchFeeds(getAuthHeader(), searchString, 50).execute();
      return response.body();
    } catch (Exception e) {
      return null;
    }
  }

  public boolean starItems(List<String> entryIds)
  {
    try {
      Response<okhttp3.ResponseBody> resp = api.tagItems(getAuthHeader(), buildStarTag(), new TagRequest(entryIds)).execute();
      return resp.isSuccessful();
    } catch (Exception e) {
      return false;
    }
  }

  private void storeTokenResponse(ExchangeCodeResponse response)
  {
    EntryManager entryManager = EntryManager.getInstance(context);
    entryManager.saveAuthToken(new AuthToken(AuthToken.AuthType.AUTH, response.access_token));
    entryManager.saveLastSuccessfulLogin();
    SharedPreferences preferences = entryManager.getSharedPreferences();

    userId = response.id;
    currentToken = response.access_token;
    refreshToken = response.refresh_token == null ? refreshToken : response.refresh_token;
    tokenExpire = response.expires_in;

    Editor e = preferences.edit();
    e.putString(FeedlyApi.SHPREF_KEY_FEEDLY_USER_ID, userId);
    e.putString(FeedlyApi.SHPREF_KEY_ACCESS_TOKEN, currentToken);
    e.putString(FeedlyApi.SHPREF_KEY_REFRESH_TOKEN, refreshToken);
    e.putLong(FeedlyApi.SHPREF_KEY_TOKEN_EXPIRE, System.currentTimeMillis() + (response.expires_in * 1000));
    e.commit();
  }

  public boolean subscribeToFeed(String feedId, String title, List<Categories> categories)
  {
    try {
      Response<okhttp3.ResponseBody> resp = api.subscribeToFeed(getAuthHeader(), new SubscribeFeedRequest(feedId, title, categories)).execute();
      return resp.isSuccessful();
    } catch (Exception e) {
      return false;
    }
  }

  public boolean unPinItems(List<String> entryIds)
  {
    try {
      Response<okhttp3.ResponseBody> resp = api.unTagItems(getAuthHeader(), buildUnpinString(entryIds)).execute();
      return resp.isSuccessful();
    } catch (Exception e) {
      return false;
    }
  }

  public boolean unStarItems(List<String> entryIds)
  {
    try {
      Response<okhttp3.ResponseBody> resp = api.unTagItems(getAuthHeader(), buildUnstarString(entryIds)).execute();
      return resp.isSuccessful();
    } catch (Exception e) {
      return false;
    }
  }
}
