package core.emulator.machine.machine8;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.PriorityQueue;

import javax.sound.sampled.LineUnavailableException;

import org.junit.Test;

import core.cpu.cpu8.Cpu65c02;
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

	private static final String DEFAULT_ROM = "ROMS/Apple2e.emu";
	private static final int GRANULARITY_BITS_PER_MS = 32;

	public static void main( String[] argList ) throws HardwareException, InterruptedException, IOException {

		String propertiesFile;
		if( argList.length==0 )
			propertiesFile = DEFAULT_ROM;
		else
			propertiesFile = argList[0];
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
			bus.coldRestart();
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
			bus.coldRestart();
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
			bus.coldRestart();
			hardwareManagerQueue.add(cpu = new Cpu65c02((MemoryBusIIe) bus, (long) (unitsPerCycle/cpuMultiplier)));
			cpu.coldRestart();
			keyboard = new KeyboardIIe((long) (unitsPerCycle/keyActionMultiplier), cpu);
			hardwareManagerQueue.add(new DisplayConsoleDebug(cpu, (long) (unitsPerCycle/cpuMultiplier)));
			((MemoryBusIIe) bus).setKeyboard(keyboard);
			((MemoryBusIIe) bus).setDisplay(null);
			hardwareManagerQueue.add(keyboard);
		} else {
			bus = new MemoryBusIIe(memory, rom16k);
			bus.coldRestart();
			hardwareManagerQueue.add(cpu = new Cpu65c02((MemoryBusIIe) bus, (long) (unitsPerCycle/cpuMultiplier)));
			cpu.coldRestart();
			keyboard = new KeyboardIIe((long) (unitsPerCycle/keyActionMultiplier), cpu);
			//hardwareManagerQueue.add(new DisplayConsoleAppleIIe((MemoryBusIIe) bus, (long) (unitsPerCycle/displayMultiplier)));
			DisplayIIe display = new DisplayIIe((MemoryBusIIe) bus, keyboard, (long) (unitsPerCycle/displayMultiplier));
			hardwareManagerQueue.add(display);
			try {
				hardwareManagerQueue.add(new Speaker1Bit((MemoryBusIIe) bus, (long) unitsPerCycle, GRANULARITY_BITS_PER_MS));
			} catch (LineUnavailableException e) {
				e.printStackTrace();
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

		// Add peripherals in slots 1-7
		if( bus instanceof MemoryBusIIe )
			for( int slot = 1; slot<=7; slot++ ) {
				((MemoryBusIIe) bus).setSlotLayout(slot, properties.getSlotLayout(slot));
				((MemoryBusIIe) bus).setSlotRom(slot, properties.getSlotRom(slot));
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
	   	emulator.start();
		System.out.println("Done");

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
