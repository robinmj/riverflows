package com.riverflows.wsclient;

import java.util.ArrayList;

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
}
