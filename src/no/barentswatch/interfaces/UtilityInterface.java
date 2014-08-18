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
