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
package org.eclipse.equinox.p2.ui.query;

import org.eclipse.equinox.internal.p2.ui.model.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.ui.model.CategoryElement;
import org.eclipse.equinox.p2.ui.operations.ProvisioningUtil;

/**
 * Collector that examines available IU's and wraps them in an
 * element representing either a category an IU.
 *  
 * @since 3.4
 */
public class AvailableIUCollector extends QueriedElementCollector {

	private boolean makeCategories;

	public AvailableIUCollector(IProvElementQueryProvider queryProvider, IQueryable queryable, boolean makeCategories) {
		super(queryProvider, queryable);
		this.makeCategories = makeCategories;
	}

	/**
	 * Accepts a result that matches the query criteria.
	 * 
	 * @param match an object matching the query
	 * @return <code>true</code> if the query should continue,
	 * or <code>false</code> to indicate the query should stop.
	 */
	public boolean accept(Object match) {
		if (!(match instanceof IInstallableUnit))
			return super.accept(match);
		IInstallableUnit iu = (IInstallableUnit) match;
		if (makeCategories && isCategory(iu))
			return super.accept(new CategoryElement(iu));
		return super.accept(makeDefaultElement(iu));
	}

	protected ProvElement makeDefaultElement(IInstallableUnit iu) {
		return new AvailableIUElement(iu, null);
	}

	protected boolean isCategory(IInstallableUnit iu) {
		return ProvisioningUtil.isCategory(iu);
	}

	protected boolean makeCategory() {
		return makeCategories;
	}
}
