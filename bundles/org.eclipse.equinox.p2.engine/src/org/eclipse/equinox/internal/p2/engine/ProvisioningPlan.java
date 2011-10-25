/*******************************************************************************
 *  Copyright (c) 2007, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.engine;

import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.metadata.InstallableUnit;
import org.eclipse.equinox.internal.p2.metadata.TranslationSupport;
import org.eclipse.equinox.internal.p2.metadata.index.*;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.KeyWithLocale;
import org.eclipse.equinox.p2.metadata.index.IIndex;
import org.eclipse.equinox.p2.metadata.index.IIndexProvider;
import org.eclipse.equinox.p2.query.*;

/**
 * @since 2.0
 */
public class ProvisioningPlan implements IProvisioningPlan {

	private final IProfile profile;
	private final List<Operand> operands = new ArrayList<Operand>();
	private final ProvisioningContext context;
	private IStatus status;
	private IProvisioningPlan installerPlan;
	private boolean sortOperands = false;

	public ProvisioningPlan(IProfile profile, Operand[] operands, ProvisioningContext context) {
		this(Status.OK_STATUS, profile, operands, context, null);
	}

	public ProvisioningPlan(IStatus status, IProfile profile, ProvisioningContext context, IProvisioningPlan installerPlan) {
		this(status, profile, null, context, installerPlan);
	}

	public ProvisioningPlan(IStatus status, IProfile profile, Operand[] operands, ProvisioningContext context, IProvisioningPlan installerPlan) {
		Assert.isNotNull(profile);
		this.status = status;
		this.profile = profile;
		if (operands != null)
			this.operands.addAll(Arrays.asList(operands));
		this.context = (context == null) ? new ProvisioningContext(profile.getProvisioningAgent()) : context;
		this.installerPlan = installerPlan;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.engine.IProvisioningPlan#getStatus()
	 */
	public IStatus getStatus() {
		return status;
	}

	public void setStatus(IStatus status) {
		this.status = status;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.engine.IProvisioningPlan#getProfile()
	 */
	public IProfile getProfile() {
		return profile;
	}

	public Operand[] getOperands() {
		return operands.toArray(new Operand[operands.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.engine.IProvisioningPlan#getRemovals()
	 */
	public IQueryable<IInstallableUnit> getRemovals() {
		return new QueryablePlan(false);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.engine.IProvisioningPlan#getAdditions()
	 */
	public IQueryable<IInstallableUnit> getAdditions() {
		return new QueryablePlan(true);
	}

	private class QueryablePlan implements IQueryable<IInstallableUnit>, IIndexProvider<IInstallableUnit> {
		private boolean addition;
		private IdIndex idIndex;
		private CapabilityIndex capabilityIndex;
		private TranslationSupport translationSupport;

		public QueryablePlan(boolean add) {
			this.addition = add;
		}

		public IQueryResult<IInstallableUnit> query(IQuery<IInstallableUnit> query, IProgressMonitor monitor) {
			if (operands == null || status.getSeverity() == IStatus.ERROR)
				return Collector.emptyCollector();
			return IndexProvider.query(this, query, monitor);
		}

		private Collection<IInstallableUnit> getIUs() {
			Collection<IInstallableUnit> units = new ArrayList<IInstallableUnit>(operands.size());
			for (Operand operand : operands) {
				if (!(operand instanceof InstallableUnitOperand))
					continue;
				InstallableUnitOperand op = ((InstallableUnitOperand) operand);
				IInstallableUnit iu = addition ? op.second() : op.first();
				if (iu != null)
					units.add(iu);
			}
			return units;
		}

		public synchronized IIndex<IInstallableUnit> getIndex(String memberName) {
			if (InstallableUnit.MEMBER_ID.equals(memberName)) {
				if (idIndex == null)
					idIndex = new IdIndex(getIUs().iterator());
				return idIndex;
			}

			if (InstallableUnit.MEMBER_PROVIDED_CAPABILITIES.equals(memberName)) {
				if (capabilityIndex == null)
					capabilityIndex = new CapabilityIndex(getIUs().iterator());
				return capabilityIndex;
			}
			return null;
		}

		public synchronized Object getManagedProperty(Object client, String memberName, Object key) {
			if (!(client instanceof IInstallableUnit))
				return null;
			IInstallableUnit iu = (IInstallableUnit) client;
			if (InstallableUnit.MEMBER_TRANSLATED_PROPERTIES.equals(memberName)) {
				if (translationSupport == null)
					translationSupport = new TranslationSupport(this);
				return key instanceof KeyWithLocale ? translationSupport.getIUProperty(iu, (KeyWithLocale) key) : translationSupport.getIUProperty(iu, key.toString());
			}
			return null;
		}

		public Iterator<IInstallableUnit> everything() {
			return getIUs().iterator();
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.engine.IProvisioningPlan#getInstallerPlan()
	 */
	public IProvisioningPlan getInstallerPlan() {
		return installerPlan;
	}

	public ProvisioningContext getContext() {
		return context;
	}

	public void setInstallerPlan(IProvisioningPlan p) {
		installerPlan = p;
	}

	public void addInstallableUnit(IInstallableUnit iu) {
		operands.add(new InstallableUnitOperand(null, iu));
	}

	public void removeInstallableUnit(IInstallableUnit iu) {
		operands.add(new InstallableUnitOperand(iu, null));
	}

	public void updateInstallableUnit(IInstallableUnit iu1, IInstallableUnit iu2) {
		operands.add(new InstallableUnitOperand(iu1, iu2));
	}

	public void setProfileProperty(String name, String value) {
		String currentValue = profile.getProperty(name);
		if (value == null && currentValue == null)
			return;
		operands.add(new PropertyOperand(name, currentValue, value));
	}

	public void setInstallableUnitProfileProperty(IInstallableUnit iu, String name, String value) {
		String currentValue = profile.getInstallableUnitProperty(iu, name);
		if (value == null && currentValue == null)
			return;
		operands.add(new InstallableUnitPropertyOperand(iu, name, currentValue, value));
	}

	public void autoSortOperands(boolean automaticallySort) {
		sortOperands = automaticallySort;
	}

	Operand[] getOrderedPlanForInstall() {
		Operand[] sortedPlan = getOperands();
		if (!sortOperands)
			return sortedPlan;

		//TODO To change for futureState
		new OperandSorter(new CompoundQueryable<IInstallableUnit>(new IQueryable[] {profile}), true).sortBundles(sortedPlan);
		return sortedPlan;
	}

	Operand[] getOrderedPlanForUninstall() {
		Operand[] sortedPlan = getOperands();
		if (!sortOperands)
			return sortedPlan;
		new OperandSorter(new QueryablePlan(false), false).sortBundles(sortedPlan);
		return sortedPlan;
	}
}
