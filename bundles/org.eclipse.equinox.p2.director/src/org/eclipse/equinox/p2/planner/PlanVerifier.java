package org.eclipse.equinox.p2.planner;

import java.util.Map;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.p2.engine.IProvisioningPlan;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;

public abstract class PlanVerifier {
	/**
	 * Verifies provisioning plan from P2 solver
	 */
	public abstract IStatus verify(IProvisioningPlan plan);

	/**
	 * Verifies possible updates
	 */
	public Map<String, IInstallableUnit> verifyUpdates(Map<String, IInstallableUnit> in) {
		return in;
	}

}
