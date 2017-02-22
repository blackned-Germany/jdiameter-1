/*
 * JBoss, Home of Professional Open Source
 * Copyright 2008, Red Hat, Inc. and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jdiameter.server.impl.app.sp;

import org.jdiameter.api.*;
import org.jdiameter.api.app.AppEvent;
import org.jdiameter.api.app.StateEvent;
import org.jdiameter.api.sp.ServerSpSession;
import org.jdiameter.api.sp.ServerSpSessionListener;
import org.jdiameter.api.sp.events.*;
import org.jdiameter.client.api.ISessionFactory;
import org.jdiameter.common.api.app.sp.ISpMessageFactory;
import org.jdiameter.common.impl.app.AppAnswerEventImpl;
import org.jdiameter.common.impl.app.AppRequestEventImpl;
import org.jdiameter.common.impl.app.sp.SpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Basic implementation of SpServerSession - can be one time - for UDR, PUR and
 * constant for SNR-PNR pair, in case when SNA contains response code from range
 * different than 2001-2004(success codes) user is responsible for maintaing
 * state - releasing etc, same goes if result code is contained
 * Experimental-Result AVP <br>
 * If SpSession moves to SpSessionState.TERMINATED - it means that no further
 * messages can be received via it and it should be discarded. <br>
 * <br>
 *
 * @author <a href = "mailto:dbeken@blackned.de"> Dincer Beken </a>
 */
public class SpServerSessionImpl extends SpSession implements ServerSpSession, EventListener<Request, Answer>, NetworkReqListener {

    private Logger logger = LoggerFactory.getLogger(SpServerSessionImpl.class);

    // Session State Handling ---------------------------------------------------
    protected Lock sendAndStateLock = new ReentrantLock();

    // Factories and Listeners --------------------------------------------------
    protected transient ISpMessageFactory factory = null;
    protected transient ServerSpSessionListener listener;

    protected ISpServerSessionData sessionData;
    protected long appId;
    public SpServerSessionImpl(ISpServerSessionData sessionData, ISpMessageFactory fct, ISessionFactory sf, ServerSpSessionListener lst) {
        super(sf, sessionData);
        if(sessionData == null) {
            throw new NullPointerException("SessionData must not be null");
        }
        if (lst == null) {
            throw new IllegalArgumentException("Listener can not be null");
        }
        if (fct.getApplicationId() < 0) {
            throw new IllegalArgumentException("ApplicationId can not be less than zero");
        }
        this.sessionData = sessionData;
        this.appId = fct.getApplicationId();
        this.listener = lst;
        this.factory = fct;
    }

    public void sendProfileUpdateAnswer(ProfileUpdateAnswer answer) throws InternalException, IllegalDiameterStateException, RouteException, OverloadException {
        send(Event.Type.SEND_PROFILE_UPDATE_ANSWER, null, answer);
    }

    public void sendPushNotificationRequest(PushNotificationRequest request) throws InternalException, IllegalDiameterStateException, RouteException, OverloadException {
        send(Event.Type.SEND_PUSH_NOTIFICATION_REQUEST, request, null);
    }

    public void sendSubscribeNotificationsAnswer(SubscribeNotificationsAnswer answer) throws InternalException, IllegalDiameterStateException, RouteException, OverloadException {
        send(Event.Type.SEND_SUBSCRIBE_NOTIFICATIONS_ANSWER, null, answer);
    }

    public void sendUserDataAnswer(UserDataAnswer answer) throws InternalException, IllegalDiameterStateException, RouteException, OverloadException {
        send(Event.Type.SEND_USER_DATA_ANSWER, null, answer);
    }

    public void receivedSuccessMessage(Request request, Answer answer) {
        AnswerDelivery rd = new AnswerDelivery();
        rd.session = this;
        rd.request = request;
        rd.answer = answer;
        super.scheduler.execute(rd);
    }

    public void timeoutExpired(Request request) {
        try {
            sendAndStateLock.lock();
            if (request.getApplicationId() == appId) {
                if (request.getCommandCode() == PushNotificationRequest.code) {
                    handleEvent(new Event(Event.Type.TIMEOUT_EXPIRES, factory.createPushNotificationRequest(request), null));
                    return;
                }
            }
        }
        catch (Exception e) {
            logger.debug("Failed to process timeout message", e);
        }
        finally {
            sendAndStateLock.unlock();
        }
    }

    public Answer processRequest(Request request) {
        RequestDelivery rd = new RequestDelivery();
        rd.session = this;
        rd.request = request;
        super.scheduler.execute(rd);
        return null;
    }

    public <E> E getState(Class<E> stateType) {
        return null;
    }

    public boolean handleEvent(StateEvent event) throws InternalException, OverloadException {
        try {
            sendAndStateLock.lock();
            Event localEvent = (Event) event;
            switch ((Event.Type) localEvent.getType()) {
                case RECEIVE_PROFILE_UPDATE_REQUEST:
                    listener.doProfileUpdateRequestEvent(this, (ProfileUpdateRequest) localEvent.getRequest());
                    break;
                case RECEIVE_PUSH_NOTIFICATION_ANSWER:
                    listener.doPushNotificationAnswerEvent(this, (PushNotificationRequest) localEvent.getRequest(), (PushNotificationAnswer) localEvent.getAnswer());
                    break;
                case RECEIVE_SUBSCRIBE_NOTIFICATIONS_REQUEST:
                    listener.doSubscribeNotificationsRequestEvent(this, (SubscribeNotificationsRequest) localEvent.getRequest());
                    break;
                case RECEIVE_USER_DATA_REQUEST:
                    listener.doUserDataRequestEvent(this, (UserDataRequest) localEvent.getRequest());
                    break;
                case SEND_PROFILE_UPDATE_ANSWER:
                    dispatchEvent(localEvent.getAnswer());
                    break;
                case SEND_PUSH_NOTIFICATION_REQUEST:
                    dispatchEvent(localEvent.getRequest());
                    break;
                case SEND_SUBSCRIBE_NOTIFICATIONS_ANSWER:
                    dispatchEvent(localEvent.getAnswer());
                    break;
                case SEND_USER_DATA_ANSWER:
                    dispatchEvent(localEvent.getAnswer());
                    break;
                case TIMEOUT_EXPIRES:
                    break;
                default:
                    logger.error("Wrong message type = {} req = {} ans = {}", new Object[]{localEvent.getType(), localEvent.getRequest(), localEvent.getAnswer()});
            }
        }
        catch (IllegalDiameterStateException idse) {
            throw new InternalException(idse);
        }
        catch (RouteException re) {
            throw new InternalException(re);
        }
        finally {
            sendAndStateLock.unlock();
        }

        return true;
    }

    public boolean isStateless() {
        return true;
    }

    protected void send(Event.Type type, AppEvent request, AppEvent answer) throws InternalException {
        try {
            //FIXME: isnt this bad? Shouldnt send be before state change?
            sendAndStateLock.lock();
            if (type != null) {
                handleEvent(new Event(type, request, answer));
            }
        }
        catch (Exception exc) {
            throw new InternalException(exc);
        }
        finally {
            sendAndStateLock.unlock();
        }
    }

    protected void dispatchEvent(AppEvent event) throws InternalException {
        try{
            session.send(event.getMessage(), this);
            // FIXME: add differentiation on server/client request
        }
        catch(Exception e) {
            logger.debug("Failed to dispatch event", e);
        }
    }

    public void release() {
        if (isValid()) {
            try {
                sendAndStateLock.lock();
                super.release();
            }
            catch (Exception e) {
                logger.debug("Failed to release session", e);
            }
            finally {
                sendAndStateLock.unlock();
            }
        }
        else {
            logger.debug("Trying to release an already invalid session, with Session ID '{}'", getSessionId());
        }
    }

    protected long extractExpiryTime(Message answer) {
        try {
            // FIXME: Replace 709 by Avp.EXPIRY_TIME
            Avp expiryTimeAvp = answer.getAvps().getAvp(709);
            return expiryTimeAvp != null ? expiryTimeAvp.getTime().getTime() : -1;
        }
        catch (AvpDataException ade) {
            logger.debug("Failure trying to extract Expiry-Time AVP value", ade);
        }

        return -1;
    }

    /* (non-Javadoc)
     * @see org.jdiameter.common.impl.app.AppSessionImpl#isReplicable()
     */
    @Override
    public boolean isReplicable() {
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (appId ^ (appId >>> 32));
        result = prime * result + ((sessionData == null) ? 0 : sessionData.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SpServerSessionImpl other = (SpServerSessionImpl) obj;
        if (appId != other.appId)
            return false;
        if (sessionData == null) {
            if (other.sessionData != null)
                return false;
        }
        else if (!sessionData.equals(other.sessionData))
            return false;
        return true;
    }


    @Override
    public void onTimer(String timerName) {
        logger.trace("onTimer({})", timerName);
    }

    private class RequestDelivery implements Runnable {
        ServerSpSession session;
        Request request;

        public void run() {
            try {
                if (request.getApplicationId() == appId) {
                    if (request.getCommandCode() == SubscribeNotificationsRequest.code) {
                        handleEvent(new Event(Event.Type.RECEIVE_SUBSCRIBE_NOTIFICATIONS_REQUEST, factory.createSubscribeNotificationsRequest(request), null));
                    }
                    else if(request.getCommandCode() == UserDataRequest.code) {
                        handleEvent(new Event(Event.Type.RECEIVE_USER_DATA_REQUEST, factory.createUserDataRequest(request), null));
                    }
                    else if(request.getCommandCode() == ProfileUpdateRequest.code) {
                        handleEvent(new Event(Event.Type.RECEIVE_PROFILE_UPDATE_REQUEST, factory.createProfileUpdateRequest(request), null));
                    }
                    else {
                        listener.doOtherEvent(session, new AppRequestEventImpl(request), null);
                    }
                }
            }
            catch (Exception e) {
                logger.debug("Failed to process request message", e);
            }
        }
    }

    private class AnswerDelivery implements Runnable {
        ServerSpSession session;
        Answer answer;
        Request request;

        public void run() {
            try {
                sendAndStateLock.lock();
                if (request.getApplicationId() == appId) {
                    if (request.getCommandCode() == PushNotificationRequest.code) {
                        handleEvent(new Event(Event.Type.RECEIVE_PUSH_NOTIFICATION_ANSWER, factory.createPushNotificationRequest(request), factory.createPushNotificationAnswer(answer)));
                        return;
                    }
                    else {
                        listener.doOtherEvent(session, new AppRequestEventImpl(request), new AppAnswerEventImpl(answer));
                    }
                }
                else {
                    logger.warn("Message with Application-Id {} reached Application Session with Application-Id {}. Skipping.", request.getApplicationId(), appId);
                }
            }
            catch (Exception e) {
                logger.debug("Failed to process success message", e);
            }
            finally {
                sendAndStateLock.unlock();
            }
        }
    }

}
