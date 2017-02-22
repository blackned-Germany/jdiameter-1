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

package org.jdiameter.common.impl.app.sp;

import org.jdiameter.api.*;
import org.jdiameter.api.app.AppAnswerEvent;
import org.jdiameter.api.app.AppRequestEvent;
import org.jdiameter.api.app.AppSession;
import org.jdiameter.api.app.StateChangeListener;
import org.jdiameter.api.sp.ClientSpSession;
import org.jdiameter.api.sp.ClientSpSessionListener;
import org.jdiameter.api.sp.ServerSpSession;
import org.jdiameter.api.sp.ServerSpSessionListener;
import org.jdiameter.api.sp.events.*;
import org.jdiameter.client.api.ISessionFactory;
import org.jdiameter.client.impl.app.sp.ISpClientSessionData;
import org.jdiameter.client.impl.app.sp.SpClientSessionImpl;
import org.jdiameter.common.api.app.IAppSessionDataFactory;
import org.jdiameter.common.api.app.sp.ISpMessageFactory;
import org.jdiameter.common.api.app.sp.ISpSessionData;
import org.jdiameter.common.api.app.sp.ISpSessionFactory;
import org.jdiameter.common.api.data.ISessionDatasource;
import org.jdiameter.server.impl.app.sp.ISpServerSessionData;
import org.jdiameter.server.impl.app.sp.SpServerSessionImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author <a href="mailto:dbeken@blackned.de"> Dincer Beken </a>
 */
public class SpSessionFactoryImpl implements ISpSessionFactory, StateChangeListener<AppSession>, ClientSpSessionListener, ServerSpSessionListener, ISpMessageFactory {

  protected Logger logger = LoggerFactory.getLogger(SpSessionFactoryImpl.class);

  // Listeners provided by developer ----------------------------------------
  protected ClientSpSessionListener clientSpSessionListener;
  protected ServerSpSessionListener serverSpSessionListener;
  protected ISpMessageFactory messageFactory;
  //not used.
  protected StateChangeListener<AppSession> stateChangeListener;

  // Our magic --------------------------------------------------------------
  protected ISessionFactory sessionFactory;
  protected ISessionDatasource sessionDataSource;
  protected IAppSessionDataFactory<ISpSessionData> sessionDataFactory;
  protected long messageTimeout = 10000; // 10s default timeout
  protected static final long applicationId = 16777280;

  public SpSessionFactoryImpl(SessionFactory sessionFactory) {
    super();
    this.sessionFactory = (ISessionFactory) sessionFactory;
    this.sessionDataSource = this.sessionFactory.getContainer().getAssemblerFacility().getComponentInstance(ISessionDatasource.class);
    this.sessionDataFactory = (IAppSessionDataFactory<ISpSessionData>) this.sessionDataSource.getDataFactory(ISpSessionData.class);
    if(this.sessionDataFactory == null) {
      logger.debug("No factory for Sp Application data, using default/local.");
      this.sessionDataFactory = new SpLocalSessionDataFactory();
    }
  }

  /**
   * @return the clientSpSessionListener
   */
  public ClientSpSessionListener getClientSpSessionListener() {
    if (this.clientSpSessionListener == null) {
      return this;
    }
    else {
      return clientSpSessionListener;
    }
  }

  /**
   * @param clientSpSessionListener
   *            the clientSpSessionListener to set
   */
  public void setClientSpSessionListener(ClientSpSessionListener clientSpSessionListener) {
    this.clientSpSessionListener = clientSpSessionListener;
  }

  /**
   * @return the serverSpSessionListener
   */
  public ServerSpSessionListener getServerSpSessionListener() {
    if (this.serverSpSessionListener == null) {
      return this;
    }
    else {
      return serverSpSessionListener;
    }
  }

  /**
   * @param serverSpSessionListener
   *            the serverSpSessionListener to set
   */
  public void setServerSpSessionListener(ServerSpSessionListener serverSpSessionListener) {
    this.serverSpSessionListener = serverSpSessionListener;
  }

  /**
   * @return the messageFactory
   */
  public ISpMessageFactory getMessageFactory() {
    if (this.messageFactory == null) {
      return this;
    }
    else {
      return messageFactory;
    }
  }

  /**
   * @param messageFactory
   *            the messageFactory to set
   */
  public void setMessageFactory(ISpMessageFactory messageFactory) {
    this.messageFactory = messageFactory;
  }

  /**
   * @return the stateChangeListener
   */
  public StateChangeListener<AppSession> getStateChangeListener() {
    return stateChangeListener;
  }

  /**
   * @param stateChangeListener
   *            the stateChangeListener to set
   */
  public void setStateChangeListener(StateChangeListener<AppSession> stateChangeListener) {
    this.stateChangeListener = stateChangeListener;
  }

  // IAppSession ------------------------------------------------------------

  /*
   * (non-Javadoc)
   * 
   * @see org.jdiameter.common.api.app.IAppSessionFactory#getNewSession(java.lang.String, java.lang.Class, 
   *   ApplicationId, java.lang.Object[])
   */
  public AppSession getNewSession(String sessionId, Class<? extends AppSession> aClass, ApplicationId applicationId, Object[] args) {
    try {
      // FIXME: add proper handling for SessionId
      if (aClass == ClientSpSession.class) {
        SpClientSessionImpl clientSession = null;

        if (sessionId == null) {
          if (args != null && args.length > 0 && args[0] instanceof Request) {
            Request request = (Request) args[0];
            sessionId = request.getSessionId();
          }
          else {
            sessionId = this.sessionFactory.getSessionId();
          }
        }

        ISpClientSessionData sessionData = (ISpClientSessionData) this.sessionDataFactory.getAppSessionData(ClientSpSession.class, sessionId);
        sessionData.setApplicationId(applicationId);
        clientSession = new SpClientSessionImpl(sessionData, this.getMessageFactory(), sessionFactory, this.getClientSpSessionListener());
        sessionDataSource.addSession(clientSession);
        clientSession.getSessions().get(0).setRequestListener(clientSession);
        return clientSession;
      }
      else if (aClass == ServerSpSession.class) {
        SpServerSessionImpl serverSession = null;

        if (sessionId == null) {
          if (args != null && args.length > 0 && args[0] instanceof Request) {
            Request request = (Request) args[0];
            sessionId = request.getSessionId();
          }
          else {
            sessionId = this.sessionFactory.getSessionId();
          }
        }
        ISpServerSessionData sessionData = (ISpServerSessionData) this.sessionDataFactory.getAppSessionData(ServerSpSession.class, sessionId);
        sessionData.setApplicationId(applicationId);
        serverSession = new SpServerSessionImpl(sessionData, this.getMessageFactory(), sessionFactory, getServerSpSessionListener());
        sessionDataSource.addSession(serverSession);
        serverSession.getSessions().get(0).setRequestListener(serverSession);
        return serverSession;
      }
      else {
        throw new IllegalArgumentException("Wrong session class: [" + aClass + "]. Supported[" + ClientSpSession.class + "]");
      }
    }
    catch (IllegalArgumentException iae) {
      throw iae;
    }
    catch (Exception e) {
      logger.error("Failure to obtain new Sp Session.", e);
    }

    return null;
  }

  @Override
  public AppSession getSession(String sessionId, Class<? extends AppSession> aClass) {
    if (sessionId == null) {
      throw new IllegalArgumentException("Session-Id must not be null");
    }
    if(!this.sessionDataSource.exists(sessionId)) {
      return null;
    }

    AppSession appSession = null;
    try {
      if(aClass == ServerSpSession.class) {
        ISpServerSessionData sessionData = (ISpServerSessionData) this.sessionDataFactory.getAppSessionData(ServerSpSession.class, sessionId);
        appSession = new SpServerSessionImpl(sessionData, this.getMessageFactory(), sessionFactory, getServerSpSessionListener());
        appSession.getSessions().get(0).setRequestListener((NetworkReqListener) appSession);
      }
      else if (aClass == ClientSpSession.class) {
        ISpClientSessionData sessionData = (ISpClientSessionData) this.sessionDataFactory.getAppSessionData(ClientSpSession.class, sessionId);
        appSession = new SpClientSessionImpl(sessionData, this.getMessageFactory(), sessionFactory, this.getClientSpSessionListener());
        appSession.getSessions().get(0).setRequestListener((NetworkReqListener) appSession);
      }
      else {
        throw new IllegalArgumentException("Wrong session class: " + aClass + ". Supported[" + ServerSpSession.class + "," + ClientSpSession.class + "]");
      }
    }
    catch (Exception e) {
      logger.error("Failure to obtain new Sp Session.", e);
    }

    return appSession;
  }

  // Methods to handle default values for user listeners --------------------
  @SuppressWarnings("unchecked")
  public void stateChanged(Enum oldState, Enum newState) {
    logger.info("Diameter Sp SessionFactory :: stateChanged :: oldState[{}], newState[{}]", oldState, newState);
  }

  /*
   * (non-Javadoc)
   * 
   * @see StateChangeListener#stateChanged(java.lang.Object, java.lang.Enum, java.lang.Enum)
   */
  @SuppressWarnings("unchecked")
  public void stateChanged(AppSession source, Enum oldState, Enum newState) {
    logger.info("Diameter Sp SessionFactory :: stateChanged :: source[{}], oldState[{}], newState[{}]", new Object[] { source, oldState, newState });
  }

  // Message Handlers -------------------------------------------------------

  /*
   * (non-Javadoc)
   * 
   * @see ClientSpSessionListener#doOtherEvent(AppSession,
   *   AppRequestEvent, AppAnswerEvent)
   */
  public void doOtherEvent(AppSession session, AppRequestEvent request, AppAnswerEvent answer)
  throws InternalException, IllegalDiameterStateException, RouteException, OverloadException {
    // TODO Auto-generated method stub
  }

  /*
   * (non-Javadoc)
   * 
   * @see ClientSpSessionListener#doProfileUpdateAnswerEvent(ClientSpSession,
   *   ProfileUpdateRequest, ProfileUpdateAnswer)
   */
  public void doProfileUpdateAnswerEvent(ClientSpSession session, ProfileUpdateRequest request, ProfileUpdateAnswer answer)
  throws InternalException, IllegalDiameterStateException, RouteException, OverloadException {
    // TODO Auto-generated method stub
  }

  /*
   * (non-Javadoc)
   * 
   * @see ClientSpSessionListener#doPushNotificationRequestEvent(
   *   ClientSpSession, PushNotificationRequest)
   */
  public void doPushNotificationRequestEvent(ClientSpSession session, PushNotificationRequest request)
  throws InternalException, IllegalDiameterStateException, RouteException, OverloadException {
    // TODO Auto-generated method stub
  }

  /*
   * (non-Javadoc)
   * 
   * @see ClientSpSessionListener#doSubscribeNotificationsAnswerEvent(ClientSpSession,
   *   SubscribeNotificationsRequest, SubscribeNotificationsAnswer)
   */
  public void doSubscribeNotificationsAnswerEvent(ClientSpSession session, SubscribeNotificationsRequest request,
                                                  SubscribeNotificationsAnswer answer) throws InternalException, IllegalDiameterStateException, RouteException, OverloadException {
    // TODO Auto-generated method stub
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * ClientSpSessionListener#doUserDataAnswerEvent(org
   * .jdiameter.ClientSpSession,
   * UserDataRequest,
   * UserDataAnswer)
   */
  public void doUserDataAnswerEvent(ClientSpSession session, UserDataRequest request, UserDataAnswer answer)
  throws InternalException, IllegalDiameterStateException, RouteException, OverloadException {
    // TODO Auto-generated method stub
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * ServerSpSessionListener#doProfileUpdateRequestEvent
   * (ServerSpSession,
   * ProfileUpdateRequest)
   */
  public void doProfileUpdateRequestEvent(ServerSpSession session, ProfileUpdateRequest request)
  throws InternalException, IllegalDiameterStateException, RouteException, OverloadException {
    // TODO Auto-generated method stub
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * ServerSpSessionListener#doPushNotificationAnswerEvent
   * (ServerSpSession,
   * PushNotificationRequest,
   * PushNotificationAnswer)
   */
  public void doPushNotificationAnswerEvent(ServerSpSession session, PushNotificationRequest request, PushNotificationAnswer answer)
  throws InternalException, IllegalDiameterStateException, RouteException, OverloadException {
    // TODO Auto-generated method stub
  }

  /*
   * (non-Javadoc)
   * 
   * @seeorg.jdiameter.ServerSpSessionListener#
   * doSubscribeNotificationsRequestEvent
   * (ServerSpSession,
   * SubscribeNotificationsRequest)
   */
  public void doSubscribeNotificationsRequestEvent(ServerSpSession session, SubscribeNotificationsRequest request)
  throws InternalException, IllegalDiameterStateException, RouteException, OverloadException {
    // TODO Auto-generated method stub
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * ServerSpSessionListener#doUserDataRequestEvent(org
   * .jdiameter.ServerSpSession,
   * UserDataRequest)
   */
  public void doUserDataRequestEvent(ServerSpSession session, UserDataRequest request)
  throws InternalException, IllegalDiameterStateException, RouteException, OverloadException {
    // TODO Auto-generated method stub
  }

  // Message Factory ----------------------------------------------------------

  public AppAnswerEvent createProfileUpdateAnswer(Answer answer) {
    return new ProfileUpdateAnswerImpl(answer);
  }

  public AppRequestEvent createProfileUpdateRequest(Request request) {
    return new ProfileUpdateRequestImpl(request);
  }

  public AppAnswerEvent createPushNotificationAnswer(Answer answer) {
    return new PushNotificationAnswerImpl(answer);
  }

  public AppRequestEvent createPushNotificationRequest(Request request) {
    return new PushNotificationRequestImpl(request);
  }

  public AppAnswerEvent createSubscribeNotificationsAnswer(Answer answer) {
    return new SubscribeNotificationsAnswerImpl(answer);
  }

  public AppRequestEvent createSubscribeNotificationsRequest(Request request) {
    return new SubscribeNotificationsRequestImpl(request);
  }

  public AppAnswerEvent createUserDataAnswer(Answer answer) {
    return new UserDataAnswerImpl(answer);
  }

  public AppRequestEvent createUserDataRequest(Request request) {
    return new UserDataRequestImpl(request);
  }

  public long getApplicationId() {
    return applicationId;
  }

  public long getMessageTimeout() {
    return messageTimeout;
  }
}
