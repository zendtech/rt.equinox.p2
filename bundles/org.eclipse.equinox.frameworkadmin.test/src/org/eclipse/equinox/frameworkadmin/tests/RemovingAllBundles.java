/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.frameworkadmin.tests;

import java.net.URISyntaxException;

import java.net.URI;

import java.io.File;
import java.io.IOException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.equinox.internal.provisional.frameworkadmin.*;
import org.osgi.framework.BundleException;

public class RemovingAllBundles extends AbstractFwkAdminTest {

	public RemovingAllBundles(String name) {
		super(name);
	}

	public void testConfigFiles() throws IllegalStateException, FrameworkAdminRuntimeException, IOException, BundleException, URISyntaxException {
		startSimpleConfiguratormManipulator();
		FrameworkAdmin fwkAdmin = getEquinoxFrameworkAdmin();
		Manipulator manipulator = fwkAdmin.getManipulator();

		File installFolder = Activator.getContext().getDataFile(RemovingAllBundles.class.getName());
		File configurationFolder = new File(installFolder, "configuration");
		String launcherName = "foo";

		LauncherData launcherData = manipulator.getLauncherData();
		launcherData.setFwConfigLocation(configurationFolder);
		launcherData.setLauncher(new File(installFolder, launcherName));
		try {
			manipulator.load();
		} catch (IllegalStateException e) {
			//TODO We ignore the framework JAR location not set exception
		}

		BundleInfo osgiBi = new BundleInfo("org.eclipse.osgi", "3.3.1", new URI(FileLocator.resolve(Activator.getContext().getBundle().getEntry("dataFile/org.eclipse.osgi.jar")).toExternalForm()), 0, true);
		BundleInfo configuratorBi = new BundleInfo("org.eclipse.equinox.simpleconfigurator", "1.0.0", new URI(FileLocator.resolve(Activator.getContext().getBundle().getEntry("dataFile/org.eclipse.equinox.simpleconfigurator.jar")).toExternalForm()), 1, true);

		manipulator.getConfigData().addBundle(osgiBi);
		manipulator.getConfigData().addBundle(configuratorBi);

		manipulator.save(false);

		File fooINI = new File(installFolder, "foo.ini");
		assertEquals(fooINI.exists(), true);

		Manipulator m2 = fwkAdmin.getManipulator();

		LauncherData launcherData2 = m2.getLauncherData();
		launcherData2.setFwConfigLocation(configurationFolder);
		launcherData2.setLauncher(new File(installFolder, launcherName));

		try {
			m2.load();
		} catch (IllegalStateException e) {
			//TODO We ignore the framework JAR location not set exception
		}

		BundleInfo[] infos = m2.getConfigData().getBundles();
		for (int i = 0; i < infos.length; i++) {
			m2.getConfigData().removeBundle(infos[i]);
		}
		m2.save(false);

		assertEquals(new File(configurationFolder + "/org.eclipse.equinox.simpleconfigurator", "bundles.info").exists(), false);
	}
}
