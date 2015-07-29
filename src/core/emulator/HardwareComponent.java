package core.emulator;

import core.exception.HardwareException;

public interface HardwareComponent {

	/**
	 * Simulates events during a cold-restart such as the machine powering off and then on
	 * @throws HardwareException
	 */
	void coldReset() throws HardwareException;
	
}
