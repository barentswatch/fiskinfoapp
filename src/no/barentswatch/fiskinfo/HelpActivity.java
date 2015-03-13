package no.barentswatch.fiskinfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import no.barentswatch.implementation.ExpandableListAdapter;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupClickListener;
import android.widget.ExpandableListView.OnGroupCollapseListener;
import android.widget.ExpandableListView.OnGroupExpandListener;
import android.widget.Toast;

/**
 * This activity display help options to the user. It has been created with
 * segmented tutorials in mind.
 * 
 */
public class HelpActivity extends BaseActivity {

	@SuppressWarnings("unused")
	private static int currentTutorial = 0;
	private ExpandableListAdapter listAdapter;
	private ExpandableListView expListView;
	private List<String> listDataHeader;
	private HashMap<String, List<String>> listDataChild;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.setContext(this);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_help_activity);

		// Retrieve the list view
		expListView = (ExpandableListView) findViewById(R.id.expandableListView1);

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

	// TODO: the data for this function does not exist. It should be written/created and then implemented.
	private void loadListData() {
		listDataHeader = new ArrayList<String>();
		listDataChild = new HashMap<String, List<String>>();

		// TODO: Add the main tutorials here
		listDataHeader.add("FiskInfo Funksjonalitet");
		listDataHeader.add("Barentswatch Funksjonalitet");

		// TODO: Add sub-tutorials here :: FiskInfo
		List<String> fiskInfoFunc = new ArrayList<String>();
		fiskInfoFunc.add("Placeholder 1");
		fiskInfoFunc.add("Placeholder 2");
		fiskInfoFunc.add("Placeholder 3");

		// TODO: Add sub-tutorials here :: Barentswatch
		List<String> bwFunc = new ArrayList<String>();
		bwFunc.add("BwPlaceholder 1");
		bwFunc.add("BwPlaceholder 2");
		bwFunc.add("BwPlaceholder 3");

		// TODO: Add header and child data
		listDataChild.put(listDataHeader.get(0), fiskInfoFunc);
		listDataChild.put(listDataHeader.get(1), bwFunc);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.help_activity, menu);
		return true;
	}

}
