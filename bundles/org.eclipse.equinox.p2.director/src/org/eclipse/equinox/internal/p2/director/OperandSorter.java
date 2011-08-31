package org.eclipse.equinox.internal.p2.director;

import java.util.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.query.*;

public class OperandSorter {
	IQueryable<IInstallableUnit> allIUs;
	boolean doingInstall = true;

	public OperandSorter(IQueryable<IInstallableUnit> allIUs, boolean installOrUninstall) {
		this.allIUs = allIUs;
		doingInstall = installOrUninstall;
	}

	public Object[][] sortBundles(IInstallableUnit[] toSort) {
		List<Object[]> references = new ArrayList<Object[]>(toSort.length);
		for (int i = 0; i < toSort.length; i++) {
			buildReferences(toSort[i], references);
		}
		Object[][] cycles = ComputeNodeOrder.computeNodeOrder(toSort, references.toArray(new Object[references.size()][]));
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

	public void buildReferences(IInstallableUnit description, List<Object[]> references) {
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
