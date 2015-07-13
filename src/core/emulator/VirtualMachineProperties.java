package core.emulator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;

import peripherals.SlotLayout;


public class VirtualMachineProperties {

	public static enum MachineLayoutType {
		DEMO_32x32,
		DEMO_32x32_CONSOLE,
		APPLE_IIE,
		DEBUG_65C02
	}

	private static final String EMU_EXTENSION = ".emu";

	private Properties properties;
	private MachineLayoutType layout;
	private SlotLayout[] slotLayout = new SlotLayout[7];
	private int programStart;
	private byte[] program;
	private byte[][] slotRom = new byte[7][];
	
	public VirtualMachineProperties( String propertiesFileName ) throws IOException {
		if( !propertiesFileName.substring(propertiesFileName.length()-4).equalsIgnoreCase(EMU_EXTENSION) )
			propertiesFileName += EMU_EXTENSION;
		properties = new Properties();
		File propertiesFile = new File(propertiesFileName);
		properties.load(new FileInputStream(propertiesFile));
		this.layout = MachineLayoutType.valueOf(properties.getProperty("machine.layout"));
		this.programStart = Integer.decode(properties.getProperty("address.start"));
		FileInputStream binStream;
		String fileName = properties.getProperty("binary.file");
		try {
			binStream = new FileInputStream(propertiesFile.getParent()+"/"+fileName);
		} catch ( FileNotFoundException e ) {
			binStream = new FileInputStream(fileName);
		}
		try {
			program = new byte[0x10000];
			int len = binStream.read(program, 0, 0x10000);
			program = Arrays.copyOf(program, len);
		} finally {
			binStream.close();
		}
		for( Integer i = 1; i<=7; i++ ) {
			String layoutStr = properties.getProperty("machine.layout.slot."+i, null);
			slotRom[i-1] = null;
			if( layoutStr!=null && layoutStr.length()>0 ) {
				slotLayout[i-1] = SlotLayout.valueOf(layoutStr);
				fileName = properties.getProperty("binary.file.slot."+i);
				try {
					binStream = new FileInputStream(propertiesFile.getParent()+"/"+fileName);
				} catch ( FileNotFoundException e ) {
					binStream = new FileInputStream(fileName);
				}
				slotRom[i-1] = new byte[0x100];
				binStream.read(slotRom[i-1], 0, 0x100);
			}
		}
	}
	public MachineLayoutType getLayout() {
		return layout;
	}
	public int getProgramStart() {
		return programStart;
	}
	public byte[] getCode() {
		return program;
	}
	public SlotLayout getSlotLayout( int slot ) {
		return slotLayout[slot-1];
	}
	public byte[] getSlotRom( int slot ) {
		return slotRom[slot-1];
	}
	public String getProperty( String propertyStr ) {
		return properties.getProperty(propertyStr);
	}
	public String getProperty( String propertyStr, String defaultStr ) {
		return properties.getProperty(propertyStr, defaultStr);
	}
	
	@Override
	public String toString() {
		return "Machine Properties [layout=" + layout + ", programStart="
				+ "0x"+Integer.toHexString(programStart) + ", size=" + "0x"+Integer.toHexString(program.length) + "]";
	}

}
