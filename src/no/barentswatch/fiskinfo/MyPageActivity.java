package no.barentswatch.fiskinfo;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import no.barentswatch.implementation.ExpandableListAdapter;
import no.barentswatch.implementation.FiskInfoUtility;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupClickListener;
import android.widget.ExpandableListView.OnGroupCollapseListener;
import android.widget.ExpandableListView.OnGroupExpandListener;
import android.widget.TextView;
import android.widget.Toast;

public class MyPageActivity extends BaseActivity {
	private ExpandableListAdapter listAdapter;
	private ExpandableListView expListView;
	private List<String> listDataHeader;
	private HashMap<String, List<String>> listDataChild;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.setContext(this);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_my_page);

		if (!getAuthenticationLevel()) {
			Intent intent = new Intent(this, MainActivity.class);
			startActivity(intent);
		}

		// Retrieve the list view
		expListView = (ExpandableListView) findViewById(R.id.myPageListView);

		// Load the data of all the tutorials into the view
		loadListData();

		listAdapter = new ExpandableListAdapter(this, listDataHeader, listDataChild);

		createSubscriptionOnClickListeners();

		expListView.setAdapter(listAdapter);

		// Listview Group click listener
		expListView.setOnGroupClickListener(new OnGroupClickListener() {

			@Override
			public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
				Toast.makeText(getApplicationContext(), "Group Clicked " + listDataHeader.get(groupPosition), Toast.LENGTH_SHORT).show();
				return false;
			}
		});

		// Listview Group expanded listener
		expListView.setOnGroupExpandListener(new OnGroupExpandListener() {

			@Override
			public void onGroupExpand(int groupPosition) {
				Toast.makeText(getApplicationContext(), listDataHeader.get(groupPosition) + " Expanded", Toast.LENGTH_SHORT).show();
			}
		});

		// Listview Group collasped listener
		expListView.setOnGroupCollapseListener(new OnGroupCollapseListener() {

			@Override
			public void onGroupCollapse(int groupPosition) {
				Toast.makeText(getApplicationContext(), listDataHeader.get(groupPosition) + " Collapsed", Toast.LENGTH_SHORT).show();

			}
		});

		// Listview on child click listener
		// expListView.setOnChildClickListener(new OnChildClickListener() {
		//
		// @Override
		// public boolean onChildClick(ExpandableListView parent, View v, int
		// groupPosition, int childPosition, long id) {
		// System.out.println("This happenu!");
		// System.out.println("This happened! \nGroupPos: " + groupPosition +
		// ", childPosition: " + childPosition + ", id: " + id);
		// Toast.makeText(getApplicationContext(),
		// listDataHeader.get(groupPosition) + " : " +
		// listDataChild.get(listDataHeader.get(groupPosition)).get(childPosition),
		// Toast.LENGTH_SHORT).show();
		// return false;
		// }
		// });

	}

	private void loadListData() {
		listDataHeader = new ArrayList<String>();
		listDataChild = new HashMap<String, List<String>>();

		listDataHeader.add(getString(R.string.my_page_registered_vessels));
		listDataHeader.add(getString(R.string.my_page_registered_tools));
		listDataHeader.add(getString(R.string.my_page_my_maps));
		listDataHeader.add(getString(R.string.my_page_my_notices));
		listDataHeader.add(getString(R.string.my_page_all_available_subscriptions));

		List<String> myVessels = new ArrayList<String>();
		// TODO: get registered vessels from BarentsWatch when its implemented
		if (myVessels.size() == 0) {
			myVessels.add(getString(R.string.my_page_service_not_available));
		} else {
			myVessels.add(getString(R.string.my_page_add_vessel));
		}

		List<String> myTools = new ArrayList<String>();
		// TODO: get registered tools from BarentsWatch once implemented
		if (myTools.size() == 0) {
			myTools.add(getString(R.string.my_page_service_not_available));
		} else {
			myTools.add(getString(R.string.my_page_add_tool));
		}

		List<String> myMaps = new ArrayList<String>();
		JSONArray mySubscriptions = authenticatedGetRequestToBarentswatchAPIService("v1/geodata/subscription");
		List<String> myMapsFieldsToExtract = new ArrayList<String>();
		myMapsFieldsToExtract.add("GeoDataServiceName");
		new FiskInfoUtility().appendSubscriptionItemsToView(mySubscriptions, myMaps, myMapsFieldsToExtract);

		if (myMaps.size() == 0) {

			myMaps.add(getString(R.string.my_page__no_registered_maps));
		} else {
			myMaps.add(getString(R.string.my_page_add_map));
		}

		List<String> myNotices = new ArrayList<String>();
		// TODO: get registered notices from BarentsWatch once its implemented
		if (myNotices.size() == 0) {
			myNotices.add(getString(R.string.my_page_no_registered_notices));
		} else {
			myNotices.add(getString(R.string.my_page_add_notice));
		}

		JSONArray availableGeoSubscriptions = getSharedCacheOfAvailableSubscriptions();
		if (availableGeoSubscriptions == null) {
			availableGeoSubscriptions = authenticatedGetRequestToBarentswatchAPIService(getString(R.string.my_page_geo_data_service));
			setSharedCacheOfAvailableSubscriptions(availableGeoSubscriptions);
		}

		List<String> availableSubscriptions = new ArrayList<String>();
		List<String> values = new ArrayList<String>();
		values.add("Name");
		values.add("DataOwner");
		values.add("LastUpdated");
		new FiskInfoUtility().appendSubscriptionItemsToView(availableGeoSubscriptions, availableSubscriptions, values);
		// Add header and child data
		listDataChild.put(listDataHeader.get(0), myVessels);
		listDataChild.put(listDataHeader.get(1), myTools);
		listDataChild.put(listDataHeader.get(2), myMaps);
		listDataChild.put(listDataHeader.get(3), myNotices);
		listDataChild.put(listDataHeader.get(4), availableSubscriptions);
	}

	private void createSubscriptionOnClickListeners() {
		expListView.setOnChildClickListener(new OnChildClickListener() {

			@Override
			public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
				System.out.println("magic!");

				if (((String) listAdapter.getGroup(groupPosition)).equals(getString(R.string.my_page_all_available_subscriptions)
						.toString())) {
					System.out.println("stuff!");
					listAdapter.getChild(groupPosition, childPosition);
					createSubscriptionInformationDialog(childPosition);
				}
				return true;
			}
		});
	}

	public void createSubscriptionInformationDialog(int JSONObjectIndex) {
		final Dialog dialog = new Dialog(this);
		dialog.requestWindowFeature(Window.FEATURE_LEFT_ICON);
		dialog.setContentView(R.layout.subscription_info_dialog);

		TextView subscriptionNameView = (TextView) dialog.findViewById(R.id.subscription_description_text_view);
		TextView subscriptionUpdatedView = (TextView) dialog.findViewById(R.id.subscription_last_updated_text_view);
		Button okButton = (Button) dialog.findViewById(R.id.dismiss_dialog_button);
		Button viewOnMapButton = (Button) dialog.findViewById(R.id.go_to_map_button);
		String subscriptionName = null;
		String subscriptionDescription = null;

		JSONArray subscriptions = getSharedCacheOfAvailableSubscriptions();
		List<String> updateValues = new ArrayList<String>();
		JSONObject currentSubscription;
		String lastUpdated = "";

		updateValues.add("Name");
		updateValues.add("DataOwner");
		updateValues.add("LastUpdated");
		updateValues.add("Description");
		updateValues.add("UpdateFrequencyText");

		if (subscriptions != null) {
			try {
				currentSubscription = getSharedCacheOfAvailableSubscriptions().getJSONObject(JSONObjectIndex);
				System.out.println(currentSubscription.getString("Name") + "\n" + currentSubscription.getString("DataOwner") + "\n"
						+ currentSubscription.getString("LastUpdated") + "\n" + currentSubscription.getString("UpdateFrequencyText") + "\n"
						+ currentSubscription.getString("Description"));
				subscriptionName = currentSubscription.getString("Name");
				subscriptionDescription = currentSubscription.getString("Description");
				lastUpdated = currentSubscription.get("LastUpdated").toString();
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}

		String[] updateDateAndTime = lastUpdated.split("T");
		lastUpdated = updateDateAndTime[1] + "  " + updateDateAndTime[0];

		subscriptionNameView.setText(subscriptionDescription);
		subscriptionUpdatedView.setText(lastUpdated);

		okButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				dialog.dismiss();
			}
		});

		viewOnMapButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Do some map stuff here so we only show this layer I
				// guess?
				loadView(MapActivity.class);
			}
		});

		int subscriptionIconId = getSubscriptionIconId(subscriptionName);

		dialog.setTitle(subscriptionName);
		dialog.setCanceledOnTouchOutside(false);
		dialog.show();
		if (subscriptionIconId != 0) {
			dialog.setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, subscriptionIconId);
		}

	}

	private int getSubscriptionIconId(String subscriptionName) {
		int retVal = 0;

		switch (subscriptionName) {
		case "Redskap":
			retVal = R.drawable.ikon_kystfiske;
			break;
		case "Iskant":
			retVal = R.drawable.ikon_is_tjenester;
			break;
		case "Havbunnsinstallasjoner":
			retVal = R.drawable.ikon_kart_til_din_kartplotter;
			break;
		case "Seismikk, planlagt":
			retVal = R.drawable.ikon_olje_og_gass;
			break;
		case "Seismikk, pågående":
			retVal = R.drawable.ikon_olje_og_gass;
			break;
		default:
			throw new InvalidParameterException("Parameter '" + subscriptionName + "' is not recognised.");
		}

		return retVal;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.my_page, menu);
		return true;
	}
}
