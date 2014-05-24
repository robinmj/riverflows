package com.riverflows.data;

/**
 * Created by robin on 5/18/14.
 */
public class FavoriteData {
    private final Favorite favorite;
    private final SiteData siteData;
    private final Variable variable;

    public FavoriteData(Favorite favorite, SiteData siteData, Variable variable) {

        assert(favorite != null);
        assert(siteData != null);
        assert(variable != null);

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

    public String getName() {
        if(favorite.getName() != null) {
            return favorite.getName();
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
