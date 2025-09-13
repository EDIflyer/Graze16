package com.grazeten.oldreader;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface OldReaderApi
{
  @GET("/reader/api/0/stream/items/contents?output=json")
  public Call<ItemContentResponse> getItemContents(@Header("Authorization") String authToken, @Query("i") String idList);

  @GET("/reader/api/0/subscription/list?output=json")
  public Call<SubscriptionResponse> getSubscriptionList(@Header("Authorization") String authToken);

  @GET("/reader/api/0/tag/list?output=json")
  public Call<TagResponse> getTagList(@Header("Authorization") String authToken);

  @GET("/reader/api/0/unread-count?output=json")
  public Call<UnreadCountResponse> getUnreadCounts(@Header("Authorization") String authToken);

  @GET("/reader/api/0/stream/items/ids?output=json;s=user/-/state/com.google/reading-list;xt=user/-/state/com.google/read")
  public Call<ItemsResponse> getUnreadItems(@Header("Authorization") String authToken, @Query("c") String continuation, @Query("r") String direction,
      @Query("nt") String newer, @Query("ot") String older, @Query("n") Integer maxItems);

  @GET("/reader/api/0/stream/items/ids?output=json;xt=user/-/state/com.google/read")
  public Call<ItemsResponse> getUnreadItemsInFolder(@Header("Authorization") String authToken, @Query("c") String continuation,
      @Query("s") String labelSearchString, @Query("r") String direction, @Query("nt") String newer, @Query("ot") String older,
      @Query("n") Integer maxItems);

  @FormUrlEncoded
  @POST("/accounts/ClientLogin")
  public Call<LoginResp> login(@Field("client") String client, @Field("accountType") String type, @Field("service") String service,
      @Field("Email") String email, @Field("Passwd") String password, @Field("output") String output);

  @POST("/reader/api/0/edit-tag?output=json")
  public Call<ResponseBody> updateArticles(@Header("Authorization") String authToken, @Body UpdateArticlesRequest update);
}
