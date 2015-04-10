package com.riverflows;

import android.app.Activity;
import android.content.Context;

import com.google.inject.AbstractModule;
import com.riverflows.data.UserAccount;
import com.riverflows.wsclient.WsSession;
import com.riverflows.wsclient.WsSessionManager;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by robin on 3/14/15.
 */
public class MockSessionManager extends AbstractModule {

    public WsSessionManager manager = mock(WsSessionManager.class);

    public WsSession session = null;

    @Override
    protected void configure() {
        when(manager.getSession(any(Context.class))).thenReturn(session);
        try {
            when(manager.getWsAuthToken(anyString(), anyString(), anyString())).then(new Answer<WsSession>(){
                @Override
                public WsSession answer(InvocationOnMock invocationOnMock) throws Throwable {
                    return createSession((String) invocationOnMock.getArguments()[1]);
                }
            });

            when(manager.loginWithGoogleOAuth2(any(Activity.class), anyString())).then(new Answer<WsSession>(){
                @Override
                public WsSession answer(InvocationOnMock invocationOnMock) throws Throwable {
                    return createSession((String) invocationOnMock.getArguments()[1]);
                }
            });
        } catch(Exception e) {
            throw new RuntimeException(e);
        }

        bind(WsSessionManager.class).toInstance(manager);
    }

    private WsSession createSession(String username) {
        if(session == null) {
            UserAccount account = new UserAccount();
            account.setEmail(username + "@gmail.com");
            session = new WsSession(username, new UserAccount(), "", System.currentTimeMillis() + 10 * 60 * 1000);
        }

        return session;
    }
}
