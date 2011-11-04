/**
 * 
 */
package com.riverflows.widget;

import java.util.concurrent.atomic.AtomicReference;

import android.app.Service;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.vending.licensing.AESObfuscator;
import com.android.vending.licensing.LicenseChecker;
import com.android.vending.licensing.LicenseCheckerCallback;
import com.android.vending.licensing.ServerManagedPolicy;

public class LicenseCheckService extends Service implements LicenseCheckerCallback {

	public enum Status {
		CHECKING(),PASSED(),FAILED(),ERROR()
	}
	
	private static final String BASE64_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAjU6i3DSyuvUyS0VN8SgT/UeRwFej7Mou7KyRQ5U0ZFpKgtd9EH0XVNLDW43NhE9NMpmjqracBW8eH/sETTjmNQXgG0dUqgVCv30FOqADPgmXxbqh5nOk2m80SwRe6ziCSBfrOCAdsRO2c65S14wsXUzTvzANTwLhNmi2W0ufJDqu6iXfpeSufvK+1B840ffeJEeXwWMSpG5rxIc0V3NtzIfqyv9NLyMTeX9RUtnRtmtWfAUIfdPypaFjdptc4A1qDxX2l8lFUo3n1Nb+U16O+YJEhSIL/5+R8qQDQWiIM2OWsKlm55qzTQTiXcc18Xpz6hrxbPeuCcovRQ0UEZJNawIDAQAB";
	
	private static final byte[] SALT = new byte[]{-93,17,46,17,109,10,85,12,-25,31,102,108,51,-15,-87,-42,-111,100,-20,-119};
	
	/**
	 * May be unexpectedly set to null
	 */
    private static volatile LicenseChecker mChecker;
	
	private static AtomicReference<LicenseCheckService.Status> status = new AtomicReference<LicenseCheckService.Status>();
    
    private static ApplicationErrorCode errorCode = null;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		
		LicenseChecker lChecker = mChecker;
		
		if(lChecker == null) {
			
			TelephonyManager tManager = (TelephonyManager)this.getSystemService(Service.TELEPHONY_SERVICE);
			String deviceId = "";
			
			if(tManager != null) {
				deviceId += tManager.getDeviceId();
			}
			
			WifiManager wManager = (WifiManager)this.getSystemService(Service.WIFI_SERVICE);
			
			if(wManager != null) {
				deviceId += deviceId + wManager.getConnectionInfo().getMacAddress();
			}
			
			Log.d(Provider.TAG,"creating license checker");
				
	        // Construct the LicenseChecker with a Policy.
			lChecker = new LicenseChecker(
	        		this, new ServerManagedPolicy(this,
	                new AESObfuscator(SALT, getClass().getPackage().getName(), deviceId)),
	            BASE64_PUBLIC_KEY  // Your public licensing key.
	            );
			mChecker = lChecker;
		}

		status.compareAndSet(null, Status.CHECKING);
		lChecker.checkAccess(this);
	}
	
	@Override
	public void dontAllow() {
		Log.d(Provider.TAG,"dontAllow");
		LicenseCheckService.status.set(Status.FAILED);
		LicenseCheckService.errorCode = null;
		
		sendBroadcast(Provider.getUpdateIntent(this));
	}
	
	@Override
	public void applicationError(ApplicationErrorCode errorCode) {
		Log.d(Provider.TAG,"applicationError: " + errorCode);
		LicenseCheckService.status.set(Status.ERROR);
		LicenseCheckService.errorCode = errorCode;

		sendBroadcast(Provider.getUpdateIntent(this));
	}
	
	@Override
	public void allow() {
		Log.d(Provider.TAG,"allow");
		LicenseCheckService.status.set(Status.PASSED);
		LicenseCheckService.errorCode = null;

		sendBroadcast(Provider.getUpdateIntent(this));
	}
	
	public static Status getStatus() {
		return status.get();
	}
	
	public static void reset() {
		mChecker = null;
		status.set(null);
		errorCode = null;
	}
	
	public static ApplicationErrorCode getErrorCode() {
		return errorCode;
	}
}