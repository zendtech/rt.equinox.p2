package org.eclipse.equinox.internal.p2.engine;

import java.util.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.query.*;

public class OperandSorter {
	IQueryable<IInstallableUnit> allIUs;
	IInstallableUnit[] sortedIUs;
	boolean doingInstall = true;
	Operand[] operandsToSort = null;

	Map<IInstallableUnit, Operand> iuToIUOperand = new HashMap<IInstallableUnit, Operand>();
	Map<IInstallableUnit, Operand> iuToIUPropertyOperand = new HashMap<IInstallableUnit, Operand>();

	public OperandSorter(IQueryable<IInstallableUnit> allIUs, boolean installOrUninstall) {
		this.allIUs = allIUs;
		doingInstall = installOrUninstall;
	}

	private void indexOperands() {
		for (int i = 0; i < operandsToSort.length; i++) {
			if (operandsToSort[i] instanceof InstallableUnitOperand) {
				InstallableUnitOperand iuo = (InstallableUnitOperand) operandsToSort[i];
				if (iuo.first() != null)
					iuToIUOperand.put(iuo.first(), iuo);
				if (iuo.second() != null)
					iuToIUOperand.put(iuo.second(), iuo);
			}

			if (operandsToSort[i] instanceof InstallableUnitPropertyOperand) {
				InstallableUnitPropertyOperand iupo = (InstallableUnitPropertyOperand) operandsToSort[i];
				iuToIUPropertyOperand.put(iupo.getInstallableUnit(), iupo);
			}
		}
	}

	public void sortOperands(Operand[] operands) {
		operandsToSort = operands;
		sortedIUs = allIUs.query(QueryUtil.ALL_UNITS, null).toUnmodifiableSet().toArray(new IInstallableUnit[0]);
		sortIUs();
		indexOperands();
		computeInstallOrder();
		computeUninstallOrder();
	}

	private ArrayList<Operand> installOrder;
	private ArrayList<Operand> uninstallOrder;

	private void computeInstallOrder() {
		installOrder = new ArrayList<Operand>(); //TODO size
		for (int i = 0; i < sortedIUs.length; i++) {
			IInstallableUnit iInstallableUnit = sortedIUs[i];

			Operand candidate = iuToIUPropertyOperand.get(iInstallableUnit);
			if (candidate != null)
				installOrder.add(iuToIUPropertyOperand.get(iInstallableUnit));

			candidate = iuToIUOperand.get(iInstallableUnit);
			if (candidate != null)
				installOrder.add(iuToIUOperand.get(iInstallableUnit));
		}
	}

	private void computeUninstallOrder() {
		uninstallOrder = new ArrayList<Operand>(); //TODO size
		for (int i = (sortedIUs.length - 1); i != 0; i--) {
			IInstallableUnit iInstallableUnit = sortedIUs[i];

			Operand candidate = iuToIUPropertyOperand.get(iInstallableUnit);
			if (candidate != null)
				uninstallOrder.add(iuToIUPropertyOperand.get(iInstallableUnit));

			candidate = iuToIUOperand.get(iInstallableUnit);
			if (candidate != null)
				uninstallOrder.add(iuToIUOperand.get(iInstallableUnit));
		}
	}

	public ArrayList<Operand> getUninstallOrder() {
		assert (operandsToSort.length == uninstallOrder.size());
		return uninstallOrder;
	}

	public ArrayList<Operand> getInstallOrder() {
		assert (operandsToSort.length == installOrder.size());
		return installOrder;
	}

	private Object[][] sortIUs() {
		List<Object[]> references = new ArrayList<Object[]>(sortedIUs.length);
		for (int i = 0; i < sortedIUs.length; i++) {
			buildReferences(sortedIUs[i], references);
		}
		Object[][] cycles = ComputeNodeOrder.computeNodeOrder(sortedIUs, references.toArray(new Object[references.size()][]));
		if (cycles.length == 0)
			return cycles;
		//		// fix up host/fragment orders (bug 184127)
		//		for (int i = 0; i < cycles.length; i++) {
		//			for (int j = 0; j < cycles[i].length; j++) {
		//				BundleDescription fragment = (BundleDescription) cycles[i][j];
		//				if (fragment.getHost() == null)
		//					continue;
		//				BundleDescription host = (BundleDescription) fragment.getHost().getSupplier();
		//				if (host == null)
		//					continue;
		//				fixFragmentOrder(host, fragment, toSort);
		//			}
		//		}
		return cycles;
	}

	private void buildReferences(IInstallableUnit description, List<Object[]> references) {
		//TODO Do we need to deal with patches in a special way?
		//TODO Do we need to deal with fragmnets?
		//TODO Do we need to deal with 

		buildReferences(description, description.getRequirements(), references);
	}

	private void buildReferences(IInstallableUnit description, Collection<IRequirement> dependencies, List<Object[]> references) {
		for (IRequirement req : dependencies) {
			IQueryResult<IInstallableUnit> matches = allIUs.query(QueryUtil.createMatchQuery(req.getMatches()), null);
			for (Iterator<IInstallableUnit> iterator = matches.iterator(); iterator.hasNext();) {
				addReference(description, iterator.next(), references);
			}
		}
	}

	private void addReference(IInstallableUnit description, IInstallableUnit reference, List<Object[]> references) {
		// build the reference from the description
		if (description == reference || reference == null)
			return;
		references.add(new Object[] {description, reference});
	}
}
