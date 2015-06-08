package no.barentswatch.baseclasses;

import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

public class ToolsGeoJson {
	private JSONObject tools;
	private String versionNumber;
	public static final String INVALID_VERSION = "INVALID VERSION";
	
	public ToolsGeoJson(Context mContext) {
		//check if exists
		SharedPreferences prefs = mContext.getSharedPreferences("no.barentswatch.fiskinfo", Context.MODE_PRIVATE);
		Boolean informationExists = prefs.getBoolean("geoJson", false);
		if (informationExists) {
			versionNumber = prefs.getString("toolsVersionNumber", null);
			String tmp = prefs.getString("toolsGeoJson", null);
			if (versionNumber == null || tmp == null) {
				tools = null;
				versionNumber = INVALID_VERSION;
			}
		} else {
			versionNumber = INVALID_VERSION;
		}
	}
	
	public ToolsGeoJson(JSONObject tools, String versionNumber) {
		this.tools = tools;
		this.versionNumber = versionNumber;
	}
	
	public JSONObject getTools() {
		return tools;
	}
	
	public String getVersionNumber() {
		return versionNumber;
	}
	
	public void setTools(JSONObject tools, String versionNumber, Context mContext) {
		Boolean updated = false;
		if (this.versionNumber == INVALID_VERSION) {
			this.tools = tools;
			this.versionNumber = versionNumber;
			updated = true;
		} else {
			if (Integer.parseInt(versionNumber) > Integer.parseInt(this.versionNumber)) {
				this.tools = tools;
				this.versionNumber = versionNumber;
				updated = true;
			}
		}
		if (updated) {
			SharedPreferences.Editor editor = mContext.getSharedPreferences("no.barentswatch.fiskinfo", Context.MODE_PRIVATE).edit();
			editor.putString("toolsGeoJson", tools.toString());
			editor.putString("toolsVersionNumber", this.versionNumber);
			editor.putBoolean("geoJson", true);
			editor.commit();
		}
	}
}
