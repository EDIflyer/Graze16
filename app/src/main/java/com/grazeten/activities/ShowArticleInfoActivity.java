package com.graze16.activities;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import com.graze16.Entry;
import com.graze16.EntryManager;
import com.graze16.R;

public class ShowArticleInfoActivity extends Activity
{
  public static final String EXTRA_ATOM_ID = "extra_atom_id";

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.show_article_info);

    String atomId = getIntent().getExtras().getString(EXTRA_ATOM_ID);
    if (atomId == null)
      finish();

    Entry entry = EntryManager.getInstance(this).findEntryByAtomId(atomId);
    if (entry == null)
      finish();

    TextView textView = (TextView) findViewById(R.id.error);
    textView.setText("" + entry.getError() + " (" + atomId + ")");

  }
}
