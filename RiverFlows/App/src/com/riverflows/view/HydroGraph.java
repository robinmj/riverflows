package com.riverflows.view;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.Paint.Align;
import android.util.Log;
import android.view.View;

import com.riverflows.data.Forecast;
import com.riverflows.data.Reading;
import com.riverflows.data.Series;

public class HydroGraph extends View {
	
	private static final String TAG = HydroGraph.class.getSimpleName();

	private double pixelsPerYUnit;
	private double xPixelsPerMs;
	
	private long xMin;
	private long xMax;
	
	private double yMax;
	
	//space between left side of canvas and y-axis
	private static final int yAxisOffset = 40;
	
	//space between x-axis and bottom of the canvas
	private static final int xAxisOffset = 30;
	
	//space between top of canvas and graph area
	private static final int topPadding = 10;
	
	//space between left side of canvas and graph area
	private static final int rightPadding = 10;
	
	/**
	 * Size of the tick marks, in pixels.
	 */
	private static final int tickSize = 3;
	
	private static final DateFormat DATE_LBL_WEEK_FORMAT = new SimpleDateFormat("EEE");
	private static final DateFormat DATE_LBL_MONTH_FORMAT = new SimpleDateFormat("dd");
	
	private static final Paint tickPaint = new Paint();
	
	private static final Paint guideLinePaint = new Paint();
	
	private Series series;
	
	public Series getSeries() {
		return series;
	}

	public void setSeries(Series series) {
		this.series = series;
		
		this.yMax  = getFriendlyYLimit();
		
		//calculate x boundaries
        GregorianCalendar dayRangeCalc = new GregorianCalendar();
        
        long minDate = Long.MAX_VALUE;
        long maxDate = Long.MIN_VALUE;
        
        for(Reading curReading: series.getReadings()) {
        	if(curReading.getDate().getTime() < minDate) {
        		minDate = curReading.getDate().getTime();
        	}
        	if(curReading.getDate().getTime() > maxDate) {
        		maxDate = curReading.getDate().getTime();
        	}
        }
        
        //x-axis minimum is the beginning of the after the day on which the first
        // point in the series falls.  This results in up to a day's worth
        // of data not being displayed, but is easier than adjusting the labeling
        // to display a partial day at the beginning of the chart.
        dayRangeCalc.setTimeInMillis(minDate);
        dayRangeCalc.set(Calendar.DAY_OF_YEAR, dayRangeCalc.get(Calendar.DAY_OF_YEAR) + 1);
        dayRangeCalc.set(Calendar.HOUR_OF_DAY, 0);
        dayRangeCalc.set(Calendar.MINUTE,0);
        dayRangeCalc.set(Calendar.SECOND,0);
        dayRangeCalc.set(Calendar.MILLISECOND,0);
        this.xMin = dayRangeCalc.getTimeInMillis();
        if(Log.isLoggable(TAG, Log.INFO)) {
        	Log.i(TAG, "xmin=" + DateFormat.getDateTimeInstance().format(dayRangeCalc.getTime()));
        }
        

        //x-axis maximum is the beginning of the day after
        // the day on which the last point in the series falls
        dayRangeCalc.setTimeInMillis(maxDate);
        dayRangeCalc.set(Calendar.DAY_OF_YEAR, dayRangeCalc.get(Calendar.DAY_OF_YEAR) + 1);
        dayRangeCalc.set(Calendar.HOUR_OF_DAY, 0);
        dayRangeCalc.set(Calendar.MINUTE,0);
        dayRangeCalc.set(Calendar.SECOND,0);
        dayRangeCalc.set(Calendar.MILLISECOND,0);
        this.xMax = dayRangeCalc.getTimeInMillis();
        if(Log.isLoggable(TAG, Log.INFO)) {
        	Log.i(TAG, "xmax=" + DateFormat.getDateTimeInstance().format(dayRangeCalc.getTime()));
        }
		
	}

	public HydroGraph(Context c) {
		super(c);

		tickPaint.setColor(Color.BLACK);
		guideLinePaint.setColor(Color.LTGRAY);
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

        if(Log.isLoggable(TAG, Log.DEBUG)) {
			Log.d(TAG, "h=" + getHeight());
			Log.d(TAG, "w=" + getWidth());
        }
		
		final float graphAreaH = getHeight() - (topPadding + xAxisOffset);
		final float graphAreaW = getWidth() - (rightPadding + yAxisOffset);
		
        //assumes that yMin is 0
		this.pixelsPerYUnit = graphAreaH / yMax;
		this.xPixelsPerMs = graphAreaW / (double)(this.xMax - this.xMin);
		
		Paint axisPaint = new Paint();
		axisPaint.setColor(Color.BLACK);
		
		//x-axis
		canvas.drawLine(yAxisOffset, getHeight() - xAxisOffset, getWidth() - rightPadding, getHeight() - xAxisOffset, axisPaint);
        if(Log.isLoggable(TAG, Log.DEBUG)) {
        	Log.d(TAG,"drawing x axis: " + yAxisOffset + "," + (getHeight() - xAxisOffset) + "," + (getWidth() - rightPadding) + "," + (getHeight() - xAxisOffset));
        }

		//y-axis
		canvas.drawLine(yAxisOffset, topPadding, yAxisOffset, getHeight() - xAxisOffset, axisPaint);
		
		//y-axis label
		Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG + Paint.SUBPIXEL_TEXT_FLAG);
		labelPaint.setColor(Color.BLACK);
		labelPaint.setTextAlign(Align.CENTER);
		labelPaint.setTypeface(Typeface.DEFAULT_BOLD);
		
		Path yAxisLabelGuide = new Path();
		yAxisLabelGuide.moveTo(12.0f, getHeight());
		yAxisLabelGuide.lineTo(12.0f, 0.0f);
		
		String label = this.series.getVariable().getName() + ", " + this.series.getVariable().getUnit();
		
		canvas.drawTextOnPath(label, yAxisLabelGuide, 0.0f, 0.0f, labelPaint);
		
		//labels, guidelines, and tick marks
		drawXLabels(canvas);
		drawYLabels(canvas, 11);
		
		drawPlot(canvas);
	}
	
	private void drawPlot(Canvas canvas) {
		
		Paint plotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		plotPaint.setColor(Color.BLUE);
		

		Paint noDataPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		noDataPaint.setColor(Color.LTGRAY);

		Paint forecastPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		forecastPaint.setColor(Color.CYAN);
		
		if(series.getReadings() == null || series.getReadings().size() == 0) {
			Log.e(getClass().getSimpleName(), "no data");
			return;
		}
		
		//find the first reading that falls within the range of the graph
		Reading startingPoint = null;
		int index = 0;
		for(;index < series.getReadings().size(); index++) {
			startingPoint = series.getReadings().get(index);
			if(startingPoint.getDate().getTime() > this.xMin) {
				break;
			}
		}

		//draw everything else
		
		float prevX = convertXValue(startingPoint.getDate());
		float prevY = (startingPoint.getValue() != null) ? convertYValue(startingPoint.getValue()) : 0.0f;
		
		float nextX;
		float nextY;
		Paint paint = plotPaint;
		for(;index < series.getReadings().size(); index++) {
			Reading r = series.getReadings().get(index);
			if(r.getValue() == null || r.getValue() < 0.0d) {
				paint = noDataPaint;
				continue;
			}
			if(r instanceof Forecast) {
				paint = forecastPaint;
			}
			
			nextX = convertXValue(r.getDate());
			nextY = convertYValue(r.getValue());
			canvas.drawLine(prevX, prevY, nextX, nextY, paint);
			paint = plotPaint;
			prevX = nextX;
			prevY = nextY;
		}
	}
	
	private float convertXValue(Date d) {
		float result = (float)(yAxisOffset + ((double)(d.getTime() - this.xMin) * this.xPixelsPerMs));
		if(result < 0 || result > getWidth()) {
			Log.e(getClass().getSimpleName(), "X coordinate out of bounds: " + result, new Exception());
		}
		return result;
	}
	
	private float convertYValue(double v) {
		float result = (float)((getHeight() - xAxisOffset) - (v * pixelsPerYUnit));
		if(result < 0 || result > getHeight()) {
			Log.e(getClass().getSimpleName(), "Y coordinate out of bounds: " + result + " value: " + v, new Exception());
		}
		return result;
	}
	
	/**
	 * Generate a nice round number to use as a Y limit without throwing the data out of proportion.
	 * @param dataYLimit
	 * @return
	 */
	private double getFriendlyYLimit() {
		
		double maxValue = Double.MIN_VALUE;
		//double minValue = Double.MAX_VALUE;
		
		
		for(Reading point:series.getReadings()){
			if(point.getValue() == null) {
				continue;
			}
			
			//find the max/min Y values
			if(point.getValue() > maxValue) {
				maxValue = point.getValue();
			} 

			/*if(point.getValue() < minValue) {
				minValue = point.getValue();
			}*/
		}
		

        if(Log.isLoggable(TAG, Log.DEBUG)) {
			//Log.d(TAG, "minValue=" + minValue);
			Log.d(TAG, "maxValue=" + maxValue);
        }
		
		/*
		//prevent dataYLimit from getting constricted by anomalous negative values
		if(minValue < 0) {
			minValue = 0;
		}
		
		//ensures that the graph will be vertically centered
		double dataYLimit = maxValue + minValue;*/
		
		//rule of thumb for keeping the graph from being too close to the top of the grid
		double dataYLimit = maxValue * 1.3d;
		
		//find the nearest power of 10, rounding up
		double zeroCount = Math.ceil(Math.log10(dataYLimit));

        if(Log.isLoggable(TAG, Log.DEBUG)) {
        	Log.d(TAG, "zeroCount=" + zeroCount);
        }
		
		double result = Math.pow(10.0, zeroCount);
		
		//it is acceptable to return a multiple of 5 if dataYLimit is less
		// than half of the closest power of 10
		double halfResult = result / 2;
		
		return (dataYLimit > halfResult) ? result : halfResult;
	}
	
	private void drawXLabels(Canvas canvas) {
		
		Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG + Paint.SUBPIXEL_TEXT_FLAG);
		labelPaint.setColor(Color.BLACK);
		labelPaint.setTextAlign(Align.CENTER);

        GregorianCalendar labelCalc = new GregorianCalendar();
        labelCalc.setTime(new Date(xMin));
		labelCalc.set(Calendar.HOUR_OF_DAY, 0);
		labelCalc.set(Calendar.MINUTE,0);
		labelCalc.set(Calendar.SECOND,0);
		labelCalc.set(Calendar.MILLISECOND,0);
		
		float zeroYCoord = (float)(getHeight() - xAxisOffset);
		
		float prevXCoord;
		float xCoord = yAxisOffset;
		String labelDayOfWeek;
		String labelDayOfMonth;

		//draw the first tickmark
		canvas.drawLine(xCoord, zeroYCoord, xCoord, zeroYCoord + tickSize, tickPaint);
		
		while(labelCalc.getTimeInMillis() < this.xMax) {
			labelDayOfWeek = DATE_LBL_WEEK_FORMAT.format(labelCalc.getTime());
			labelDayOfMonth = DATE_LBL_MONTH_FORMAT.format(labelCalc.getTime());
			
			//increment day
			labelCalc.set(Calendar.DAY_OF_YEAR, labelCalc.get(Calendar.DAY_OF_YEAR) + 1);
			
			prevXCoord = xCoord;
			xCoord = convertXValue(labelCalc.getTime());

	        if(Log.isLoggable(TAG, Log.DEBUG)) {
	        	Log.d(TAG, "drawing x axis label at " + xCoord + "," + zeroYCoord);
	        }
			
			//label the range between this tick and the previous one
			canvas.drawText(labelDayOfWeek, (xCoord + prevXCoord) / 2.0f, zeroYCoord + 14, labelPaint);
			canvas.drawText(labelDayOfMonth, (xCoord + prevXCoord) / 2.0f, zeroYCoord + 25, labelPaint);

			//draw the guideline
			canvas.drawLine(xCoord, zeroYCoord - 1, xCoord, topPadding, guideLinePaint);
			
			//draw the tickmark
			canvas.drawLine(xCoord, zeroYCoord, xCoord, zeroYCoord + tickSize, tickPaint);
		}
	}
	
	private void drawYLabels(Canvas canvas, int labelCount) {

		Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG + Paint.SUBPIXEL_TEXT_FLAG);
		labelPaint.setColor(Color.BLACK);
		labelPaint.setTextAlign(Align.RIGHT);
		
		double labelValueIncr = this.yMax / (double)(labelCount - 1);
		
		float zeroXCoord = yAxisOffset;
		
		float maxXCoord = convertXValue(new Date(this.xMax));
		
		float yCoord = (float)(getHeight() - xAxisOffset);

		//draw the tickmark and label for the zero value
		canvas.drawLine(zeroXCoord, yCoord, zeroXCoord - tickSize, yCoord, tickPaint);
		canvas.drawText("0", zeroXCoord - (tickSize + 2), yCoord + 5.0f, labelPaint);
		
		double labelValue;
		for(int a = 1; a < labelCount; a++) {
			labelValue = (double)a * labelValueIncr;
			yCoord = convertYValue(labelValue);

	        if(Log.isLoggable(TAG, Log.DEBUG)) {
	        	Log.d(TAG, "drawing y axis label at " + zeroXCoord + "," + yCoord);
	        }

			//draw the guideline
			canvas.drawLine(zeroXCoord + 1, yCoord, maxXCoord, yCoord, guideLinePaint);
			
			//draw the tickmark
			canvas.drawLine(zeroXCoord, yCoord, zeroXCoord - tickSize, yCoord, tickPaint);
			
			
			//draw the label
			canvas.drawText(formatYLabel(labelValue), zeroXCoord - (tickSize + 2), yCoord + 5.0f, labelPaint);
		}
	}
	
	private String formatYLabel(double value) {
		
		//limit to 3 significant figures
		double magnitude = Math.pow(10, Math.floor(Math.log10(value)) - 2);
		value = Math.floor(value / magnitude);
		
		//downcast to float to chop off any dangling digits
		float valueF = (float)(value * magnitude);
		
		String suffix = "";
		
		//abbreviate thousands and millions- the effectiveness of the abbreviations
		// depends upon whether getFriendlyYLimit() to produce a limit that is evenly
		// divisible by 10.
		if(valueF >= 1000000.0f) {
			valueF = (valueF / 1000000.0f);
			suffix = "M";
		} else if(valueF >= 1000.0d) {
			valueF = (valueF / 1000.0f);
			suffix = "K";
		}

		String labelStr = valueF + "";
		
		//chop off the spurious zero
        if(labelStr.endsWith(".0")) {
        	labelStr = labelStr.substring(0, labelStr.length() - 2);
        }
        
        return labelStr + suffix;
	}
}
