package no.barentswatch.fiskinfo;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import no.barentswatch.baseclasses.Line;
import no.barentswatch.baseclasses.Point;
import no.barentswatch.baseclasses.Polygon;
import no.barentswatch.implementation.ExpandableListAdapter;
import no.barentswatch.implementation.FiskInfoPolygon2D;
import no.barentswatch.implementation.FiskInfoUtility;
import no.barentswatch.implementation.GpsLocationTracker;
import no.barentswatch.implementation.NoDefaultSpinner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.OnNavigationListener;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import ch.boye.httpclientandroidlib.HttpEntity;
import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.NameValuePair;
import ch.boye.httpclientandroidlib.client.ClientProtocolException;
import ch.boye.httpclientandroidlib.client.entity.UrlEncodedFormEntity;
import ch.boye.httpclientandroidlib.client.methods.CloseableHttpResponse;
import ch.boye.httpclientandroidlib.client.methods.HttpGet;
import ch.boye.httpclientandroidlib.client.methods.HttpPost;
import ch.boye.httpclientandroidlib.client.utils.URLEncodedUtils;
import ch.boye.httpclientandroidlib.impl.client.CloseableHttpClient;
import ch.boye.httpclientandroidlib.impl.client.HttpClients;
import ch.boye.httpclientandroidlib.message.BasicNameValuePair;
import ch.boye.httpclientandroidlib.protocol.HTTP;
import ch.boye.httpclientandroidlib.util.EntityUtils;

/**
 * Note this class should not be implemented the way it is, this class should
 * interact with a wrapper class so that the action bar field is created at
 * compile-time and therefore avoiding the tests and manipulation concerning the
 * action bar usage and design in this file.
 * 
 * @author pc-utleie.no
 * 
 */
@SuppressLint("InflateParams")
public class BaseActivity extends ActionBarActivity {
	public ActionBar actionBar;
	private static boolean userIsAuthenticated;
	public Spinner spinner;
	public ArrayAdapter<String> adapter;
	public ActionBar.OnNavigationListener navigationListener;
	private Context mContext;
	public boolean firstTimeSelect = true;
	private static boolean applicationStartup = true;
	private static SharedPreferences prefs;
	private int previousSelectionActionBar = -1;
	private JSONArray sharedCacheOfAvailableSubscriptions;

	/*
	 * these value refer to the index of the units in the string array
	 * 'measurement_units' and are only here so we don't need to look them up
	 * every time we update the seek bar.
	 */
	private String lastSetStartingPosition = null;
	private String lastSetEndPosition = null;

	/**
	 * These variables should really, really, REALLY! Not be here, however
	 * Barentswatch.no/pilot uses the most incompetent auth scheme since the
	 * beginning of mankind, this is justified. It uses time-expirable tokens,
	 * and http with plaintext sending of usr/pwd, therefore we cannot provide
	 * security for the usr/pwd without causing relogging...... Therefore we
	 * store the usr/pwd as strings since its sent as plaintext anyways, and we
	 * reckon its a bit harder to root the phone attach a debugger and listen to
	 * the app for these variables, rather than fire up wireshark and laugh all
	 * the way to the bank. Seriously, normally we would only read the token....
	 * We're not this incompetent, atleast we like to believe this.
	 * (non-Javadoc)
	 * 
	 * @see android.support.v7.app.ActionBarActivity#onCreate(android.os.Bundle)
	 */
	private static String storedUsername;
	private static String storedPassword;
	private static JSONObject storedToken;

	// END STUPIDITY

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		setContentView(R.layout.activity_base);
		if (applicationStartup) {
			try {
				getAuthenticationCredientialsFromSharedPrefrences();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		applicationStartup = false;

		initalizeAndConfigureActionBar();
	}

	private void getAuthenticationCredientialsFromSharedPrefrences() throws Exception {
		prefs = this.getSharedPreferences("no.barentswatch.fiskinfo", Context.MODE_PRIVATE);
		String authWritten = prefs.getString("authWritten", null);
		if (authWritten != null) {
			storedUsername = prefs.getString("username", null);
			storedPassword = prefs.getString("password", null);
			String tokenAsString = prefs.getString("token", null);
			storedToken = new JSONObject(tokenAsString);

			long timestampTimeOfAuthGranularitySeconds = prefs.getLong("timeOfAuth", -1);
			long timestampNowGranularitySeconds = System.currentTimeMillis() / 1000L;
			System.out.println("Time of Auth: " + timestampTimeOfAuthGranularitySeconds + "Timestamp now: " + timestampNowGranularitySeconds);
			if (timestampTimeOfAuthGranularitySeconds == -1 || (timestampNowGranularitySeconds - timestampTimeOfAuthGranularitySeconds) > storedToken.getInt("expires_in") + 600) {
				authenticateUserCredentials(storedUsername, storedPassword);
			}
			System.out.println("stored token: " + storedToken);
			setAuthentication(true);
			if (storedUsername == null || storedPassword == null || storedToken == null) {
				invalidateAuthenticationData();
			}
		}
	}

	/**
	 * Invalidates the data stored in Shared Preferences
	 */
	private void invalidateAuthenticationData() {
		String authWritten;
		System.out.println("Could not read proper from disk at startup, invalidating the data");
		storedUsername = storedPassword = null;
		storedToken = null;
		authWritten = null;
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString("authWritten", authWritten);
		editor.commit();
		setAuthentication(false);
	}

	/**
	 * Initializes and configures the ActionBar depending on the users API-Level
	 * The backward compatible version uses a hacked together spinner for the
	 * drop down menu in the action bar, as it did not exist prior to API-level
	 * 11.
	 */
	private void initalizeAndConfigureActionBar() {

		actionBar = getSupportActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#085382")));

		initalizeAndConfigureActionBarSpinners();

		actionBar.setDisplayHomeAsUpEnabled(false);
		actionBar.setDisplayShowTitleEnabled(true);
		actionBar.setListNavigationCallbacks(adapter, navigationListener);
		actionBar.setSelectedNavigationItem(adapter.getCount());
	}

	private void initalizeAndConfigureActionBarSpinners() {
		spinner = (Spinner) findViewById(R.id.actionBarNavigationList);
		if (userIsAuthenticated) {
			initializeAuthenticatedActionBarSpinner();
		} else {
			initializeAndSetUpNonAuthenticatedActionBarSpinner();
		}
	}

	private void initializeAndSetUpNonAuthenticatedActionBarSpinner() {
		adapter = createAndInitializeHintAdapter(R.array.non_authenticated_user_actionbar_options, "Meny", mContext);
		spinner.setAdapter(adapter);
		spinner.setSelection(adapter.getCount());		
		
		navigationListener = new OnNavigationListener() {

			@Override
			public boolean onNavigationItemSelected(int position, long itemId) {
				if (firstTimeSelect == true) {
					firstTimeSelect = false;
					return true;
				}

				switch (position) {
				case 0: // Logg inn
					mContext = getContext();
					ShowLoginDialog(mContext, R.string.app_name, findViewById(R.layout.dialog_login));
					break;
				case 1: // om
					mContext = getContext();
					createNewPositiveDialog(mContext, R.string.app_name, R.string.about_page_welcome_text);
					break;
				case 2: // Hjelp
					loadView(HelpActivity.class);
					break;
				default:
					return true;
				}
				return false;
			}
		};
		
					
	}
	
	private void initializeAuthenticatedActionBarSpinner() {
		adapter = createAndInitializeHintAdapter(R.array.authenticated_user_actionbar_options, "Meny", mContext);
		spinner.setAdapter(adapter);
		spinner.setSelection(adapter.getCount());

		navigationListener = new OnNavigationListener() {

			@Override
			public boolean onNavigationItemSelected(int position, long itemId) {
				if (firstTimeSelect == true) {
					firstTimeSelect = false;
					return true;
				}
				switch (position) {
				case 0: // Min side
					loadView(MyPageActivity.class);
					break;
				case 1: // Map View
					loadView(MapActivity.class);
					break;
				case 2: // om
					mContext = getContext();
					createNewPositiveDialog(mContext, R.string.app_name, R.string.about_page_welcome_text);
					break;
				case 3: // hjelp
					loadView(HelpActivity.class);
					break;
				case 4: // logg ut
					invalidateAuthenticationData();
					loadView(MainActivity.class);
					break;
				default:
					return false;
				}
				return true;
			}
		};
	}

	/**
	 * This function creates a "<code>positive</code>:see google docs" pop up
	 * dialog which takes in the context of the current activity
	 * 
	 * @param activityContext
	 *            the context of the current activity
	 * @param rPathToTitleOfPopup
	 *            The R.path to the title
	 * @param rPathToTextInTheBodyOfThePopup
	 *            The R.path to the text which will be contained in the body
	 */
	public void createNewPositiveDialog(Context activityContext, int rPathToTitleOfPopup, int rPathToTextInTheBodyOfThePopup) {
		AlertDialog.Builder builder = new AlertDialog.Builder(activityContext);
		builder.setTitle(rPathToTitleOfPopup);
		builder.setMessage(rPathToTextInTheBodyOfThePopup);
		builder.setPositiveButton("OK", null);
		AlertDialog dialog = builder.show();
		dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
			
			@Override
			public void onDismiss(DialogInterface dialog) {
				// TODO Auto-generated method stub
				actionBar.setSelectedNavigationItem(adapter.getCount());
			}
		});

		TextView messageView = (TextView) dialog.findViewById(android.R.id.message);
		messageView.setGravity(Gravity.CENTER);
	}

	public void createConfirmOverWriteDialog(Context activityContext, final EditText coordinateField, final String userCoordinates) {
		AlertDialog.Builder builder = new AlertDialog.Builder(activityContext);
		builder.setTitle(R.string.register_tool_confirm_overwrite_title);

		switch (coordinateField.getId()) {
		case R.id.registerStartingCoordinatesOfTool:
			builder.setMessage(R.string.register_tool_confirm_overwrite_of_coordinates_start);
			break;
		case R.id.registerEndCoordinatesOfTool:
			builder.setMessage(R.string.register_tool_confirm_overwrite_of_coordinates_end);
			break;
		default:
			return;
		}

		builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (coordinateField.getId()) {
				case R.id.registerStartingCoordinatesOfTool:
					lastSetStartingPosition = userCoordinates;
					break;
				case R.id.registerEndCoordinatesOfTool:
					lastSetEndPosition = userCoordinates;
					break;
				}
				coordinateField.setText("");
				coordinateField.setText(userCoordinates);
			}
		});
		builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				return;
			}
		});

		AlertDialog dialog = builder.show();

		TextView messageView = (TextView) dialog.findViewById(android.R.id.message);
		messageView.setGravity(Gravity.CENTER);
	}

	/**
	 * This function creates a dialog which gives a description of the symbols
	 * that populate the map.
	 * 
	 * @param ActivityContext
	 *            The context of the current activity.
	 */
	public void displaySymbolExplanation(Context ActivityContext) {
		LayoutInflater layoutInflater = getLayoutInflater();
		View view = layoutInflater.inflate(R.layout.symbol_explanation, (null));
		final AlertDialog builder = new AlertDialog.Builder(ActivityContext).create();
		builder.setTitle(R.string.map_symbol_explanation);
		builder.setView(view);

		Button okButton = (Button) view.findViewById(R.id.symbolOkButton);
		okButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				builder.dismiss();
			}
		});
		builder.show();
	}

	/**
	 * This function creates a dialog which allows the user to register a item
	 * or tool used.
	 * 
	 * @param activityContext
	 *            The context of the current activity.
	 */
	public void registerItemAndToolUsed(Context activityContext) {
		LayoutInflater layoutInflater = getLayoutInflater();
		View view = layoutInflater.inflate(R.layout.dialog_register_tool, (null));
		final AlertDialog builder = new AlertDialog.Builder(activityContext).create();
		builder.setTitle(R.string.register_tool_dialog_title);
		builder.setView(view);
		final EditText startingCoordinates = (EditText) view.findViewById(R.id.registerStartingCoordinatesOfTool);
		final EditText endCoordinates = (EditText) view.findViewById(R.id.registerEndCoordinatesOfTool);
		final TextView invalidInputFeedback = (TextView) view.findViewById(R.id.RegisterToolInvalidInputTextView);

		if (lastSetStartingPosition != null) {
			startingCoordinates.setText(lastSetStartingPosition);
		}
		if (lastSetEndPosition != null) {
			endCoordinates.setText(lastSetEndPosition);
		}

		final Spinner projectionSpinner = (Spinner) view.findViewById(R.id.projectionChangingSpinner);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.projections, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		projectionSpinner.setPrompt("Velg projeksjon");
		projectionSpinner.setAdapter(new NoDefaultSpinner(adapter, R.layout.spinner_layout_select_projection, activityContext));
		
		final Spinner itemSpinner = (Spinner) view.findViewById(R.id.registerMiscType);
		ArrayAdapter<CharSequence> itemAdapter = ArrayAdapter.createFromResource(this, R.array.tool_types, android.R.layout.simple_spinner_item);
		itemAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		itemSpinner.setPrompt("Velg redskapstype");
		itemSpinner.setAdapter(new NoDefaultSpinner(itemAdapter, R.layout.spinner_layout_choose_tool, activityContext));

		Button fetchToolStartingCoordinatesButton = (Button) view.findViewById(R.id.dialogFetchUserStartingCoordinates);
		Button fetchToolEndCoordinatesButton = (Button) view.findViewById(R.id.dialogFetchUserEndCoordinates);

		fetchToolStartingCoordinatesButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				setToolCoordinatesPosition(startingCoordinates);
			}
		});

		fetchToolEndCoordinatesButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				setToolCoordinatesPosition(endCoordinates);
			}
		});

		acceptButtonRegister(view, builder, startingCoordinates, endCoordinates, invalidInputFeedback, projectionSpinner, itemSpinner);

		Button cancelButton = (Button) view.findViewById(R.id.DialogCancelRegistration);
		cancelButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				builder.dismiss();
			}
		});

		builder.show();
	}

	private void acceptButtonRegister(View view, final AlertDialog builder, final EditText startingCoordinates, final EditText endCoordinates, final TextView invalidInputFeedback,
			final Spinner projectionSpinner, final Spinner itemSpinner) {
		Button acceptButton = (Button) view.findViewById(R.id.dialogAcceptRegistration);
		acceptButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				// Handle registration here. It is not yet implemented in the BW
				// API
				String ToolstartingCoordinates = startingCoordinates.getText().toString();
				String ToolendCoordinates = endCoordinates.getText().toString();

				invalidInputFeedback.setVisibility(android.view.View.INVISIBLE);

				if (projectionSpinner.getSelectedItem() == null) {
					invalidInputFeedback.setText(getString(R.string.register_tool_no_projection_selected));
					invalidInputFeedback.setVisibility(android.view.View.VISIBLE);
					return;
				} else if (itemSpinner.getSelectedItem() == null) {
					invalidInputFeedback.setText(getString(R.string.register_tool_no_tool_selected));
					invalidInputFeedback.setVisibility(android.view.View.VISIBLE);
					return;
					
				}

				if ( new FiskInfoUtility().checkCoordinates(ToolstartingCoordinates, projectionSpinner.getSelectedItem().toString()) == false) {
					invalidInputFeedback.setText(getString(R.string.register_tool_invalid_coordinate_format));
					invalidInputFeedback.setVisibility(android.view.View.VISIBLE);

					startingCoordinates.requestFocus();
					startingCoordinates.setError(getString(R.string.register_tool_invalid_coordinate_format));

				} else if (new FiskInfoUtility().checkCoordinates(ToolendCoordinates, projectionSpinner.getSelectedItem().toString()) == false) {
					invalidInputFeedback.setText(getString(R.string.register_tool_invalid_coordinate_format));
					invalidInputFeedback.setVisibility(android.view.View.VISIBLE);

					endCoordinates.requestFocus();
					endCoordinates.setError(getString(R.string.register_tool_invalid_coordinate_format));
				} else {
					startingCoordinates.setError(null);
					endCoordinates.setError(null);

					// TODO: send data to whoever should receive it and wait for
					// confirmation that things went OK
					Toast result = Toast.makeText(getContext(), "item registered", Toast.LENGTH_LONG);
					result.show();

					builder.dismiss();
				}
			}
		});
	}



	/**
	 * Sets the contents of the given EditText to be equal to the GPS position
	 * of the user.
	 * 
	 * @param coordinateField
	 */
	public void setToolCoordinatesPosition(EditText coordinateField) {
		GpsLocationTracker mGpsLocationTracker = new GpsLocationTracker(getContext());
		/**
		 * Set GPS Location fetched address and place them in the input for
		 * coordinates. As GPS-coordinates are locale independent this function
		 * uses hardcoded values for separators to avoid dependency issues. Note
		 * that the Android location API gives coordinates in WGS84 - ellipsoid,
		 * which is the same as EPSG:4326!
		 */
		if (mGpsLocationTracker.canGetLocation()) {
			double latitude = mGpsLocationTracker.getLatitude();
			double longitude = mGpsLocationTracker.getLongitude();
			Log.i("GPS-LocationTracker", String.format("latitude: %s", latitude));
			Log.i("GPS-LocationTracker", String.format("longitude: %s", longitude));

			String objectCoordinates = coordinateField.getText().toString();
			String userCoordinates = String.valueOf(latitude) + "," + String.valueOf(longitude);

			if (objectCoordinates.length() == 0) {
				coordinateField.append(userCoordinates);
				switch (coordinateField.getId()) {
				case R.id.registerStartingCoordinatesOfTool:
					lastSetStartingPosition = userCoordinates;
					break;
				case R.id.registerEndCoordinatesOfTool:
					lastSetEndPosition = userCoordinates;
					break;
				default:
					return;
				}
			} else {
				createConfirmOverWriteDialog(mContext, coordinateField, userCoordinates);
			}
			coordinateField.setError(null);
		} else {
			mGpsLocationTracker.showSettingsAlert();
		}
	}

	/**
	 * This function creates a pop up dialog which takes in the context of the
	 * current activity
	 * 
	 * @param activityContext
	 *            the context of the current activity
	 * @param rPathToTitleOfPopup
	 *            The R.path to the title
	 * @param customView
	 *            the custom view for the dialog
	 */
	public void ShowLoginDialog(Context activityContext, int rPathToTitleOfPopup, View customView) {
		LayoutInflater layoutInflater = getLayoutInflater();
		View view = layoutInflater.inflate(R.layout.dialog_login, null);
		AlertDialog.Builder builder = new AlertDialog.Builder(activityContext);
		builder.setTitle(rPathToTitleOfPopup);

		final EditText usernameEditText = (EditText) view.findViewById(R.id.LoginDialogEmailField);
		final EditText passwordEditText = (EditText) view.findViewById(R.id.loginDialogPasswordField);
		final TextView incorrectCredentialsTextView = (TextView) view.findViewById(R.id.loginIncorrectCredentialsTextView);

		Button loginButton = (Button) view.findViewById(R.id.loginDialogButton);
		Button cancelButton = (Button) view.findViewById(R.id.cancel_login_button);
		builder.setView(view);
		final AlertDialog dialog = builder.create();
		
		loginButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (incorrectCredentialsTextView.getVisibility() == 0) {
					incorrectCredentialsTextView.setVisibility(android.view.View.INVISIBLE);
				}

				if (!isNetworkAvailable()) {
					incorrectCredentialsTextView.setText(getString(R.string.no_internet_access));
					incorrectCredentialsTextView.setVisibility(android.view.View.VISIBLE);
					return;
				}

				String usernameText = usernameEditText.getText().toString();
				String passwordText = passwordEditText.getText().toString();

				if (!validateEmail(usernameText)) {
					usernameEditText.requestFocus();
					usernameEditText.setError(getString(R.string.login_invalid_email));
					return;
				}
				usernameEditText.setError(null);

				if (passwordText.length() == 0) {
					passwordEditText.requestFocus();
					passwordEditText.setError(getString(R.string.login_password_field_empty_string));
					return;
				}
				passwordEditText.setError(null);

				try {
					authenticateUserCredentials(usernameText, passwordText);
				} catch (Exception e) {
					e.printStackTrace();
				}

				if (userIsAuthenticated) {
					loadView(MyPageActivity.class);
				} else {
					incorrectCredentialsTextView.setText(getString(R.string.login_incorrect_credentials));
					incorrectCredentialsTextView.setVisibility(android.view.View.VISIBLE);
					return;
				}
			}
		});

		cancelButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				dialog.dismiss();
	
			}
		});
		
		dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
			
			@Override
			public void onDismiss(DialogInterface dialog) {
				// TODO Auto-generated method stub
				actionBar.setSelectedNavigationItem(adapter.getCount());
			}
		});
		
		dialog.show();
	}

	protected boolean isNetworkAvailable() {
		ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
		return (activeNetworkInfo != null && activeNetworkInfo.isConnected());
	}

	/**
	 * The following 2 functions exists because we need to be backward
	 * compatible
	 * 
	 * @return
	 */
	public Context getContext() {
		return mContext;
	}

	/**
	 * 
	 * @param mContext
	 */
	public void setContext(Context mContext) {
		this.mContext = mContext;
	}

	public int getPreviousSelectionActionBar() {
		return previousSelectionActionBar;
	}

	public void setPreviousSelectionActionBar(int selection) {
		previousSelectionActionBar = selection;
	}

	/**
	 * Checks that the given string is a valid E-mail address
	 * 
	 * @param address
	 *            the address to check
	 * @return true if address is a valid E-mail address, false otherwise.
	 */
	public boolean validateEmail(String address) {
		String regex = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@" + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(address);
		return matcher.matches();
	}

	/**
	 * Attempts to authenticate the given credentials with BarentsWatch. Will
	 * set userIsAuthenticated to true if authentication is successful.
	 * 
	 * @param username
	 *            the username to use for authentication
	 * @param password
	 *            the password to use for authentication
	 */
	// TODO: Change from hardcoded variables to using the actual username and
	// password
	public void authenticateUserCredentials(final String username, final String password) throws Exception {
		final AtomicReference<String> responseAsString = new AtomicReference<String>();
		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					CloseableHttpClient httpclient = HttpClients.createDefault();
					try {
						HttpPost httpPost = new HttpPost("https://pilot.barentswatch.net/api/token");
						httpPost.addHeader(HTTP.CONTENT_TYPE, "application/x-www-form-urlencoded");
						List<NameValuePair> postParameters = new ArrayList<NameValuePair>();
						postParameters.add(new BasicNameValuePair("grant_type", "password"));
						postParameters.add(new BasicNameValuePair("username", "crono142@gmail.com"));
						postParameters.add(new BasicNameValuePair("password", "8fgP8pDuhFcespv"));
						httpPost.setEntity(new UrlEncodedFormEntity(postParameters));

						CloseableHttpResponse response = httpclient.execute(httpPost);
						try {
							responseAsString.set(EntityUtils.toString(response.getEntity()));
						} finally {
							response.close();
						}

					} finally {
						httpclient.close();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

		thread.start();
		try {
			thread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		String barentswatchResponse = responseAsString.get();
		JSONObject barentsWatchResponseToken = new JSONObject(barentswatchResponse);

		saveUserCredentialsToSharedPreferences(barentsWatchResponseToken);
		getAuthenticationCredientialsFromSharedPrefrences();

		setAuthentication(true);
		loadView(MyPageActivity.class);
	}

	/**
	 * Writes the userCredentials to the Shared Preferences automatically in
	 * <code>Context.MODE_PRIVATE</code>
	 * 
	 * @param barentsWatchResponseToken
	 *            The responsetoken given by the Barentswatch authentication API
	 */
	private void saveUserCredentialsToSharedPreferences(JSONObject barentsWatchResponseToken) {
		prefs = this.getSharedPreferences("no.barentswatch.fiskinfo", Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString("authWritten", "Ishould not return null should I now");
		editor.putString("username", "crono142@gmail.com");
		editor.putString("password", "8fgP8pDuhFcespv");

		editor.putString("token", barentsWatchResponseToken.toString());
		long timestampSecondsGranularity = System.currentTimeMillis() / 1000L;
		editor.putLong("timeOfAuth", timestampSecondsGranularity);
		editor.commit();
	}

	/**
	 * Loads the activity of the given class
	 * 
	 * @param activityClass
	 *            A generic class, namely the class instance of the view to load
	 */
	public void loadView(Class<?> activityClass) {
		mContext = getContext();
		try {
			actionBar.setSelectedNavigationItem(adapter.getCount());
			Intent intent = new Intent(mContext, activityClass);
			startActivity(intent);
		} catch (Exception e) {
			Log.e("Failed activity switch", "Could not switch to " + activityClass.getSimpleName() + " Activity");
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.base, menu);

		return true;
	}

	public boolean getAuthenticationLevel() {
		return BaseActivity.userIsAuthenticated;
	}

	public void setAuthentication(boolean authLevel) {
		BaseActivity.userIsAuthenticated = authLevel;
	}
	
	private static ArrayAdapter<String> createAndInitializeHintAdapter(int rPathToArray, String hint, Context context) {
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, R.layout.spinner_item) {

			@Override
			public View getView(int position, View convertView, ViewGroup parent) {

				View v = super.getView(position, convertView, parent);
				if (position == getCount()) {
					((TextView) v.findViewById(android.R.id.text1)).setText("");
					((TextView) v.findViewById(android.R.id.text1)).setHint(getItem(getCount())); // "Hint to be displayed"
				}

				return v;
			}

			@Override
			public int getCount() {
				return super.getCount() - 1; // you dont display
												// last item. It is
												// used as hint.
			}

		};
		for (String field : context.getResources().getStringArray(rPathToArray)) {
			adapter.add(field);
		}
		// HACK WARNING: Adding last entry so we can display spinner
		// hint
		adapter.add(hint);
		adapter.setDropDownViewResource(R.layout.spinner_item);
		return adapter;
	}


	/**
	 * Sends a request to BarentsWatch for the given service, which returns a
	 * JSONArray on success.
	 * 
	 * @param service
	 *            The service to call in the API.
	 * @return A JSONArray containing the response from BarentsWatch if the
	 *         request succeeded, null otherwise.
	 */
	public JSONArray authenticatedGetRequestToBarentswatchAPIService(final String service) {
		if (!userIsAuthenticated) {
			Log.e("FiskInfo", "This should never happen. User must be logged in before we fetch the users geodata subs");
			return null;
		}

		final AtomicReference<String> responseAsString = new AtomicReference<String>();
		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					CloseableHttpClient httpclient = HttpClients.createDefault();
					try {
						String base_url = "http://pilot.barentswatch.net/api/" + service;
						List<NameValuePair> getParameters = new ArrayList<NameValuePair>(1);
						getParameters.add(new BasicNameValuePair("access_token", storedToken.getString("access_token")));
						String paramsString = URLEncodedUtils.format(getParameters, "UTF-8");

						HttpGet httpGet = new HttpGet(base_url + "?" + paramsString);
						httpGet.addHeader(HTTP.CONTENT_TYPE, "application/json");

						CloseableHttpResponse response = httpclient.execute(httpGet);
						try {
							responseAsString.set(EntityUtils.toString(response.getEntity()));
						} finally {
							response.close();
						}

					} finally {
						httpclient.close();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

		thread.start();
		try {
			thread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		String barentswatchResponse = responseAsString.get();
		if (barentswatchResponse == null || barentswatchResponse.trim().length() == 0) {
			return null;
		}
		try {
			JSONArray barentswatchAPIJSONResponse = new JSONArray(barentswatchResponse);
			return barentswatchAPIJSONResponse;
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * This functions creates a dialog which allows the user to export different
	 * map layers.
	 * 
	 * @param ActivityContext
	 *            The context of the current activity.
	 * @return True if the export succeeded, false otherwise.
	 */
	public boolean exportMapLayerToUser(Context activityContext) {
		LayoutInflater layoutInflater = getLayoutInflater();
		View view = layoutInflater.inflate(R.layout.dialog_export_metadata, (null));
		final AlertDialog builder = new AlertDialog.Builder(activityContext).create();
		builder.setTitle(R.string.map_export_metadata_title);
		builder.setView(view);
		final AtomicReference<String> selectedHeader = new AtomicReference<String>();
		final AtomicReference<String> selectedFormat = new AtomicReference<String>();
		ExpandableListView expListView = (ExpandableListView) view.findViewById(R.id.exportMetadataMapServices);
		final List<String> listDataHeader = new ArrayList<String>();
		final HashMap<String, List<String>> listDataChild = new HashMap<String, List<String>>();
		final Map<String, String> nameToApiNameResolver = new HashMap<String, String>();
		
		JSONArray availableSubscriptions = getSharedCacheOfAvailableSubscriptions();
		if (availableSubscriptions == null) {
			availableSubscriptions = authenticatedGetRequestToBarentswatchAPIService(getString(R.string.my_page_geo_data_service));
			setSharedCacheOfAvailableSubscriptions(availableSubscriptions);
		}

		for (int i = 0; i < availableSubscriptions.length(); i++) {
			try {
				JSONObject currentSub = availableSubscriptions.getJSONObject(i);
				nameToApiNameResolver.put(currentSub.getString("Name"), currentSub.getString("ApiName"));
				listDataHeader.add(currentSub.getString("Name"));
				List<String> availableDownloadFormatsOfCurrentLayer = new ArrayList<String>();
				JSONArray availableFormats = currentSub.getJSONArray("Formats");
				for (int j = 0; j < availableFormats.length(); j++) {
					availableDownloadFormatsOfCurrentLayer.add(availableFormats.getString(j));
				}
				listDataChild.put(listDataHeader.get(i), availableDownloadFormatsOfCurrentLayer);
			} catch (JSONException e) {
				e.printStackTrace();
				Log.d("ExportMapLAyerToUser", "Invalid JSON returned from API CALL");
				return false;
			}
		}
		ExpandableListAdapter listAdapter = new ExpandableListAdapter(activityContext, listDataHeader, listDataChild);
		expListView.setAdapter(listAdapter);

		// Listview on child click listener
		expListView.setOnChildClickListener(new OnChildClickListener() {

			@Override
			public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
				selectedHeader.set(nameToApiNameResolver.get(listDataHeader.get(groupPosition)));
				selectedHeader.set(listDataHeader.get(groupPosition));
				selectedFormat.set(listDataChild.get(listDataHeader.get(groupPosition)).get(childPosition));
				return true;
			}
		});

		Button downloadButton = (Button) view.findViewById(R.id.metadataDownloadButton);
		downloadButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				new DownloadMapLayerFromBarentswatchApiInBackground().execute(selectedHeader.get(), selectedFormat.get());
				builder.dismiss();
			}
		});

		builder.show();

		return true;
	}

	/**
	 * DOCUMENTATION OUTDATED: THIS FUNCTION SHOULD BE REFACTORED, INTERFACED
	 * AND BETTER CLASS CODE
	 * 
	 * Downloading available map layers from Barentswatch has been placed in its
	 * own AsyncTask and specifically tailored to do that one task. The
	 * reasoning behind this process is that multiple inheritance is disallowed
	 * in Java, and since we want the downloading of file(s) to be done in the
	 * background. Therefore creating a separate logic for this functionality,
	 * which has a 1-1 relationship and has no possibility of re-use, makes
	 * sense.
	 * 
	 * @param 0 Apiname (name of the map layer)
	 * @param 1 Output format of the map layer
	 * @param 2 User defined name of the file to download
	 * @param 3 User position Longitude
	 * @param 4 User position Latitude
	 * @param 5 User position distance
	 * @param 6 Tells the class that this is a alarm file
	 */
/**
	 * DOCUMENTATION OUTDATED: THIS FUNCTION SHOULD BE REFACTORED, INTERFACED
	 * AND BETTER CLASS CODE
	 * 
	 * Downloading available map layers from Barentswatch has been placed in its
	 * own AsyncTask and specifically tailored to do that one task. The
	 * reasoning behind this process is that multiple inheritance is disallowed
	 * in Java, and since we want the downloading of file(s) to be done in the
	 * background. Therefore creating a separate logic for this functionality,
	 * which has a 1-1 relationship and has no possibility of re-use, makes
	 * sense.
	 * 
	 * @param 0 Apiname (name of the map layer)
	 * @param 1 Output format of the map layer
	 * @param 2 User defined name of the file to download
	 * @param 3 User position Longitude
	 * @param 4 User position Latitude
	 * @param 5 User position distance
	 * @param 6 Tells the class that this is a alarm file
	 */
	public class DownloadMapLayerFromBarentswatchApiInBackground extends AsyncTask<String, String, byte[]> {
		protected String writableName;
		protected String format;
		protected String lon = null;
		protected String lat = null;
		protected String distance = null;
		protected String apiName = null;
		protected boolean alarmFile;

		protected void parseParameters(String[] params) {
			apiName = params[0];
			format = params[1];
			writableName = apiName;
			if (params.length > 2 && params.length < 8) {
				writableName = params[2] != null ? params[2] : apiName;
				lon = params[3];
				lat = params[4];
				distance = params[5];
				alarmFile = params[6].equalsIgnoreCase("true") ? true : false;
			}
		}

		@Override
		protected byte[] doInBackground(String... params) {
			parseParameters(params);

			InputStream data = null;
			byte[] rawData = null;

			CloseableHttpClient httpClient = HttpClients.createDefault();
			try {
				List<NameValuePair> getParameters = new ArrayList<NameValuePair>(1);
				if (lon != null && lat != null && distance != null) {
					getParameters.add(new BasicNameValuePair("lon", lon));
					getParameters.add(new BasicNameValuePair("lat", lat));
					getParameters.add(new BasicNameValuePair("distance", distance));
				}
				getParameters.add(new BasicNameValuePair("access_token", storedToken.getString("access_token")));

				String paramsString = URLEncodedUtils.format(getParameters, "UTF-8");
				HttpGet httpGet;

				httpGet = new HttpGet("http://pilot.barentswatch.net/api/v1/geodata/" + apiName + "/download?format=" + format + "&" + paramsString);
				httpGet.addHeader(HTTP.CONTENT_TYPE, "application/json");
				Log.d("FiskInfo GetRequest", "The current get request is: " + httpGet.getRequestLine());
				HttpResponse httpResponse = httpClient.execute(httpGet);

				// Check is authentication to the server passed
				if (httpResponse.getStatusLine().getStatusCode() == 401) {
					// TODO: Get 2.opinion <-> Might clear and reload token, not
					// sure
					Log.d("FiskInfo", "User not authenticated for the request: " + httpGet.getRequestLine());
					finish();
				}

				HttpEntity responseEntity = httpResponse.getEntity();

				if (responseEntity instanceof HttpEntity && responseEntity != null) {
					data = responseEntity.getContent();
					try {
						rawData = new FiskInfoUtility().toByteArray(data);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}

				EntityUtils.consume(responseEntity);
			} catch (ClientProtocolException CPException) {
				rawData = null;
				Log.d("FiskInfo", "Recieved a client protocol exception: " + CPException.getMessage());

			} catch (IOException ioException) {
				rawData = null;
				Log.d("FiskInfo", "IOException: " + ioException.getMessage());
			} catch (JSONException jsonException) {
				rawData = null;
				Log.d("FiskInfo", "Recieved malformed JSON data from Barentswatch: " + jsonException.getMessage());
			}
			if (data == null) {
				Log.d("FiskInfo", "ApiError. Did not recieve data from Barentswatch");
			}
			return rawData;
		}

		@Override
		protected void onPostExecute(byte[] data) {
			OutputStream outputStream = null;
			if (data == null) {
				return;
			}
			if (isExternalStorageWritable()) {
				String directoryPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString();
				String directoryName = "FiskInfo";
				String filePath = directoryPath + "/" + directoryName + "/";

				File directory = new File(directoryPath, directoryName);

				if (!(directory.exists())) {
					directory.mkdirs();
				}
				if (!alarmFile) {
					writeMapLayerToExternalStorage(data, outputStream, filePath);
				} else {
					System.out.println("I should write the alarm file");
					writeAlarmFileToExternalStorage(data, outputStream, filePath);
				}
			} else {
				Toast error = Toast.makeText(getContext(), "Nedlastningen feilet, venligst sjekk at du har plass til filen p? mobilen", Toast.LENGTH_LONG);
				error.show();
				return;
			}

			Toast toast = Toast.makeText(getContext(), "NedlastningenFullf?rt", Toast.LENGTH_LONG);
			toast.show();
		}

		private void writeMapLayerToExternalStorage(byte[] data, OutputStream outputStream, String filePath) {
			try {
				outputStream = new FileOutputStream(new File(filePath + writableName + "." + format));
				outputStream.write(data);
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (outputStream != null) {
					try {
						outputStream.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}

		private void writeAlarmFileToExternalStorage(byte[] data, OutputStream outputStream, String filePath) {
			try {
				InputStream inputStream = new ByteArrayInputStream(data);
				BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
				FiskInfoPolygon2D serializablePolygon2D = new FiskInfoPolygon2D();

				String line = null;
				boolean startSet = false;
				String[] convertedLine = null;
				List<Point> shape = new ArrayList<Point>();
				while ((line = reader.readLine()) != null) {
					// We are supporting API 8, so this is:
					// IsNullOrEmpty();
					Point currPoint = new Point();
					if (line == "" || line.length() == 0 || line == null) {
						continue;
					}
					if (Character.isLetter(line.charAt(0))) {
						continue;
					}

					convertedLine = line.split("\\s+");
					if (convertedLine[3].equalsIgnoreCase("Garnstart") && startSet == true) {
						if (shape.size() == 1) {
							// Point
							serializablePolygon2D.addPoint(shape.get(0));
							shape = new ArrayList<Point>();
						} else if (shape.size() == 2) {
							// line
							serializablePolygon2D.addLine(new Line(shape.get(0), shape.get(1)));
							shape = new ArrayList<Point>();
						} else {
							serializablePolygon2D.addPolygon(new Polygon(shape));
							shape = new ArrayList<Point>();
						}
						startSet = false;
					}

					if (convertedLine[3].equalsIgnoreCase("Garnstart") && startSet == false) {
						double lat = Double.parseDouble(convertedLine[0]) / 60;
						double lon = Double.parseDouble(convertedLine[1]) / 60;
						currPoint.setNewPointValues(lat, lon);
						shape.add(currPoint);
						startSet = true;
					} else if (convertedLine[3].equalsIgnoreCase("Brunsirkel")) {
						double lat = Double.parseDouble(convertedLine[0]) / 60;
						double lon = Double.parseDouble(convertedLine[1]) / 60;
						currPoint.setNewPointValues(lat, lon);
						shape.add(currPoint);
					}
				}

				reader.close();
				new FiskInfoUtility().serializeFiskInfoPolygon2D(filePath + writableName, serializablePolygon2D);
				outputStream = new FileOutputStream(new File(filePath + writableName + "." + format));
				outputStream.write(data);

			} catch (IOException e) {
				e.printStackTrace();
			} catch (ArrayIndexOutOfBoundsException e) {
				Log.e("FiskInfo", "We should've received a file without any tools");
				Toast error = Toast.makeText(getContext(), "Ingen redskaper i omr?det du definerte", Toast.LENGTH_LONG);
				error.show();
				return;
			} finally {
				if (outputStream != null) {
					try {
						outputStream.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}

	}


	/**
	 * Checks if external storage is available for read and write.
	 * 
	 * @return True if external storage is available, false otherwise.
	 */
	public boolean isExternalStorageWritable() {
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * @return the sharedCacheOfAvailableSubscriptions
	 */
	public JSONArray getSharedCacheOfAvailableSubscriptions() {
		return sharedCacheOfAvailableSubscriptions;
	}

	/**
	 * @param sharedCacheOfAvailableSubscriptions
	 *            the sharedCacheOfAvailableSubscriptions to set
	 */
	public void setSharedCacheOfAvailableSubscriptions(JSONArray sharedCacheOfAvailableSubscriptions) {
		this.sharedCacheOfAvailableSubscriptions = sharedCacheOfAvailableSubscriptions;
	}
}
