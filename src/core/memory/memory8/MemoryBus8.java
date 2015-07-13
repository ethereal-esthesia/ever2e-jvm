package core.memory.memory8;

import core.emulator.HardwareComponent;
import core.exception.HardwareException;

public class MemoryBus8 implements HardwareComponent {

	protected Memory8 memory;
	
	public MemoryBus8( Memory8 memory ) {
		this.memory = memory;
	}

	public Memory8 getMemory() {
		return memory;
	}

	public void coldRestart() throws HardwareException {
		memory.coldRestart();
		for( int i = getMaxAddress()-1; i>=0; i-- ) 
			memory.setByte(i, 0x00);
	}

	public int getByte( int address ) {
		return memory.getByte(address);
	}

	public void setByte( int address, int value ) {
		memory.setByte(address, value);
	}

	public int getWord16LittleEndian( int address ) {
		return getByte(address) | ( getByte(address+1) << 8 );
	}
	
	public int getWord16LittleEndian( int address, int addressMask ) {
		return getByte(address&addressMask) | ( getByte((address+1)&addressMask) << 8 );
	}
	
	public int getWord16BigEndian( int address ) {
		return getByte(address+1) | ( getByte(address) << 8 );
	}
	
	public int getWord16BigEndian( int address, int addressMask ) {
		return getByte((address+1)&addressMask) | ( getByte(address&addressMask) << 8 );
	}
	
	public int getMaxAddress() {
		return memory.getMaxAddress();
	}

}
