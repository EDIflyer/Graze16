package com.graze16.auth;

public interface IAuthenticationCallback
{

  public void onAuthTokenReceived(String googleAccount, String authToken);

  public void onError(Exception e);
}
