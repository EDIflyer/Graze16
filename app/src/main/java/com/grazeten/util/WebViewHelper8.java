package com.graze16.util;

import android.webkit.WebSettings;
import android.webkit.WebSettings.PluginState;
import android.webkit.WebView;

import com.graze16.EntryManager;
import com.graze16.PL;

public class WebViewHelper8
{
  public static void setupWebView(EntryManager entryManager, WebView webView)
  {
    WebSettings settings = webView.getSettings();
    String state = entryManager.getPlugins();
    settings.setPluginState(PluginState.valueOf(state));
    PL.log("SetupWebView. Plugin State=" + settings.getPluginState(), webView.getContext());
    settings.setJavaScriptEnabled(true);
    settings.setAllowFileAccess(true);
  }
}
