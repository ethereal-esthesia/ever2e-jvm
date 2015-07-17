package core.emulator;

import java.util.Random;

import core.exception.HardwareException;

public abstract class HardwareManager implements HardwareComponent, Comparable<HardwareManager> {

	private static Random rand = new Random();
	private long id = rand.nextLong();
	private long nextActionCycleUnits;
	protected long unitsPerCycle;
	private static long offsetUnits;
	
	public HardwareManager( long unitsPerCycle ) {
		super();
		this.unitsPerCycle = unitsPerCycle;
		resetCycleCount();
	}

	/**
	 * @return Number of cycles until next call
	 * @throws HardwareException
	 */
	public abstract void cycle() throws HardwareException;

	public void incSleepCycles( long sleepCycles ) {
		nextActionCycleUnits += sleepCycles*unitsPerCycle;
	}

	/**
	 * Resets cycle unit count for the given cycle manager.
	 * Unless this method is overridden, hardware timing will be offset slightly
	 * to maintain cycling in the order competing managers were reset.
	 */
	public void resetCycleCount() {
		nextActionCycleUnits = offsetUnits;
		offsetUnits = (long) ((offsetUnits+unitsPerCycle/(Math.sqrt(unitsPerCycle)))%unitsPerCycle);
	}
	
	public long getNextCycleUnits() {
		return nextActionCycleUnits;
	}

	public void setUnitsPerCycle( long unitsPerCycle ) {
		this.unitsPerCycle = unitsPerCycle;
	}
	
	public long getUnitsPerCycle() {
		return unitsPerCycle;
	}
	
	@Override
	public int compareTo(HardwareManager manager) {
		long sign = getNextCycleUnits()-manager.getNextCycleUnits();
		if( sign==0 ) {
			if( manager.id==id )
				return 0;
			return manager.id<id ? -1:1;
		}
		return sign<0 ? -1:1;
	}

}
