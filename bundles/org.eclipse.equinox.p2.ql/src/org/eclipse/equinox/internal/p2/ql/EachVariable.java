/*******************************************************************************
 * Copyright (c) 2009 Cloudsmith Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ql;

/**
 * An expression representing a variable stack in the current thread.
 */
final class EachVariable extends Variable {
	static final String KEYWORD_EACH = "_"; //$NON-NLS-1$

	EachVariable(String name) {
		super(name);
	}

	int countReferenceToEverything() {
		return 0;
	}
}
