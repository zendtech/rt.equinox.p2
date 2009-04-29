/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.internal.repository.tools;

import java.net.MalformedURLException;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.artifact.repository.CompositeArtifactRepository;
import org.eclipse.equinox.internal.p2.repository.helpers.RepositoryHelper;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.repository.ICompositeRepository;
import org.eclipse.equinox.internal.provisional.p2.repository.IRepository;
import org.eclipse.osgi.util.NLS;

public class CompositeRepositoryApplication extends AbstractApplication {
	private List childrenToAdd = new ArrayList();
	private List childrenToRemove = new ArrayList();
	private boolean failOnExists = false;
	private String comparatorID = null;

	public IStatus run(IProgressMonitor monitor) throws ProvisionException {
		try {
			initializeRepos(new NullProgressMonitor());
			// load repository
			ICompositeRepository metadataRepo = (ICompositeRepository) destinationMetadataRepository;
			CompositeArtifactRepository artifactRepo = (CompositeArtifactRepository) destinationArtifactRepository;

			// Remove children from the Composite Repositories
			for (Iterator iterator = childrenToRemove.iterator(); iterator.hasNext();) {
				RepositoryDescriptor child = (RepositoryDescriptor) iterator.next();
				if (child.isArtifact() && artifactRepo != null)
					artifactRepo.removeChild(child.getRepoLocation());
				if (child.isMetadata() && metadataRepo != null)
					metadataRepo.removeChild(child.getRepoLocation());
			}

			// Add children to the Composite Repositories
			for (Iterator iterator = childrenToAdd.iterator(); iterator.hasNext();) {
				RepositoryDescriptor child = (RepositoryDescriptor) iterator.next();
				if (child.isArtifact() && artifactRepo != null)
					artifactRepo.addChild(child.getRepoLocation());
				if (child.isMetadata() && metadataRepo != null)
					metadataRepo.addChild(child.getRepoLocation());
			}

			if (comparatorID != null)
				if (!artifactRepo.validate(comparatorID))
					return new Status(IStatus.ERROR, Activator.ID, NLS.bind(Messages.CompositeRepositoryApplication_failedComparator, comparatorID));
			return Status.OK_STATUS;
		} finally {
			finalizeRepositories();
		}
	}

	public void addChild(RepositoryDescriptor child) {
		childrenToAdd.add(child);
	}

	public void removeChild(RepositoryDescriptor child) {
		childrenToRemove.add(child);
	}

	public void setFailOnExists(boolean value) {
		failOnExists = value;
	}

	protected IArtifactRepository initializeDestination(RepositoryDescriptor toInit, IArtifactRepositoryManager mgr) throws ProvisionException {
		// remove the repo first.
		mgr.removeRepository(toInit.getRepoLocation());

		// first try and load to see if one already exists at that location.
		try {
			IArtifactRepository repository = mgr.loadRepository(toInit.getRepoLocation(), null);
			if (validRepositoryLocation(repository) && initDestinationRepository(repository, toInit))
				return repository;
			throw new ProvisionException(new Status(IStatus.INFO, Activator.ID, NLS.bind(Messages.CompositeRepository_composite_repository_exists, toInit.getRepoLocation())));
		} catch (ProvisionException e) {
			// re-throw the exception if we got anything other than "repo not found"
			if (e.getStatus().getCode() != ProvisionException.REPOSITORY_NOT_FOUND) {
				if (e.getCause() instanceof MalformedURLException)
					throw new ProvisionException(NLS.bind(Messages.exception_invalidDestination, toInit.getRepoLocation()), e.getCause());
				throw e;
			}
		}

		IArtifactRepository source = null;
		try {
			if (toInit.getFormat() != null)
				source = mgr.loadRepository(toInit.getFormat(), 0, null);
		} catch (ProvisionException e) {
			//Ignore.
		}
		//This code assumes source has been successfully loaded before this point
		try {
			//No existing repository; create a new repository at destinationLocation but with source's attributes.
			IArtifactRepository repo = mgr.createRepository(toInit.getRepoLocation(), toInit.getName() != null ? toInit.getName() : (source != null ? source.getName() : Messages.CompositeRepository_default_artifactRepo_name), IArtifactRepositoryManager.TYPE_COMPOSITE_REPOSITORY, source != null ? source.getProperties() : null);
			initRepository(repo, toInit);
			return repo;
		} catch (IllegalStateException e) {
			mgr.removeRepository(toInit.getRepoLocation());
			throw e;
		}
	}

	protected IMetadataRepository initializeDestination(RepositoryDescriptor toInit, IMetadataRepositoryManager mgr) throws ProvisionException {
		// remove the repo first.
		mgr.removeRepository(toInit.getRepoLocation());

		// first try and load to see if one already exists at that location.
		try {
			IMetadataRepository repository = mgr.loadRepository(toInit.getRepoLocation(), null);
			if (!validRepositoryLocation(repository) && initDestinationRepository(repository, toInit))
				throw new ProvisionException(new Status(IStatus.INFO, Activator.ID, NLS.bind(Messages.CompositeRepository_composite_repository_exists, toInit.getRepoLocation())));
			return repository;
		} catch (ProvisionException e) {
			// re-throw the exception if we got anything other than "repo not found"
			if (e.getStatus().getCode() != ProvisionException.REPOSITORY_NOT_FOUND) {
				if (e.getCause() instanceof MalformedURLException)
					throw new ProvisionException(NLS.bind(Messages.exception_invalidDestination, toInit.getRepoLocation()), e.getCause());
				throw e;
			}
		}

		IMetadataRepository source = null;
		try {
			if (toInit.getFormat() != null)
				source = mgr.loadRepository(toInit.getFormat(), 0, null);
		} catch (ProvisionException e) {
			//Ignore
		}
		//This code assumes source has been successfully loaded before this point
		try {
			//No existing repository; create a new repository at destinationLocation but with source's attributes.
			IMetadataRepository repo = mgr.createRepository(toInit.getRepoLocation(), toInit.getName() != null ? toInit.getName() : (source != null ? source.getName() : Messages.CompositeRepository_default_metadataRepo_name), IMetadataRepositoryManager.TYPE_COMPOSITE_REPOSITORY, source != null ? source.getProperties() : null);
			initRepository(repo, toInit);
			return repo;
		} catch (IllegalStateException e) {
			mgr.removeRepository(toInit.getRepoLocation());
			throw e;
		}
	}

	/*
	 * Determine if the repository is valid for this operation
	 */
	private boolean validRepositoryLocation(IRepository repository) throws ProvisionException {
		if (repository instanceof ICompositeRepository) {
			// if we have an already existing repository at that location, then throw an error
			// if the user told us to
			if (failOnExists)
				throw new ProvisionException(NLS.bind(Messages.CompositeRepository_composite_repository_exists, repository.getLocation()));
			RepositoryHelper.validDestinationRepository(repository);
			return true;
		}
		// we have a non-composite repo at this location. that is ok because we can co-exist.
		return true;
	}

	/*
	 * Initialize a new repository
	 */
	private void initRepository(IRepository repository, RepositoryDescriptor desc) {
		RepositoryHelper.validDestinationRepository(repository);
		if (desc.isCompressed() && !repository.getProperties().containsKey(IRepository.PROP_COMPRESSED))
			repository.setProperty(IRepository.PROP_COMPRESSED, String.valueOf(true));
	}

	public void setComparator(String value) {
		comparatorID = value;
	}
}
