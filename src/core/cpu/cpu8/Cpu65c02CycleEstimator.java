package core.cpu.cpu8;

import java.util.EnumSet;

import core.cpu.cpu8.Cpu65c02.AddressMode;
import core.memory.memory8.MemoryBusIIe;

public final class Cpu65c02CycleEstimator {

	private static final EnumSet<Cpu65c02Opcode> LDA_FAMILY = Cpu65c02Opcode.ldaFamily();

	private Cpu65c02CycleEstimator() {
	}

	public static int predictInstructionCycles(MemoryBusIIe memory, Register reg, Opcode op, int pc) {
		int cycles = op.getCycleTime();
		Integer machineCode = op.getMachineCode();
		if( machineCode==null )
			return cycles;
		Cpu65c02Opcode opcode = Cpu65c02Opcode.fromOpcodeByte(machineCode.intValue());
		if( opcode==null || !LDA_FAMILY.contains(opcode) )
			return cycles;
		int operandCounter = (pc+1)&0xffff;
		switch( op.getAddressMode() ) {
			case ABS_X: {
				int base = memory.getWord16LittleEndian(operandCounter);
				int eff = (base + reg.getX()) & 0xffff;
				if( (base>>8)!=(eff>>8) )
					cycles++;
				break;
			}
			case ABS_Y: {
				int base = memory.getWord16LittleEndian(operandCounter);
				int eff = (base + reg.getY()) & 0xffff;
				if( (base>>8)!=(eff>>8) )
					cycles++;
				break;
			}
			case IND_Y: {
				int ptr = memory.getByte(operandCounter);
				int base = memory.getWord16LittleEndian(ptr, 0xff);
				int eff = (base + reg.getY()) & 0xffff;
				if( (base>>8)!=(eff>>8) )
					cycles++;
				break;
			}
			default:
				break;
		}
		return cycles;
	}
}
