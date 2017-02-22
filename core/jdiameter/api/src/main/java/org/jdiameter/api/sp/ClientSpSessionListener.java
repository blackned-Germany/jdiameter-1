package org.jdiameter.api.sp;

import org.jdiameter.api.IllegalDiameterStateException;
import org.jdiameter.api.InternalException;
import org.jdiameter.api.OverloadException;
import org.jdiameter.api.RouteException;
import org.jdiameter.api.app.AppAnswerEvent;
import org.jdiameter.api.app.AppRequestEvent;
import org.jdiameter.api.app.AppSession;
import org.jdiameter.api.sp.events.*;

/**
 * Created by dbeken on 23.02.2016.
 */
public interface ClientSpSessionListener {
    public void doSubscribeNotificationsAnswerEvent(ClientSpSession session, SubscribeNotificationsRequest request, SubscribeNotificationsAnswer answer)throws InternalException, IllegalDiameterStateException, RouteException, OverloadException;
    public void doProfileUpdateAnswerEvent(ClientSpSession session, ProfileUpdateRequest request, ProfileUpdateAnswer answer) throws InternalException, IllegalDiameterStateException, RouteException, OverloadException;
    public void doPushNotificationRequestEvent(ClientSpSession session, PushNotificationRequest request)throws InternalException, IllegalDiameterStateException, RouteException, OverloadException;
    public void doUserDataAnswerEvent(ClientSpSession session, UserDataRequest request, UserDataAnswer answer) throws InternalException, IllegalDiameterStateException, RouteException, OverloadException;

    /**
     * Notifies this SpSessionEventListener that the ClientSpSession has recived not Sp message.
     * @param session parent application session (FSM)
     * @param request request object
     * @param answer answer object
     * @throws InternalException The InternalException signals that internal error is occurred.
     * @throws IllegalDiameterStateException The IllegalStateException signals that session has incorrect state (invalid).
     * @throws RouteException The NoRouteException signals that no route exist for a given realm.
     * @throws OverloadException The OverloadException signals that destination host is overloaded.
     */
    void doOtherEvent(AppSession session, AppRequestEvent request, AppAnswerEvent answer)
            throws InternalException, IllegalDiameterStateException, RouteException, OverloadException;


}
