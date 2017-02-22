package org.jdiameter.common.impl.app.sp;

import org.jdiameter.api.NetworkReqListener;
import org.jdiameter.api.app.StateChangeListener;
import org.jdiameter.api.app.StateMachine;
import org.jdiameter.client.api.ISessionFactory;
import org.jdiameter.common.api.app.sp.ISpSessionData;
import org.jdiameter.common.impl.app.AppSessionImpl;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by dbeken on 23.02.2016.
 */
public abstract class SpSession extends AppSessionImpl implements NetworkReqListener, StateMachine {

    protected Lock sendAndStateLock = new ReentrantLock();

    protected transient List<StateChangeListener> stateListeners = new CopyOnWriteArrayList<StateChangeListener>();

    public SpSession(ISessionFactory sf, ISpSessionData data) {
        super(sf, data);
    }

    public void addStateChangeNotification(StateChangeListener listener) {
        if (!stateListeners.contains(listener)) {
            stateListeners.add(listener);
        }
    }

    public void removeStateChangeNotification(StateChangeListener listener) {
        stateListeners.remove(listener);
    }

    public void release() {
        //stateListeners.clear();
        super.release();
    }

}
