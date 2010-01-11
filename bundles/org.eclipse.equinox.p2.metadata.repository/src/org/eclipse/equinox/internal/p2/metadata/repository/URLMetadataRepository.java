/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Prashant Deva - Bug 194674 [prov] Provide write access to metadata repository
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata.repository;

import org.eclipse.equinox.p2.repository.metadata.spi.AbstractMetadataRepository;

import java.net.URI;
import java.util.Map;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.query.IQuery;
import org.eclipse.equinox.p2.metadata.query.IQueryResult;

/**
 * A metadata repository backed by an arbitrary URL.
 */
public class URLMetadataRepository extends AbstractMetadataRepository {

	public static final String CONTENT_FILENAME = "content"; //$NON-NLS-1$
	public static final String XML_EXTENSION = ".xml"; //$NON-NLS-1$
	private static final String REPOSITORY_TYPE = URLMetadataRepository.class.getName();
	private static final Integer REPOSITORY_VERSION = new Integer(1);

	transient protected URI content;
	protected IUMap units = new IUMap();

	public static URI getActualLocation(URI base) {
		return getActualLocation(base, XML_EXTENSION);
	}

	public static URI getActualLocation(URI base, String extension) {
		if (extension == null)
			extension = XML_EXTENSION;
		return URIUtil.append(base, CONTENT_FILENAME + extension);
	}

	public URLMetadataRepository() {
		super();
	}

	public URLMetadataRepository(URI location, String name, Map<String, String> properties) {
		super(name == null ? (location != null ? location.toString() : "") : name, REPOSITORY_TYPE, REPOSITORY_VERSION.toString(), location, null, null, properties); //$NON-NLS-1$
		content = getActualLocation(location);
	}

	// this is synchronized because content can be initialized in initializeAfterLoad
	protected synchronized URI getContentURL() {
		return content;
	}

	public synchronized void initialize(RepositoryState state) {
		this.name = state.Name;
		this.type = state.Type;
		this.version = state.Version.toString();
		this.provider = state.Provider;
		this.description = state.Description;
		this.location = state.Location;
		this.properties = state.Properties;
		this.units.addAll(state.Units);
	}

	// Use this method to setup any transient fields etc after the object has been restored from a stream
	public synchronized void initializeAfterLoad(URI repoLocation) {
		this.location = repoLocation;
		content = getActualLocation(location);
	}

	public boolean isModifiable() {
		return false;
	}

	public synchronized IQueryResult<IInstallableUnit> query(IQuery<IInstallableUnit> query, IProgressMonitor monitor) {
		if (query instanceof InstallableUnitQuery)
			return units.query((InstallableUnitQuery) query);
		return query.perform(units.iterator());
	}
}
