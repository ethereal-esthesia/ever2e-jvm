package device.display;

import core.exception.HardwareException;
import core.emulator.HardwareManager;

public abstract class DisplayConsole extends HardwareManager {

	Long timer;
	Double fpsAvg;
	
	public DisplayConsole( long unitsPerCycle ) {
		super(unitsPerCycle);
	}
	
	protected void showFps(){
		long newTimer = System.currentTimeMillis();
		if( timer!=null ) {
			double fps = 1000d/(newTimer-timer);
			if( fpsAvg==null )
				fpsAvg = fps;
			fpsAvg = (fpsAvg*7d+fps)/8d;
			System.out.println(String.format("%.1f fps", fpsAvg));
			System.out.println();
		}
		timer = newTimer;
	}

	@Override
	public void coldReset() throws HardwareException {
		timer = null;
		fpsAvg = null;
	}

}
