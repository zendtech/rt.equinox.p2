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

package org.eclipse.equinox.internal.p2.ui.viewers;

import java.text.NumberFormat;
import java.util.HashMap;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.*;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.model.ProvElement;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvUI;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvUIImages;
import org.eclipse.equinox.internal.provisional.p2.ui.model.IUElement;
import org.eclipse.equinox.internal.provisional.p2.ui.viewers.IUColumnConfig;
import org.eclipse.jface.viewers.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.FilteredTree;

/**
 * Label provider for showing IU's in a table.  Clients can configure
 * what is shown in each column.
 * 
 * @since 3.4
 */
public class IUDetailsLabelProvider extends ColumnLabelProvider implements ITableLabelProvider, IFontProvider {
	final static int PRIMARY_COLUMN = 0;
	final static String BLANK = ""; //$NON-NLS-1$
	private String toolTipProperty = null;
	private FilteredTree filteredTree;
	private boolean useBoldFont = false;

	private IUColumnConfig[] columnConfig;
	Shell shell;
	HashMap jobs = new HashMap();

	public IUDetailsLabelProvider() {
		this(null, null, null);
	}

	public IUDetailsLabelProvider(FilteredTree filteredTree, IUColumnConfig[] columnConfig, Shell shell) {
		this.filteredTree = filteredTree;
		if (columnConfig == null)
			this.columnConfig = ProvUI.getIUColumnConfig();
		else
			this.columnConfig = columnConfig;
		this.shell = shell;
	}

	public String getText(Object obj) {
		return getColumnText(obj, 0);
	}

	public Image getImage(Object obj) {
		return getColumnImage(obj, columnConfig[0].columnField);
	}

	public String getColumnText(Object element, int columnIndex) {
		int columnContent = IUColumnConfig.COLUMN_ID;
		if (columnIndex < columnConfig.length) {
			columnContent = columnConfig[columnIndex].columnField;
		}

		IInstallableUnit iu = (IInstallableUnit) ProvUI.getAdapter(element, IInstallableUnit.class);
		if (iu == null) {
			if (columnIndex == 0) {
				if (element instanceof ProvElement)
					return ((ProvElement) element).getLabel(element);
				return element.toString();
			}
			return BLANK;
		}

		switch (columnContent) {
			case IUColumnConfig.COLUMN_ID :
				return iu.getId();
			case IUColumnConfig.COLUMN_NAME :
				String name = iu.getProperty(IInstallableUnit.PROP_NAME);
				if (name != null)
					return name;
				return BLANK;
			case IUColumnConfig.COLUMN_VERSION :
				// If it's an element, determine if version should be shown
				if (element instanceof IUElement) {
					if (((IUElement) element).shouldShowVersion())
						return iu.getVersion().toString();
					return BLANK;
				}
				// It's a raw IU, return the version
				return iu.getVersion().toString();

			case IUColumnConfig.COLUMN_SIZE :
				if (element instanceof IUElement && ((IUElement) element).shouldShowSize())
					return getIUSize((IUElement) element);
				return BLANK;
		}
		return BLANK;
	}

	public Image getColumnImage(Object element, int index) {
		if (index == PRIMARY_COLUMN) {
			if (element instanceof ProvElement)
				return ((ProvElement) element).getImage(element);
			if (ProvUI.getAdapter(element, IInstallableUnit.class) != null)
				return ProvUIImages.getImage(ProvUIImages.IMG_IU);
		}
		return null;
	}

	private String getIUSize(final IUElement element) {
		long size = element.getSize();
		// If size is already known, or we already tried
		// to get it, don't try again
		if (size != IUElement.SIZE_UNKNOWN)
			return getFormattedSize(size);
		if (!jobs.containsKey(element)) {
			Job resolveJob = new Job(element.getIU().getId()) {

				protected IStatus run(IProgressMonitor monitor) {
					if (shell == null || shell.isDisposed())
						return Status.OK_STATUS;

					element.computeSize();

					// If we still could not compute size, give up
					if (element.getSize() == IUElement.SIZE_UNKNOWN)
						return Status.OK_STATUS;

					if (shell == null || shell.isDisposed())
						return Status.OK_STATUS;
					shell.getDisplay().asyncExec(new Runnable() {

						public void run() {
							fireLabelProviderChanged(new LabelProviderChangedEvent(IUDetailsLabelProvider.this, element));
						}
					});

					return Status.OK_STATUS;
				}
			};
			jobs.put(element, resolveJob);
			resolveJob.setSystem(true);
			resolveJob.addJobChangeListener(new JobChangeAdapter() {
				public void done(IJobChangeEvent event) {
					jobs.remove(element);
				}
			});
			resolveJob.schedule();
		}
		return ProvUIMessages.IUDetailsLabelProvider_ComputingSize;
	}

	private String getFormattedSize(long size) {
		if (size == IUElement.SIZE_UNKNOWN || size == IUElement.SIZE_UNAVAILABLE)
			return ProvUIMessages.IUDetailsLabelProvider_Unknown;
		if (size > 1000L) {
			long kb = size / 1000L;
			return NLS.bind(ProvUIMessages.IUDetailsLabelProvider_KB, NumberFormat.getInstance().format(new Long(kb)));
		}
		return NLS.bind(ProvUIMessages.IUDetailsLabelProvider_Bytes, NumberFormat.getInstance().format(new Long(size)));
	}

	public void setToolTipProperty(String propertyName) {
		toolTipProperty = propertyName;
	}

	public void setUseBoldFontForFilteredItems(boolean useBoldFont) {
		this.useBoldFont = useBoldFont;
	}

	public String getToolTipText(Object element) {
		IInstallableUnit iu = (IInstallableUnit) ProvUI.getAdapter(element, IInstallableUnit.class);
		if (iu == null || toolTipProperty == null)
			return null;
		return iu.getProperty(toolTipProperty);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IFontProvider#getFont(java.lang.Object)
	 */
	public Font getFont(Object element) {
		if (filteredTree != null && useBoldFont) {
			return FilteredTree.getBoldFont(element, filteredTree, filteredTree.getPatternFilter());
		}
		return null;
	}
}
