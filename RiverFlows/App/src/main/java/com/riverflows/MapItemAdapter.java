/**
 * 
 */
package com.riverflows;

import java.util.ArrayList;
import java.util.List;

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

import com.riverflows.data.MapItem;
import com.riverflows.data.Reading;
import com.riverflows.data.Series;
import com.riverflows.wsclient.Utils;

public class MapItemAdapter extends BaseAdapter implements Filterable {
	
	public static final String TAG = Home.TAG;
	
    private LayoutInflater inflater;
    
    private Object arrayLock = new Object();
    
    private List<MapItem> items;
    private List<MapItem> displayedItems;

    public MapItemAdapter(Context context, List<MapItem> items) {
        // Cache the LayoutInflate to avoid asking for a new one each time.
        this.inflater = LayoutInflater.from(context);
        this.items = items;
        this.displayedItems = items;
    }
    
	@Override
	public int getCount() {
		return displayedItems.size();
	}
	
	@Override
	public long getItemId(int position) {
		return getItemId(getItem(position));
	}
	
	public static final long getItemId(MapItem item) {
        return item.hashCode();
	}
	
	@Override
	public MapItem getItem(int position) {
		if(displayedItems.size() == 0)
			return null;
		
		return displayedItems.get(position);
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// A ViewHolder keeps references to children views to avoid unneccessary calls
        // to findViewById() on each row.
        MapItemAdapter.ViewHolder holder;

        // When convertView is not null, we can reuse it directly, there is no need
        // to reinflate it. We only inflate a new View when the convertView supplied
        // by ListView is null.
        if (convertView == null) {
            convertView = this.inflater.inflate(R.layout.site_list_item, null);

            // Creates a ViewHolder and store references to the two children views
            // we want to bind data to.
            holder = new ViewHolder();
            holder.text = (TextView) convertView.findViewById(R.id.list_item_txt);
            holder.agencyIcon = (ImageView)convertView.findViewById(R.id.agencyIcon);
            holder.destinationIcon = (ImageView)convertView.findViewById(R.id.destinationIcon);

            convertView.setTag(holder);
        } else {
            // Get the ViewHolder back to get fast access to the TextView
            // and the ImageView.
            holder = (MapItemAdapter.ViewHolder) convertView.getTag();
        }

        // Bind the data efficiently with the holder.
        holder.mapItem = this.displayedItems.get(position);

        if(holder.mapItem.isDestination()) {
            holder.text.setText(holder.mapItem.destinationFacet.getDestination().getName());
            holder.agencyIcon.setVisibility(View.GONE);
            holder.destinationIcon.setVisibility(View.VISIBLE);
        } else {
            holder.text.setText(holder.mapItem.getSite().getName());
            String siteAgency = holder.mapItem.getSite().getAgency();
            holder.agencyIcon.setVisibility(View.VISIBLE);
            holder.destinationIcon.setVisibility(View.GONE);

            Integer agencyIconResId = Home.getAgencyIconResId(siteAgency);
            if (agencyIconResId != null) {
                holder.agencyIcon.setImageResource(agencyIconResId);
            } else {
                Log.e(TAG, "no icon for agency: " + siteAgency);
                holder.agencyIcon.setVisibility(View.GONE);
            }
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
	
	private Filter mapItemFilter =  new Filter() {
		@Override
		protected FilterResults performFiltering(CharSequence constraint) {
			FilterResults results = new FilterResults();

            if (constraint == null || constraint.length() == 0) {
                ArrayList<MapItem> list = new ArrayList<MapItem>(MapItemAdapter.this.items);
                results.values = list;
                results.count = list.size();
            } else {
                String prefixString = constraint.toString().toLowerCase();

                final List<MapItem> values = MapItemAdapter.this.items;
                final int count = values.size();

                final ArrayList<MapItem> newValues = new ArrayList<MapItem>(count);

                for (int i = 0; i < count; i++) {
                    final MapItem item = values.get(i);
                    final String valueText = item.getName().toLowerCase();

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
			MapItemAdapter.this.displayedItems = (List<MapItem>) results.values;
            if (results.count > 0) {
            	MapItemAdapter.this.notifyDataSetChanged();
            } else {
            	MapItemAdapter.this.notifyDataSetInvalidated();
            }
        }
	};
	
	@Override
	public Filter getFilter() {
		return mapItemFilter;
	}

    static class ViewHolder {
    	MapItem mapItem;
        TextView text;
        ImageView agencyIcon;
        ImageView destinationIcon;
    }


    /**
     * Inserts the specified object at the specified index in the array.
     *
     * @param object The object to insert into the array.
     * @param index The index at which the object must be inserted.
     */
    public void insert(MapItem object, int index) {
        if (items != null) {
            synchronized (arrayLock) {
            	items.add(index, object);
                notifyDataSetChanged();
            }
        } else {
            displayedItems.add(index, object);
            notifyDataSetChanged();
        }
    }

    /**
     * Removes the specified object from the array.
     *
     * @param object The object to remove.
     */
    public void remove(MapItem object) {
        if (items != null) {
            synchronized (arrayLock) {
                items.remove(object);
            }
        } else {
        	displayedItems.remove(object);
        }
        notifyDataSetChanged();
    }
}