package com.metaio.cloud.plugin.view;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.MenuItem;

import com.metaio.R;


public class POIDetailDialog extends FragmentActivity

{
	POIDetailFragment mFragment;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.poiactivity);
		mFragment = (POIDetailFragment)getSupportFragmentManager().findFragmentById(R.id.poidetailfragment);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		if (item.getItemId() == android.R.id.home)
		{
			setResult(RESULT_CANCELED);
			finish();
		}
		return super.onOptionsItemSelected(item);
	}
}
