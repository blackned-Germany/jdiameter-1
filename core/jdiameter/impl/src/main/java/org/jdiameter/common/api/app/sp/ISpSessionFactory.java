package org.jdiameter.common.api.app.sp;

import org.jdiameter.api.app.AppSession;
import org.jdiameter.api.app.StateChangeListener;
import org.jdiameter.api.sp.ClientSpSessionListener;
import org.jdiameter.api.sp.ServerSpSessionListener;
import org.jdiameter.common.api.app.IAppSessionFactory;

/**
 * Created by dbeken on 23.02.2016.
 */
public interface ISpSessionFactory extends IAppSessionFactory {
    public void setClientSpSessionListener(ClientSpSessionListener v);

    public ClientSpSessionListener getClientSpSessionListener();

    public void setServerSpSessionListener(ServerSpSessionListener v);

    public ServerSpSessionListener getServerSpSessionListener();

    public void setStateChangeListener(StateChangeListener<AppSession> v);

    public StateChangeListener<AppSession> getStateChangeListener();

    public void setMessageFactory(ISpMessageFactory factory);

    public ISpMessageFactory getMessageFactory();

}
