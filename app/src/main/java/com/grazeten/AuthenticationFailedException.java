package com.graze16;

public class AuthenticationFailedException extends Exception
{
  private static final long serialVersionUID = -5861326694838181414L;

  public AuthenticationFailedException(String message)
  {
    super(message);
  }
}
