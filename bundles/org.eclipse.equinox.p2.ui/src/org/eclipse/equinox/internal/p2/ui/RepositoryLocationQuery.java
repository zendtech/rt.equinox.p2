/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.equinox.internal.p2.ui;

import java.net.URI;
import org.eclipse.equinox.p2.metadata.query.ExpressionQuery;

/**
 * RepositoryLocationQuery yields true for all URI elements.  
 * 
 * @since 3.5
 */
public class RepositoryLocationQuery extends ExpressionQuery<URI> {

	public RepositoryLocationQuery() {
		super(URI.class, matchAll());
	}
}
