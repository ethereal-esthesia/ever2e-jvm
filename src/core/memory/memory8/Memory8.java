package core.memory.memory8;

import java.util.Arrays;

import core.exception.HardwareException;
import core.emulator.HardwareComponent;

public class Memory8 implements HardwareComponent {

	private int size;
	private byte address [];
	
	public Memory8( int size ) {
		this.size = size;
	}

	@Override
	public void coldReset() throws HardwareException {
		if( size<=0 )
			throw new HardwareException("Memory size not supported");
		address = new byte[size];
	}

	public int getByte( int address ) {
		return Byte.toUnsignedInt(this.address[address]);
	}

	public void setByte(int address, int value) {
		this.address[address] = (byte) value;
	}

	public int getMaxAddress() {
		return size;
	}

	@Override
	public String toString() {
		return "Memory8 [size=" + size + ", address="
				+ Arrays.toString(address) + "]";
	}

}
