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
package no.barentswatch.implementation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.json.JSONException;
import org.json.JSONObject;

public class ToolsInfo {
	private static ToolsInfo instance = null;

	String geoJson;
	private String versionNumber;

	private ToolsInfo() {
		try {
			geoJson = getStringFromFile("redskapsInfoJSON.json");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static ToolsInfo get() {
		if (instance == null)
			instance = getSync();
		return instance;
	}

	private static synchronized ToolsInfo getSync() {
		if (instance == null)
			instance = new ToolsInfo();
		return instance;
	}

	public static String convertStreamToString(InputStream is) throws Exception {
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		StringBuilder sb = new StringBuilder();
		String line = null;
		while ((line = reader.readLine()) != null) {
			sb.append(line).append("\n");
		}
		reader.close();
		return sb.toString();
	}

	public JSONObject getGeoJson() {
		JSONObject obj = null;
		try {
			obj = new JSONObject(geoJson);
		} catch (JSONException e) {
			System.out.println("Jesus we failed");
			e.printStackTrace();
		}
		System.out.println(geoJson);
		return obj;

	}

	public static String getStringFromFile(String filePath) throws Exception {
		 InputStream fin = (FiskInfoApp.getInstance().getAssets().open(filePath));
	     String ret = convertStreamToString(fin);
	     //Make sure you close all streams.
	     fin.close();        
	     return ret;
	}
}