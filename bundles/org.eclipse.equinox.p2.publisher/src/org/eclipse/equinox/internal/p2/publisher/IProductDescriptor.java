/*******************************************************************************
 * Copyright (c) 2008 Code 9 and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.publisher;

import java.util.List;

public interface IProductDescriptor {

	public String getLauncherName();

	/**
	 * Returns the list of all bundles in this product.
	 * @param includeFragments whether or not to include the fragments in the return value
	 * @return the list of bundles in this product
	 */
	public List getBundles(boolean includeFragments);

	public List getFragments();

	public List getFeatures();

	public String getConfigIniPath(String os);

	public String getId();

	public String getSplashLocation();

	public String getProductName();

	public boolean useFeatures();

	public String getVersion();

	public String getVMArguments(String os);

	public String getProgramArguments(String os);

}