package com.riverflows.data;

import org.apache.commons.lang.ObjectUtils;

/**
 * Created by robin on 5/18/14.
 */
public class FavoriteData {
    private final Favorite favorite;
    private final SiteData siteData;
    private final Variable variable;
    private final Exception exception;

    public FavoriteData(Favorite favorite, SiteData siteData, Variable variable) {
        this(favorite,siteData,variable,null);
    }

    public FavoriteData(Favorite favorite, SiteData siteData, Variable variable, Exception exception) {

        this.exception = exception;

        if(favorite == null) {
            throw new NullPointerException();
        }
        //TODO allow null siteData or variable if exception is non-null
        if(siteData == null) {
            throw new NullPointerException("empty siteData returned for " + favorite.getSite().getSiteId() + " " + favorite.getVariable());
        }
        if(variable == null) {
            throw new NullPointerException("Variable not found for " + favorite.getSite().getAgency() + " " + favorite.getVariable() + "?");
        }

        this.favorite = favorite;
        this.siteData = siteData;
        this.variable = variable;
    }

    public Favorite getFavorite() {
        return favorite;
    }

    public SiteData getSiteData() {
        return siteData;
    }

    public Exception getException() { return exception; }

    public String getName() {
        if(favorite.getName() != null) {
            return favorite.getName();
        }

		if(favorite.getDestinationFacet() != null && favorite.getDestinationFacet().getDestination().getName() != null) {
			return favorite.getDestinationFacet().getDestination().getName();
		}

        return siteData.getSite().getName();
    }

    public Variable getVariable() {
        return this.variable;
    }

    public Series getSeries() {
        return siteData.getDatasets().get(this.variable.getCommonVariable());
    }
}
