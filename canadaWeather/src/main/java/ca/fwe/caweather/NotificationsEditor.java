package ca.fwe.caweather;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;
import ca.fwe.weather.LocationPickerActivity;
import ca.fwe.weather.WeatherApp;
import ca.fwe.weather.backend.LocationDatabase;
import ca.fwe.weather.backend.NotificationsReceiver;
import ca.fwe.weather.backend.UpdatesManager;
import ca.fwe.weather.backend.UpdatesReceiver;
import ca.fwe.weather.core.ForecastLocation;

public class NotificationsEditor extends ListActivity implements OnItemClickListener {

	private static final int REQUEST_LOCATION = 11 ;

	private int lang ;
	private LocationsAdapter adapter ;
	private UpdatesManager uManager ;
	private LocationDatabase locDb ;
	private NoDataAdapter noDataAdapter ;

	public void onCreate(Bundle savedInstanceState) {
		this.setTheme(WeatherApp.getThemeId(this)) ;
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.notification_editor);

		lang = WeatherApp.getLanguage(this) ;
		adapter = new LocationsAdapter() ;
		uManager = new UpdatesManager(this) ;
		locDb = ((WeatherApp)this.getApplication()).getLocationDatabase(this) ;
		noDataAdapter = new NoDataAdapter() ;
		getListView().setOnItemClickListener(this);

		if(getActionBar() != null) getActionBar().setDisplayHomeAsUpEnabled(true);

		this.reloadList();
	}

	private void reloadList() {
		List<Uri> uris = uManager.getNotificationUpdateUris() ;
		if(uris.size() > 0) {
			List<ForecastLocation> locations = new ArrayList<>() ;
			for(Uri u: uris) {
				ForecastLocation l = locDb.getLocation(u) ;
				if(l != null) {
					locations.add(l) ;
				} else {
					Log.e("NotificationsEditor", "uri not found in location database! " + u) ;
				}
			}
			adapter.reset(locations);
		} else {
			getListView().setAdapter(noDataAdapter);
		}
	}

	private void confirmRemove(final ForecastLocation l) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this) ;
		builder.setTitle(R.string.notifications_remove_title) ;
		builder.setMessage(String.format(getString(R.string.notifications_remove_message), l.getName(lang))) ;
		builder.setNegativeButton(R.string.cancel, null) ;
		builder.setPositiveButton(R.string.notifications_remove, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				uManager.removeNotification(l.getUri());
				Intent i = new Intent(NotificationsReceiver.ACTION_NOTIFICATION_REMOVED) ;
				i.setData(l.getUri()) ;
				sendBroadcast(i) ;
				reloadList() ;
			}
		}) ;
		builder.create().show();
	}



	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		this.getMenuInflater().inflate(R.menu.notifiation_editor, menu);
		return true ;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId() ;
		if(id == android.R.id.home) {
			this.onBackPressed();
			return true ;
		} else if(id == R.id.menu_add) {
			Intent i = new Intent(this, LocationPickerActivity.class) ;
			startActivityForResult(i, REQUEST_LOCATION) ;
			return true ;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}


	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		log("receiving result with code " + requestCode + " and result " + resultCode) ;
		if(requestCode == REQUEST_LOCATION) {
			if(resultCode == RESULT_OK) {
				if(data != null) {
					Uri uri = data.getData() ;
					if(uri != null) {
						uManager.addNotification(uri);
						reloadList() ;
						Intent bIntent = new Intent(UpdatesReceiver.ACTION_ENSURE_UPDATED) ;
						sendBroadcast(bIntent) ;
					} else {
						toast(R.string.forecast_error_location_request) ;
						log("result back from location browser with no uri") ;
					}
				} else {
					toast(R.string.forecast_error_location_request) ;
					log("result back from location browser with no data intent") ;
				}
			}
		} else {
			super.onActivityResult(requestCode, resultCode, data);
		}
	}


	private void toast(int messageId) {
		Toast.makeText(this, messageId, Toast.LENGTH_SHORT).show();
	}

	private static void log(String message) {
		Log.i("NotificationsEditor", message) ;
	}

	private class LocationsAdapter extends ArrayAdapter<ForecastLocation> {

		public LocationsAdapter() {
			super(NotificationsEditor.this, android.R.layout.simple_list_item_1) ;
		}

		public void reset(List<? extends ForecastLocation> newList) {
			this.clear();
			this.addAll(newList);
			getListView().setAdapter(this);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			TextView v = (TextView) super.getView(position, convertView, parent);
			this.modifyTextView(this.getItem(position), v);
			return v ;
		}

		protected void modifyTextView(ForecastLocation object, TextView v) {
			v.setText(object.toString(lang));
		}

	}

	private class NoDataAdapter extends ArrayAdapter<String> {

		public NoDataAdapter() {
			super(NotificationsEditor.this, android.R.layout.simple_list_item_1);
			this.add(getString(R.string.notifications_nodata_message));
		}

	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		if(adapter != null && position < adapter.getCount()) {
			this.confirmRemove(adapter.getItem(position));
		} else {
			// no data adapter
            Log.i("NotificationsEditor", "no data adapter item click");
		}
	}

}
