package device.display;

import core.cpu.cpu8.Cpu65c02;
import core.exception.HardwareException;
import core.memory.memory8.Memory8;
import core.memory.memory8.MemoryBusIIe;

public class DisplayConsoleAppleIIe extends DisplayConsole {
	
	private MemoryBusIIe memoryBus;
	private Memory8 memory;

	public DisplayConsoleAppleIIe( MemoryBusIIe memoryBus, long unitsPerCycle ) {
		super(unitsPerCycle);
		this.memoryBus = memoryBus;
		this.memory = memoryBus.getMemory();
	}

	@Override
	public void cycle() throws HardwareException {
		incSleepCycles(1);
		StringBuffer buf = new StringBuffer();
		buf.append("┌");
		for( int x = 0; x<40; x++ ) 
			buf.append("─");
		buf.append("┐\n");
		for( int y = 0; y<24; y++ ) {
			buf.append("│");
			for( int x = 0; x<40; x++ ) 
				buf.append(transliterate(memory.getByte(
						getAddressLo40(memoryBus.isPage2() ? 2:1, y, x))));
			buf.append("│\n");
		}
		buf.append("└");
		for( int x = 0; x<40; x++ ) 
			buf.append("─");
		buf.append("┘\n");
		System.out.println(buf.toString());
		showFps();
	}

	@Override
	public void coldReset() throws HardwareException {
		super.coldReset();
	}

	public static int getAddressLo40( int page, int scanline, int offset )
	{
		int address = page<<3;
		address |= scanline&0x0007;
		scanline >>= 3;
		address <<= 7;
		address += scanline*40 + offset;
		return address;
	}

	public static int getAddressHi40( int page, int scanline, int offset )
	{
		int address = page<<3;
		address |= scanline&0x0007;
		scanline >>= 3;
		address <<= 3;
		address |= scanline&0x0007;
		scanline >>= 3;
		address <<= 7;
		address += scanline*40 + offset;
		return address;
	}

	private static int memBuf [];

	public void showChanges() {
		
		if( memBuf==null ) {
			memBuf = new int [0x10000];
			for( int i = 0x400; i<0x800; i++ )
				memBuf[i] = -1;
		}
		else {
			for( int y = 0; y<24; y++ ) {
				for( int x = 0; x<40; x++ ) {
					if( memory.getByte(getAddressLo40(1, y, x))!=memBuf[getAddressLo40(1, y, x)] ) {
						System.out.print(Cpu65c02.getHexString(getAddressLo40(1, y, 0), 4)+": ");
						for( x = 0; x<40; x++ ) {
							System.out.print(transliterate(memory.getByte(getAddressLo40(1, y, x))));
							memBuf[getAddressLo40(1, y, x)] = memory.getByte(getAddressLo40(1, y, x));
						}
						System.out.println();
					}
				}
			}
		}
		
	}

	private char transliterate(int ascii) {
		ascii &= 0x7f;
		if( ascii<0x20 || ascii==0x7f )
			ascii = 0x7e;
		return (char) ascii;
	}

}
