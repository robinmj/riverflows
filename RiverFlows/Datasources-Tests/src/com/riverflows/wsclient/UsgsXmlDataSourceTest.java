package com.riverflows.wsclient;

import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import com.riverflows.data.Reading;
import com.riverflows.data.Series;
import com.riverflows.data.Site;
import com.riverflows.data.SiteData;
import com.riverflows.data.USState;
import com.riverflows.data.Variable;
import com.riverflows.data.Variable.CommonVariable;

public class UsgsXmlDataSourceTest extends TestCase {
	
	private static final String XML_FILE_PATH = "http://localhost/USGS_state_data/";

	private static SimpleDateFormat valueDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZZZ");
	
	public void testGetAllSites() throws Throwable {
		for(int a = 0; a < USState.values().length; a++) {
			USState state = USState.values()[a];
			
			String url = XML_FILE_PATH + state.getAbbrev() + ".xml";
			
			Map<String,SiteData> data = UsgsXmlDataSource.getSiteData(url,false);
			Site site = data.values().iterator().next().getSite();
			USState assignedState = site.getState();
			
			//TODO remove Puerto Rico from the list of states?
			if(state != USState.PR) {
				assertEquals(state, assignedState);
			}
		}
	}

	public void testGetSites() throws Throwable {
		
		String url = XML_FILE_PATH + "AL.xml";
			
		Map<String,SiteData> data = UsgsXmlDataSource.getSiteData(url,false);
		
		SiteData d1 = data.get("02428400");
		
		assertEquals(USState.AL, d1.getSite().getState());
		assertEquals("02428400", d1.getSite().getId());
		assertEquals(31.61515849d, d1.getSite().getLatitude());
        assertEquals(-87.550548d, d1.getSite().getLongitude());
        assertEquals("ALABAMA RIVER AT CLAIBORNE L&D NEAR MONROEVILLE", d1.getSite().getName());
        
        Set<String> expectedVars = new HashSet<String>();
        expectedVars.add(UsgsCsvDataSource.VTYPE_STREAMFLOW_CFS.getId());
        expectedVars.add(UsgsCsvDataSource.VTYPE_GAUGE_HEIGHT_FT.getId());
        
        Variable[] supportedVars = d1.getSite().getSupportedVariables();
        for(int a = 0; a < supportedVars.length; a++) {
        	assertTrue("unexpected variable: " + supportedVars[a].getId() + " " + supportedVars[a].getName(), expectedVars.contains(supportedVars[a].getId()));
        }
		
		USState state = USState.CO;
		url = XML_FILE_PATH + state.getAbbrev() + ".xml";
		
		data = UsgsXmlDataSource.getSiteData(url,false);
		
		SiteData silverton = data.get("09359020");
		Series ht = silverton.getDatasets().get(CommonVariable.GAUGE_HEIGHT_FT);
		Reading recent = ht.getReadings().get(0);
		//qualifiers are not currently supported by UsgsXmlDataSource
		//assertEquals("P Ice", recent.getQualifiers());
		//assertEquals(null, recent.getValue());
		//assertEquals("2011-02-13T06:00:00.000-0700", valueDateFormat.format(recent.getDate()));

		state = USState.WY;
		url = XML_FILE_PATH + state.getAbbrev() + ".xml";
		
		data = UsgsXmlDataSource.getSiteData(url,false);
		
		SiteData yellowstone = data.get("06186500");
		Series cfs = yellowstone.getDatasets().get(CommonVariable.STREAMFLOW_CFS);
		recent = cfs.getReadings().get(0);
		//qualifiers are not currently supported by UsgsXmlDataSource
		//assertEquals("P Ice", recent.getQualifiers());
		assertEquals(null, recent.getValue());
		assertEquals("2011-12-26T19:30:00.000-0700", valueDateFormat.format(recent.getDate()));
		
	}
}
