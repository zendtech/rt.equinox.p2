/*******************************************************************************
 * Copyright (c) 2008 Code 9 and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.publisher.actions;

import java.util.Properties;
import org.eclipse.equinox.internal.p2.publisher.IPublishingAdvice;
import org.eclipse.equinox.internal.provisional.frameworkadmin.BundleInfo;

public interface IConfigAdvice extends IPublishingAdvice {

	public static final String ID = "config_advice"; //$NON-NLS-1$

	public BundleInfo[] getBundles();

	public Properties getProperties();
}
