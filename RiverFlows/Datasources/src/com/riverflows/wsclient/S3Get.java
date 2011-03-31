package com.riverflows.wsclient;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.riverflows.data.USState;

public abstract class S3Get {

	public static final String DEFAULT_CHARSET = "UTF8";
	
	/** 15 minutes */
	public static final long SIG_LIFETIME_S = 60 * 15;
	
	
	
	public static String getS3URL(String key,String secret, USState state) {
		
		String bucket = "riverflows-sites";
		String objectKey = state.getAbbrev() + ".csv";
		
		long expireTime = (System.currentTimeMillis() / 1000) + SIG_LIFETIME_S;
		
		String sig = generateSignedCredential(bucket, objectKey, expireTime, secret);
		
		String destURL = "https://s3.amazonaws.com/" + bucket + "/" + objectKey + "?AWSAccessKeyId="+key+"&Signature=" + sig + "&Expires=" + expireTime;
		return destURL;
	}
	
	
	public static String generateSignedCredential(String bucket, String objectKey, long expireTime, String secretKey) {
		byte[] secretKeyBytes;
		byte[] dataToSign;
		try {
			secretKeyBytes = secretKey.getBytes(DEFAULT_CHARSET);
			
			//String fmt = "EEE, dd MMM yyyy HH:mm:ss ";
			//SimpleDateFormat df = new SimpleDateFormat(fmt, Locale.US);
			//df.setTimeZone(TimeZone.getTimeZone("GMT"));
			
			StringBuilder stringToSign = new StringBuilder();
			stringToSign.append("GET").append("\n");
			stringToSign.append("").append("\n");
			stringToSign.append("text/csv").append("\n");
			//stringToSign.append(df.format(timestamp) + "GMT").append("\n");
			stringToSign.append(expireTime).append("\n");
			stringToSign.append("/" + bucket + "/" + objectKey );
			
			dataToSign = stringToSign.toString().getBytes(DEFAULT_CHARSET);
		
			Mac mac = Mac.getInstance("HmacSHA1");
	        SecretKey signingKey = new SecretKeySpec(secretKeyBytes, "HmacSHA1");
	        mac.init(signingKey);
	        
	        //return new String(Base64.encodeBase64(mac.doFinal(dataToSign)),"ASCII");
	        return null;
		} catch(NoSuchAlgorithmException nsae) {
			throw new RuntimeException(nsae);
		} catch(InvalidKeyException ike) {
			throw new RuntimeException(ike);
		} catch(UnsupportedEncodingException uee) {
			throw new RuntimeException(uee);
		}
	}
}
