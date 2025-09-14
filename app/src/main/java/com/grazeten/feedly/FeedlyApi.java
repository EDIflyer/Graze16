package com.graze16.feedly;

import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface FeedlyApi
{
  // public static final String BASE_URL = "http://10.1.0.110:8080";
  public static final String BASE_URL                  = "https://cloud.feedly.com";
  public static final String AUTH_URL                  = BASE_URL + "/v3/auth/auth";

  public static final String CLIENT_ID                 = "graze16";
  public static final String REDIRECT_URI              = "http://localhost";

  public static final String SHPREF_KEY_FEEDLY_USER_ID = "Feedly_User_Id";
  public static final String SHPREF_KEY_ACCESS_CODE    = "Feedly_Access_Code";
  public static final String SHPREF_KEY_ACCESS_TOKEN   = "Feedly_Access_Token";
  public static final String SHPREF_KEY_REFRESH_TOKEN  = "Feedly_Refresh_Token";
  public static final String SHPREF_KEY_TOKEN_EXPIRE   = "Feedly_Access_Token_Expire";

  @DELETE("/v3/subscriptions/{feedId}")
  public Call<ResponseBody> deleteSubscription(@Header("Authorization") String authToken, @Path("feedId") String feedId);

  @POST("/v3/auth/token")
  public Call<ExchangeCodeResponse> getAccessToken(@Query("code") String authCode, @Query("client_id") String clientId,
      @Query("client_secret") String clientSecret, @Query("redirect_uri") String redirectUri, @Query("state") String state,
      @Query("grant_type") String grantType);

  @GET("/v3/categories")
  public Call<ResponseBody> getCategories(@Header("Authorization") String authToken);

  @GET("/v3/markers/reads")
  public Call<LatestRead> getLatestRead(@Header("Authorization") String authToken, @Query("newerThan") Long newerThan);

  @GET("/v3/streams/contents")
  public Call<StreamContentResponse> getStreamContent(@Header("Authorization") String authToken, @Query("streamId") String streamId,
      @Query("count") Integer count, @Query("ranked") String ranked, @Query("unreadOnly") Boolean unreadOnly,
      @Query("newerThan") Long timestamp, @Query("continuation") String continuation);

  @GET("/v3/streams/ids")
  public Call<StreamIdsResponse> getStreamIds(@Header("Authorization") String authToken, @Query("streamId") String streamId,
      @Query("count") Integer count, @Query("ranked") String ranked, @Query("unreadOnly") Boolean unreadOnly,
      @Query("newerThan") Long timestamp, @Query("continuation") String continuation);

  @GET("/v3/subscriptions")
  public Call<List<Subscriptions>> getSubscriptions(@Header("Authorization") String authToken);

  @GET("/v3/markers/counts")
  public Call<UnreadCountResponse> getUnreadCounts(@Header("Authorization") String authToken);

  @POST("/v3/markers")
  public Call<ResponseBody> markItems(@Header("Authorization") String authToken, @Body MarkRequest data);

  @POST("/v3/auth/token")
  public Call<ExchangeCodeResponse> refreshToken(@Query("refresh_token") String refreshToken, @Query("client_id") String clientId,
      @Query("client_secret") String clientSecret, @Query("grant_type") String grantType);

  @GET("/v3/search/feeds")
  public Call<SearchFeedsResponse> searchFeeds(@Header("Authorization") String authToken, @Query("q") String searchString, @Query("n") Integer count);

  @POST("/v3/subscriptions")
  public Call<ResponseBody> subscribeToFeed(@Header("Authorization") String authToken, @Body SubscribeFeedRequest request);

  @PUT("/v3/tags/{tagList}")
  public Call<ResponseBody> tagItems(@Header("Authorization") String authToken, @Path("tagList") String tagList, @Body TagRequest tagRequest);

  @DELETE("/v3/tags/{tagList}")
  public Call<ResponseBody> unTagItems(@Header("Authorization") String authToken, @Path("tagList") String tagList);
}
