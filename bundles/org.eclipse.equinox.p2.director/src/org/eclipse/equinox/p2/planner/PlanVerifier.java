package org.eclipse.equinox.p2.planner;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.p2.engine.IProvisioningPlan;

public abstract class PlanVerifier {
	/**
	 * Verifies provisioning plan from P2 solver
	 */
	public abstract IStatus verify(IProvisioningPlan plan);

}
