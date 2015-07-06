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
import android.graphics.Paint.Align;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.riverflows.Home;
import com.riverflows.data.Category;
import com.riverflows.data.DecoratedCategory;
import com.riverflows.data.Forecast;
import com.riverflows.data.Reading;
import com.riverflows.data.Series;
import com.riverflows.data.Variable;

public class HydroGraph extends View {
	
	private static final String TAG = Home.TAG;

	private double pixelsPerYUnit;
	private double xPixelsPerMs;
	
	private long xMin;
	private long xMax;
	
	private double yMax;
	private double yMin;
	
	private boolean zeroMinimum = false;
	
	private String convertToUnit = null;
	
	private boolean hasLegend = false;
	
	//space between left side of canvas and y-axis
	private final int yAxisOffset;
	
	//space between x-axis and bottom of the canvas
	private final int xAxisOffset;
	
	//label text size, in pixels
	private final float labelTextSize;
	
	//space between top of canvas and graph area
	private final int topPadding;
	
	//space between right side of canvas and graph area
	private static final int rightPadding = 10;

	private final int legendWidth;
	private final int legendHeight;
	private final int legendTopMargin;
	private final int legendLeftMargin;
	
	private final int legendPadding;
	
	/**
	 * if xPixelsPerMs is below this number, then only include the day of week label for every other day
	 */
	private static final double LABEL_DAY_OF_WEEK_EVERY_OTHER_DAY_THRESHOLD = 0.000000292397661d;
	
	/**
	 * Size of the tick marks, in pixels.
	 */
	private static final int tickSize = 3;
	
	private static final DateFormat DATE_LBL_WEEK_FORMAT = new SimpleDateFormat("EEE");
	private static final DateFormat DATE_LBL_MONTH_FORMAT = new SimpleDateFormat("d");
	
	private static final Paint tickPaint = new Paint();
	
	private static final Paint guideLinePaint = new Paint();
	
	private static final Paint plotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

	private static final Paint noDataPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

	private static final Paint forecastPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	
	static {
		tickPaint.setColor(Color.BLACK);
		guideLinePaint.setColor(Color.LTGRAY);
		plotPaint.setColor(Color.BLUE);
		noDataPaint.setColor(Color.LTGRAY);
		forecastPaint.setColor(Color.CYAN);
		forecastPaint.setStrokeWidth(4.0f);
	}
	
	private Series series;
    private DecoratedCategory[] categories;
	
	public Series getSeries() {
		return series;
	}

    public void setSeries(Series series, boolean zeroMinimum) {
        this.setSeries(series, new DecoratedCategory[0], zeroMinimum);
    }

	public void setSeries(Series series, DecoratedCategory[] categories, boolean zeroMinimum) {
		this.series = series;
        this.categories = categories;
		
		this.zeroMinimum = zeroMinimum;
        
        long minDate = Long.MAX_VALUE;
        long maxDate = Long.MIN_VALUE;
        
        for(Reading curReading: series.getReadings()) {
        	if(curReading.getDate().getTime() < minDate) {
        		minDate = curReading.getDate().getTime();
        	}
        	if(curReading.getDate().getTime() > maxDate) {
        		maxDate = curReading.getDate().getTime();
        	}
        	
        	if(curReading instanceof Forecast) {
        		this.hasLegend = true;
        	}
        }
		
		double[] limits = getFriendlyYLimits();

		this.yMin  = limits[0];
		this.yMax  = limits[1];
		
		//calculate x boundaries
        GregorianCalendar dayRangeCalc = new GregorianCalendar();
        
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
		float scaledDensity = c.getResources().getDisplayMetrics().scaledDensity;
		labelTextSize = scaledDensity * 10f;

        topPadding = (int)(scaledDensity * 10f);

		//2 pixels padding before Y axis label, 2 pixels padding after, 4-character
		// Y tick label will be a little under thrice the size of the label text, leave space for the tick marks
		yAxisOffset = (int)(2f + labelTextSize + 2f + (2.7f * labelTextSize) + tickSize);
		//yAxisOffset = (int)(scaledDensity * 30f) + 10;
		xAxisOffset = (int)(scaledDensity * 22f) + 8;

        legendWidth = (int)(scaledDensity * 95f);
        legendHeight = (int)(scaledDensity * 50f);
        legendTopMargin = (int)(scaledDensity * 10f);
        legendLeftMargin = (int)(scaledDensity * 10f);

        legendPadding = (int)(scaledDensity * 10f);
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
		
		this.pixelsPerYUnit = graphAreaH / (this.yMax - this.yMin);
		this.xPixelsPerMs = graphAreaW / (double)(this.xMax - this.xMin);
		
		if(Log.isLoggable(TAG, Log.DEBUG)) {
			Log.d(TAG, "xPixelsPerMs: " + xPixelsPerMs + " pixelsPerYUnit: " + pixelsPerYUnit);
		}
		
		Paint axisPaint = new Paint();
		axisPaint.setColor(Color.BLACK);

        drawCategories(canvas);
		
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
		labelPaint.setTextSize(labelTextSize);
		
		Path yAxisLabelGuide = new Path();
		yAxisLabelGuide.moveTo(labelTextSize + 2, getHeight());
		yAxisLabelGuide.lineTo(labelTextSize + 2, 0.0f);
		
		Variable var = this.series.getVariable();
		
		String label = var.getName();
		
		if(convertToUnit != null) {
			label = label + ", " + convertToUnit;
		} else if(!TextUtils.isEmpty(var.getUnit())) {
			if(var.getUnit().trim().length() > 0) {
				label = label +  ", " + var.getUnit();
			}
		}
		
		canvas.drawTextOnPath(label, yAxisLabelGuide, 0.0f, 0.0f, labelPaint);
		
		//labels, guidelines, and tick marks
		drawXLabels(canvas);
		drawYLabels(canvas, 11);
		
		drawPlot(canvas);
		
		if(this.hasLegend) {
			drawLegend(canvas);
		}
	}
	
	/**
	 * @param canvas
	 */
	private void drawPlot(Canvas canvas) {
		
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
		Reading lastObserved = null;
		for(;index < series.getReadings().size(); index++) {
			Reading r = series.getReadings().get(index);
			if(r.getValue() == null) {
				paint = noDataPaint;
				continue;
			}
			if(r instanceof Forecast) {
				if(lastObserved != null && r.getDate().before(lastObserved.getDate())) {
					//don't show forecasts that come before the observed data
					continue;
				}
				paint = forecastPaint;
			} else {
				lastObserved = r;
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
		float result = (float)((getHeight() - xAxisOffset) - ((v - this.yMin) * pixelsPerYUnit));
		if(result < 0 || result > getHeight()) {
			Log.e(getClass().getSimpleName(), "Y coordinate out of bounds: " + result + " value: " + v, new Exception());
		}
		return result;
	}
	
	/**
	 * Generate a nice round numbers to use as a Y limits without throwing the data out of proportion.
	 * TODO make this work properly for a labelCount of other than 11
	 * @return
	 */
	private double[] getFriendlyYLimits() {

		double maxValue = Double.MIN_VALUE;
		double minValue = Double.MAX_VALUE;
		
		for(Reading point:series.getReadings()){
			if(point.getValue() == null) {
				continue;
			}
			
			//find the max/min Y values
			if(point.getValue() > maxValue) {
				maxValue = point.getValue();
			} 
	
			if(point.getValue() < minValue) {
				minValue = point.getValue();
			}
		}

        //show all categories, if specified
        if(zeroMinimum) {
            for (DecoratedCategory decoratedCategory : categories) {

                Double categoryMax = decoratedCategory.category.getMax();

                if (categoryMax != null && categoryMax > maxValue) {
                    maxValue = categoryMax;
                }
            }
        }
			
	
	    if(Log.isLoggable(TAG, Log.DEBUG)) {
			Log.d(TAG, "minValue=" + minValue);
			Log.d(TAG, "maxValue=" + maxValue);
	    }
	    
	    //don't allow a zero minimum if there are negative values
	    if(minValue < 0.0) {
	    	this.zeroMinimum = false;
	    }
	    
	    double[] limits = new double[2];

		//rule of thumb for keeping the graph from being too close to the top of the grid
	    limits[1] = maxValue + Math.abs(maxValue * (this.hasLegend ? 0.2d : 0.02d));
		
	    if(zeroMinimum) {
	    	limits[0] = 0.0d;
	    } else {
			//rule of thumb for keeping the graph from being too close to the bottom of the grid
		    limits[0] = minValue - Math.abs(minValue * 0.02d);
	    }
	    
		double yRange = limits[1] - limits[0];
		
		if(yRange == 0.0d) {
			//this will only happen if minValue and maxValue are both 0
			limits[1] = 0.0d;
			limits[1] = 1.0d;
			return limits;
		}
		
		//find the nearest power of 10, rounding up
		double zeroCount = Math.ceil(Math.log10(yRange));

        if(Log.isLoggable(TAG, Log.DEBUG)) {
        	Log.d(TAG, "zeroCount=" + zeroCount);
        }
		
		double result = Math.pow(10.0, zeroCount);
		
		//it is acceptable to return a multiple of 5 or 2 if yRange is less
		// than half or a fifth of the closest power of 10
		double halfResult = result / 2;
		if(yRange < halfResult) {
			double fifthResult = result / 5;
			yRange = (yRange < fifthResult) ? fifthResult : halfResult;
		} else {
			yRange = result;
		}
		
		if(!zeroMinimum) {
			//make sure the y minimum has a common factor with 1/10th of yRange
			
			double factor = yRange / 10.0d;
			
			limits[0] = Math.floor(limits[0] / factor) * factor;
		}
		
		limits[1] = yRange + limits[0];
		
		return limits;
	}
	
	private void drawXLabels(Canvas canvas) {
		
		Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG + Paint.SUBPIXEL_TEXT_FLAG);
		labelPaint.setColor(Color.BLACK);
		labelPaint.setTextAlign(Align.CENTER);
		labelPaint.setTextSize(labelTextSize);

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
	        if(xPixelsPerMs >= LABEL_DAY_OF_WEEK_EVERY_OTHER_DAY_THRESHOLD
	        		|| labelCalc.get(Calendar.DAY_OF_YEAR) % 2 == 0) {
	        	canvas.drawText(labelDayOfWeek, (xCoord + prevXCoord) / 2.0f, zeroYCoord + tickSize + 1 + labelTextSize, labelPaint);
	        }
			canvas.drawText(labelDayOfMonth, (xCoord + prevXCoord) / 2.0f, zeroYCoord + tickSize + 1 + labelTextSize + 1 + labelTextSize, labelPaint);

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
		labelPaint.setTextSize(labelTextSize);
		
		double labelValueIncr = (this.yMax - this.yMin) / (double)(labelCount - 1);
		
		float zeroXCoord = yAxisOffset;
		
		float maxXCoord = convertXValue(new Date(this.xMax));
		
		float yCoord = (float)(getHeight() - xAxisOffset);

		//draw the tickmark and label for the minimum value - no guideline required since it is
		// on the x-axis
		canvas.drawLine(zeroXCoord, yCoord, zeroXCoord - tickSize, yCoord, tickPaint);
		canvas.drawText(formatYLabel(this.yMin), zeroXCoord - (tickSize + 2), yCoord + 5.0f, labelPaint);
		
		double labelValue;
		for(int a = 1; a < labelCount; a++) {
			labelValue = (double)a * labelValueIncr + this.yMin;
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
		
		if(value == 0.0d) {
			return "0";
		}
		
		//limit to 3 significant figures
		double magnitude = Math.pow(10, Math.floor(Math.log10(Math.abs(value))) - 2);
		value = Math.round(value / magnitude);
		
		//downcast to float to chop off any dangling digits
		float valueF = (float)(value * magnitude);
		
		String suffix = "";
		
		//abbreviate thousands and millions- the effectiveness of the abbreviations
		// depends upon whether getFriendlyYLimit() to produce a limit that is evenly
		// divisible by 10.
		if(Math.abs(valueF) >= 1000000.0f) {
			valueF = (valueF / 1000000.0f);
			suffix = "M";
		} else if(Math.abs(valueF) >= 1000.0d) {
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
	
	private void drawLegend(Canvas canvas) {
		Rect legendOutline = new Rect(yAxisOffset + legendLeftMargin,
				topPadding + legendTopMargin,
				yAxisOffset + legendLeftMargin + legendWidth,
				topPadding + legendTopMargin + legendHeight);
		//draw legend box
		canvas.drawRect(legendOutline, guideLinePaint);
		Rect legendFill = new Rect(legendOutline.left + 1, legendOutline.top + 1, legendOutline.right - 1, legendOutline.bottom - 1);
		
		Paint background = new Paint();
		background.setColor(Color.WHITE);
		
		//fill legend box
		canvas.drawRect(legendFill, background);

		Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG + Paint.SUBPIXEL_TEXT_FLAG);
		labelPaint.setColor(Color.BLACK);
		labelPaint.setTextAlign(Align.LEFT);
        labelPaint.setTextSize(labelTextSize);
		
		canvas.drawLine(legendFill.left + legendPadding, legendFill.top + legendPadding + 0.5f * labelTextSize,
				legendFill.left + legendPadding + labelTextSize, legendFill.top + legendPadding + 0.5f * labelTextSize, plotPaint);

		canvas.drawText("Observed", legendFill.left + legendPadding + 1.5f * labelTextSize, legendFill.top + legendPadding + labelTextSize, labelPaint);
		
		canvas.drawLine(legendFill.left + legendPadding, legendFill.top + legendPadding + 2f * labelTextSize,
				legendFill.left + legendPadding + labelTextSize, legendFill.top + legendPadding + 2f * labelTextSize, forecastPaint);

		canvas.drawText("Forecast", legendFill.left + legendPadding + 1.5f * labelTextSize, legendFill.top + legendPadding + 2.5f * labelTextSize, labelPaint);
	}

    private void drawCategories(Canvas canvas) {
        for(DecoratedCategory decoratedCategory:categories) {
            Double catMax = decoratedCategory.category.getMax();
            Double catMin = decoratedCategory.category.getMin();

            if(catMax != null && catMax < yMin) {
                //category is below graph range, so don't draw it
                continue;
            }
            if(catMin != null && catMin > yMax) {
                //category is above graph range, so don't draw it
                continue;
            }

            float rectTop = topPadding;

            if(catMax != null) {
                rectTop = convertYValue(catMax);
            }

            float rectBottom = getHeight() - xAxisOffset;

            if(catMin != null) {
                rectBottom = convertYValue(catMin);
            }

            Paint bgPaint = new Paint();
            bgPaint.setColor(decoratedCategory.bgColor);

            canvas.drawRect((float)yAxisOffset, rectTop, (float)(getWidth() - rightPadding), rectBottom, bgPaint);

            Paint textPaint = new Paint();
            textPaint.setColor(decoratedCategory.textColor);
            textPaint.setTypeface(Typeface.DEFAULT_BOLD);
            textPaint.setTextAlign(Align.CENTER);
            textPaint.setTextSize(labelTextSize * 2.0f);

            float xCenter = yAxisOffset + ((float)(getWidth() - rightPadding - yAxisOffset) / 2.0f);
            float yCenter = rectTop + ((rectBottom - rectTop) / 2.0f);

            if(decoratedCategory.displayName != null) {
                canvas.drawText(decoratedCategory.displayName, xCenter, yCenter, textPaint);
            }
        }
    }
}
