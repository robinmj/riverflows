/**
 * 
 */
package com.riverflows;

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

import com.riverflows.data.FavoriteData;
import com.riverflows.data.Reading;
import com.riverflows.data.Series;
import com.riverflows.wsclient.DataSourceController;
import com.riverflows.wsclient.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

public class FavoriteAdapter extends BaseAdapter implements Filterable {

	public static final String TAG = Home.TAG;

    private LayoutInflater inflater;

    private Object arrayLock = new Object();

    private List<FavoriteData> favorites;
    private List<FavoriteData> displayedFavorites;

    public FavoriteAdapter(Context context, List<FavoriteData> favorites) {
        // Cache the LayoutInflate to avoid asking for a new one each time.
        this.inflater = LayoutInflater.from(context);
        this.favorites = favorites;
        this.displayedFavorites = favorites;
    }

	@Override
	public int getCount() {
		return displayedFavorites.size();
	}

	@Override
	public long getItemId(int position) {
		return getItemId(getItem(position));
	}

	public static final long getItemId(FavoriteData item) {
		long varHashCode = 0;
		try {
			varHashCode = item.getSiteData().getDatasets().values().iterator().next().getVariable().getId().hashCode();
		} catch(ArrayIndexOutOfBoundsException aioobe) {
		} catch(NoSuchElementException nsee) {
		} catch(NullPointerException npe) {
		}

		try {
			return item.getFavorite().getSite().getSiteId().hashCode() ^ varHashCode;
		} catch(NullPointerException npe) {
		}
		return -1;

	}

	@Override
	public FavoriteData getItem(int position) {
		if(displayedFavorites.size() == 0)
			return null;

		return displayedFavorites.get(position);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// A ViewHolder keeps references to children views to avoid unneccessary calls
        // to findViewById() on each row.
        FavoriteAdapter.ViewHolder holder;

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
            holder = (FavoriteAdapter.ViewHolder) convertView.getTag();
        }

        // Bind the data efficiently with the holder.
        holder.data = this.displayedFavorites.get(position);
        holder.text.setText(holder.data.getName());
        String siteAgency = holder.data.getFavorite().getSite().getAgency();
        holder.agencyIcon.setVisibility(View.VISIBLE);
        
        Integer agencyIconResId = Home.getAgencyIconResId(siteAgency);
        if(agencyIconResId != null) {
            holder.agencyIcon.setImageResource(agencyIconResId);
        } else {
        	Log.e(TAG, "no icon for agency: " + siteAgency);
            holder.agencyIcon.setVisibility(View.GONE);
        }
        
        //display the last reading for this site, if present
        Series flowSeries = holder.data.getSeries();
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
			Reading lastReading = s.getLastObservation();
			
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
                ArrayList<FavoriteData> list = new ArrayList<FavoriteData>(FavoriteAdapter.this.favorites);
                results.values = list;
                results.count = list.size();
            } else {
                String prefixString = constraint.toString().toLowerCase();

                final List<FavoriteData> values = FavoriteAdapter.this.favorites;
                final int count = values.size();

                final ArrayList<FavoriteData> newValues = new ArrayList<FavoriteData>(count);

                for (int i = 0; i < count; i++) {
                    final FavoriteData item = values.get(i);
                    final String valueText = item.getFavorite().getSite().getName().toLowerCase();

                    // First match against the whole, non-splitted value
                    if (valueText.startsWith(prefixString)) {
                        newValues.add(item);
                    } else {
                        final String[] words = valueText.split(" ");
                        final int wordCount = words.length;

                        for (int k = 0; k < wordCount; k++) {
                            if (words[k].startsWith(prefixString)) {
                                newValues.add(item);
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
			FavoriteAdapter.this.displayedFavorites = (List<FavoriteData>) results.values;
            if (results.count > 0) {
            	FavoriteAdapter.this.notifyDataSetChanged();
            } else {
            	FavoriteAdapter.this.notifyDataSetInvalidated();
            }
        }
	};
	
	@Override
	public Filter getFilter() {
		return stationListFilter;
	}

    static class ViewHolder {
    	FavoriteData data;
        TextView text;
        TextView subtext;
        ImageView agencyIcon;
    }


    /**
     * Inserts the specified object at the specified index in the array.
     *
     * @param object The object to insert into the array.
     * @param index The index at which the object must be inserted.
     */
    public void insert(FavoriteData object, int index) {
        if (favorites != null) {
            synchronized (arrayLock) {
            	favorites.add(index, object);
                notifyDataSetChanged();
            }
        } else {
            displayedFavorites.add(index, object);
            notifyDataSetChanged();
        }
    }

    /**
     * Removes the specified object from the array.
     *
     * @param object The object to remove.
     */
    public void remove(FavoriteData object) {
        if (favorites != null) {
            synchronized (arrayLock) {
                favorites.remove(object);
            }
        } else {
        	displayedFavorites.remove(object);
        }
        notifyDataSetChanged();
    }
}