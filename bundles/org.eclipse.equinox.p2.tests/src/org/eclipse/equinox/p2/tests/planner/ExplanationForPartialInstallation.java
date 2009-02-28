package org.eclipse.equinox.p2.tests.planner;

import org.eclipse.equinox.internal.provisional.p2.core.Version;
import org.eclipse.equinox.internal.provisional.p2.core.VersionRange;
import org.eclipse.equinox.internal.provisional.p2.director.*;
import org.eclipse.equinox.internal.provisional.p2.engine.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class ExplanationForPartialInstallation extends AbstractProvisioningTest {
	private IProfile profile;
	private IPlanner planner;
	private IEngine engine;
	private IInstallableUnit sdk;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		sdk = createIU("SDK", Version.fromOSGiVersion(new org.osgi.framework.Version("1.0.0")), createRequiredCapabilities(IInstallableUnit.NAMESPACE_IU_ID, "SDKPart", new VersionRange("[1.0.0, 1.0.0]"), null));
		IInstallableUnit sdkPart = createIU("SDKPart", Version.fromOSGiVersion(new org.osgi.framework.Version("1.0.0")), true);
		IInstallableUnit sdkPart2 = createIU("SDKPart", Version.fromOSGiVersion(new org.osgi.framework.Version("2.0.0")), true);

		createTestMetdataRepository(new IInstallableUnit[] {sdk, sdkPart, sdkPart2});

		profile = createProfile("TestProfile." + getName());
		planner = createPlanner();
		engine = createEngine();

		ProfileChangeRequest pcr = new ProfileChangeRequest(profile);
		pcr.addInstallableUnits(new IInstallableUnit[] {sdk});
		engine.perform(profile, new DefaultPhaseSet(), planner.getProvisioningPlan(pcr, null, null).getOperands(), null, null);

	}

	public void testPartialProblemSingleton() {
		//CDT will have a singleton conflict with SDK
		//EMF will be good
		IInstallableUnit cdt = createIU("CDT", Version.fromOSGiVersion(new org.osgi.framework.Version("1.0.0")), createRequiredCapabilities(IInstallableUnit.NAMESPACE_IU_ID, "SDKPart", new VersionRange("[2.0.0, 2.0.0]"), null));

		IInstallableUnit emf = createIU("EMF", Version.fromOSGiVersion(new org.osgi.framework.Version("1.0.0")), true);

		createTestMetdataRepository(new IInstallableUnit[] {cdt, emf});
		ProfileChangeRequest pcr = new ProfileChangeRequest(profile);
		pcr.addInstallableUnits(new IInstallableUnit[] {cdt, emf});
		ProvisioningPlan plan = planner.getProvisioningPlan(pcr, null, null);
		System.out.println(plan.getRequestStatus().getExplanations());
		assertTrue(plan.getRequestStatus().getConflictsWithInstalledRoots().contains(cdt));
		assertFalse(plan.getRequestStatus().getConflictsWithInstalledRoots().contains(emf));
		assertFalse(plan.getRequestStatus().getConflictsWithInstalledRoots().contains(sdk));

		//		assertTrue(plan.getRequestStatus(cdt).getSeverity() == IStatus.ERROR);
		//		assertTrue(plan.getRequestStatus(cdt).getConflictsWithAnyRoots().contains(sdk));
		//		assertTrue(plan.getRequestStatus(cdt).getConflictsWithInstalledRoots().contains(sdk));
		//
		//		assertTrue(plan.getRequestStatus(emf).getSeverity() != IStatus.ERROR);
		//		assertEquals(0, plan.getRequestStatus(emf).getConflictsWithAnyRoots().size());
		//		assertEquals(0, plan.getRequestStatus(emf).getConflictsWithInstalledRoots().size());
		//
		//		assertNull(plan.getRequestStatus(sdk));
	}

	public void testPartialProblemRequirement() {
		//CDT will be missing a requirement
		//EMF will be good
		IInstallableUnit cdt = createIU("CDT", Version.fromOSGiVersion(new org.osgi.framework.Version("1.0.0")), createRequiredCapabilities(IInstallableUnit.NAMESPACE_IU_ID, "MissingPart", new VersionRange("[2.0.0, 2.0.0]"), null));

		IInstallableUnit emf = createIU("EMF", Version.fromOSGiVersion(new org.osgi.framework.Version("1.0.0")), true);

		createTestMetdataRepository(new IInstallableUnit[] {cdt, emf});
		ProfileChangeRequest pcr = new ProfileChangeRequest(profile);
		pcr.addInstallableUnits(new IInstallableUnit[] {cdt, emf});
		ProvisioningPlan plan = planner.getProvisioningPlan(pcr, null, null);
		System.out.println(plan.getRequestStatus().getExplanations());
		assertTrue(plan.getRequestStatus().getConflictsWithInstalledRoots().contains(cdt));
		assertFalse(plan.getRequestStatus().getConflictsWithInstalledRoots().contains(emf));
		assertFalse(plan.getRequestStatus().getConflictsWithInstalledRoots().contains(sdk));

		//		assertTrue(plan.getRequestStatus(cdt).getSeverity() == IStatus.ERROR);
		//		assertEquals(0, plan.getRequestStatus(cdt).getConflictsWithAnyRoots().size());
		//		assertEquals(0, plan.getRequestStatus(cdt).getConflictsWithInstalledRoots().size());
		//
		//		assertTrue(plan.getRequestStatus(emf).getSeverity() != IStatus.ERROR);
		//		assertEquals(0, plan.getRequestStatus(emf).getConflictsWithAnyRoots().size());
		//		assertEquals(0, plan.getRequestStatus(emf).getConflictsWithInstalledRoots().size());
	}
}