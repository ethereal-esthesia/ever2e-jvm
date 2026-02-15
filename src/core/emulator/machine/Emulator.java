package core.emulator.machine;

import java.util.PriorityQueue;

import core.exception.HardwareException;
import core.emulator.HardwareManager;

public class Emulator {
	public static interface StepListener {
		void onStep( long step, HardwareManager manager );
	}
	public static interface StepPhaseListener {
		boolean onStepPhase( long step, HardwareManager manager, boolean preCycle );
	}

	protected PriorityQueue<HardwareManager> hardwareManagerQueue;
	protected int granularityBitsPerSecond;
	
	public Emulator(PriorityQueue<HardwareManager> hardwareManagerQueue, int granularityBitsPerMs)
			throws HardwareException {
		this.hardwareManagerQueue = hardwareManagerQueue;
		this.granularityBitsPerSecond = granularityBitsPerMs;
		coldReset();
	}

	public void start() throws HardwareException, InterruptedException {
		start(-1, null, null);
	}

	public long start( long maxSteps, HardwareManager stepManager ) throws HardwareException, InterruptedException {
		return start(maxSteps, stepManager, null);
	}

	public long start( long maxSteps, HardwareManager stepManager, StepListener stepListener ) throws HardwareException, InterruptedException {
		return startWithStepPhases(maxSteps, stepManager, stepListener==null ? null : (step, manager, preCycle) -> {
			if( !preCycle )
				stepListener.onStep(step, manager);
			return true;
		});
	}

	public long startWithStepPhases( long maxSteps, HardwareManager stepManager, StepPhaseListener stepPhaseListener ) throws HardwareException, InterruptedException {
		long timer = System.currentTimeMillis();
		long oldTime = timer;
		long steps = 0;
		do {
			HardwareManager nextManager = hardwareManagerQueue.remove();
			long newTime = System.currentTimeMillis();
			if( newTime-oldTime<0 )
				timer = newTime;
			else if( newTime-oldTime>100 )
				timer += newTime-oldTime-100;
			oldTime = newTime;
			long clockTime = newTime-timer;
			if( maxSteps<0 ) {
				long waitTime = (nextManager.getNextCycleUnits()>>granularityBitsPerSecond)-clockTime;
				if( waitTime>0 )
					Thread.sleep(waitTime);
			}
			if( maxSteps>=0 && nextManager==stepManager ) {
				long stepNumber = steps+1;
				if( stepPhaseListener!=null && !stepPhaseListener.onStepPhase(stepNumber, nextManager, true) ) {
					hardwareManagerQueue.add(nextManager);
					break;
				}
				steps = stepNumber;
			}
			nextManager.cycle();
			if( maxSteps>=0 && nextManager==stepManager ) {
				if( stepPhaseListener!=null && !stepPhaseListener.onStepPhase(steps, nextManager, false) ) {
					hardwareManagerQueue.add(nextManager);
					break;
				}
				if( steps>=maxSteps ) {
					hardwareManagerQueue.add(nextManager);
					break;
				}
			}
			hardwareManagerQueue.add(nextManager);
		} while(true);		
		return steps;
	}

	public void coldReset() throws HardwareException {
		HardwareManager[] managerList = new HardwareManager[hardwareManagerQueue.size()];
		for( HardwareManager manager : hardwareManagerQueue.toArray(managerList) )
			manager.coldReset();
	}

}
