package com.riverflows.factory;

import com.riverflows.data.Reading;
import com.riverflows.data.Series;
import com.riverflows.data.SiteData;
import com.riverflows.data.Variable;
import com.riverflows.wsclient.CODWRDataSource;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.TimeZone;

/**
 * Created by robin on 11/16/14.
 */
public final class SiteDataFactory {

    private static SiteData clearCreekData = null;

    public static SiteData getClearCreekData() {

        if(clearCreekData != null) return clearCreekData;

        SiteData d = new SiteData();
        d.setSite(SiteFactory.getClearCreek());
        Map<Variable.CommonVariable, Series> datasets = d.getDatasets();

        Series cfs = new Series();
        cfs.setReadings(new ArrayList<Reading>());
        cfs.setVariable(CODWRDataSource.VTYPE_STREAMFLOW_CFS);

        datasets.put(CODWRDataSource.VTYPE_STREAMFLOW_CFS.getCommonVariable(), cfs);

        Series ht = new Series();
        ht.setReadings(new ArrayList<Reading>());
        ht.setVariable(CODWRDataSource.VTYPE_GAUGE_HEIGHT_FT);

        datasets.put(CODWRDataSource.VTYPE_GAUGE_HEIGHT_FT.getCommonVariable(), ht);


        GregorianCalendar cal = new GregorianCalendar();
        cal.set(2011, Calendar.MARCH, 7, 13, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.setTimeZone(TimeZone.getTimeZone("GMT-07:00"));

        Reading r = new Reading();
        r.setDate(cal.getTime());
        r.setQualifiers("B");

        cfs.getReadings().add(r);

        r = new Reading();
        r.setDate(cal.getTime());
        r.setValue(-541.18d);

        ht.getReadings().add(r);

        cal.set(2011, Calendar.MARCH, 7, 13, 30, 0);

        r = new Reading();
        r.setDate(cal.getTime());
        r.setQualifiers("B");

        cfs.getReadings().add(r);

        r = new Reading();
        r.setDate(cal.getTime());
        r.setValue(-312.08d);

        ht.getReadings().add(r);

        cal.set(2011, Calendar.MARCH, 7, 13, 45, 0);

        r = new Reading();
        r.setDate(cal.getTime());
        r.setQualifiers("E");

        cfs.getReadings().add(r);

        r = new Reading();
        r.setDate(cal.getTime());
        r.setValue(1077.06d);

        ht.getReadings().add(r);

        return clearCreekData = d;
    }
}
