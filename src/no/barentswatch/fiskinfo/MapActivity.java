/**
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
package no.barentswatch.fiskinfo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.json.JSONException;
import org.json.JSONObject;

import no.barentswatch.baseclasses.Point;
import no.barentswatch.baseclasses.ToolsGeoJson;
import no.barentswatch.implementation.FiskInfoPolygon2D;
import no.barentswatch.implementation.FiskInfoUtility;
import no.barentswatch.implementation.FiskinfoScheduledTaskExecutor;
import no.barentswatch.implementation.GpsLocationTracker;
import no.barentswatch.implementation.ToolsInfo;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Vibrator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.webkit.GeolocationPermissions;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import ch.boye.httpclientandroidlib.client.methods.CloseableHttpResponse;
import ch.boye.httpclientandroidlib.client.methods.HttpGet;
import ch.boye.httpclientandroidlib.impl.client.CloseableHttpClient;
import ch.boye.httpclientandroidlib.impl.client.HttpClients;
import ch.boye.httpclientandroidlib.util.EntityUtils;

public class MapActivity extends BaseActivity {
	private WebView browser;
	private boolean alarmSet = false;
	private static int currentlySelected = 0;
	private final double minLenghtMeters = 100;
	private final double maxLenghtMeters = 1852;
	private final double minLenghtNauticalMiles = minLenghtMeters / 1852;
	private final double maxLenghtNauticalMiles = maxLenghtMeters / 1852;
	private final double stepSizeMeters = (maxLenghtMeters - minLenghtMeters) / 100;
	private final double stepSizeNauticalMiles = (maxLenghtNauticalMiles - minLenghtNauticalMiles) / 100;
	private GpsLocationTracker mGpsLocationTracker;
	private boolean alarmFiring = false;
	private FiskInfoPolygon2D tools = null;
	private boolean cacheDeserialized = false;
	private ToolsGeoJson mTools = null;

	/*
	 * these value refer to the index of the units in the string array
	 * 'measurement_units' and are only here so we don't need to look them up
	 * every time we update the seek bar.
	 */
	private final int meterIndex = 0;
	private final int nauticalMileIndex = 1;

	protected AsyncTask<String, String, byte[]> cacheWriter;
	protected double cachedLat;
	protected double cachedLon;
	protected String cachedDistance;

	public MediaPlayer mediaPlayer = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.setContext(this);
		mTools = new ToolsGeoJson(getContext());
		setGeoJsonFile(null);
		super.onCreate(savedInstanceState);
		getMapTools();
		setContentView(R.layout.activity_map);
		configureWebParametersAndLoadDefaultMapApplication();
	}

	@SuppressLint({ "SetJavaScriptEnabled" })
	private void configureWebParametersAndLoadDefaultMapApplication() {
		browser = new WebView(getContext());
		browser = (WebView) findViewById(R.id.browserWebView);
		browser.getSettings().setJavaScriptEnabled(true);
		browser.getSettings().setDomStorageEnabled(true);
		browser.getSettings().setGeolocationEnabled(true);
		browser.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);

		browser.addJavascriptInterface(new JavaScriptInterface(getContext()), "Android");
		browser.setWebViewClient(new barentswatchFiskInfoWebClient());
		browser.setWebChromeClient(new WebChromeClient() {

			public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
				Log.d("geolocation permission", "permission >>>" + origin);
				callback.invoke(origin, true, false);
			}

			@Override
			public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
				Log.i("my log", "Alert box popped");
				return super.onJsAlert(view, url, message, result);
			}
		});
		updateMapTools();
		browser.loadUrl("file:///android_asset/mapApplication.html");

	}

	private class barentswatchFiskInfoWebClient extends WebViewClient {
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			view.loadUrl(url);
			return true;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.

		getMenuInflater().inflate(R.menu.map, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Context currentContext = getContext();
		// Handle item selection
		switch (item.getItemId()) {
		// case R.id.register_misc:
		// registerItemAndToolUsed(currentContext);
		// return true;
		case R.id.update_map:
			loadView(MapActivity.class);
			return true;
		case R.id.zoom_to_user_position:
			browser.loadUrl("javascript:zoomToUserPosition()");
			return true;
		case R.id.export_metadata_to_user:
			exportMapLayerToUser(currentContext);
			return true;
		case R.id.symbol_explanation:
			displaySymbolExplanation(currentContext);
			return true;
			// case R.id.ocean_currents:
			// String OMFG = displayCurrentOceanCurrents(currentContext);
			// browser.loadData(OMFG, "image/svg+xml", "UTF-8");
			// browser.getSettings().setSupportZoom(true);
			// browser.getSettings().setBuiltInZoomControls(true);
			// return true;
		case R.id.setProximityAlert:
			setProximityAlertDialog(currentContext);
			return true;
		case R.id.check_polar_low:
			showPolarLowDialog();
			return true;
		case R.id.choose_map_layers:
			showMapLayersDialog();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * 
	 */
	public void showMapLayersDialog() {
		final Dialog dialog = new Dialog(this);
		dialog.requestWindowFeature(Window.FEATURE_LEFT_ICON);
		dialog.setContentView(R.layout.dialog_select_map_layers);

		final LinearLayout mapLayerLayout = (LinearLayout) dialog.findViewById(R.id.map_layers_checkbox_layout);
		Button okButton = (Button) dialog.findViewById(R.id.dismiss_dialog_button);
		Button cancelButton = (Button) dialog.findViewById(R.id.go_to_map_button);

		for(int i = 0; i < 5; i++) {
			View mapLayerRow = getMapLayerCheckBoxRow(getContext(), Integer.toString(i));
			mapLayerLayout.addView(mapLayerRow);
		}
		
		okButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {

				for(int i = 0; i < mapLayerLayout.getChildCount(); i++) {
					if(((CheckBox)((TableRow)mapLayerLayout.getChildAt(i)).getChildAt(0)).isChecked()) {
						// TODO: Add layer to list
						
					}
				}
				// TODO: Implement logic for adding map layers here.				
				
				
				
				dialog.dismiss();
			}
		});

		cancelButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dialog.dismiss();
			}
		});

		dialog.setTitle(R.string.choose_map_layers);
		dialog.setCanceledOnTouchOutside(false);
		dialog.show();
	}
	
	
	public View getMapLayerCheckBoxRow(Context context, String mapLayerName) {
		TableRow tr = new TableRow(context);
		View v = LayoutInflater.from(context).inflate(R.layout.map_layer_check_box_row, tr, false);
		final int tablePadding = 5;
		TextView textView = (TextView) v.findViewById(R.id.map_layer_row_text_view);
		textView.setText(mapLayerName);
		v.setPadding(tablePadding, tablePadding, tablePadding, tablePadding);
		
		return v;
	}
	
	/**
	 * 
	 * @param activityContext
	 *            The context of the current activity
	 */
	@SuppressLint("InflateParams")
	public void setProximityAlertDialog(Context activityContext) {
		LayoutInflater layoutInflater = getLayoutInflater();
		View view = layoutInflater.inflate(R.layout.dialog_scheduled_task_executor, null);
		final AlertDialog builder = new AlertDialog.Builder(activityContext).create();
		builder.setTitle(R.string.map_set_proximity_alert_title);
		builder.setView(view);

		final EditText distanceEditText = (EditText) view.findViewById(R.id.scheduledProximityRangeEditText);
		final SeekBar seekbar = (SeekBar) view.findViewById(R.id.scheduledSetProximityRangeSeekBar);
		distanceEditText.setText(String.valueOf(minLenghtMeters));

		final Spinner measuringUnitSpinner = (Spinner) view.findViewById(R.id.scheduledMeasuringUnitsSpinner);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.measurement_units,
				android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		measuringUnitSpinner.setAdapter(adapter);

		seekbar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if (fromUser == true) {
					if (currentlySelected == meterIndex) {
						distanceEditText.setText(String.valueOf((int) (minLenghtMeters + (stepSizeMeters * progress))));
					} else if (currentlySelected == nauticalMileIndex) {
						distanceEditText.setText(String.valueOf(minLenghtNauticalMiles + (stepSizeNauticalMiles * progress)));
					}
				}
			}
		});

		measuringUnitSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

				double distance = (distanceEditText.length() != 0 ? Double.parseDouble(distanceEditText.getText().toString()) : 0);

				System.out.println("posistion: " + position);
				System.out.println("currentlySelected: " + position);

				if (position == meterIndex) {
					if (position != currentlySelected) {
						distance = convertDistance(distance, position);
						distanceEditText.setText(String.valueOf(distance));
						currentlySelected = position;
					}
				} else if (position == nauticalMileIndex) {
					if (position != currentlySelected) {
						distance = convertDistance(distance, position);
						distanceEditText.setText(String.valueOf(distance));
						currentlySelected = position;
					}
				} else {
					return;
				}
				System.out.println("distance: " + distance);

				int newProgress = findProgress(distance, position);
				seekbar.setProgress(newProgress);
				System.out.println("progress: " + newProgress);
				distanceEditText.setText(String.valueOf(distance));
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});

		Button setProximityAlertButton = (Button) view.findViewById(R.id.scheduledSetProximityCheckerDialogButton);
		Button cancelButton = (Button) view.findViewById(R.id.cancel_button);

		builder.setView(view);
		builder.setCanceledOnTouchOutside(false);

		setProximityAlertButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {

				if (!alarmSet) {
					mGpsLocationTracker = new GpsLocationTracker(getContext());
					double latitude, longitude = 0;
					if (mGpsLocationTracker.canGetLocation()) {
						latitude = mGpsLocationTracker.getLatitude();
						cachedLat = latitude;
						longitude = mGpsLocationTracker.getLongitude();
						cachedLon = longitude;
					} else {
						mGpsLocationTracker.showSettingsAlert();
						return;
					}
					String distance = distanceEditText.getText().toString();
					cachedDistance = distance;

					cacheWriter = new DownloadMapLayerFromBarentswatchApiInBackground().execute("fishingfacility", "OLEX", "cachedResults",
							String.valueOf(longitude), String.valueOf(latitude), distance, "true");
					alarmSet = true;
					runScheduledAlarm();
				}

				builder.dismiss();
			}
		});

		cancelButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				builder.cancel();

			}
		});

		builder.show();
	}
	
	

	public void showProximityAlertDialog(Context activityContext) {
		LayoutInflater layoutInflater = getLayoutInflater();
		View view = layoutInflater.inflate(R.layout.dialog_proximity_alert, null);
		final AlertDialog builder = new AlertDialog.Builder(activityContext).create();
		builder.setTitle(R.string.map_proximity_alert_title);
		builder.setView(view);

		Button goToMyPositionButton = (Button) view.findViewById(R.id.proximityAlertViewPositionButton);
		Button cancelProximityAlert = (Button) view.findViewById(R.id.proximityAlertCancelButton);
		builder.setView(view);

		goToMyPositionButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				builder.dismiss();
				browser.loadUrl("javascript:zoomToUserPosition()");
			}
		});

		cancelProximityAlert.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO: stop alert

				builder.dismiss();
			}
		});

		builder.show();
	}

	/**
	 * Notifies the user through vibration and sound that he is on collision
	 * course with a object.
	 * TODO: show dialog and allow user to turn off alarm. Until then, only make phone vibrate.
	 * 
	 */
	private void notifyUserOfProximityAlert() {
		Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		long[] pattern = { 0, // Start immediately
				500, 200, 500, 200, 500, 200, 500, 200, 500, 200, 500, 200, 500, 200, 500, 200, 500, 200, 500, 200, 500, 200, 500, 200 };

		vibrator.vibrate(pattern, -1);

//		MediaPlayer mediaPlayer = MediaPlayer.create(getContext(), R.raw.terran_2);
//		if (mediaPlayer == null) {
//			return;
//		}
//		mediaPlayer.start();

	}

	/**
	 * Calculates what the progress of the seek bar should be based on the given
	 * distance and measurement unit.
	 * 
	 * @param distance
	 *            the distance used to determine the progress. If outside the
	 *            min-max bounds, it returns the minimum or maximum value.
	 * @param unit
	 *            the measurement unit used to determine the progress <= -1:
	 *            unsupported unit 0: meters 1: nautical miles >= 2: unsupported
	 *            unit
	 * @return the new position of the progress indicator for the seek bar.
	 */
	protected int findProgress(double distance, int unit) {
		int progress = 0;

		if (unit == meterIndex) {
			if (distance <= minLenghtMeters) {
				return 0;
			} else if (distance >= maxLenghtMeters) {
				return 100;
			}
			progress = (int) ((distance - minLenghtMeters) / stepSizeMeters);
			return progress;
		} else if (unit == nauticalMileIndex) {
			if (distance <= minLenghtNauticalMiles) {
				return 0;
			} else if (distance >= maxLenghtNauticalMiles) {
				return 100;
			}
			progress = (int) ((distance - minLenghtNauticalMiles) / stepSizeNauticalMiles);
			return progress;
		} else {
			return 0;
		}
	}

	/**
	 * This function converts meters to NauticalMiles and vice versa
	 * 
	 * @param distance
	 *            The distance to convert
	 * @param conversion
	 *            what to convert <= -1: Unsupported conversion, returns null.
	 *            0: Converts the given distance from nautical miles to meters.
	 *            1: Converts the given distance from meters to nautical miles.
	 *            >= 2: Unsupported conversion, returns null.
	 * @return the distance converted to the new unit of measurement
	 */
	protected double convertDistance(double distance, int conversion) {
		double tmp;
		if (conversion == 0) {
			tmp = distance * 1852;
			if (tmp >= maxLenghtMeters) {
				return maxLenghtMeters;
			} else if (tmp <= minLenghtMeters) {
				return minLenghtMeters;
			} else {
				return distance * 1852;
			}
		} else if (conversion == 1) {
			tmp = distance / 1852;
			if (tmp >= maxLenghtNauticalMiles) {
				return maxLenghtNauticalMiles;
			} else if (tmp <= minLenghtNauticalMiles) {
				return minLenghtNauticalMiles;
			} else {
				return distance / 1852;
			}
		} else {
			return 0;
		}
	}

	private void runScheduledAlarm() {
		new FiskinfoScheduledTaskExecutor(2).scheduleAtFixedRate(new Runnable() {

			@Override
			public void run() {
				// Need to get alarm status and handle kill
				if (!cacheDeserialized) {
					if (checkCacheWriterStatus()) {
						String directoryPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString();
						String directoryName = "FiskInfo";
						String filename = "cachedResults";
						String filePath = directoryPath + "/" + directoryName + "/" + filename;
						tools = new FiskInfoUtility().deserializeFiskInfoPolygon2D(filePath);
						cacheDeserialized = true;
						// DEMO: HER LEGGER VI INN PUNKTENE SOM VI SKAL SKREMME
						// FOLK MED!
						// Point point = new Point(69.650543, 18.956831);
						// tools.addPoint(point);
					}
				} else {
					if (alarmFiring) {
						System.out.println("SHieeeeet");
						notifyUserOfProximityAlert();
					} else {
						double latitude, longitude = 0;
						if (mGpsLocationTracker.canGetLocation()) {
							latitude = mGpsLocationTracker.getLatitude();
							cachedLat = latitude;
							longitude = mGpsLocationTracker.getLongitude();
							cachedLon = longitude;
							System.out.println("Lat; " + latitude + "lon: " + longitude);
							Log.i("GPS-LocationTracker", String.format("latitude: %s", latitude));
							Log.i("GPS-LocationTracker", String.format("longitude: %s", longitude));
						} else {
							mGpsLocationTracker.showSettingsAlert();
							return;
						}
						Point userPosition = new Point(cachedLat, cachedLon);
						if (!tools.checkCollsionWithPoint(userPosition, Double.parseDouble(cachedDistance))) {
							System.out.println("We no crash");
							return;
						}
						// shieeeet
						alarmFiring = true;
					}
				}

				System.out.println("BEEP");
			}

		}, 5, 20, TimeUnit.SECONDS); // <num1> is initial delay,<num2> is the subsequent delay between each call
	}

	private boolean checkCacheWriterStatus() {
		if (cacheWriter.getStatus() == AsyncTask.Status.FINISHED) {
			return true;
		}
		return false;
	}

	private void getMapTools() {
		if (mTools.getVersionNumber().equals(ToolsGeoJson.INVALID_VERSION)) {
			try {
				new DownloadMapLayerFromBarentswatchApiInBackground().execute("fishingfacility", "JSON").get();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			//Do versioning
		}
	}
	
	private void updateMapTools() {
		String tools = null;
		tools = getGeoJsonFile();
		while(tools == null) {
			tools = getGeoJsonFile();
		}
		setGeoJsonFile(null);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	    Date now = new Date();
	    String strDate = sdf.format(now);
	    System.out.println("TIS BETTER FCKIN WORK");
		try {
			mTools.setTools(new JSONObject(tools), strDate, getContext());
			System.out.println("Right vegeta");
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public class JavaScriptInterface {
		Context mContext;

		JavaScriptInterface(Context context) {
			mContext = context;
		}

		@android.webkit.JavascriptInterface
		public JSONObject getGeoJson() {
			JSONObject mordi = null;
			try {
				System.out.println("DO I FAIL?");
				JSONObject fnName = mTools.getTools();
				mordi = fnName;
			} catch (Exception e) {
				System.out.println("I FAILED");
				e.printStackTrace();
			}
			return mordi;

		}
	}

	public static String convertStreamToString(InputStream is) throws Exception {
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		StringBuilder sb = new StringBuilder();
		String line = null;
		while ((line = reader.readLine()) != null) {
			sb.append(line);
		}
		reader.close();
		return sb.toString();
	}

	public static JSONObject getStringFromFile(InputStream in) throws Exception {
		InputStream fin = in;
		String ret = convertStreamToString(fin);
		JSONObject dearGod = new JSONObject(ret);
		fin.close();
		System.out.println(dearGod.toString());
		return dearGod;
	}
}
