package com.riverflows.test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import android.test.AndroidTestCase;

import com.riverflows.data.Reading;
import com.riverflows.data.Series;
import com.riverflows.data.Site;
import com.riverflows.data.SiteData;
import com.riverflows.data.SiteId;
import com.riverflows.data.USState;
import com.riverflows.data.Variable;
import com.riverflows.data.Variable.CommonVariable;
import com.riverflows.db.DatasetsDaoImpl;
import com.riverflows.db.SitesDaoImpl;
import com.riverflows.wsclient.UsgsCsvDataSource;
import com.riverflows.wsclient.UsgsXmlDataSource;

public class DatasetsDaoTest extends AndroidTestCase {
	
	public void testCreateDataset() throws Throwable {
		
		SiteData caSite = new SiteData();
		caSite.setSite(new Site());
		caSite.getSite().setSiteId(new SiteId("USGS","11303000"));
		caSite.getSite().setState(USState.CA);
		caSite.getSite().setSupportedVariables(new Variable[]{UsgsCsvDataSource.VTYPE_GAUGE_HEIGHT_FT,
				UsgsCsvDataSource.VTYPE_STREAMFLOW_CFS});
		caSite.getSite().setName("STANISLAUS R A RIPON CA");
		caSite.getSite().setLatitude(37.72965078d);
		caSite.getSite().setLongitude(-121.1104934d);
		
		Date readingDate = UsgsXmlDataSource.parseDate("2011-02-13T04:00:00.000-08:00");
		
		Series streamflowReadings = new Series();
		streamflowReadings.setVariable(UsgsCsvDataSource.VTYPE_STREAMFLOW_CFS);
		streamflowReadings.setReadings(new ArrayList<Reading>());
		Reading streamflowReading = new Reading();
		streamflowReading.setDate(readingDate);
		streamflowReading.setValue(231d);
		streamflowReadings.getReadings().add(streamflowReading);
		caSite.getDatasets().put(CommonVariable.STREAMFLOW_CFS, streamflowReadings);
		
		Series gaugeHeightReadings = new Series();
		gaugeHeightReadings.setVariable(UsgsCsvDataSource.VTYPE_GAUGE_HEIGHT_FT);
		gaugeHeightReadings.setReadings(new ArrayList<Reading>());
		Reading gaugeHeightReading = new Reading();
		streamflowReading.setDate(readingDate);
		streamflowReading.setValue(37.75d);
		streamflowReadings.getReadings().add(gaugeHeightReading);
		caSite.getDatasets().put(CommonVariable.GAUGE_HEIGHT_FT, gaugeHeightReadings);
		
		List<SiteData> caSites = new ArrayList<SiteData>();
		caSites.add(caSite);
		
		SitesDaoImpl.saveSites(getContext(), USState.CA, caSites);
		
		caSites = SitesDaoImpl.getSitesInState(getContext(), USState.CA, null);
		if(caSites.size() == 0) {
			throw new RuntimeException("CA has no sites");
		}
		
		Random r = new Random();
		
		int pos = (int)(r.nextFloat() * (float)caSites.size());
		Site testSite = caSites.get(pos).getSite();
		
		Series testDataSet = new Series();
		Variable var = testSite.getSupportedVariables()[0];
		testDataSet.setVariable(var);
		
		List<Reading> testReadings = new ArrayList<Reading>();
		
		Reading testReading1 = new Reading();
		testReading1.setDate(new Date());
		testReading1.setValue(1.4d);
		testReading1.setQualifiers("");
		testReadings.add(testReading1);

		Reading testReading2 = new Reading();
		testReading2.setDate(new Date());
		testReading2.setValue(5.4d);
		testReading2.setQualifiers("Ice");
		testReadings.add(testReading2);

		Reading testReading3 = new Reading();
		testReading3.setDate(new Date());
		testReading3.setValue(0.4d);
		testReading3.setQualifiers("");
		testReadings.add(testReading3);
		
		testDataSet.setReadings(testReadings);
		
		int siteId = testSite.getSiteId().getPrimaryKey();
		
		DatasetsDaoImpl.saveDataset(getContext(), siteId, testDataSet);
		
		Series result = DatasetsDaoImpl.getDataset(getContext(), siteId, var.getId());
		
		assertEquals(result.getVariable().getId(), var.getId());
		
		Reading r0 = result.getReadings().get(0);
		assertEquals(1.4d, r0.getValue());
		assertEquals("",r0.getQualifiers());
		
		Reading r1 = result.getReadings().get(1);
		assertEquals(5.4d, r1.getValue());
		assertEquals("Ice", r1.getQualifiers());
		
		Reading r2 = result.getReadings().get(2);
		assertEquals(0.4d, r2.getValue());
		assertEquals("",r2.getQualifiers());
		
		assertEquals(3, result.getReadings().size());
	}
}
