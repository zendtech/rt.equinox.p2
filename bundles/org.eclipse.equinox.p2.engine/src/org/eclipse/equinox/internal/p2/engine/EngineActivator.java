/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.engine;

import org.eclipse.equinox.internal.provisional.p2.core.eventbus.IProvisioningEventBus;
import org.eclipse.equinox.internal.provisional.p2.engine.Engine;
import org.eclipse.equinox.internal.provisional.p2.engine.IEngine;
import org.osgi.framework.*;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class EngineActivator implements BundleActivator, ServiceTrackerCustomizer {
	private static BundleContext context;
	public static final String ID = "org.eclipse.equinox.p2.engine"; //$NON-NLS-1$

	private ServiceRegistration registration;

	private ServiceTracker tracker;

	public static BundleContext getContext() {
		return context;
	}

	public Object addingService(ServiceReference reference) {
		if (registration == null) {
			IProvisioningEventBus eventBus = (IProvisioningEventBus) context.getService(reference);
			registration = context.registerService(IEngine.SERVICE_NAME, new Engine(eventBus), null);
			return eventBus;
		}
		return null;
	}

	public void modifiedService(ServiceReference reference, Object service) {
		// nothing to do
	}

	public void removedService(ServiceReference reference, Object service) {
		if (registration != null) {
			registration.unregister();
			registration = null;
		}
	}

	public void start(BundleContext aContext) throws Exception {
		EngineActivator.context = aContext;
		tracker = new ServiceTracker(aContext, IProvisioningEventBus.SERVICE_NAME, this);
		tracker.open();
	}

	public void stop(BundleContext aContext) throws Exception {
		tracker.close();
		tracker = null;

		EngineActivator.context = null;
	}

}
