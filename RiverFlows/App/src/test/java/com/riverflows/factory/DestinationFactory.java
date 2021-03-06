package com.riverflows.factory;

import com.riverflows.data.Destination;
import com.riverflows.data.UserAccount;

/**
 * Created by robin on 11/16/14.
 */
public final class DestinationFactory {
    public static Destination getClearCreek() {
        Destination clearCreek = new Destination();
        clearCreek.setId(6);
        clearCreek.setUser(new UserAccount());
        clearCreek.setName("Terrible Destination");

        clearCreek.setSite(SiteFactory.getClearCreek());

        return clearCreek;
    }

    public static Destination getFountainCreek() {
        Destination newDestination = new Destination();
        newDestination.setId(9);
        newDestination.setUser(new UserAccount());
        newDestination.setName("Excellent Destination");
        newDestination.setSite(SiteFactory.getFountainCreek());

        return newDestination;
    }
}
