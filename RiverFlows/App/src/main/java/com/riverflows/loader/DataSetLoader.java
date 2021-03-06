package com.riverflows.loader;

import android.app.Activity;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.google.inject.Inject;
import com.riverflows.App;
import com.riverflows.ViewSite;
import com.riverflows.data.Site;
import com.riverflows.data.SiteData;
import com.riverflows.data.Variable;
import com.riverflows.wsclient.DataParseException;
import com.riverflows.wsclient.DataSourceController;

import org.apache.http.NoHttpResponseException;

import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;

import io.fabric.sdk.android.Fabric;
import roboguice.RoboGuice;

/**
 * Created by robin on 12/5/14.
 */
public class DataSetLoader extends AsyncTaskLoader<SiteData> {

    private String errorMsg = null;
    private Variable variable;
    private Site site;
    private SiteData data;
    private boolean hardRefresh = false;

    @Inject
    private DataSourceController dataSourceController;

    public DataSetLoader(Activity activity, Site site, Variable variable, boolean hardRefresh) {
        super(activity);
        this.variable = variable;
        this.site = site;
        this.hardRefresh = hardRefresh;
        RoboGuice.getInjector(activity).injectMembers(this);
    }

    @Override
    protected void onStartLoading() {
        super.onStartLoading();
        if(this.data == null) {
            forceLoad();
        }
    }

    @Override
    public SiteData loadInBackground() {
        SiteData result = null;

        try {
            Variable[] variables = site.getSupportedVariables();
            if(variables.length == 0) {
                if(variable != null) {
                    variables = new Variable[]{variable};
                } else {
                    //TODO send user to update site
                    errorMsg = "Sorry, this version of RiverFlows doesn't support any of the gauges at '" + site.toString() + "'";
                    return null;
                }
            } else if(variable != null) {
                //ensure that the specified variable comes first so it is
                // guaranteed that the datasource will attempt to retrieve its data.
                boolean found = false;
                for(int a = 0; a < variables.length; a++) {
                    if(variables[a].equals(variable)) {
                        variables[a] = variables[0];
                        variables[0] = variable;
                        found = true;
                        break;
                    }
                }
                if(!found) {
                    Log.e(App.TAG, "could not find " + variable.getId() + " in supported vars for " + site.getSiteId());
                    variables = new Variable[]{variable};
                }
            }

            if(Fabric.isInitialized()) {
                Crashlytics.setString(ViewSite.KEY_SITE, site.getId());
                Crashlytics.setString(ViewSite.KEY_VARIABLE, variables[0].getId());
            }

            return this.dataSourceController.getSiteData(site, variables, this.hardRefresh);
        } catch(UnknownHostException uhe) {
            errorMsg = "Lost network connection.";
            Log.w(App.TAG, site.toString(), uhe);
        } catch(NoHttpResponseException nhre) {
            errorMsg = "No response from " + site;
            Log.w(App.TAG, site.toString(), nhre);
        } catch(SocketException se) {
            errorMsg = "Connection Timed Out";
        } catch(IOException ioe) {
            errorMsg = "Could not retrieve site data: an I/O error has occurred.";
            Log.e(App.TAG, site.getId(), ioe);

            Crashlytics.logException(ioe);
        } catch(DataParseException dpe) {
            errorMsg = "Could not process data from " + site + "; " + dpe.getMessage();
            Log.w(App.TAG, site.toString(), dpe);
        } catch(Exception e) {
            errorMsg = "Error loading data from " + site + "; " + e.getMessage();
            Log.e(App.TAG, site.toString(), e);

            Crashlytics.logException(e);
        }
        this.data = result;
        return result;
    }

    public String getErrorMsg() {
        return errorMsg;
    }
}
