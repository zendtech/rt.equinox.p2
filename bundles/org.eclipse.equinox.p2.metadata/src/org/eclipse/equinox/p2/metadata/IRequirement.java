/******************************************************************************* 
* Copyright (c) 2009 IBM and others. All rights reserved. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0 which accompanies this distribution, and is
* available at http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   IBM - initial API and implementation
******************************************************************************/
package org.eclipse.equinox.p2.metadata;

import org.eclipse.equinox.p2.metadata.query.IQuery;

/**
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 * @since 2.0
 */
public interface IRequirement {

	public int getMin();

	public int getMax();

	public IQuery getMatches();

	public IQuery getFilter();

	public boolean isGreedy();
}