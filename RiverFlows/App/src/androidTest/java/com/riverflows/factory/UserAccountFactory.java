package com.riverflows.factory;

import com.riverflows.data.UserAccount;

/**
 * Created by robin on 11/18/14.
 */
public class UserAccountFactory {
    private static UserAccount robin;

    public static UserAccount getRobin() {
        if(robin != null) {
            return robin;
        }

        UserAccount account = new UserAccount();
        account.setEmail("robin.m.j@gmail.com");

        return robin = account;
    }
}
