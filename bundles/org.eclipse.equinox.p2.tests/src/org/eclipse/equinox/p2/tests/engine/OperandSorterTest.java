package org.eclipse.equinox.p2.tests.engine;

import org.eclipse.equinox.internal.p2.engine.*;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class OperandSorterTest extends AbstractProvisioningTest {

	public void testSort() {
		IRequirement[] req = new IRequirement[1];
		req[0] = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "B", VersionRange.emptyRange, null, false, false, true);
		IInstallableUnit a = createIU("A", req);
		IInstallableUnit b = createIU("B");
		IInstallableUnit c = createIU("C");

		{
			InstallableUnitOperand op = new InstallableUnitOperand(createResolvedIU(a), null);
			InstallableUnitOperand op2 = new InstallableUnitOperand(createResolvedIU(b), null);
			InstallableUnitOperand[] operands = new InstallableUnitOperand[] {op, op2};

			OperandSorter sorter = new OperandSorter(createTestMetdataRepository(new IInstallableUnit[] {a, b, c}).query(QueryUtil.ALL_UNITS, null), true);
			sorter.sortOperands(operands);
			System.out.println(sorter.getInstallOrder());
			System.out.println(sorter.getUninstallOrder());
		}

		{
			InstallableUnitOperand op = new InstallableUnitOperand(createResolvedIU(a), null);
			//			InstallableUnitOperand op2 = new InstallableUnitOperand(createResolvedIU(b), null);
			//			InstallableUnitPropertyOperand op3 = new InstallableUnitPropertyOperand(b, "key", null, "val2");
			InstallableUnitPropertyOperand op4 = new InstallableUnitPropertyOperand(a, "key", "addVal", null);
			Operand[] operands = new Operand[] {op, op4};

			OperandSorter sorter = new OperandSorter(createTestMetdataRepository(new IInstallableUnit[] {a, b, c}).query(QueryUtil.ALL_UNITS, null), true);
			sorter.sortOperands(operands);
			System.out.println(sorter.getInstallOrder());
			System.out.println(sorter.getUninstallOrder());
		}
	}

	//Nothing to do
	//Try sorting with property related operands

	//Try sorting with dep order different between the pre and the post
}
