package device.display;

import core.cpu.cpu8.Cpu65c02;
import core.exception.HardwareException;
import core.memory.memory8.Memory8;
import core.memory.memory8.MemoryBus8;

public class Display32x32Console extends DisplayConsole {

	private static final char COLOR_DECODE [] = {' ','^','>','╚','v','║','╔','╠','<','╝','═','╩','╗','╣','╦','╬'};

	private static final int X_SIZE = 32;
	private static final int Y_SIZE = 32;

	private Memory8 memory;
	private int memBuf [];
	
	public Display32x32Console(MemoryBus8 memoryBus, long unitsPerCycle ) {
		super(unitsPerCycle);
		this.memory = memoryBus.getMemory();
	}

	@Override
	public void cycle() {
		incSleepCycles(1);
		StringBuffer buf = new StringBuffer();
		buf.append("┌");
		for( int x = 0; x<X_SIZE; x++ ) 
			buf.append("─");
		buf.append("┐\n");
		for( int y = 0; y<Y_SIZE; y++ ) {
			buf.append("│");
			for( int x = 0; x<X_SIZE; x++ ) 
				buf.append(transliterate(memory.getByte(getAddressLo(1, y, x))));
			buf.append("│\n");
		}
		buf.append("└");
		for( int x = 0; x<X_SIZE; x++ ) 
			buf.append("─");
		buf.append("┘\n");
		System.out.println(buf.toString());
		showFps();
	}
	
	@Override
	public void coldReset() throws HardwareException {
		super.coldReset();
	}

	public static int getAddressLo( int page, int scanline, int offset )
	{
		return 0x0200+(scanline<<5)+offset;
	}

	public void showChanges() {
		
		if( memBuf==null ) {
			memBuf = new int [0x10000];
			for( int i = 0x200; i<0x800; i++ )
				memBuf[i] = -1;
		}
		else {
			for( int y = 0; y<Y_SIZE; y++ ) {
				for( int x = 0; x<X_SIZE; x++ ) {
					if( memory.getByte(getAddressLo(1, y, x))!=memBuf[getAddressLo(1, y, x)] ) {
						System.out.print(Cpu65c02.getHexString(getAddressLo(1, y, 0), 4)+": ");
						for( x = 0; x<40; x++ ) {
							System.out.print(" "+transliterate(memory.getByte(getAddressLo(1, y, x))));
							memBuf[getAddressLo(1, y, x)] = memory.getByte(getAddressLo(1, y, x));
						}
						System.out.println();
					}
				}
			}
		}
		
	}

	private char transliterate(int value) {
		return COLOR_DECODE[value&0x0f];
	}

}
