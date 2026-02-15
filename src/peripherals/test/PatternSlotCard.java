package peripherals.test;

import peripherals.PeripheralIIe;
import core.emulator.VirtualMachineProperties;
import core.exception.HardwareException;
import core.memory.memory8.MemoryBusIIe.SwitchSet8;

/**
 * Minimal test card with deterministic ROM contents.
 * Default pattern is one byte repeated across the 256-byte slot ROM.
 */
public class PatternSlotCard extends PeripheralIIe {

	private final int slot;
	private final byte[] rom256b = new byte[0x100];
	private final SwitchSet8 switchSet = new SwitchSet8() {
		@Override
		public int readMem(int address) {
			return 0x00;
		}

		@Override
		public void writeMem(int address, int value) {
			// No soft-switch behavior for this test card.
		}

		@Override
		public void warmReset() {
			// Nothing to reset.
		}
	};

	public PatternSlotCard(int slot, long unitsPerCycle, VirtualMachineProperties properties) {
		super(unitsPerCycle);
		this.slot = slot;
		String prop = properties.getProperty("machine.layout.slot." + slot + ".pattern");
		int pattern = slot & 0xff;
		if( prop!=null && prop.trim().length()>0 )
			pattern = parsePattern(prop.trim(), slot);
		byte fill = (byte) (pattern & 0xff);
		for( int i = 0; i<rom256b.length; i++ )
			rom256b[i] = fill;
	}

	private int parsePattern(String value, int slotNumber) {
		try {
			if( value.toLowerCase().startsWith("0x") )
				return Integer.parseInt(value.substring(2), 16);
			if( value.matches("^[0-9A-Fa-f]{1,2}$") && value.matches(".*[A-Fa-f].*") )
				return Integer.parseInt(value, 16);
			return Integer.parseInt(value, 10);
		} catch (NumberFormatException e) {
			System.err.println("Warning: invalid slot pattern \""+value+"\" for slot "+slotNumber+
					"; using slot number");
			return slotNumber;
		}
	}

	@Override
	public void cycle() throws HardwareException {
		// Keep scheduler moving; this card has no active timing behavior.
		incSleepCycles(1);
	}

	@Override
	public void coldReset() throws HardwareException {
		// Stateless test card.
	}

	@Override
	public byte[] getRom256b() {
		return rom256b;
	}

	@Override
	public SwitchSet8 getSwitchSet() {
		return switchSet;
	}

	@Override
	public String toString() {
		return "PatternSlotCard(slot="+slot+", pattern=0x"+
				Integer.toHexString(Byte.toUnsignedInt(rom256b[0])).toUpperCase()+")";
	}
}
