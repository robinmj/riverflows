package org.robolectric.shadows;

import android.support.v7.internal.view.SupportMenuInflater;
import android.view.Menu;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

/**
 * http://stackoverflow.com/questions/24771306/android-content-res-resourcesnotfoundexception-in-robolectric-test?lq=1
 */
@Implements(SupportMenuInflater.class)
public class ShadowSupportMenuInflater extends ShadowMenuInflater {
    @Implementation
    public void inflate(int menuRes, Menu menu) {
        super.inflate(menuRes, menu);
    }
}