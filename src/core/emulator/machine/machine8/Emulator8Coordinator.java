package core.emulator.machine.machine8;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.awt.GraphicsEnvironment;
import java.text.DecimalFormat;
import java.util.PriorityQueue;

import org.junit.Test;

import peripherals.PeripheralIIe;
import core.cpu.cpu8.Cpu65c02;
import core.cpu.cpu8.Opcode;
import core.emulator.HardwareManager;
import core.emulator.VirtualMachineProperties;
import core.emulator.VirtualMachineProperties.MachineLayoutType;
import core.emulator.machine.Emulator;
import core.exception.HardwareException;
import core.memory.memory8.Memory8;
import core.memory.memory8.MemoryBus8;
import core.memory.memory8.MemoryBusDemo8;
import core.memory.memory8.MemoryBusIIe;
import device.display.Display32x32;
import device.display.Display32x32Console;
import device.display.DisplayConsoleDebug;
import device.display.DisplayIIe;
import device.keyboard.KeyboardIIe;
import device.speaker.Speaker1Bit;

public class Emulator8Coordinator {

	private static final String DEFAULT_MACHINE = "ROMS/Apple2e.emu";
	private static final int GRANULARITY_BITS_PER_MS = 32;

	private static int parseByteArg(String value, String argName) {
		String raw = value.trim();
		int parsed;
		if( raw.toLowerCase().startsWith("0x") ) {
			parsed = Integer.parseInt(raw.substring(2), 16);
		}
		else if( raw.matches("^[0-9A-Fa-f]{1,2}$") && raw.matches(".*[A-Fa-f].*") ) {
			parsed = Integer.parseInt(raw, 16);
		}
		else {
			parsed = Integer.parseInt(raw, 10);
		}
		if( parsed<0 || parsed>0xff )
			throw new IllegalArgumentException(argName+" must be in [0..255], got "+value);
		return parsed;
	}

	public static void main( String[] argList ) throws HardwareException, InterruptedException, IOException {

		String propertiesFile = DEFAULT_MACHINE;
		long maxCpuSteps = -1;
		String traceFile = null;
		String tracePhase = "pre";
		Integer resetPFlagValue = null;
		for( int i = 0; i<argList.length; i++ ) {
			String arg = argList[i];
			if( "--steps".equals(arg) ) {
				if( i+1>=argList.length )
					throw new IllegalArgumentException("Missing value for --steps");
				maxCpuSteps = Long.parseLong(argList[++i]);
			}
			else if( arg.startsWith("--steps=") ) {
				maxCpuSteps = Long.parseLong(arg.substring("--steps=".length()));
			}
			else if( "--trace-file".equals(arg) ) {
				if( i+1>=argList.length )
					throw new IllegalArgumentException("Missing value for --trace-file");
				traceFile = argList[++i];
			}
			else if( arg.startsWith("--trace-file=") ) {
				traceFile = arg.substring("--trace-file=".length());
			}
			else if( "--trace-phase".equals(arg) ) {
				if( i+1>=argList.length )
					throw new IllegalArgumentException("Missing value for --trace-phase");
				tracePhase = argList[++i];
			}
			else if( arg.startsWith("--trace-phase=") ) {
				tracePhase = arg.substring("--trace-phase=".length());
			}
			else if( "--post".equals(arg) ) {
				tracePhase = "post";
			}
			else if( "--reset-pflag-value".equals(arg) ) {
				if( i+1>=argList.length )
					throw new IllegalArgumentException("Missing value for --reset-pflag-value");
				resetPFlagValue = parseByteArg(argList[++i], "--reset-pflag-value");
			}
			else if( arg.startsWith("--reset-pflag-value=") ) {
				resetPFlagValue = parseByteArg(arg.substring("--reset-pflag-value=".length()), "--reset-pflag-value");
			}
			else {
				propertiesFile = arg;
			}
		}
		tracePhase = tracePhase.trim().toLowerCase();
		if( !"pre".equals(tracePhase) && !"post".equals(tracePhase) )
			throw new IllegalArgumentException("Unsupported --trace-phase value: "+tracePhase+" (expected pre or post)");
		System.out.println("Loading \""+propertiesFile+"\" into memory");
		VirtualMachineProperties properties = new VirtualMachineProperties(propertiesFile);

		double cpuMultiplier = new Double(properties.getProperty("machine.cpu.mult", "1")); // 1020484hz

		double cpuClock = 1020484d;  // 1020484hz
		double unitsPerCycle = (1000l<<GRANULARITY_BITS_PER_MS)/cpuClock;  // 20 bits per ms granularity
		double displayMultiplier = 1d;  // 59.92fps
		double keyActionMultiplier = 1./17030.;

		PriorityQueue<HardwareManager> hardwareManagerQueue = new PriorityQueue<>();

		System.out.println(properties);

		// Set up machine based on layout selection
		
		byte[] rom16k = new byte[0x4000];
		Memory8 memory = new Memory8(0x20000);
		MemoryBus8 bus;
		Cpu65c02 cpu;
		KeyboardIIe keyboard = null;

		if( properties.getLayout()==MachineLayoutType.DEMO_32x32 ) {
			bus = new MemoryBusDemo8(memory, keyboard);
			bus.coldReset();
			cpuMultiplier /= 32d;
			displayMultiplier = 60d/cpuClock;
			hardwareManagerQueue.add(cpu = new Cpu65c02((MemoryBusIIe) bus, (long) (unitsPerCycle/cpuMultiplier)));
			keyboard = new KeyboardIIe((long) (unitsPerCycle/keyActionMultiplier), cpu);
			cpu.getRegister().setA(0);
			cpu.getRegister().setX(0);
			cpu.getRegister().setY(0);
			cpu.getRegister().setS(0);
			cpu.getRegister().setP(0);
			hardwareManagerQueue.add(new Display32x32(bus, keyboard, (long) (unitsPerCycle/displayMultiplier)));
			hardwareManagerQueue.add(keyboard);
		} else if( properties.getLayout()==MachineLayoutType.DEMO_32x32_CONSOLE ) {
			displayMultiplier = 10d/cpuClock;
			bus = new MemoryBusDemo8(memory, null);
			bus.coldReset();
			cpuMultiplier /= 32d;
			hardwareManagerQueue.add(cpu = new Cpu65c02((MemoryBusIIe) bus, (long) (unitsPerCycle/cpuMultiplier)));
			cpu.getRegister().setA(0);
			cpu.getRegister().setX(0);
			cpu.getRegister().setY(0);
			cpu.getRegister().setS(0);
			cpu.getRegister().setP(0);
			hardwareManagerQueue.add(new Display32x32Console(bus, (long) (unitsPerCycle/displayMultiplier)));
		} else if( properties.getLayout()==MachineLayoutType.DEBUG_65C02 ) {
			bus = new MemoryBusIIe(memory, rom16k);
			bus.coldReset();
			hardwareManagerQueue.add(cpu = new Cpu65c02((MemoryBusIIe) bus, (long) (unitsPerCycle/cpuMultiplier)));
			cpu.coldReset();
			keyboard = new KeyboardIIe((long) (unitsPerCycle/keyActionMultiplier), cpu);
			hardwareManagerQueue.add(new DisplayConsoleDebug(cpu, (long) (unitsPerCycle/cpuMultiplier)));
			((MemoryBusIIe) bus).setKeyboard(keyboard);
			((MemoryBusIIe) bus).setDisplay(null);
			hardwareManagerQueue.add(keyboard);
		} else {
			bus = new MemoryBusIIe(memory, rom16k);
			bus.coldReset();
			hardwareManagerQueue.add(cpu = new Cpu65c02((MemoryBusIIe) bus, (long) (unitsPerCycle/cpuMultiplier)));
			cpu.coldReset();
			keyboard = new KeyboardIIe((long) (unitsPerCycle/keyActionMultiplier), cpu);
			DisplayIIe display = null;
			if( GraphicsEnvironment.isHeadless() ) {
				System.out.println("Running headless: display output disabled");
			}
			else {
				display = new DisplayIIe((MemoryBusIIe) bus, keyboard, (long) (unitsPerCycle/displayMultiplier));
				hardwareManagerQueue.add(display);
			}
			try {
				hardwareManagerQueue.add(new Speaker1Bit((MemoryBusIIe) bus, (long) unitsPerCycle, GRANULARITY_BITS_PER_MS));
			} catch (Exception e) {
				System.out.println("Warning: Speaker initialization unavailable: " + e.getClass().getSimpleName());
			}
			((MemoryBusIIe) bus).setKeyboard(keyboard);
			((MemoryBusIIe) bus).setDisplay(display);
			hardwareManagerQueue.add(keyboard);
		}

		byte [] program = properties.getCode();

		// Set up intial ROM or RAM program
		int addr = properties.getProgramStart();
		for( byte opcode : program ) {
			if( bus.getClass()==MemoryBusIIe.class && addr>=MemoryBusIIe.ROM_START )
				rom16k[addr-MemoryBusIIe.ROM_START] = opcode;
			else
				memory.setByte(addr, opcode);
			addr++;
		}

		// Set program start
		if( properties.getCode().length+properties.getProgramStart()<0xfffd ) {
			if( bus.getClass()==MemoryBusIIe.class ) {
				rom16k[0xfffc-MemoryBusIIe.ROM_START] = (byte) properties.getProgramStart();
				rom16k[0xfffd-MemoryBusIIe.ROM_START] = (byte) (properties.getProgramStart()>>8);
			} else {
				memory.setByte(0xfffc, properties.getProgramStart());
				memory.setByte(0xfffd, properties.getProgramStart()>>8);
			}
		}

		System.out.println();
		
		// Add peripherals in slots 1-7
		if( bus instanceof MemoryBusIIe )
			for( int slot = 1; slot<=7; slot++ ) {
				PeripheralIIe card = null;
				try {
					String peripheralClass = properties.getSlotLayout(slot);
					if( peripheralClass!=null )
						peripheralClass = "peripherals."+peripheralClass;
					PeripheralIIe peripheralCard = peripheralClass==null ? null :
							(PeripheralIIe) Class.forName(peripheralClass).
							getConstructor(int.class, long.class, VirtualMachineProperties.class).newInstance(
									slot, (long) unitsPerCycle, properties);
					card = ((MemoryBusIIe) bus).setSlot(slot, peripheralCard);
					((MemoryBusIIe) bus).setSlotRom(slot, card==null ? null:card.getRom256b());
					if( card!=null )
						hardwareManagerQueue.add(card);
				} catch ( Exception e ) {
					if( e.getCause()!=null && Exception.class.isInstance(e) )
						e = (Exception) e.getCause();
					System.out.println("Warning: Unable to load peripheral class for slot "+slot+
							(e.getLocalizedMessage()==null?"":":\n"+e.getLocalizedMessage()));
					((MemoryBusIIe) bus).resetSlot(slot);
					((MemoryBusIIe) bus).setSlotRom(slot, null);
				} finally {
					System.out.println("Slot "+slot+": "+(card==null?"empty":card));
				}
			}
		
		System.out.println();
		System.out.println("--------------------------------------");
		System.out.println("          Starting Emulation          ");
		System.out.println("--------------------------------------");
		System.out.println();

	   	DecimalFormat format = new DecimalFormat("0.######E0");
		HardwareManager[] managerList = new HardwareManager[hardwareManagerQueue.size()];
		for( HardwareManager manager : hardwareManagerQueue.toArray(managerList) )
			System.out.println(manager.getClass().getSimpleName()+"@"+
					format.format(cpuClock*unitsPerCycle/manager.getUnitsPerCycle())+"Hz");

		System.out.println("");
	   	Emulator emulator = new Emulate65c02(hardwareManagerQueue, GRANULARITY_BITS_PER_MS);
	   	if( resetPFlagValue!=null )
	   		cpu.setResetPOverride(resetPFlagValue);
	   	if( maxCpuSteps>=0 ) {
	   		PrintWriter traceWriter = null;
	   		if( traceFile!=null ) {
	   			traceWriter = new PrintWriter(new FileWriter(traceFile));
	   			traceWriter.println("step,event_type,event,pc,opcode,a,x,y,p,s,mnemonic,mode");
	   		}
	   		final PrintWriter finalTraceWriter = traceWriter;
	   		final String finalTracePhase = tracePhase;
	   		long steps = emulator.startWithStepPhases(maxCpuSteps, cpu, (step, manager, preCycle) -> {
	   			if( finalTraceWriter==null )
	   				return;
	   			if( "pre".equals(finalTracePhase) && !preCycle )
	   				return;
	   			if( "post".equals(finalTracePhase) && preCycle )
	   				return;
	   			Opcode opcode;
	   			int pc;
	   			boolean isResetEvent;
	   			if( "pre".equals(finalTracePhase) ) {
	   				opcode = cpu.getPendingOpcode();
	   				pc = cpu.getPendingPC();
	   				String mnemonic = opcode.getMnemonic()==null ? "" : opcode.getMnemonic().toString().trim();
	   				isResetEvent = "RES".equals(mnemonic);
	   			}
	   			else {
	   				Opcode executedOpcode = cpu.getOpcode();
	   				String executedMnemonic = executedOpcode.getMnemonic()==null ? "" : executedOpcode.getMnemonic().toString().trim();
	   				isResetEvent = "RES".equals(executedMnemonic);
	   				if( isResetEvent ) {
	   					opcode = executedOpcode;
	   					pc = cpu.getRegister().getPC();
	   				}
	   				else {
	   					opcode = cpu.getPendingOpcode();
	   					pc = cpu.getPendingPC();
	   				}
	   			}
	   			String eventType = isResetEvent ? "event":"instr";
	   			String event = isResetEvent ? "RESET":"";
	   			Integer machineCode = opcode.getMachineCode();
	   			finalTraceWriter.println(
	   					step + "," +
	   					eventType + "," +
	   					event + "," +
	   					Cpu65c02.getHexString(pc, 4) + "," +
	   					(machineCode==null?"--":Cpu65c02.getHexString(machineCode, 2)) + "," +
	   					Cpu65c02.getHexString(cpu.getRegister().getA(), 2) + "," +
	   					Cpu65c02.getHexString(cpu.getRegister().getX(), 2) + "," +
	   					Cpu65c02.getHexString(cpu.getRegister().getY(), 2) + "," +
	   					Cpu65c02.getHexString(cpu.getRegister().getP(), 2) + "," +
	   					Cpu65c02.getHexString(cpu.getRegister().getS(), 2) + "," +
	   					opcode.getMnemonic() + "," +
	   					opcode.getAddressMode()
	   			);
	   		});
	   		if( traceWriter!=null )
	   			traceWriter.close();
			System.out.println("Stopped after "+steps+" CPU steps");
			System.out.println("PC="+Cpu65c02.getHexString(cpu.getRegister().getPC(), 4)+
					" A="+Cpu65c02.getHexString(cpu.getRegister().getA(), 2)+
					" X="+Cpu65c02.getHexString(cpu.getRegister().getX(), 2)+
					" Y="+Cpu65c02.getHexString(cpu.getRegister().getY(), 2)+
					" P="+Cpu65c02.getHexString(cpu.getRegister().getP(), 2)+
					" S="+Cpu65c02.getHexString(cpu.getRegister().getS(), 2));
			if( traceFile!=null )
				System.out.println("Trace written: "+traceFile);
	   	}
	   	else {
	   		emulator.start();
			System.out.println("Done");
	   	}

	}

	private static void printFlags(int origA, int origB, int value) {
		System.out.print(Cpu65c02.getHexString(value&0xff, 2)+" ");
		System.out.print((((origA^value)&(origB^value)&0x80)!=0) ? "V":"v");
		System.out.print(value==0 ? "Z":"z");
		System.out.print((value&0x80)!=0 ? "N":"n");
		System.out.println((value&0x100)!=0 ? "C":"c");

	}

	@Test
	public static void testOpcode() {

		boolean carry = false;
		do {
			for( int reg_getA = 0; reg_getA<256; reg_getA++ )
				for( int mem = 0; mem<256; mem++ ) {
					System.out.print(Cpu65c02.getHexString(reg_getA&0xff, 2)+" + ");
					System.out.print(Cpu65c02.getHexString(mem&0xff, 2)+" + ");
					System.out.print((carry ? 1:0)+" = ");
					int value = mem;
					int regA = reg_getA;
					int valAdd = value;
					value += regA;
					if( !carry )
						value++;
					printFlags(regA, valAdd, value);

				}
			carry = !carry;
		} while( carry );

	}

	@Test
	public static void displayOpcodes() {
		for( int x = 0; x<256; x++ )
			if( Cpu65c02.OPCODE[x].getMnemonic()==Cpu65c02.OpcodeMnemonic.NOP )
				System.out.println(x);
	}

}
