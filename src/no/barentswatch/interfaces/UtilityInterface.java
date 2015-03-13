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
package no.barentswatch.interfaces;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import org.json.JSONArray;

import no.barentswatch.implementation.FiskInfoPolygon2D;

public interface UtilityInterface {
	public int copy(InputStream input, OutputStream output) throws IOException;
	public long copyLarge(InputStream input, OutputStream output) throws IOException;
	public byte[] toByteArray(InputStream input) throws IOException;
	public void serializeFiskInfoPolygon2D(String path, FiskInfoPolygon2D polygon);
	public FiskInfoPolygon2D deserializeFiskInfoPolygon2D(String path);
	public Double truncateDecimal(double x, int numberofDecimals);
	public void appendSubscriptionItemsToView(JSONArray subscriptions, List<String> field, List<String> fieldsToExtract);
	public boolean checkCoordinates(String coordinates, String projection);
}
