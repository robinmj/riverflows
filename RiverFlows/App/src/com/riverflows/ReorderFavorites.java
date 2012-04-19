package com.riverflows;

import java.util.Date;
import java.util.List;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.commonsware.cwac.tlv.TouchListView;
import com.riverflows.data.Favorite;
import com.riverflows.data.Variable;
import com.riverflows.db.FavoritesDaoImpl;
import com.riverflows.wsclient.DataSourceController;

public class ReorderFavorites extends ListActivity {
	
	private LoadFavoritesTask loadTask = null;
	
	private FavoriteAdapter adapter = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.reorder_favorites);

		TouchListView lv = (TouchListView)getListView();
		
		lv.setDropListener(onDrop);
		
		View footer = getLayoutInflater().inflate(R.layout.reorder_favorites_footer, getListView(), false);
		getListView().addFooterView(footer);
		
		Button saveButton = (Button)findViewById(R.id.save_button);
		
		saveButton.setOnClickListener(saveListener);
		
		Button cancelButton = (Button)findViewById(R.id.cancel_button);
		cancelButton.setOnClickListener(cancelListener);
		
		this.loadTask = getLastNonConfigurationInstance();
		
		if(this.loadTask != null) {
			if(!this.loadTask.running) {
				if(this.loadTask.favorites == null || this.loadTask.errorMsg != null) {
					loadSites();
				} else {
					displayFavorites();
				}
			} else {
				//if the loadTask is running, just wait until it finishes
				setStatusText("Loading...");
			}
		} else {
			loadSites();
		}
		
		setTitle("Reorder Favorites");
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		
		loadSites();
	}
	
	@Override
    public LoadFavoritesTask onRetainNonConfigurationInstance() {
        return this.loadTask;
    }
    
    @Override
    public LoadFavoritesTask getLastNonConfigurationInstance() {
    	return (LoadFavoritesTask)super.getLastNonConfigurationInstance();
    }
    
    @Override
    protected void onStop() {
    	super.onStop();
    	this.loadTask.cancel(false);
    	this.loadTask = null;
    }
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	    case R.id.mi_home:
	    	startActivityIfNeeded(new Intent(this, Home.class), -1);
	    	return true;
	    case R.id.mi_about:
			Intent i = new Intent(this, About.class);
			startActivity(i);
	        return true;
	    default:
	        return super.onOptionsItemSelected(item);
	    }
	}
	
	/**
	 * @param hardRefresh if true, discard persisted site data as well
	 */
	public void loadSites() {
		setStatusText("Loading Sites...");
		
		this.loadTask = new LoadFavoritesTask();
		
		this.loadTask.execute();
	}
	
	public void displayFavorites() {
		if(this.loadTask.favorites != null) {
			adapter = new FavoriteAdapter(getApplicationContext(), this.loadTask.favorites);
			setListAdapter(adapter);
		}
		
		if(this.loadTask.favorites == null || this.loadTask.errorMsg != null) {
			setStatusText(this.loadTask.errorMsg);
		}
	}
	
	private void setStatusText(String s) {
		((TextView)findViewById(android.R.id.empty)).setText(s);
	}
	
	public class LoadFavoritesTask extends AsyncTask<Integer, Integer, List<Favorite>> {

		public final Date loadTime = new Date();
		public List<Favorite> favorites = null;
		
		public boolean running = false;
		
		public String errorMsg = null;
		
		protected void setLoadErrorMsg(String errorMsg) {
			this.errorMsg = errorMsg;
		}
		
		@Override
		protected List<Favorite> doInBackground(Integer... params) {
			running = true;
			try {
				return FavoritesDaoImpl.getFavorites(getApplicationContext(), null, null);
			} catch(Exception e) {
				setLoadErrorMsg(e.getMessage());
				Log.e(getClass().getName(), "",e);
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(List<Favorite> result) {
			super.onPostExecute(result);
			if(result != null) {
				this.favorites = result;
			}
			ReorderFavorites.this.displayFavorites();
			running = false;
		}
	}
	
	protected Date getLastLoadTime() {
		if(this.loadTask == null) {
			return null;
		}
		
		return this.loadTask.loadTime;
	}

    private static class ViewHolder {
    	Favorite favorite;
        TextView text;
        TextView subtext;
        //ImageView agencyIcon;
    }
	
	private class FavoriteAdapter extends ArrayAdapter<Favorite> {

		public FavoriteAdapter(Context context, List<Favorite> objects) {
			super(context, R.layout.reorder_favorites_item, R.id.list_item_txt, objects);
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// A ViewHolder keeps references to children views to avoid unneccessary calls
	        // to findViewById() on each row.
	        ReorderFavorites.ViewHolder holder;

	        // When convertView is not null, we can reuse it directly, there is no need
	        // to reinflate it. We only inflate a new View when the convertView supplied
	        // by ListView is null.
	        if (convertView == null) {
	            convertView = getLayoutInflater().inflate(R.layout.reorder_favorites_item, parent, false);

	            // Creates a ViewHolder and store references to the two children views
	            // we want to bind data to.
	            holder = new ViewHolder();
	            holder.text = (TextView) convertView.findViewById(R.id.favorite_txt);
	            holder.subtext = (TextView) convertView.findViewById(R.id.favorite_subtext);
	            //holder.agencyIcon = (ImageView)convertView.findViewById(R.id.favorite_agencyIcon);

	            convertView.setTag(holder);
	        } else {
	            // Get the ViewHolder back to get fast access to the TextView
	            // and the ImageView.
	            holder = (ReorderFavorites.ViewHolder) convertView.getTag();
	        }

	        // Bind the data efficiently with the holder.
	        
	        holder.favorite = getItem(position);
	        if(holder.favorite.getName() == null) {
	        	holder.text.setText(holder.favorite.getSite().getName());
	        } else {
	        	holder.text.setText(holder.favorite.getName());
	        }
	        
	        /*
	        String siteAgency = holder.favorite.getSite().getAgency();
	        holder.agencyIcon.setVisibility(View.VISIBLE);
	        
	        Integer agencyIconResId = Home.getAgencyIconResId(siteAgency);
	        if(agencyIconResId != null) {
	            holder.agencyIcon.setImageResource(agencyIconResId);
	        } else {
	        	Log.e(TAG, "no icon for agency: " + siteAgency);
	            holder.agencyIcon.setVisibility(View.GONE);
	        }*/
	        
	        if(holder.favorite.getVariable() != null) {
	        	Variable var = DataSourceController.getVariable(holder.favorite.getSite().getAgency(), holder.favorite.getVariable());
	        	if(var != null) {
			        holder.subtext.setText(var.getName());
	        	}
	        }

	        return convertView;
		}
		
	}
	
	private TouchListView.DropListener onDrop=new TouchListView.DropListener() {
		@Override
		public void drop(int from, int to) {
				Favorite item=adapter.getItem(from);
				
				adapter.remove(item);
				if(to < adapter.getCount()) {
					adapter.insert(item, to);
				} else {
					adapter.add(item);
				}
		}
	};
	
	private OnClickListener saveListener = new OnClickListener() {
		public void onClick(View v) {
			FavoriteAdapter adapter = (FavoriteAdapter)getListAdapter();
			if(adapter != null) {
				for(int a = 0; a < loadTask.favorites.size(); a++) {
					Favorite favorite = adapter.getItem(a);
					favorite.setOrder(a);
					
					FavoritesDaoImpl.updateFavorite(getApplicationContext(), favorite);
				}
			}
			
			setResult(RESULT_OK);
			finish();
		}
	};
	
	private OnClickListener cancelListener = new OnClickListener() {
		public void onClick(View v) {
			setResult(RESULT_CANCELED);
			finish();
		}
	};
}
