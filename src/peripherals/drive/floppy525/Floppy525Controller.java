package peripherals.drive.floppy525;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.xml.bind.DatatypeConverter;

import peripherals.PeripheralIIe;
import core.emulator.VirtualMachineProperties;
import core.exception.HardwareException;
import core.memory.memory8.MemoryBusIIe.SwitchSet8;

/*
 * The following pages in Sather's Understanding the Apple IIe
 * were used as a reference
 * 9-7 stepper motor
 * 9-12 overview
 * 9-14 data register
 * 9-15 sequencer
 * 9-21 write protect
 * 9-25 write example
 * 9-26 Group Code Recording translation tables & read example
 * 9-27 detailed read explanation
 * 9-27 / 9-30 syncing strategy
 * 9-28 disk byte layout
 */

public class Floppy525Controller extends PeripheralIIe {

	private String[] fileName = new String[2];

	private static final byte[] DOS_ORDER_SECTORS = { 0, 7, 14, 6, 13, 5, 12, 4, 11, 3, 10, 2, 9, 1, 8, 15 };
	
	private static final byte[] PRODOS_ORDER_SECTORS = { 0, 8, 1, 9, 2, 10, 3, 11, 4, 12, 5, 13, 6, 14, 7, 15 };
	
	private static final byte[] ROM = DatatypeConverter.parseHexBinary(
			"A220A000A203863C8A0A243CF010053C49FF297EB0084AD0FB989D5603C8E810"+
			"E52058FFBABD00010A0A0A0A852BAABD8EC0BD8CC0BD8AC0BD89C0A050BD80C0"+
			"9829030A052BAABD81C0A95620A8FC8810EB8526853D8541A90885271808BD8C"+
			"C010FB49D5D0F7BD8CC010FBC9AAD0F3EABD8CC010FBC996F0092890DF49ADF0"+
			"25D0D9A0038540BD8CC010FB2A853CBD8CC010FB253C88D0EC28C53DD0BEA540"+
			"C541D0B8B0B7A056843CBC8CC010FB59D602A43C88990003D0EE843CBC8CC010"+
			"FB59D602A43C9126C8D0EFBC8CC010FB59D602D087A000A256CA30FBB1265E00"+
			"032A5E00032A9126C8D0EEE627E63DA53DCD0008A62B90DB4C01080000000000");

	private static final String EXT_PRODOS_ORDER = "PO";
	private static final String EXT_DOS_ORDER = "DSK";
	private static final String EXT_DOS_ORDER_EXPLICIT = "DO";
	private static final String NIBBLE_EXT = "NIB";

	private static final int TRACK_TOTAL = 35;
	private static final int SECTOR_TOTAL = 16;
	private static final int SECTOR_BYTES = 416;
	private static final int TRACK_BYTES = SECTOR_BYTES*SECTOR_TOTAL;

	private static final int PHASE0_MASK = 0x01;
	private static final int PHASE1_MASK = 0x02;
	private static final int PHASE2_MASK = 0x04;
	private static final int PHASE3_MASK = 0x08;
	
	private static final int PHASE_SHIFT[] = new int[] {
		-1, // 0000
		 0, // 0001
		 1, // 0010
		-1, // 0011
		 2, // 0100
		-1, // 0101
		-1, // 0110
		-1, // 0111
		 3, // 1000
		-1, // 1001
		-1, // 1010
		-1, // 1011
		-1, // 1100
		-1, // 1101
		-1, // 1110
		-1  // 1111
	};
	
	private int slot;

	private boolean[] driveOnPrevious = new boolean[2];
	
	private int driveSelect;
	
	private boolean[] driveOn = new boolean[2];
	private int[] driveOffRequest = new int[2];
	private boolean[] writeOn = new boolean[2];

	private int[] phase = new int[2];
	
	private int[] headHalfTrack = new int[2];
	private int[] headSectorByte = new int[2];

	private boolean[] readOnly = new boolean[2];

	private byte[][] diskImage;

//	private int[] dataShift = new int[2];
//	private int[] dataSelect = new int[2];
	private int dataRegister = 0;
	private int writeRequestRegister = 0;
	private int writeRegister = -1;

	private boolean[] driveChange = new boolean [2];

	private SwitchSet8 switchSet = new SwitchSet8() {

		@Override
		public int readMem( int address ){

			Boolean msb = null;
			
			// Drive switch information found in Sather 9-12
			switch( address&0x000f ) {
			case 0x00:  // Phase 0 off
				if( driveOn[driveSelect] ) {
					phase[driveSelect] &= ~PHASE0_MASK;
					moveHead();
				}
				break;
			case 0x01:  // Phase 0 on
				if( driveOn[driveSelect] ) {
					phase[driveSelect] |= PHASE0_MASK;
					moveHead();
				}
				break;
			case 0x02:  // Phase 1 off
				if( driveOn[driveSelect] ) {
					phase[driveSelect] &= ~PHASE1_MASK;
					moveHead();
				}
				break;
			case 0x03:  // Phase 1 on
				if( driveOn[driveSelect] ) {
					phase[driveSelect] |= PHASE1_MASK;
					moveHead();
				}
				break;
			case 0x04:  // Phase 2 off
				if( driveOn[driveSelect] ) {
					phase[driveSelect] &= ~PHASE2_MASK;
					moveHead();
				}
				break;
			case 0x05:  // Phase 2 on
				if( driveOn[driveSelect] ) {
					phase[driveSelect] |= PHASE2_MASK;
					moveHead();
				}
				break;
			case 0x06:  // Phase 3 off
				if( driveOn[driveSelect] ) {
					phase[driveSelect] &= ~PHASE3_MASK;
					moveHead();
				}
				break;
			case 0x07:  // Phase 3 on
				if( driveOn[driveSelect] ) {
					phase[driveSelect] |= PHASE3_MASK;
					moveHead();
				}
				break;
			case 0x08:  // Drive off
				if( driveOn[driveSelect] && driveOffRequest[driveSelect]==-1 )
					driveOffRequest[driveSelect] = 0;
				break;
			case 0x09:  // Drive on
				startDrive(driveSelect);
				driveOffRequest[driveSelect] = -1;
				break;
			case 0x0a:  // Drive 1 select
				driveSelect = 0;
				break;
			case 0x0b:  // Drive 2 select
				driveSelect = 1;
				break;
			case 0x0c:  // Poll read / set up reading
				if( writeOn[driveSelect] )
					writeRegister = writeRequestRegister;
				break;
			case 0x0d:  // Load (needed for writes)
				break;
			case 0x0e:  // Read
				writeOn[driveSelect] = false;
				if( driveOn[driveSelect] )
					msb = readOnly[driveSelect];
				break;
			case 0x0f:  // Write
				writeOn[driveSelect] = true;
				break;
			}
			displayDriveStatus();
			
			int data = msb==null ? dataRegister:( msb ? dataRegister|0x80:dataRegister&0x7f );
			if( (dataRegister&0x80)!=0 )
				dataRegister = 0x00;
			return data;
		}
		
		private void moveHead() {
			int phaseShift = PHASE_SHIFT[phase[driveSelect]];
			int currentPhase = headHalfTrack[driveSelect]&0x03;
			if( phaseShift>=0 ) {
				// TODO add required delay
				int currentTrack = headHalfTrack[driveSelect];
				if( Math.abs(phaseShift-currentPhase)==1 )
					headHalfTrack[driveSelect] += phaseShift-currentPhase;
				else if( Math.abs(phaseShift-currentPhase)==3 )
					headHalfTrack[driveSelect] -= (phaseShift-currentPhase)>0 ? 1:-1;
				if( headHalfTrack[driveSelect]<0 )
					headHalfTrack[driveSelect] = 0;
				else if( headHalfTrack[driveSelect]>=TRACK_TOTAL<<1 )
					headHalfTrack[driveSelect] = (TRACK_TOTAL<<1)-1;
				if( currentTrack!=headHalfTrack[driveSelect] )
					System.out.println("Slot "+slot+", drive "+(driveSelect+1)+", track "+headHalfTrack[driveSelect]/2d+" selected");
			}
		}

		@Override
		public void writeMem( int address, int value ){
			if( ((address&0x000f)==0x000d || (address&0x000f)==0x000f) && driveOn[driveSelect] ) 
				writeRequestRegister = value;
			driveChange[driveSelect] = true;
			readMem(address);
		}

		@Override
		public void warmReset() {
			driveSelect = 0;
			killDrive(0);
			killDrive(1);
			writeOn[1] = false;
			writeOn[1] = false;
			displayDriveStatus();
		}

	};

	private void displayDriveStatus() {
		for( int drive = 0; drive<2; drive++ )
			if( driveOnPrevious[drive]!=driveOn[drive] ) {
				System.out.println("Slot "+slot+", drive "+(driveSelect+1)+" "+
						(driveOn[drive] ? "started":"stopped"));
				driveOnPrevious[drive] = driveOn[drive];
			}
	}

	public Floppy525Controller( int slot, long unitsPerCycle, VirtualMachineProperties properties ) throws IOException, HardwareException {
		super(unitsPerCycle);
		this.slot = slot;
		fileName[0] = properties.getProperty("machine.layout.slot."+slot+".drive.1.file", null);
		fileName[1] = properties.getProperty("machine.layout.slot."+slot+".drive.2.file", null);
		driveOnPrevious[driveSelect] = false;
		headHalfTrack[0] = 69;
		headHalfTrack[1] = 69;
		driveOffRequest[0] = -1;
		driveOffRequest[1] = -1;
		coldReset();
	}

	@Override
	public void coldReset() throws HardwareException {
		switchSet.warmReset();
	}

	@Override
	public void cycle() throws HardwareException {
		
/*		TODO: the following shift-method may be used in future implementations,
 *            but not with existing emulated disk formats
 *            due to lack of nibble cycle-timing calculations when generating and
 *            even reading in images on existing emulators

		incSleepCycles(4);

		if( driveOn[driveSelect] ){
			dataSelect[driveSelect] <<= 1;
			dataSelect[driveSelect] &= 0xffff;
			dataShift[driveSelect]++;
			if( dataShift[driveSelect]==8 ) {
				dataShift[driveSelect] = 0;
				headSectorByte[driveSelect]++;
				if( headSectorByte[driveSelect]==TRACK_BYTES )
					headSectorByte[driveSelect] = 0;
				dataSelect[driveSelect] |= Byte.toUnsignedInt(
						diskImage[headHalfTrack[driveSelect]>>1][headSectorByte[driveSelect]]);
			}
			if( (dataSelect[driveSelect]&0x8000)!=0 ) {
				dataRegister = dataSelect[driveSelect]>>8;
				dataSelect[driveSelect] &= 0xff;
			} else if( (dataRegister&0x80)==0 )
				dataRegister = dataSelect[driveSelect]>>8;
		}
*/		

		incSleepCycles(32);

		if( driveOn[driveSelect] ) {
			headSectorByte[driveSelect]++;
			if( headSectorByte[driveSelect]==TRACK_BYTES )
				headSectorByte[driveSelect] = 0;
			if( !writeOn[driveSelect] ) {
				dataRegister = Byte.toUnsignedInt(
						diskImage[headHalfTrack[driveSelect]>>1][headSectorByte[driveSelect]]);
			}
		}

		if( writeRegister>=0 ) {
			diskImage[headHalfTrack[driveSelect]>>1][headSectorByte[driveSelect]] =
					(byte) writeRegister;
			writeRegister = -1;
		}
		
		for( int drive = 0; drive<2; drive++ ) {
			if( driveOffRequest[drive]>=0 &&
					driveOffRequest[drive]++==0x40000>>3 ) {
				driveOffRequest[drive] = -1;
				killDrive(drive);
			}
		}

	}
	
	private void loadImage( int drive ) {
		
		diskImage = new byte[TRACK_TOTAL][TRACK_BYTES];
		FileInputStream binStream = null;
		try {
			String fileName = this.fileName[drive].trim();
			File file = new File(fileName);
			readOnly[0] = !file.canWrite();
			binStream = new FileInputStream(file);
			String fileExt = fileName.trim().substring(fileName.lastIndexOf('.')+1).toUpperCase();
			switch( fileExt ) {
			case EXT_PRODOS_ORDER:
				for( int track = 0; track<TRACK_TOTAL; track++ )
					for( int sector = 0; sector<16; sector++ )
						binStream.read(diskImage[track], sector*SECTOR_BYTES+0, 0x100);
				throw new IOException("ProDOS images not yet supported, please convert to NIB format.");
			case EXT_DOS_ORDER:
			case EXT_DOS_ORDER_EXPLICIT:
				for( int track = 0; track<TRACK_TOTAL; track++ )
					for( int sector = 0; sector<16; sector++ )
						binStream.read(diskImage[track], sector*SECTOR_BYTES+0, 0x100);
				throw new IOException("DOS images not yet supported, please convert to NIB format.");
			case NIBBLE_EXT:
				for( int track = 0; track<TRACK_TOTAL; track++ )
					binStream.read(diskImage[track], 0, TRACK_BYTES);
				break;
			default:
				throw new IOException(fileExt+" format not supported.");
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if( binStream!=null ) {
				try {
					binStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
	}

	private void saveImage( int drive ) {

		FileOutputStream binStream = null;
		try {
			String fileName = this.fileName[drive].trim();
			binStream = new FileOutputStream(fileName);
			String fileExt = fileName.trim().substring(fileName.lastIndexOf('.')+1).toUpperCase();
			switch( fileExt ) {
			case EXT_PRODOS_ORDER:
				throw new IOException("ProDOS images not yet supported, please convert to NIB format.");
			case EXT_DOS_ORDER:
			case EXT_DOS_ORDER_EXPLICIT:
				throw new IOException("DOS images not yet supported, please convert to NIB format.");
			case NIBBLE_EXT:
				for( int track = 0; track<TRACK_TOTAL; track++ )
					binStream.write(diskImage[track], 0, TRACK_BYTES);
				break;
			default:
				throw new IOException(fileExt+" format not supported.");
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if( binStream!=null )
					binStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	private void startDrive( int drive ) {
		if( driveOn[drive] )
			return;
		driveOn[drive] = true;
		displayDriveStatus();
		loadImage(drive);
	}
		
	private void killDrive( int drive ) {
		if( !driveOn[drive] )
			return;
		driveOn[drive] = false;
		displayDriveStatus();
		if( !driveChange[drive] )
			return;
		driveChange[drive] = false;
		saveImage(drive);
	}

	@Override
	public byte[] getRom256b(){
		return ROM;
	}
	
	public int getDrive() {
		return driveSelect+1;
	}

	public void setDrive( int drive ) {
		driveSelect = drive-1;
	}

	@Override
	public String toString() {
		return "Floppy 5.25\" Disk II Controller";
	}

	@Override
	public SwitchSet8 getSwitchSet() {
		return switchSet;
	}

}
