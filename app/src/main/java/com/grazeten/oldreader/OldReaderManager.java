package com.grazeten.oldreader;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.Response;

import com.grazeten.AuthenticationFailedException;
import com.grazeten.oldreader.ItemsResponse.Item;

public class OldReaderManager
{
  private static final String FOLDER_SEARCH_PREFIX = "user/-/label/";
  // private static final String SERVER = "http://10.1.0.110:8080";
  private static final String SERVER               = "https://theoldreader.com";
  private OldReaderApi        api                  = new Retrofit.Builder()
      .baseUrl(SERVER)
      .addConverterFactory(GsonConverterFactory.create())
      .build()
      .create(OldReaderApi.class);
  private String              authToken            = null;

  private String getAuthHeader()
  {
    return "GoogleLogin auth=" + authToken;
  }

  public ItemContentResponse getItemContents(ItemsResponse itemResp)
  {
    try {
      StringBuilder items = new StringBuilder();

      for (Item item : itemResp.itemRefs)
      {
        if (items.length() == 0)
        {
          items.append(item.id);
        }
        else
        {
          items.append("&i=" + item.id);
        }
      }

      Response<ItemContentResponse> response = api.getItemContents(getAuthHeader(), items.toString()).execute();
      return response.body();
    } catch (Exception e) {
      return null;
    }
  }

  public SubscriptionResponse getSubscriptionList()
  {
    try {
      Response<SubscriptionResponse> response = api.getSubscriptionList(getAuthHeader()).execute();
      return response.body();
    } catch (Exception e) {
      return null;
    }
  }

  public TagResponse getTagList()
  {
    try {
      Response<TagResponse> response = api.getTagList(getAuthHeader()).execute();
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

  public ItemsResponse getUnreadItems(String continuation, String folderName, boolean newestFirst, Long lastUpdate, Integer maxItems)
  {
    String direction = null;
    String ot = null;
    String nt = null;

    if (newestFirst)
    {
      ot = lastUpdate.toString();
    }
    else
    {
      direction = "o";
      nt = lastUpdate.toString();
    }

    try {
      if (folderName == null)
      {
        Response<ItemsResponse> response = api.getUnreadItems(getAuthHeader(), continuation, direction, nt, ot, maxItems).execute();
        return response.body();
      }
      else
      {
        Response<ItemsResponse> response = api.getUnreadItemsInFolder(getAuthHeader(), continuation, FOLDER_SEARCH_PREFIX + folderName, direction, nt, ot, maxItems).execute();
        return response.body();
      }
    } catch (Exception e) {
      return null;
    }
  }

  public boolean haveAuthToken()
  {
    return authToken != null;
  }

  public LoginResp login(String email, String password) throws AuthenticationFailedException
  {
    try {
      Response<LoginResp> response = api.login("GrazeTEN", "HOSTED_OR_GOOGLE", "reader", email, password, "json").execute();
      LoginResp data = response.body();
      
      if (data != null) {
        authToken = data.Auth;
      }

      if ((authToken == null) || (data != null && data.errors != null))
      {
        throw new AuthenticationFailedException(data != null && data.errors != null ? data.errors.get(0) : "Authentication failed");
      }

      return data;
    } catch (Exception e) {
      throw new AuthenticationFailedException(e.getMessage());
    }
  }

  public Response<okhttp3.ResponseBody> updateArticles(UpdateArticlesRequest update)
  {
    try {
      return api.updateArticles(getAuthHeader(), update).execute();
    } catch (Exception e) {
      return null;
    }
  }
}
