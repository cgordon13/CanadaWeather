package ca.fwe.weather;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.RemoteViews;
import ca.fwe.weather.backend.LocationDatabase;
import ca.fwe.weather.backend.UpdatesManager;
import ca.fwe.weather.backend.UpdatesReceiver;
import ca.fwe.weather.core.Forecast;
import ca.fwe.weather.core.ForecastLocation;
import ca.fwe.weather.core.Units;
import ca.fwe.weather.core.Units.UnitSet;
import ca.fwe.weather.util.ForecastDownloader;
import ca.fwe.weather.util.ForecastDownloader.Modes;
import ca.fwe.weather.util.ForecastDownloader.ReturnTypes;

public abstract class ForecastWidgetProvider extends AppWidgetProvider {

	@Override
	public void onReceive(Context context, Intent intent) {
		log("receving intent with action " + intent.getAction()) ;
		if(intent.getAction().equals(ForecastDownloader.ACTION_FORECAST_DOWNLOADED)) {
			Uri data = intent.getData() ;
			log("received broadcast forecast downloaded intent with data " + data) ;
			if(data != null) {
				UpdatesManager updatesManager = new UpdatesManager(context) ;
				LocationDatabase locDb = this.getLocationDatabase(context) ;
				int[] widgetIds = updatesManager.getWidgetIds(data) ;
				AppWidgetManager manager = AppWidgetManager.getInstance(context) ;
				if(widgetIds.length > 0) {
					for(int widgetId: widgetIds)
						this.updateWidget(context, updatesManager, locDb, manager, widgetId, Modes.LOAD_CACHED);
				} else {
					log("no widgets in database with uri " + data) ;
				}
			} else {
				//do nothing
			}
		} else {
			super.onReceive(context, intent);
		}
	}

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		//just send broadcast of ACTION_FORCE_UPDATE, onUpdate gets called when the app
		//is installed, when device turns on, etc.
		log("onUpdate received, sending ACTION_ENSURE_UPDATED") ;
		Intent i = new Intent(UpdatesReceiver.ACTION_ENSURE_UPDATED) ;
		context.sendBroadcast(i) ;
	}

	@Override
	public void onDeleted(Context context, int[] appWidgetIds) {
		UpdatesManager manager = new UpdatesManager(context) ;
		for(int widgetId: appWidgetIds) {
			manager.removeWidget(widgetId);
		}
		Intent i = new Intent(UpdatesReceiver.ACTION_FORCE_UPDATE_ALL) ;
		context.sendBroadcast(i) ;
	}

	private void updateWidget(final Context context, UpdatesManager updatesManager, LocationDatabase locDb,
			final AppWidgetManager manager, final int widgetId, Modes downloadMode) {
		int lang = WeatherApp.getLanguage(context) ;
		Uri uri = updatesManager.getForecastLocation(widgetId) ;
		if(uri != null) {
			ForecastLocation l = locDb.getLocation(uri) ;
			if(l != null) {
				UnitSet unitset = Units.getUnitSet(WeatherApp.prefs(context).getString(WeatherApp.PREF_KEY_UNITS, Units.UNITS_DEFAULT)) ;
				final Forecast forecast = new Forecast(context, l, lang);
				forecast.setUnitSet(unitset);
				ForecastDownloader downloader = new ForecastDownloader(forecast, new ForecastDownloader.OnForecastDownloadListener() {
					@Override
					public void onForecastDownload(Forecast forecast, Modes mode, ReturnTypes result) {
						log("forecast downloaded with return type: " + result) ;
						boolean error = false ;
						switch(result) {
						case IO_ERROR:
							error = true ;
							break;
						case XML_ERROR:
							error = true ;
							break ;
						case UNKNOWN_ERROR:
							error = true ;
							break;
						case NO_CACHED_FORECAST_ERROR:
							error = true ;
							break ;
						default:
							error = false ;
							break;
						}
						putForecast(context, manager, widgetId, forecast, error) ;
					}
				}, downloadMode) ;
				downloader.download();
			} else {
				log("no location found for uri " + uri + " and widget " + widgetId) ;
			}
		} else {
			log("widget not yet registered! id " + widgetId) ;
		}
	}

	private void putForecast(Context context, AppWidgetManager manager, int widgetId, Forecast forecast, boolean error) {
		RemoteViews view = this.createWidgetView(context, forecast, error) ;
		if(view != null)
			manager.updateAppWidget(widgetId, view);
	}

	protected abstract LocationDatabase getLocationDatabase(Context context) ;
	protected abstract RemoteViews createWidgetView(Context context, Forecast forecast, boolean error) ;

	private static void log(String message) {
		Log.i("ForecastWidgetProvider", message) ;
	}

}
