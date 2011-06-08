/**
 * 
 */
package com.riverflows;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import com.riverflows.data.Reading;
import com.riverflows.data.Series;
import com.riverflows.data.SiteData;
import com.riverflows.wsclient.AHPSXmlDataSource;
import com.riverflows.wsclient.CODWRDataSource;
import com.riverflows.wsclient.DataSourceController;
import com.riverflows.wsclient.UsgsCsvDataSource;
import com.riverflows.Utils;

public class SiteAdapter extends BaseAdapter implements Filterable {
	
	public static final String TAG = SiteAdapter.class.getSimpleName();
	
    private LayoutInflater inflater;
    private List<SiteData> stations;
    private List<SiteData> displayedStations;

    public SiteAdapter(Context context, List<SiteData> stations) {
        // Cache the LayoutInflate to avoid asking for a new one each time.
        this.inflater = LayoutInflater.from(context);
        this.stations = stations;
        this.displayedStations = stations;
    }
    
	@Override
	public int getCount() {
		return displayedStations.size();
	}
	
	@Override
	public long getItemId(int position) {
		return getItemId(getItem(position));
	}
	
	public static final long getItemId(SiteData item) {
		long varHashCode = 0;
		try {
			varHashCode = item.getDatasets().values().iterator().next().getVariable().getId().hashCode();
		} catch(ArrayIndexOutOfBoundsException aioobe) {
		} catch(NoSuchElementException nsee) {
		} catch(NullPointerException npe) {
		}
		
		try {
			return item.getSite().getSiteId().hashCode() ^ varHashCode;
		} catch(NullPointerException npe) {
		}
		return -1;
		
	}
	
	@Override
	public SiteData getItem(int position) {
		if(displayedStations.size() == 0)
			return null;
		
		return displayedStations.get(position);
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// A ViewHolder keeps references to children views to avoid unneccessary calls
        // to findViewById() on each row.
        SiteAdapter.ViewHolder holder;

        // When convertView is not null, we can reuse it directly, there is no need
        // to reinflate it. We only inflate a new View when the convertView supplied
        // by ListView is null.
        if (convertView == null) {
            convertView = this.inflater.inflate(R.layout.site_list_item, null);

            // Creates a ViewHolder and store references to the two children views
            // we want to bind data to.
            holder = new ViewHolder();
            holder.text = (TextView) convertView.findViewById(R.id.list_item_txt);
            holder.subtext = (TextView) convertView.findViewById(R.id.subtext);
            holder.agencyIcon = (ImageView)convertView.findViewById(R.id.agencyIcon);

            convertView.setTag(holder);
        } else {
            // Get the ViewHolder back to get fast access to the TextView
            // and the ImageView.
            holder = (SiteAdapter.ViewHolder) convertView.getTag();
        }

        // Bind the data efficiently with the holder.
        holder.station = this.displayedStations.get(position);
        holder.text.setText(holder.station.getSite().getName());
        String siteAgency = holder.station.getSite().getAgency();
        holder.agencyIcon.setVisibility(View.VISIBLE);
        if(UsgsCsvDataSource.AGENCY.equals(siteAgency)) {
            holder.agencyIcon.setImageResource(R.drawable.usgs);
        } else if(AHPSXmlDataSource.AGENCY.equals(siteAgency)) {
            holder.agencyIcon.setImageResource(R.drawable.ahps);
        } else if(CODWRDataSource.AGENCY.equals(siteAgency)) {
            holder.agencyIcon.setImageResource(R.drawable.codwr);
        } else {
        	Log.e(TAG, "no icon for agency: " + siteAgency);
            holder.agencyIcon.setVisibility(View.GONE);
        }
        
        //display the last reading for this site, if present
        Series flowSeries = DataSourceController.getPreferredSeries(holder.station);
    	Reading lastReading = getLastReadingValue(flowSeries);
    	
        if(lastReading == null) {
        	holder.subtext.setText("");
        } else {
        	if(lastReading.getValue() == null) {
        		if(lastReading.getQualifiers() == null) {
        			holder.subtext.setText("unknown");
        		} else {
            		holder.subtext.setText(lastReading.getQualifiers());
        		}
        	} else {

        		//use this many significant figures for decimal values
    			int sigfigs = 4;
    			
    			String readingStr = Utils.abbreviateNumber(lastReading.getValue(), sigfigs);
        		
        		holder.subtext.setText(readingStr + " " + flowSeries.getVariable().getUnit());
        	}
        
        	//TODO come up with a better way of conveying a stale reading
	        //30 minutes ago
	        //Date staleReadingDate = new Date(System.currentTimeMillis() - (30 * 60 * 1000));
	        
	        //if(lastReading.getDate().before(staleReadingDate)) {
	        //	holder.subtext.setText("");
	        //}
        }

        return convertView;
    }
	
	private Reading getLastReadingValue(Series s) {
		if(s == null)
			return null;
		
		if(s.getReadings() == null) {
			Log.e(TAG, "null readings");
			return null;
		}
		
		try {
			Reading lastReading = DataSourceController.getLastObservation(s);
			
			if(lastReading == null) {
				Log.i(TAG, "null reading");
				return null;
			}
			
			return lastReading;
			
		} catch(IndexOutOfBoundsException ioobe) {
			//there will be an empty readings list if the site has no recent reading
			return null;
		}
		
	}
	
	private Filter stationListFilter =  new Filter() {
		@Override
		protected FilterResults performFiltering(CharSequence constraint) {
			FilterResults results = new FilterResults();

            if (constraint == null || constraint.length() == 0) {
                ArrayList<SiteData> list = new ArrayList<SiteData>(SiteAdapter.this.stations);
                results.values = list;
                results.count = list.size();
            } else {
                String prefixString = constraint.toString().toLowerCase();

                final List<SiteData> values = SiteAdapter.this.stations;
                final int count = values.size();

                final ArrayList<SiteData> newValues = new ArrayList<SiteData>(count);

                for (int i = 0; i < count; i++) {
                    final SiteData station = values.get(i);
                    final String valueText = station.getSite().getName().toLowerCase();

                    // First match against the whole, non-splitted value
                    if (valueText.startsWith(prefixString)) {
                        newValues.add(station);
                    } else {
                        final String[] words = valueText.split(" ");
                        final int wordCount = words.length;

                        for (int k = 0; k < wordCount; k++) {
                            if (words[k].startsWith(prefixString)) {
                                newValues.add(station);
                                break;
                            }
                        }
                    }
                }

                results.values = newValues;
                results.count = newValues.size();
            }
			
			return results;
		}
		
		@Override
		protected void publishResults(CharSequence constraint,
				FilterResults results) {
			SiteAdapter.this.displayedStations = (List<SiteData>) results.values;
            if (results.count > 0) {
            	SiteAdapter.this.notifyDataSetChanged();
            } else {
            	SiteAdapter.this.notifyDataSetInvalidated();
            }
        }
	};
	
	@Override
	public Filter getFilter() {
		return stationListFilter;
	}

    static class ViewHolder {
    	SiteData station;
        TextView text;
        TextView subtext;
        ImageView agencyIcon;
    }
}