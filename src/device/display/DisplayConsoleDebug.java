package device.display;

import core.cpu.cpu8.Cpu65c02;
import core.exception.HardwareException;
import core.memory.memory8.Memory8;

public class DisplayConsoleDebug extends DisplayConsole {

	private Memory8 memory;
	private Cpu65c02 cpu;
	private long lastUnits;
	private static int memBuf [];
	
	public DisplayConsoleDebug( Cpu65c02 cpu, long unitsPerCycle ) {
		super(unitsPerCycle);
		this.cpu = cpu;
		memory = cpu.getMemoryBus().getMemory();
	}

private boolean record = false;
	
	@Override
	public void cycle() throws HardwareException {
		incSleepCycles(1);
		if( cpu.getNextCycleUnits()>lastUnits ) {
			if( cpu.getOpcode().getMnemonic()==Cpu65c02.OpcodeMnemonic.BRK ) {
				System.out.println("Pausing 1 second for BRK command . . .");
				try{ Thread.sleep(1000); } catch ( InterruptedException e ) {}
			}
//			if( cpu.getRegister().getPCH()<0xc0 )
				System.out.println(cpu.getOpcodeString()+"   "+cpu.getRegister().toString());
		}
		lastUnits = cpu.getNextCycleUnits();
		for( int i = 0; i<0x100; i++ )
			if( memory.getByte(i)!=memBuf[i] && i!=0xfe) {
				System.out.println(Cpu65c02.getHexString(i, 4)+": "+Cpu65c02.getHexString(memory.getByte(i), 2));
				memBuf[i] = memory.getByte(i);
			}
	}

	@Override
	public void coldReset() throws HardwareException {
		lastUnits = -1;
		memBuf = new int [memory.getMaxAddress()];
		for( int i = 0; i<memory.getMaxAddress(); i++ )
			memBuf[i] = memory.getByte(i);
	}
	
}
