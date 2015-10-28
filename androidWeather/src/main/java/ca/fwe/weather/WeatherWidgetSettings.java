package ca.fwe.weather;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import ca.fwe.weather.backend.LocationDatabase;
import ca.fwe.weather.core.ForecastLocation;

public abstract class WeatherWidgetSettings extends Activity {

	protected static final int REQUEST_LOCATION = 11 ;
	
	private WeatherApp app ;
	private LocationDatabase locDb ;
	
	private ForecastLocation location ;
	protected int lang ;
	private int widgetId ;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		app = (WeatherApp)this.getApplication() ;
		lang = WeatherApp.getLanguage(this) ;
		locDb = app.getLocationDatabase(this) ;
		this.setResult(RESULT_CANCELED);
		widgetId = this.getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ;
		if(widgetId == -1) {
			//no widget id, can't add widget
			log("no widget id found! quitting.", null) ;
			this.finish();
		}
	}
	
	protected int getWidgetId() {
		return widgetId ;
	}
	
	protected void setLocation(ForecastLocation location) {
		if(location != null) {
			log("setting location to " + location.getUri()) ;
		} else {
			log("setting location to null") ;
		}
		this.location = location ;
	}
	
	protected ForecastLocation getLocation() {
		return location ;
	}
		
	protected void launchLocationPicker() {
		Intent i = new Intent(this, LocationPickerActivity.class) ;
		this.startActivityForResult(i, REQUEST_LOCATION);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(requestCode == REQUEST_LOCATION) {
			if(resultCode == RESULT_OK) {
				if(data != null) {
					Uri uri = data.getData() ;
					if(uri != null) {
						ForecastLocation l = locDb.getLocation(uri) ;
						if(l != null) {
							this.setLocation(l);
						} else {
							//location not found
							log("location not found in database! " + uri, null) ;
						}
					} else {
						//no uri
						log("no uri from locationpicker!", null) ;
					}
				} else {
					//null data
					log("no data from locationpicker!", null) ;
				}
			} else {
				//cancelled, no location
				this.setLocation(null);
			}
		} else {
			super.onActivityResult(requestCode, resultCode, data);
		}
	}
	
	private static void log(String message) {
		Log.i("WeatherWidgetSettings", message) ;
	}
	
	private static void log(String message, Exception error) {
		if(error != null) {
			Log.e("WeatherWidgetSetings", message, error) ;
		} else {
			Log.e("WeatherWidgetSettings", message) ;
		}
	}
	
}
