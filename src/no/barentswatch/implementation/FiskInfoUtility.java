package no.barentswatch.implementation;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.StreamCorruptedException;
import java.math.BigDecimal;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import no.barentswatch.interfaces.UtilityInterface;

public class FiskInfoUtility implements UtilityInterface {
	private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

	// copy from InputStream
	// -----------------------------------------------------------------------
	/**
	 * Copy bytes from an <code>InputStream</code> to an
	 * <code>OutputStream</code>.
	 * <p>
	 * This method buffers the input internally, so there is no need to use a
	 * <code>BufferedInputStream</code>.
	 * <p>
	 * Large streams (over 2GB) will return a bytes copied value of
	 * <code>-1</code> after the copy has completed since the correct number of
	 * bytes cannot be returned as an int. For large streams use the
	 * <code>copyLarge(InputStream, OutputStream)</code> method.
	 * 
	 * @param input
	 *            the <code>InputStream</code> to read from
	 * @param output
	 *            the <code>OutputStream</code> to write to
	 * @return the number of bytes copied
	 * @throws NullPointerException
	 *             if the input or output is null
	 * @throws IOException
	 *             if an I/O error occurs
	 * @throws ArithmeticException
	 *             if the byte count is too large
	 */
	@Override
	public int copy(InputStream input, OutputStream output) throws IOException {
		long count = copyLarge(input, output);
		if (count > Integer.MAX_VALUE) {
			return -1;
		}
		return (int) count;
	}

	/**
	 * Copy bytes from a large (over 2GB) <code>InputStream</code> to an
	 * <code>OutputStream</code>.
	 * <p>
	 * This method buffers the input internally, so there is no need to use a
	 * <code>BufferedInputStream</code>.
	 * 
	 * @param input
	 *            the <code>InputStream</code> to read from
	 * @param output
	 *            the <code>OutputStream</code> to write to
	 * @return the number of bytes copied
	 * @throws NullPointerException
	 *             if the input or output is null
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	@Override
	public long copyLarge(InputStream input, OutputStream output) throws IOException {
		byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
		long count = 0;
		int n = 0;
		while (-1 != (n = input.read(buffer))) {
			output.write(buffer, 0, n);
			count += n;
		}
		return count;
	}

	// read toByteArray
	// -----------------------------------------------------------------------
	/**
	 * Get the contents of an <code>InputStream</code> as a <code>byte[]</code>.
	 * <p>
	 * This method buffers the input internally, so there is no need to use a
	 * <code>BufferedInputStream</code>.
	 * 
	 * @param input
	 *            the <code>InputStream</code> to read from
	 * @return the requested byte array
	 * @throws NullPointerException
	 *             if the input is null
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	@Override
	public byte[] toByteArray(InputStream input) throws IOException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		copy(input, output);
		return output.toByteArray();
	}

	// Serialize Fiskinfo Polygon2D
	// -----------------------------------------------------------------------
	/**
	 * Get the contents of an <code>FiskInfoPolygon2D</code> and writes it to
	 * disk This method catches all <code>Exceptions</code> internally,
	 * therefore it should be usable directly
	 * 
	 * @param path
	 *            the <code>Path</code> of the file to write
	 * @param polygon
	 *            the <code>FiskInfoPolygon2d</code> which is written to disk
	 */
	public void serializeFiskInfoPolygon2D(String path, FiskInfoPolygon2D polygon) {
		try {
			FileOutputStream fileOut = new FileOutputStream(path);
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(polygon);
			out.close();
			fileOut.close();
			Log.d("FiskInfo", "Serialization successfull, the data should be stored in the specified path");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Deserialize Fiskinfo Polygon2D
	// -----------------------------------------------------------------------
	/**
	 * Retrieve a serialized <code>FiskInfoPolygon2D</code> from disk as a
	 * <code>FiskInfoPolygon2D class</code>
	 * 
	 * @param path
	 *            the <code>Path</code> to read from
	 * @return the Requested <code>FiskInfoPolygon2D</code>
	 */
	public FiskInfoPolygon2D deserializeFiskInfoPolygon2D(String path) {
		FiskInfoPolygon2D polygon = null;
		try {
			FileInputStream fileIn = new FileInputStream(path);
			ObjectInputStream in = new ObjectInputStream(fileIn);
			polygon = (FiskInfoPolygon2D) in.readObject();

			in.close();
			fileIn.close();
			Log.d("FiskInfo", "Deserialization successfull, the data should be stored in the inputclass");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (StreamCorruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			System.out.println("Couldnt find class. I am doing something wrong I guess");
			e.printStackTrace();
		}
		return polygon;
	}
	
	/**
	 * Truncates a double value to the number of decimals given
	 * 
	 * @param number
	 * 			The number to truncate
	 * @param numberofDecimals
	 * 			Number of decimals of the truncated number
	 * @return
	 */
	public Double truncateDecimal(double number, int numberofDecimals) {
		if (number > 0) {
			return new BigDecimal(String.valueOf(number)).setScale(numberofDecimals, BigDecimal.ROUND_FLOOR).doubleValue();
		} else {
			return new BigDecimal(String.valueOf(number)).setScale(numberofDecimals, BigDecimal.ROUND_CEILING).doubleValue();
		}
	}
	
	/**
	 * 
	 */
	public void appendSubscriptionItemsToView(JSONArray subscriptions, List<String> field, List<String> fieldsToExtract) {
		if ((subscriptions.isNull(0)) || (subscriptions == null)) {
			return;
		}
		for (int i = 0; i < subscriptions.length(); i++) {
			try {
				JSONObject currentSubscription = subscriptions.getJSONObject(i);
				for (String fieldValue : fieldsToExtract) {
					field.add(currentSubscription.getString(fieldValue));
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}
	
}
