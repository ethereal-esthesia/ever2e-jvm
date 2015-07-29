package core.emulator.machine;

import java.util.PriorityQueue;

import core.exception.HardwareException;
import core.emulator.HardwareManager;

public class Emulator {

	protected PriorityQueue<HardwareManager> hardwareManagerQueue;
	protected int granularityBitsPerSecond;
	
	public Emulator(PriorityQueue<HardwareManager> hardwareManagerQueue, int granularityBitsPerMs)
			throws HardwareException {
		this.hardwareManagerQueue = hardwareManagerQueue;
		this.granularityBitsPerSecond = granularityBitsPerMs;
		coldReset();
	}

	public void start() throws HardwareException, InterruptedException {
		long timer = System.currentTimeMillis();
		long oldTime = timer;
		do {
			HardwareManager nextManager = hardwareManagerQueue.remove();
			long newTime = System.currentTimeMillis();
			if( newTime-oldTime<0 )
				timer = newTime;
			else if( newTime-oldTime>100 )
				timer += newTime-oldTime-100;
			oldTime = newTime;
			long clockTime = newTime-timer;
			long waitTime = (nextManager.getNextCycleUnits()>>granularityBitsPerSecond)-clockTime;
			if( waitTime>0 )
				Thread.sleep(waitTime);
			nextManager.cycle();
			hardwareManagerQueue.add(nextManager);
		} while(true);		
	}

	public void coldReset() throws HardwareException {
		HardwareManager[] managerList = new HardwareManager[hardwareManagerQueue.size()];
		for( HardwareManager manager : hardwareManagerQueue.toArray(managerList) )
			manager.coldReset();
	}

}
