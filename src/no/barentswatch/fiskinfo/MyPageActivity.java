package no.barentswatch.fiskinfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import no.barentswatch.implementation.ExpandableListAdapter;
import no.barentswatch.implementation.FiskInfoUtility;

import org.json.JSONArray;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupClickListener;
import android.widget.ExpandableListView.OnGroupCollapseListener;
import android.widget.ExpandableListView.OnGroupExpandListener;
import android.widget.Toast;

public class MyPageActivity extends BaseActivity {
	private ExpandableListAdapter listAdapter;
	private ExpandableListView expListView;
	private List<String> listDataHeader;
	private HashMap<String, List<String>> listDataChild;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.setContext(this);
		super.setPreviousSelectionActionBar(0);
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
		expListView.setOnChildClickListener(new OnChildClickListener() {

			@Override
			public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
				Toast.makeText(getApplicationContext(), listDataHeader.get(groupPosition) + " : " + listDataChild.get(listDataHeader.get(groupPosition)).get(childPosition), Toast.LENGTH_SHORT).show();
				return false;
			}
		});

	}
	
	private void loadListData() {
		listDataHeader = new ArrayList<String>();
		listDataChild = new HashMap<String, List<String>>();

		listDataHeader.add(getString(R.string.myPageRegisteredVessels));
		listDataHeader.add(getString(R.string.myPageRegisteredTools));
		listDataHeader.add(getString(R.string.myPageMaps));
		listDataHeader.add(getString(R.string.myPageMyNotices));
		listDataHeader.add(getString(R.string.allAvailableSubscriptions));

		List<String> myVessels = new ArrayList<String>();
		// TODO: get registered vessels from BarentsWatch when its implemented
		if (myVessels.size() == 0) {
			myVessels.add(getString(R.string.myPageNoRegisteredVessels));
		} else {
			myVessels.add(getString(R.string.myPageAddVessel));
		}

		List<String> myTools = new ArrayList<String>();
		// TODO: get registered tools from BarentsWatch once implemented
		if (myTools.size() == 0) {
			myTools.add(getString(R.string.myPageNoRegisteredTools));
		} else {
			myTools.add(getString(R.string.myPageAddTool));
		}

		List<String> myMaps = new ArrayList<String>();
		JSONArray mySubscriptions = authenticatedGetRequestToBarentswatchAPIService("geodatasubscription");
		List<String> myMapsFieldsToExtract = new ArrayList<String>();
		myMapsFieldsToExtract.add("GeoDataServiceName");
		new FiskInfoUtility().appendSubscriptionItemsToView(mySubscriptions, myMaps, myMapsFieldsToExtract);
		
		if (myMaps.size() == 0) {
			
			myMaps.add(getString(R.string.myPageNoRegisteredMaps));
		} else {
			myMaps.add(getString(R.string.myPageAddMap));
		}

		List<String> myNotices = new ArrayList<String>();
		// TODO: get registered notices from BarentsWatch once its implemented
		if (myNotices.size() == 0) {
			myNotices.add(getString(R.string.myPageNoRegisteredNotices));
		} else {
			myNotices.add(getString(R.string.myPageAddNotice));
		}

		JSONArray availableGeoSubscriptions = getSharedCacheOfAvailableSubscriptions();
		if(availableGeoSubscriptions == null) {
			availableGeoSubscriptions = authenticatedGetRequestToBarentswatchAPIService(getString(R.string.myPageGeoDataService));
			setSharedCacheOfAvailableSubscriptions(availableGeoSubscriptions);
		}
		
		List<String> availableSubscriptions = new ArrayList<String>();
		List<String> values = new ArrayList<String>();
		values.add("Name");
		new FiskInfoUtility().appendSubscriptionItemsToView(availableGeoSubscriptions, availableSubscriptions, values);
		// Add header and child data
		listDataChild.put(listDataHeader.get(0), myVessels);
		listDataChild.put(listDataHeader.get(1), myTools);
		listDataChild.put(listDataHeader.get(2), myMaps);
		listDataChild.put(listDataHeader.get(3), myNotices);
		listDataChild.put(listDataHeader.get(4), availableSubscriptions);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.my_page, menu);
		return true;
	}
}
