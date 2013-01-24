package com.riverflows.wsclient;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.http.client.ClientProtocolException;

import com.riverflows.data.Favorite;
import com.riverflows.data.Site;
import com.riverflows.data.SiteData;
import com.riverflows.data.SiteId;
import com.riverflows.data.Variable;

public interface DataSource {
	/**
	 * 
	 * @return string identifier of the agency from which this data source downloads its data.
	 */
	public String getAgency();
	
	public Variable getVariable(String variableId);
	
	/**
	 * @return an array of variables accepted by this agency, ordered in terms of preference
	 *  for preview purposes.
	 */
	public Variable[] getAcceptedVariables();
	
	/**
	 * Download the last readings for a list of sites and their respective variables
	 * @param hardRefresh TODO
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public Map<SiteId,SiteData> getSiteData(List<Favorite> sites, boolean hardRefresh) throws ClientProtocolException, IOException;
	
	/**
	 * Download readings from the last week for a given site and variables.
	 * @param site
	 * @param variableTypes a suggestion of the variables to retrieve, in order of preference.  The DataSource
	 * implementation may truncate this array if it only supports retrieving data for a limited number of variables
	 * at once, but it will always attempt to retrieve data for the first variable.
	 * @param hardRefresh TODO
	 * @return 
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public SiteData getSiteData(Site site, Variable[] variableTypes, boolean hardRefresh) throws ClientProtocolException, IOException;
}
