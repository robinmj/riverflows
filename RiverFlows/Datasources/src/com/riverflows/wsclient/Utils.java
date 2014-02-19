package com.riverflows.wsclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;

public class Utils {
	
	/**
	 * Different from String.split() in that it interprets two consecutive occurrances of a separator
	 * as two separators delimiting an empty string, rather than as a single separator.  In other words,
	 * the length of the resulting array will always be s + 1 where s is the number of occurances of
	 * the separator in str.
	 * @param str
	 * @param separator
	 * @return
	 */
	public static String[] split(String str, char separator) {
		ArrayList<String> pieces = new ArrayList<String>(10);
		
		StringBuilder currentPiece = new StringBuilder();
		for(int index = 0; index < str.length(); index++) {
			if(str.charAt(index) == separator) {
				pieces.add(currentPiece.toString());
				currentPiece = new StringBuilder();
			} else {
				currentPiece.append(str.charAt(index));
			}
		}
		
		if(currentPiece != null) {
			pieces.add(currentPiece.toString());
		}
		
		String[] result = new String[pieces.size()];
		pieces.toArray(result);
		return result;
	}
	
	public static String join(String separator, String...words) {
		StringBuilder result = new StringBuilder();
		for(String currentWord: words) {
			if(currentWord == null || currentWord.length() == 0) {
				continue;
			}
			result.append(currentWord);
			result.append(separator);
		}
		if(result.length() > separator.length()) {
			result.setLength(result.length() - separator.length());
		}
		
		return result.toString();
	}
	
	public static String abbreviateNumber(double number, int sigfigs) {
		
		if(number == 0.0) {
			return "0";
		}
		
		double value = number;
		double magnitude = Math.log10(Math.abs(value));
		
		if(magnitude >= (sigfigs - 1)) {
			//round down to the nearest whole number, drop the trailing zero
			return ((int)number) + "";
		}
		
		double factor = Math.pow(10, sigfigs - Math.ceil(magnitude));
		
		if(value > 0.0) {
			value = Math.floor(value * factor);
		} else {
			value = Math.ceil(value * factor);
		}
		value = value / factor;
		
		return value + "";
	}

	public static String encodeUrl(Map<String, List<String>> parameters) {
		if(parameters == null) {
			return "";
		}

		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (String key : parameters.keySet()) {

			List<String> values = parameters.get(key);
			if (values != null) {
				for(String value : values) {
					if(first) {
						first = false;
					} else {
						sb.append("&");
					}

					sb.append(URLEncoder.encode(key) + "=" + URLEncoder.encode(value));
				}
			}
		}
		return sb.toString();
	}
	
	public static String responseString(HttpResponse response) throws IOException {
		InputStream responseStream = response.getEntity().getContent();
        
        //Header[] lengthHeaders = response.getHeaders("content-length");
        //Header[] lengthHeaders = response.getHeaders("character-encoding");
		
		return getString(responseStream);
	}
	
	public static String getString(InputStream in) throws IOException {
		StringBuilder sb = new StringBuilder();
        BufferedReader r = new BufferedReader(new InputStreamReader(in), 4096);
        for (String line = r.readLine(); line != null; line = r.readLine()) {
                sb.append(line);
        }
        in.close();
        return sb.toString();
	}
}
