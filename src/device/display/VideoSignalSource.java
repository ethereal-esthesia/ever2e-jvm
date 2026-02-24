package device.display;

public interface VideoSignalSource {
	int getLastRead();
	boolean isVbl();
	int getVScan();
}
