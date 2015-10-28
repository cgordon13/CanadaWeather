package ca.fwe.caweather;

import java.io.File;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import ca.fwe.caweather.backend.CityPageLocationDatabase;
import ca.fwe.caweather.radar.RadarCacheManager;
import ca.fwe.weather.WeatherApp;
import ca.fwe.weather.backend.FilesManager;
import ca.fwe.weather.backend.LocationDatabase;
import ca.fwe.weather.backend.UpdatesReceiver;
import ca.fwe.weather.backend.UserLocationsList;
import ca.fwe.weather.core.ForecastLocation;

public class CanadaWeatherApp extends WeatherApp {



	@Override
	public void onCreate() {
		super.onCreate();

		//clean cache
		FilesManager fm = new FilesManager(this) ;
		RadarCacheManager rcm = new RadarCacheManager(this) ;
		fm.deleteOldCacheFiles();
		rcm.cleanCache();
		
		//ensure things are up-to-date
		Intent i = new Intent(UpdatesReceiver.ACTION_ENSURE_UPDATED) ;
		sendBroadcast(i) ;
	}

	@Override
	public void onUpgrade(int version1, int version2) {
		if(version1 <= 20) {
			//load old locations list
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this) ;
			
			Log.i("CanadaWeatherApp", "upgrading from version 20 (pre rewrite)") ;
			if(prefs.contains("locations")) {
				CityPageLocationDatabase db = new CityPageLocationDatabase(this) ;
				UserLocationsList list = new UserLocationsList(getLocationDatabase(this)) ;
				String[] parts = prefs.getString("locations", null).split(";") ;
				Log.i("CanadaWeatherApp", "found locations list") ;
				for(String part:parts) {
					Log.i("CanadaWeatherApp", "finding sitecode for " + part) ;
					ForecastLocation l = db.getBySitecode(part) ;
					if(l != null) {
						Log.i("CanadaWeatherApp", "adding uri " + l.getUri()) ;
						list.addLocation(l.getUri()) ;
					} else {
						Log.e("CanadaWeatherApp", "could not find sitecode " + part) ;
						//won't work for super old versions but should work for most lists
					}
				}
				list.close();
				SharedPreferences.Editor edit = prefs.edit() ;
				edit.remove("locations") ;
				edit.commit() ;
			}

			//clear all cache files
			File dir = this.getCacheDir() ;
			File cache = new File(dir, "cache_forecast") ;
			if(cache.isDirectory()) {
				File[] files = cache.listFiles() ;
				if(files != null) {
					for(File f: files) {
						f.delete() ;
						Log.i("CanadaWeatherApp", "deleted " + f) ;
					}
				}
			}
			
			//annnd the old radar cache
			File radarCache = new File(dir, "cache_radar") ;
			if(radarCache.isDirectory()) {
				File[] files = radarCache.listFiles() ;
				if(files != null) {
					for(File f: files) {
						f.delete() ;
						Log.i("CanadaWeatherApp", "deleted " + f) ;
					}
				}
			}
			
			Log.i("CanadaWeatherApp", "upgrade complete") ;

		}
		
	}

	@Override
	public LocationDatabase getLocationDatabase(Context context) {
		return new CityPageLocationDatabase(context) ;
	}

}
