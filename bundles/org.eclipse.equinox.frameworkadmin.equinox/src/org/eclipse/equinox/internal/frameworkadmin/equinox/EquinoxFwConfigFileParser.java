/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.frameworkadmin.equinox;

import java.io.*;
import java.net.*;
import java.util.Enumeration;
import java.util.Properties;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.internal.frameworkadmin.equinox.utils.FileUtils;
import org.eclipse.equinox.internal.frameworkadmin.utils.Utils;
import org.eclipse.equinox.internal.provisional.frameworkadmin.*;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

public class EquinoxFwConfigFileParser {
	private static final String CONFIG_DIR = "@config.dir/"; //$NON-NLS-1$
	private static final String KEY_ECLIPSE_PROV_DATA_AREA = "eclipse.p2.data.area"; //$NON-NLS-1$
	private static final String KEY_ORG_ECLIPSE_EQUINOX_SIMPLECONFIGURATOR_CONFIGURL = "org.eclipse.equinox.simpleconfigurator.configUrl"; //$NON-NLS-1$
	private static final String REFERENCE_SCHEME = "reference:"; //$NON-NLS-1$
	private static final String FILE_PROTOCOL = "file:"; //$NON-NLS-1$
	private static boolean DEBUG = false;

	public EquinoxFwConfigFileParser(BundleContext context) {
		//Empty
	}

	private static StringBuffer toOSGiBundleListForm(BundleInfo bundleInfo, URI location) {
		StringBuffer locationString = new StringBuffer(REFERENCE_SCHEME);
		if (URIUtil.isFileURI(location))
			locationString.append(URIUtil.toUnencodedString(location));
		else if (location.getScheme() == null)
			locationString.append(FILE_PROTOCOL).append(URIUtil.toUnencodedString(location));
		else
			locationString = new StringBuffer(URIUtil.toUnencodedString(location));

		int startLevel = bundleInfo.getStartLevel();
		boolean toBeStarted = bundleInfo.isMarkedAsStarted();

		StringBuffer sb = new StringBuffer();
		sb.append(locationString);
		if (startLevel == BundleInfo.NO_LEVEL && !toBeStarted)
			return sb;
		sb.append('@');
		if (startLevel != BundleInfo.NO_LEVEL)
			sb.append(startLevel);
		if (toBeStarted)
			sb.append(":start"); //$NON-NLS-1$
		return sb;
	}

	private static boolean getMarkedAsStartedFormat(String startInfo) {
		if (startInfo == null)
			return false;
		startInfo = startInfo.trim();
		int colon = startInfo.indexOf(':');
		if (colon > -1) {
			return startInfo.substring(colon + 1).equals("start"); //$NON-NLS-1$
		}
		return startInfo.equals("start"); //$NON-NLS-1$
	}

	private static int getStartLevel(String startInfo) {
		if (startInfo == null)
			return BundleInfo.NO_LEVEL;
		startInfo = startInfo.trim();
		int colon = startInfo.indexOf(":"); //$NON-NLS-1$
		if (colon > 0) {
			try {
				return Integer.parseInt(startInfo.substring(0, colon));
			} catch (NumberFormatException e) {
				return BundleInfo.NO_LEVEL;
			}
		}
		return BundleInfo.NO_LEVEL;
	}

	static boolean isFwDependent(String key) {
		// TODO This algorithm is temporal. 
		if (key.startsWith(EquinoxConstants.PROP_EQUINOX_DEPENDENT_PREFIX))
			return true;
		return false;
	}

	private void readBundlesList(Manipulator manipulator, URI osgiInstallArea, String value) throws NumberFormatException {
		ConfigData configData = manipulator.getConfigData();
		if (value != null) {
			String[] bInfoStrings = Utils.getTokens(value, ","); //$NON-NLS-1$
			for (int i = 0; i < bInfoStrings.length; i++) {
				String entry = bInfoStrings[i].trim();
				entry = FileUtils.removeEquinoxSpecificProtocols(entry);

				int indexStartInfo = entry.indexOf('@');
				String location = (indexStartInfo == -1) ? entry : entry.substring(0, indexStartInfo);
				URI realLocation = null;
				try {
					if (manipulator.getLauncherData().getFwJar() != null)
						realLocation = URIUtil.makeAbsolute(URIUtil.fromString(location), manipulator.getLauncherData().getFwJar().getParentFile().toURI());
				} catch (URISyntaxException e) {
					Log.log(LogService.LOG_ERROR, "Can't make absolute...");
					continue;
				}
				String slAndFlag = (indexStartInfo > -1) ? entry.substring(indexStartInfo + 1) : null;

				boolean markedAsStarted = getMarkedAsStartedFormat(slAndFlag);
				int startLevel = getStartLevel(slAndFlag);

				if (realLocation != null) {
					configData.addBundle(new BundleInfo(realLocation, startLevel, markedAsStarted));
					return;
				}
				if (location != null && location.startsWith(FILE_PROTOCOL))
					try {
						configData.addBundle(new BundleInfo(URIUtil.fromString(location), startLevel, markedAsStarted));
						return;
					} catch (URISyntaxException e) {
						//Ignore
					}

				//Fallback case, we use the location as a string
				configData.addBundle(new BundleInfo(location, null, null, startLevel, markedAsStarted));
			}
		}
	}

	private void writeBundlesList(File fwJar, Properties props, URI base, BundleInfo[] bundles) {
		//framework jar does not get stored on the bundle list, figure out who that is.

		StringBuffer osgiBundlesList = new StringBuffer();
		for (int j = 0; j < bundles.length; j++) {
			if (fwJar != null) {
				if (URIUtil.sameURI(fwJar.toURI(), bundles[j].getLocation()))
					continue;
			} else if (EquinoxConstants.FW_SYMBOLIC_NAME.equals(bundles[j].getSymbolicName()))
				continue;

			URI location = fwJar != null ? URIUtil.makeRelative(bundles[j].getLocation(), fwJar.getParentFile().toURI()) : bundles[j].getLocation();
			osgiBundlesList.append(toOSGiBundleListForm(bundles[j], location));
		}
		props.setProperty(EquinoxConstants.PROP_BUNDLES, osgiBundlesList.toString());
	}

	/**
	 * inputFile must be not a directory but a file.
	 * 
	 * @param manipulator
	 * @param inputFile
	 * @throws IOException
	 */
	public void readFwConfig(Manipulator manipulator, File inputFile) throws IOException, URISyntaxException {
		if (inputFile.isDirectory())
			throw new IllegalArgumentException(NLS.bind(Messages.exception_inputFileIsDirectory, inputFile));

		//Initialize data structures
		ConfigData configData = manipulator.getConfigData();
		LauncherData launcherData = manipulator.getLauncherData();
		configData.initialize();
		configData.setBundles(null);

		String launcherName = null;
		String launcherPath = null;

		// load configuration properties
		Properties props = loadProperties(inputFile);

		// load shared configuration properties
		Properties sharedConfigProperties = getSharedConfiguration(props.getProperty(EquinoxConstants.PROP_SHARED_CONFIGURATION_AREA), ParserUtils.getOSGiInstallArea(manipulator.getLauncherData()));
		if (sharedConfigProperties != null) {
			sharedConfigProperties.putAll(props);
			props = sharedConfigProperties;
		}

		//Start figuring out stuffs 
		URI rootURL = launcherData.getLauncher() != null ? launcherData.getLauncher().getParentFile().toURI() : null;

		readFwJarLocation(configData, launcherData, props);
		URI configArea = inputFile.getParentFile().toURI();
		readLauncherPath(props, rootURL);
		readp2DataArea(props, configArea);
		readSimpleConfiguratorURL(props, configArea);
		readBundlesList(manipulator, ParserUtils.getOSGiInstallArea(launcherData).toURI(), props.getProperty(EquinoxConstants.PROP_BUNDLES));
		readInitialStartLeve(configData, props);
		readDefaultStartLevel(configData, props);
		//		if (key.equals(EquinoxConstants.PROP_LAUNCHER_NAME))
		//			if (launcherData.getLauncher() == null)
		//				launcherName = value;
		//		if (key.equals(EquinoxConstants.PROP_LAUNCHER_PATH))
		//			if (launcherData.getLauncher() == null)
		//				launcherPath = value;
		String[] KNOWN_PROPERTIES = {EquinoxConstants.PROP_BUNDLES, EquinoxConstants.PROP_INITIAL_STARTLEVEL, EquinoxConstants.PROP_BUNDLES_STARTLEVEL};
		top: for (Enumeration enumeration = props.keys(); enumeration.hasMoreElements();) {
			String key = (String) enumeration.nextElement();
			for (int i = 0; i < KNOWN_PROPERTIES.length; i++) {
				if (KNOWN_PROPERTIES[i].equals(key))
					continue top;
			}
			String value = props.getProperty(key);
			if (isFwDependent(key))
				configData.setFwDependentProp(key, value);
			else
				configData.setFwIndependentProp(key, value);
		}
		if (launcherName != null && launcherPath != null) {
			launcherData.setLauncher(new File(launcherPath, launcherName + EquinoxConstants.EXE_EXTENSION));
		}
		Log.log(LogService.LOG_INFO, NLS.bind(Messages.log_configFile, inputFile.getAbsolutePath()));
	}

	private void readDefaultStartLevel(ConfigData configData, Properties props) {
		if (props.getProperty(EquinoxConstants.PROP_BUNDLES_STARTLEVEL) != null)
			configData.setInitialBundleStartLevel(Integer.parseInt(props.getProperty(EquinoxConstants.PROP_BUNDLES_STARTLEVEL)));
	}

	private void writeDefaultStartLevel(ConfigData configData, Properties props) {
		if (configData.getInitialBundleStartLevel() != BundleInfo.NO_LEVEL)
			props.setProperty(EquinoxConstants.PROP_BUNDLES_STARTLEVEL, Integer.toString(configData.getInitialBundleStartLevel()));
	}

	private void readInitialStartLeve(ConfigData configData, Properties props) {
		if (props.getProperty(EquinoxConstants.PROP_INITIAL_STARTLEVEL) != null)
			configData.setBeginningFwStartLevel(Integer.parseInt(props.getProperty(EquinoxConstants.PROP_INITIAL_STARTLEVEL)));
	}

	private void writeInitialStartLevel(ConfigData configData, Properties props) {
		if (configData.getBeginingFwStartLevel() != BundleInfo.NO_LEVEL)
			props.setProperty(EquinoxConstants.PROP_INITIAL_STARTLEVEL, Integer.toString(configData.getBeginingFwStartLevel()));
	}

	private File readFwJarLocation(ConfigData configData, LauncherData launcherData, Properties props) throws URISyntaxException {
		File fwJar = null;
		if (props.getProperty(EquinoxConstants.PROP_OSGI_FW) != null) {
			URI absoluteFwJar = null;
			absoluteFwJar = URIUtil.makeAbsolute(new URI(props.getProperty(EquinoxConstants.PROP_OSGI_FW)), ParserUtils.getOSGiInstallArea(launcherData).toURI());

			props.setProperty(EquinoxConstants.PROP_OSGI_FW, absoluteFwJar.toString());
			String fwJarString = props.getProperty(EquinoxConstants.PROP_OSGI_FW);
			if (fwJarString != null) {
				fwJar = URIUtil.toFile(absoluteFwJar);
				if (fwJar == null)
					throw new IllegalStateException("Can't determinate the osgi.framework location");
				//Here we overwrite the value read from eclipse.ini, because the value of osgi.framework always takes precedence.
				launcherData.setFwJar(fwJar);
			} else {
				throw new IllegalStateException("Can't determinate the osgi.framework location");
			}
		}
		if (launcherData.getFwJar() != null)
			configData.addBundle(new BundleInfo(launcherData.getFwJar().toURI()));
		return launcherData.getFwJar();
	}

	private void writeFwJarLocation(ConfigData configData, LauncherData launcherData, Properties props) {
		if (launcherData.getFwJar() == null)
			return;
		props.setProperty(EquinoxConstants.PROP_OSGI_FW, URIUtil.toUnencodedString(URIUtil.makeRelative(launcherData.getFwJar().toURI(), ParserUtils.getOSGiInstallArea(launcherData).toURI())));
	}

	private static Properties loadProperties(File inputFile) throws FileNotFoundException, IOException {
		Properties props = new Properties();
		InputStream is = null;
		try {
			is = new FileInputStream(inputFile);
			props.load(is);
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				Log.log(LogService.LOG_WARNING, NLS.bind(Messages.log_failed_reading_properties, inputFile));
			}
			is = null;
		}
		return props;
	}

	private File findSharedConfigIniFile(URL rootURL, String sharedConfigurationArea) {
		URL sharedConfigurationURL = null;
		try {
			sharedConfigurationURL = new URL(sharedConfigurationArea);
		} catch (MalformedURLException e) {
			// unexpected since this was written by the framework
			Log.log(LogService.LOG_WARNING, NLS.bind(Messages.log_shared_config_url, sharedConfigurationArea));
			return null;
		}

		// check for a relative URL
		if (!sharedConfigurationURL.getPath().startsWith("/")) { //$NON-NLS-1$
			if (!rootURL.getProtocol().equals(sharedConfigurationURL.getProtocol())) {
				Log.log(LogService.LOG_WARNING, NLS.bind(Messages.log_shared_config_relative_url, rootURL.toExternalForm(), sharedConfigurationArea));
				return null;
			}
			try {
				sharedConfigurationURL = new URL(rootURL, sharedConfigurationArea);
			} catch (MalformedURLException e) {
				// unexpected since this was written by the framework
				Log.log(LogService.LOG_WARNING, NLS.bind(Messages.log_shared_config_relative_url, rootURL.toExternalForm(), sharedConfigurationArea));
				return null;
			}
		}
		File sharedConfigurationFolder = new File(sharedConfigurationURL.getPath());
		if (!sharedConfigurationFolder.isDirectory()) {
			Log.log(LogService.LOG_WARNING, NLS.bind(Messages.log_shared_config_file_missing, sharedConfigurationFolder));
			return null;
		}

		File sharedConfigIni = new File(sharedConfigurationFolder, EquinoxConstants.CONFIG_INI);
		if (!sharedConfigIni.exists()) {
			Log.log(LogService.LOG_WARNING, NLS.bind(Messages.log_shared_config_file_missing, sharedConfigIni));
			return null;
		}

		return sharedConfigIni;
	}

	private void readp2DataArea(Properties props, URI configArea) throws URISyntaxException {
		if (props.getProperty(KEY_ECLIPSE_PROV_DATA_AREA) != null) {
			String url = props.getProperty(KEY_ECLIPSE_PROV_DATA_AREA);
			if (url != null) {
				if (url.startsWith(CONFIG_DIR))
					url = "file:" + url.substring(CONFIG_DIR.length()); //$NON-NLS-1$
				props.setProperty(KEY_ECLIPSE_PROV_DATA_AREA, URIUtil.makeAbsolute(URIUtil.fromString(url), configArea).toString());
			}
		}
	}

	private void writep2DataArea(ConfigData configData, Properties props, URI configArea) throws URISyntaxException {
		String dataArea = getFwProperty(configData, KEY_ECLIPSE_PROV_DATA_AREA);
		if (dataArea != null) {
			String result = URIUtil.toUnencodedString(URIUtil.makeRelative(URIUtil.fromString(dataArea), configArea));
			//We only relativize up to the level where the p2 and config folder are siblings (e.g. foo/p2 and foo/config)
			//FIXME NEed to review if this logic is correct
			if (result.startsWith("../..")) //$NON-NLS-1$
				result = dataArea;
			else if (!result.equals(dataArea))
				result = CONFIG_DIR + result.substring(5);
			props.setProperty(KEY_ECLIPSE_PROV_DATA_AREA, result);
		}
	}

	private void readLauncherPath(Properties props, URI root) throws URISyntaxException {
		if (props.getProperty(EquinoxConstants.PROP_LAUNCHER_PATH) != null)
			props.setProperty(EquinoxConstants.PROP_LAUNCHER_PATH, URIUtil.makeAbsolute(URIUtil.fromString(props.getProperty(EquinoxConstants.PROP_LAUNCHER_PATH)), root).toString());
	}

	private void writeLauncherPath(ConfigData configData, Properties props, URI root) throws URISyntaxException {
		String value = getFwProperty(configData, EquinoxConstants.PROP_LAUNCHER_PATH);
		if (value != null)
			props.setProperty(EquinoxConstants.PROP_LAUNCHER_PATH, URIUtil.toUnencodedString(URIUtil.makeRelative(URIUtil.fromString(value), root)));
	}

	private void readSimpleConfiguratorURL(Properties props, URI configArea) throws URISyntaxException {
		if (props.getProperty(KEY_ORG_ECLIPSE_EQUINOX_SIMPLECONFIGURATOR_CONFIGURL) != null)
			props.setProperty(KEY_ORG_ECLIPSE_EQUINOX_SIMPLECONFIGURATOR_CONFIGURL, URIUtil.makeAbsolute(URIUtil.fromString(props.getProperty(KEY_ORG_ECLIPSE_EQUINOX_SIMPLECONFIGURATOR_CONFIGURL)), configArea).toString());
	}

	private void writeSimpleConfiguratorURL(ConfigData configData, Properties props, URI configArea) throws URISyntaxException {
		//FIXME How would someone set such a value.....
		String value = getFwProperty(configData, KEY_ORG_ECLIPSE_EQUINOX_SIMPLECONFIGURATOR_CONFIGURL);
		if (value != null)
			props.setProperty(KEY_ORG_ECLIPSE_EQUINOX_SIMPLECONFIGURATOR_CONFIGURL, URIUtil.toUnencodedString(URIUtil.makeRelative(URIUtil.fromString(value), configArea)));
	}

	private String getFwProperty(ConfigData data, String key) {
		if (isFwDependent(key))
			return data.getFwDependentProp(key);
		return data.getFwIndependentProp(key);
	}

	public void saveFwConfig(BundleInfo[] bInfos, Manipulator manipulator, boolean backup, boolean relative) throws IOException {//{
		ConfigData configData = manipulator.getConfigData();
		LauncherData launcherData = manipulator.getLauncherData();
		//Get the OSGi jar from the bundle.info
		File fwJar = EquinoxBundlesState.getSystemBundleFromBundleInfos(configData);
		launcherData.setFwJar(fwJar);

		File outputFile = launcherData.getFwConfigLocation();
		if (outputFile.exists()) {
			if (outputFile.isFile()) {
				if (!outputFile.getName().equals(EquinoxConstants.CONFIG_INI))
					throw new IllegalStateException(NLS.bind(Messages.exception_fwConfigLocationName, outputFile.getAbsolutePath(), EquinoxConstants.CONFIG_INI));
			} else { // Directory
				outputFile = new File(outputFile, EquinoxConstants.CONFIG_INI);
			}
		} else {
			if (!outputFile.getName().equals(EquinoxConstants.CONFIG_INI)) {
				if (!outputFile.mkdir())
					throw new IOException(NLS.bind(Messages.exception_failedToCreateDir, outputFile));
				outputFile = new File(outputFile, EquinoxConstants.CONFIG_INI);
			}
		}
		String header = "This configuration file was written by: " + this.getClass().getName(); //$NON-NLS-1$

		Properties configProps = new Properties();
		writeFwJarLocation(configData, launcherData, configProps);
		try {
			writeLauncherPath(configData, configProps, null);
			URI configArea = manipulator.getLauncherData().getFwConfigLocation().toURI();
			writep2DataArea(configData, configProps, configArea);
			writeSimpleConfiguratorURL(configData, configProps, configArea);
			writeBundlesList(launcherData.getFwJar(), configProps, ParserUtils.getOSGiInstallArea(launcherData).toURI(), bInfos);
			writeInitialStartLevel(configData, configProps);
			writeDefaultStartLevel(configData, configProps);
		} catch (URISyntaxException e) {
			throw new FrameworkAdminRuntimeException(e, "saving config.ini");
		}
		//		final File launcher = launcherData.getLauncher();
		//		if (launcher != null) {
		//			String launcherName = launcher.getName();
		//			if (launcherName.endsWith(EquinoxConstants.EXE_EXTENSION)) {
		//				props.setProperty(EquinoxConstants.PROP_LAUNCHER_NAME, launcherName.substring(0, launcherName.lastIndexOf(EquinoxConstants.EXE_EXTENSION)));
		//				props.setProperty(EquinoxConstants.PROP_LAUNCHER_PATH, launcher.getParentFile().getAbsolutePath());
		//			}
		//		}
		//		if (props.getProperty(EquinoxConstants.PROP_LAUNCHER_PATH) != null)
		//			props.setProperty(EquinoxConstants.PROP_LAUNCHER_PATH, URIUtil.makeRelative(URIUtil.fromString(props.getProperty(EquinoxConstants.PROP_LAUNCHER_PATH)), rootURI).toString());

		//TODO The following merging operations are suspicious.
		configProps = Utils.appendProperties(configProps, configData.getFwIndependentProps());

		configProps = Utils.appendProperties(configProps, configData.getFwDependentProps());

		//Deal with the fw jar and ensure it is not set. 
		//TODO This can't be done before because of the previous calls to appendProperties

		if (configProps == null || configProps.size() == 0) {
			Log.log(LogService.LOG_WARNING, this, "saveFwConfig() ", Messages.log_configProps); //$NON-NLS-1$
			return;
		}
		Utils.createParentDir(outputFile);

		if (DEBUG)
			Utils.printoutProperties(System.out, "configProps", configProps); //$NON-NLS-1$

		if (backup)
			if (outputFile.exists()) {
				File dest = Utils.getSimpleDataFormattedFile(outputFile);
				if (!outputFile.renameTo(dest))
					throw new IOException(NLS.bind(Messages.exception_failedToRename, outputFile, dest));
				Log.log(LogService.LOG_INFO, this, "saveFwConfig()", NLS.bind(Messages.log_renameSuccessful, outputFile, dest)); //$NON-NLS-1$
			}

		FileOutputStream out = null;
		try {
			out = new FileOutputStream(outputFile);
			//			configProps = makeRelative(configProps, launcherData.getLauncher().getParentFile().toURI(), fwJar, outputFile.getParentFile(), getOSGiInstallArea(manipulator.getLauncherData()));
			filterPropertiesFromSharedArea(configProps, launcherData);
			configProps.store(out, header);
			Log.log(LogService.LOG_INFO, NLS.bind(Messages.log_fwConfigSave, outputFile));
		} finally {
			try {
				out.flush();
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			out = null;
		}
	}

	private void filterPropertiesFromSharedArea(Properties configProps, LauncherData launcherData) {
		//Remove from the config file that we are about to write the properties that are unchanged compared to what is in the base 
		Properties sharedConfigProperties = getSharedConfiguration(configProps.getProperty(EquinoxConstants.PROP_SHARED_CONFIGURATION_AREA), ParserUtils.getOSGiInstallArea(launcherData));
		if (sharedConfigProperties == null)
			return;
		Enumeration keys = configProps.propertyNames();
		while (keys.hasMoreElements()) {
			String key = (String) keys.nextElement();
			String sharedValue = sharedConfigProperties.getProperty(key);
			if (sharedValue == null)
				continue;
			if (equalsIgnoringSeparators(sharedValue, configProps.getProperty(key)))
				configProps.remove(key);
		}
	}

	private boolean equalsIgnoringSeparators(String s1, String s2) {
		if (s1 == null && s2 == null)
			return true;
		if (s1 == null || s2 == null)
			return false;
		StringBuffer sb1 = new StringBuffer(s1);
		StringBuffer sb2 = new StringBuffer(s2);
		canonicalizePathsForComparison(sb1);
		canonicalizePathsForComparison(sb2);
		return sb1.toString().equals(sb2.toString());
	}

	private void canonicalizePathsForComparison(StringBuffer s) {
		final String[] tokens = new String[] {"\\\\", "\\", "//", "/"}; //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$//$NON-NLS-4$
		for (int t = 0; t < tokens.length; t++) {
			int idx = s.length();
			for (int i = s.length(); i != 0 && idx != -1; i--) {
				idx = s.toString().lastIndexOf(tokens[t], idx);
				if (idx != -1)
					s.replace(idx, idx + tokens[t].length(), "^"); //$NON-NLS-1$
			}
		}
	}

	private Properties getSharedConfiguration(String sharedConfigurationArea, File baseFile) {
		if (sharedConfigurationArea == null)
			return null;
		File sharedConfigIni;
		try {
			sharedConfigIni = findSharedConfigIniFile(baseFile.toURL(), sharedConfigurationArea);
		} catch (MalformedURLException e) {
			return null;
		}
		if (sharedConfigIni != null && sharedConfigIni.exists())
			try {
				return loadProperties(sharedConfigIni);
			} catch (FileNotFoundException e) {
				return null;
			} catch (IOException e) {
				return null;
			}
		return null;
	}
}
