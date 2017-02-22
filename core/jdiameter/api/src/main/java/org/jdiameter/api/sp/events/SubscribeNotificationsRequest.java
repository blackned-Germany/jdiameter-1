/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat, Inc. and individual contributors
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

package org.jdiameter.api.sp.events;

import org.jdiameter.api.app.AppRequestEvent;

/**
 * The Subscribe-Notifications-Request (SNR) command, indicated by the Command-Code field set to
 * 308 and the �R� bit set in the Command Flags field, is sent by a Diameter client to a Diameter
 * server in order to request notifications of changes in user data.
 *
 * @author <a href="mailto:brainslog@gmail.com"> Alexandre Mendonca </a>
 * @author <a href="mailto:baranowb@gmail.com"> Bartosz Baranowski </a>
 */
public interface SubscribeNotificationsRequest extends AppRequestEvent {
	
  public static final String _SHORT_NAME = "SNR";
	public static final String _LONG_NAME = "Subscribe-Notifications-Request";
	
	public static final int code = 308;
	
}
