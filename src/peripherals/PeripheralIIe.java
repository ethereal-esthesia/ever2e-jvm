package peripherals;

import core.emulator.HardwareManager;
import core.memory.memory8.MemoryBusIIe.SwitchSet8;

public abstract class PeripheralIIe extends HardwareManager  {

	public PeripheralIIe(long unitsPerCycle) {
		super(unitsPerCycle);
	}

	public abstract byte[] getRom256b();
	
	public abstract SwitchSet8 getSwitchSet();

}
