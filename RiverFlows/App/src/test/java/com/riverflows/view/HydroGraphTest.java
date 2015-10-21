package com.riverflows.view;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

import com.riverflows.RobolectricGradleTestRunner;
import com.riverflows.data.Category;
import com.riverflows.data.DecoratedCategory;
import com.riverflows.data.Site;
import com.riverflows.data.SiteData;
import com.riverflows.data.SiteId;
import com.riverflows.data.USState;
import com.riverflows.data.Variable;
import com.riverflows.factory.SiteFactory;
import com.riverflows.wsclient.DataSourceController;
import com.riverflows.wsclient.UsgsCsvDataSource;

import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.robolectric.Robolectric;

import java.io.File;
import java.io.FileInputStream;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyFloat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricGradleTestRunner.class)
public class HydroGraphTest {
    class YAxisLabelMatcher extends ArgumentMatcher<String> {

        private String[] expectedLabels;

        private float max;
        private float min;

        public YAxisLabelMatcher(String[] expectedLabels) {
            this.expectedLabels = expectedLabels;
            this.min = Float.parseFloat(expectedLabels[0]);
            this.max = Float.parseFloat(expectedLabels[expectedLabels.length-1]);
        }

        @Override
        public boolean matches(Object label) {
            try {
                float labelNum = Float.parseFloat((String) label);

                //not a y axis label
                if(labelNum < this.min) {
                    System.out.println("not a y axis label value " + labelNum);
                    return false;
                }
                if(labelNum > this.max) {
                    System.out.println("not a y axis label value " + labelNum);
                    return false;
                }
            } catch(NumberFormatException nfe) {
                System.out.println("not a y axis label " + nfe.toString());
                return false;
            }

            for(String expectedLabel : expectedLabels) {
                if(label.equals(expectedLabel)) {
                    System.out.println("expected " + label);
                    return true;
                }
            }
            System.err.println("not expected " + label);

            return false;
        }
    }

    private static final UsgsCsvDataSource src = new UsgsCsvDataSource();

    private HydroGraph graph;
    private Canvas mockCanvas;

    @Before
    public void setup() {
        graph = new HydroGraph(Robolectric.application);
    }

    private void initGraphLayout() {
        graph.measure(1073742904,1073743282);
        graph.layout(0,0,1080,1458);
    }

    @Test
    public void testSmallYAxisLabels() throws Throwable {

        File responseFile = new File("testdata/vallecito.csv");

        HttpResponse response = new BasicHttpResponse(new BasicStatusLine(
                new ProtocolVersion("HTTP", 1, 1), 200, ""));
        response.setStatusCode(200);

        response.setEntity(new InputStreamEntity(new FileInputStream(responseFile), responseFile.length()));

        Robolectric.addPendingHttpResponse(response);

        Site vallecito = SiteFactory.getVallecitoCreek();
        Variable[] vars = vallecito.getSupportedVariables();

        SiteData data = src.getSiteData(vallecito,vars,true);

        mockCanvas = mock(Canvas.class);

        graph.setSeries(data.getDatasets().get(vars[1].getCommonVariable()), false);
        initGraphLayout();

        graph.onDraw(mockCanvas);

        String[] yLabels = {"1.8","1.85","1.9","1.95","2","2.05","2.1","2.15","2.2","2.25","2.3"};

        verify(mockCanvas, atLeast(11)).drawText(argThat(new YAxisLabelMatcher(yLabels)), anyFloat(), anyFloat(), any(Paint.class));
    }

    @Test
    public void testNullReadings() throws Throwable {

        File responseFile = new File("testdata/eqp_failure.csv");

        HttpResponse response = new BasicHttpResponse(new BasicStatusLine(
                new ProtocolVersion("HTTP", 1, 1), 200, ""));
        response.setStatusCode(200);

        response.setEntity(new InputStreamEntity(new FileInputStream(responseFile), responseFile.length()));

        Robolectric.addPendingHttpResponse(response);

        Site vallecito = SiteFactory.getVallecitoCreek();
        Variable[] vars = vallecito.getSupportedVariables();

        SiteData data = src.getSiteData(vallecito,vars,true);

        mockCanvas = mock(Canvas.class);

        graph.setSeries(data.getDatasets().get(vars[1].getCommonVariable()), false);
        initGraphLayout();

        graph.onDraw(mockCanvas);

        String[] yLabels = {"0.8","0.9","1","1.1","1.2","1.3","1.4","1.5","1.6","1.7"};

        verify(mockCanvas, atLeast(10)).drawText(argThat(new YAxisLabelMatcher(yLabels)), anyFloat(), anyFloat(), any(Paint.class));
    }

    @Test
    public void testSmallYAxisLabelsWithZeroMin() throws Throwable {

        File responseFile = new File("testdata/vallecito.csv");

        HttpResponse response = new BasicHttpResponse(new BasicStatusLine(
                new ProtocolVersion("HTTP", 1, 1), 200, ""));
        response.setStatusCode(200);

        response.setEntity(new InputStreamEntity(new FileInputStream(responseFile), responseFile.length()));

        Robolectric.addPendingHttpResponse(response);

        Site vallecito = SiteFactory.getVallecitoCreek();
        Variable[] vars = vallecito.getSupportedVariables();

        SiteData data = src.getSiteData(vallecito,vars,true);

        mockCanvas = mock(Canvas.class);

        graph.setSeries(data.getDatasets().get(vars[1].getCommonVariable()), true);
        initGraphLayout();

        graph.onDraw(mockCanvas);

        String[] yLabels = {"0","0.5","1","1.5","2","2.5","3","3.5","4","4.5","5"};

        verify(mockCanvas, atLeast(11)).drawText(argThat(new YAxisLabelMatcher(yLabels)), anyFloat(), anyFloat(), any(Paint.class));
    }

    @Test
    public void testMedYAxisLabels() throws Throwable {

        File responseFile = new File("testdata/vallecito.csv");

        HttpResponse response = new BasicHttpResponse(new BasicStatusLine(
                new ProtocolVersion("HTTP", 1, 1), 200, ""));
        response.setStatusCode(200);

        response.setEntity(new InputStreamEntity(new FileInputStream(responseFile), responseFile.length()));

        Robolectric.addPendingHttpResponse(response);

        Site vallecito = SiteFactory.getVallecitoCreek();
        Variable[] vars = vallecito.getSupportedVariables();

        SiteData data = src.getSiteData(vallecito,vars,true);

        mockCanvas = mock(Canvas.class);

        graph.setSeries(data.getDatasets().get(vars[0].getCommonVariable()), false);
        initGraphLayout();

        graph.onDraw(mockCanvas);

        String[] yLabels = {"200","220","240","260","280","300","320","340","360","380","400"};

        verify(mockCanvas, atLeast(11)).drawText(argThat(new YAxisLabelMatcher(yLabels)), anyFloat(), anyFloat(), any(Paint.class));

    }

    @Test
    public void testMedYAxisLabelsWithZeroMin() throws Throwable {

        File responseFile = new File("testdata/vallecito.csv");

        HttpResponse response = new BasicHttpResponse(new BasicStatusLine(
                new ProtocolVersion("HTTP", 1, 1), 200, ""));
        response.setStatusCode(200);

        response.setEntity(new InputStreamEntity(new FileInputStream(responseFile), responseFile.length()));

        Robolectric.addPendingHttpResponse(response);

        Site vallecito = SiteFactory.getVallecitoCreek();
        Variable[] vars = vallecito.getSupportedVariables();

        SiteData data = src.getSiteData(vallecito,vars,true);

        mockCanvas = mock(Canvas.class);

        graph.setSeries(data.getDatasets().get(vars[0].getCommonVariable()), true);
        initGraphLayout();

        graph.onDraw(mockCanvas);

        verify(mockCanvas).drawText(eq("0"), anyFloat(), anyFloat(), any(Paint.class));
        verify(mockCanvas).drawText(eq("100"), anyFloat(), anyFloat(), any(Paint.class));
        verify(mockCanvas).drawText(eq("150"), anyFloat(), anyFloat(), any(Paint.class));
        verify(mockCanvas).drawText(eq("200"), anyFloat(), anyFloat(), any(Paint.class));
        verify(mockCanvas).drawText(eq("250"), anyFloat(), anyFloat(), any(Paint.class));
        verify(mockCanvas).drawText(eq("300"), anyFloat(), anyFloat(), any(Paint.class));
        verify(mockCanvas).drawText(eq("350"), anyFloat(), anyFloat(), any(Paint.class));
        verify(mockCanvas).drawText(eq("400"), anyFloat(), anyFloat(), any(Paint.class));
        verify(mockCanvas).drawText(eq("450"), anyFloat(), anyFloat(), any(Paint.class));
        verify(mockCanvas).drawText(eq("500"), anyFloat(), anyFloat(), any(Paint.class));
    }

    @Test
    public void testHighYAxisLabels() throws Throwable {

        File responseFile = new File("testdata/southplatte.csv");

        HttpResponse response = new BasicHttpResponse(new BasicStatusLine(
                new ProtocolVersion("HTTP", 1, 1), 200, ""));
        response.setStatusCode(200);

        response.setEntity(new InputStreamEntity(new FileInputStream(responseFile), responseFile.length()));

        Robolectric.addPendingHttpResponse(response);

        Site southPlatte = SiteFactory.getSouthPlatteAtNorthPlatte();
        Variable[] vars = southPlatte.getSupportedVariables();

        SiteData data = src.getSiteData(southPlatte,vars,true);

        mockCanvas = mock(Canvas.class);

        graph.setSeries(data.getDatasets().get(vars[0].getCommonVariable()), false);
        initGraphLayout();

        graph.onDraw(mockCanvas);

        verify(mockCanvas).drawText(eq("9K"), anyFloat(), anyFloat(), any(Paint.class));
        verify(mockCanvas).drawText(eq("10K"), anyFloat(), anyFloat(), any(Paint.class));
        verify(mockCanvas).drawText(eq("11K"), anyFloat(), anyFloat(), any(Paint.class));
        verify(mockCanvas).drawText(eq("12K"), anyFloat(), anyFloat(), any(Paint.class));
        verify(mockCanvas).drawText(eq("13K"), anyFloat(), anyFloat(), any(Paint.class));
        verify(mockCanvas).drawText(eq("14K"), anyFloat(), anyFloat(), any(Paint.class));
        verify(mockCanvas).drawText(eq("15K"), anyFloat(), anyFloat(), any(Paint.class));
        verify(mockCanvas).drawText(eq("16K"), anyFloat(), anyFloat(), any(Paint.class));
        verify(mockCanvas).drawText(eq("17K"), anyFloat(), anyFloat(), any(Paint.class));
        verify(mockCanvas).drawText(eq("18K"), anyFloat(), anyFloat(), any(Paint.class));
        verify(mockCanvas).drawText(eq("19K"), anyFloat(), anyFloat(), any(Paint.class));

    }

    @Test
    public void testHighYAxisLabelsWithZeroMin() throws Throwable {

        File responseFile = new File("testdata/southplatte.csv");

        HttpResponse response = new BasicHttpResponse(new BasicStatusLine(
                new ProtocolVersion("HTTP", 1, 1), 200, ""));
        response.setStatusCode(200);

        response.setEntity(new InputStreamEntity(new FileInputStream(responseFile), responseFile.length()));

        Robolectric.addPendingHttpResponse(response);

        Site southPlatte = SiteFactory.getSouthPlatteAtNorthPlatte();
        Variable[] vars = southPlatte.getSupportedVariables();

        SiteData data = src.getSiteData(southPlatte,vars,true);

        mockCanvas = mock(Canvas.class);

        graph.setSeries(data.getDatasets().get(vars[0].getCommonVariable()), true);
        initGraphLayout();

        graph.onDraw(mockCanvas);

        verify(mockCanvas).drawText(eq("0"), anyFloat(), anyFloat(), any(Paint.class));
        verify(mockCanvas).drawText(eq("2K"), anyFloat(), anyFloat(), any(Paint.class));
        verify(mockCanvas).drawText(eq("4K"), anyFloat(), anyFloat(), any(Paint.class));
        verify(mockCanvas).drawText(eq("6K"), anyFloat(), anyFloat(), any(Paint.class));
        verify(mockCanvas).drawText(eq("8K"), anyFloat(), anyFloat(), any(Paint.class));
        verify(mockCanvas).drawText(eq("10K"), anyFloat(), anyFloat(), any(Paint.class));
        verify(mockCanvas).drawText(eq("12K"), anyFloat(), anyFloat(), any(Paint.class));
        verify(mockCanvas).drawText(eq("14K"), anyFloat(), anyFloat(), any(Paint.class));
        verify(mockCanvas).drawText(eq("16K"), anyFloat(), anyFloat(), any(Paint.class));
        verify(mockCanvas).drawText(eq("18K"), anyFloat(), anyFloat(), any(Paint.class));
        verify(mockCanvas).drawText(eq("20K"), anyFloat(), anyFloat(), any(Paint.class));
    }


    @Test
    public void testYOutOfBounds() throws Throwable {

        File responseFile = new File("testdata/verde.csv");

        HttpResponse response = new BasicHttpResponse(new BasicStatusLine(
                new ProtocolVersion("HTTP", 1, 1), 200, ""));
        response.setStatusCode(200);

        response.setEntity(new InputStreamEntity(new FileInputStream(responseFile), responseFile.length()));

        Robolectric.addPendingHttpResponse(response);

        Site verde = SiteFactory.getVerde();
        Variable[] vars = verde.getSupportedVariables();

        SiteData data = src.getSiteData(verde,vars,true);

        mockCanvas = mock(Canvas.class);

        graph.setSeries(data.getDatasets().get(vars[0].getCommonVariable()), false);
        initGraphLayout();

        graph.onDraw(mockCanvas);

        assertThat(graph.getYMin(), equalTo(100.0d));
        assertThat(graph.getYMax(), equalTo(1100.0d));
    }

    @Test
    public void testCategories() throws Throwable {

        File responseFile = new File("testdata/vallecito.csv");

        HttpResponse response = new BasicHttpResponse(new BasicStatusLine(
                new ProtocolVersion("HTTP", 1, 1), 200, ""));
        response.setStatusCode(200);

        response.setEntity(new InputStreamEntity(new FileInputStream(responseFile), responseFile.length()));

        Robolectric.addPendingHttpResponse(response);

        Site vallecito = SiteFactory.getVallecitoCreek();
        Variable[] vars = vallecito.getSupportedVariables();

        SiteData data = src.getSiteData(vallecito,vars,true);

        DecoratedCategory[] categories = new DecoratedCategory[5];
        for(int a = 0; a < categories.length; a++) {
            categories[a] = new DecoratedCategory();
        }

        categories[4].category = new Category("highPlus", null, 800d);
        categories[4].displayName = "High Plus";
        categories[3].category = new Category("high", 800d, 600d);
        categories[3].displayName = "High";
        categories[2].category = new Category("med", 600d, 400d);
        categories[2].displayName = "Medium";
        categories[1].category = new Category("low", 400d, 200d);
        categories[1].displayName = "Low";
        categories[0].category = new Category("tooLow", 200d, null);
        categories[0].displayName = "Too Low";

        mockCanvas = mock(Canvas.class);

        graph.setSeries(data.getDatasets().get(vars[0].getCommonVariable()), categories, true);
        initGraphLayout();

        graph.onDraw(mockCanvas);

        verify(mockCanvas).drawText(eq("High Plus"), anyFloat(), anyFloat(), any(Paint.class));
        verify(mockCanvas).drawText(eq("High"), anyFloat(), anyFloat(), any(Paint.class));
        verify(mockCanvas).drawText(eq("Medium"), anyFloat(), anyFloat(), any(Paint.class));
        verify(mockCanvas).drawText(eq("Low"), anyFloat(), anyFloat(), any(Paint.class));
        verify(mockCanvas).drawText(eq("Too Low"), anyFloat(), anyFloat(), any(Paint.class));
    }
}