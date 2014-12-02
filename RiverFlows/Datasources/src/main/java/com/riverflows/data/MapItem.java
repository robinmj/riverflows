package com.riverflows.data;

import com.riverflows.wsclient.DataSourceController;

import java.util.Comparator;

/**
 * Created by robin on 11/14/14.
 */
public class MapItem {
    public final SiteData siteData;

    public final DestinationFacet destinationFacet;

    private final Variable variable;

    private final boolean favorite;

    public MapItem(SiteData siteData, Variable variable, boolean favorite) {
        assert(siteData != null);
        assert(siteData.getSite().getId() != null);

        this.siteData = siteData;

        this.destinationFacet = null;

        this.variable = variable;

        this.favorite = favorite;
    }

    public MapItem(DestinationFacet destinationFacet, boolean favorite) {
        assert(destinationFacet != null);
        assert(destinationFacet.getId() != null);

        this.siteData = null;

        this.destinationFacet = destinationFacet;

        this.variable = null;

        this.favorite = favorite;
    }

    public Site getSite() {
        if(destinationFacet != null) {
            return destinationFacet.getDestination().getSite();
        }
        return siteData.getSite();
    }

    public Variable getVariable() {
        if(variable != null) {
            return variable;
        }

        Series data = DataSourceController.getPreferredSeries(siteData);
        if(data == null) {
            return null;
        }

        return data.getVariable();
    }

    public Series getPreferredSeries() {
        if(siteData == null) {
            return null;
        }

        if(variable != null) {
            return siteData.getDatasets().get(variable.getCommonVariable());
        }

        return DataSourceController.getPreferredSeries(siteData);
    }

    public boolean isDestination() {
        return destinationFacet != null;
    }

    public boolean isFavorite() {
        return favorite;
    }

    public String getName() {
        if(destinationFacet != null) {
            return destinationFacet.getDestination().getName();
        }
        return getSite().getName();
    }

    public static final Comparator<MapItem> SORT_BY_NAME = new Comparator<MapItem>() {
        @Override
        public int compare(MapItem object1, MapItem object2) {
            if(object1 == null || object1.getName() == null) {
                return -1;
            }
            if(object2 == null || object2.getName() == null) {
                return 1;
            }
            return object1.getName().toUpperCase().compareTo(object2.getName().toUpperCase());
        }
    };

    @Override
    public int hashCode() {
        if(destinationFacet != null) {
            return destinationFacet.getId().hashCode();
        }

        return siteData.getSite().getId().hashCode();
    }
}
