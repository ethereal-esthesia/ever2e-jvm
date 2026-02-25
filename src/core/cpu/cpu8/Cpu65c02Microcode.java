package core.cpu.cpu8;

import core.cpu.cpu8.Cpu65c02.AddressMode;
import core.cpu.cpu8.Cpu65c02.OpcodeMnemonic;

/**
 * Opcode-level microcode timing helpers for operand read cycle placement.
 *
 * Offsets are relative to instruction start cycle (0-based) and identify
 * when the effective operand data read occurs on the CPU bus.
 */
public final class Cpu65c02Microcode {

	public enum MicroOp {
		M_NONE,
		M_READ_IMM,
		M_READ_ZPG,
		M_READ_ZPG_XY,
		M_READ_ABS,
		M_READ_ABS_XY,
		M_READ_IND_X,
		M_READ_IND_Y,
		M_READ_ZPG_IND
	}

	public static final class OpcodeMicroInstr {
		private final int opcodeByte;
		private final Opcode opcode;
		private final MicroOp microOp;
		private final int readCycleNoCross;
		private final int readCycleCross;

		private OpcodeMicroInstr(int opcodeByte, Opcode opcode, MicroOp microOp, int readCycleNoCross, int readCycleCross) {
			this.opcodeByte = opcodeByte;
			this.opcode = opcode;
			this.microOp = microOp;
			this.readCycleNoCross = readCycleNoCross;
			this.readCycleCross = readCycleCross;
		}

		public int getOpcodeByte() {
			return opcodeByte;
		}

		public Opcode getOpcode() {
			return opcode;
		}

		public MicroOp getMicroOp() {
			return microOp;
		}

		public boolean usesMemoryDataRead() {
			return microOp!=MicroOp.M_NONE;
		}

		public int getOperandReadCycleOffset(boolean pageCrossed) {
			if( !usesMemoryDataRead() )
				return -1;
			return pageCrossed ? readCycleCross : readCycleNoCross;
		}
	}

	private static final OpcodeMicroInstr[] OPCODE_MICROCODE = buildOpcodeTable();

	private Cpu65c02Microcode() {
	}

	private static boolean usesMemoryDataRead(OpcodeMnemonic mnemonic) {
		if( mnemonic==null )
			return false;
		switch( mnemonic ) {
			case ADC:
			case AND:
			case ASL:
			case BIT:
			case CMP:
			case CPX:
			case CPY:
			case DEC:
			case EOR:
			case INC:
			case LDA:
			case LDX:
			case LDY:
			case LSR:
			case ORA:
			case ROL:
			case ROR:
			case SBC:
			case TRB:
			case TSB:
				return true;
			default:
				return false;
		}
	}

	private static OpcodeMicroInstr[] buildOpcodeTable() {
		OpcodeMicroInstr[] table = new OpcodeMicroInstr[256];
		for( int op = 0; op<256; op++ ) {
			Opcode opcode = Cpu65c02.OPCODE[op];
			MicroOp microOp = classifyMicroOp(opcode);
			int noCross = cycleNoCross(microOp);
			int cross = cycleCross(microOp);
			table[op] = new OpcodeMicroInstr(op, opcode, microOp, noCross, cross);
		}
		return table;
	}

	private static MicroOp classifyMicroOp(Opcode opcode) {
		if( opcode==null || opcode.getMnemonic()==null || !usesMemoryDataRead(opcode.getMnemonic()) )
			return MicroOp.M_NONE;
		AddressMode mode = opcode.getAddressMode();
		if( mode==null )
			return MicroOp.M_NONE;
		switch( mode ) {
			case IMM:
				return MicroOp.M_READ_IMM;
			case ZPG:
				return MicroOp.M_READ_ZPG;
			case ZPG_X:
			case ZPG_Y:
				return MicroOp.M_READ_ZPG_XY;
			case ABS:
				return MicroOp.M_READ_ABS;
			case ABS_X:
			case ABS_Y:
				return MicroOp.M_READ_ABS_XY;
			case IND_X:
				return MicroOp.M_READ_IND_X;
			case IND_Y:
				return MicroOp.M_READ_IND_Y;
			case ZPG_IND:
				return MicroOp.M_READ_ZPG_IND;
			default:
				return MicroOp.M_NONE;
		}
	}

	private static int cycleNoCross(MicroOp microOp) {
		switch( microOp ) {
			case M_READ_IMM:
				return 1;
			case M_READ_ZPG:
				return 2;
			case M_READ_ZPG_XY:
				return 3;
			case M_READ_ABS:
				return 3;
			case M_READ_ABS_XY:
				return 3;
			case M_READ_IND_X:
				return 5;
			case M_READ_IND_Y:
				return 4;
			case M_READ_ZPG_IND:
				return 4;
			default:
				return -1;
		}
	}

	private static int cycleCross(MicroOp microOp) {
		switch( microOp ) {
			case M_READ_ABS_XY:
				return 4;
			case M_READ_IND_Y:
				return 5;
			default:
				return cycleNoCross(microOp);
		}
	}

	public static OpcodeMicroInstr microInstrForOpcodeByte(int opcodeByte) {
		return OPCODE_MICROCODE[opcodeByte & 0xff];
	}

	public static OpcodeMicroInstr microInstrForOpcode(Opcode opcode) {
		if( opcode==null || opcode.getMachineCode()==null )
			return null;
		return microInstrForOpcodeByte(opcode.getMachineCode());
	}

	public static boolean usesMemoryDataRead(Opcode opcode) {
		OpcodeMicroInstr instr = microInstrForOpcode(opcode);
		return instr!=null && instr.usesMemoryDataRead();
	}

	public static int operandReadCycleOffset(Opcode opcode, boolean pageCrossed) {
		OpcodeMicroInstr instr = microInstrForOpcode(opcode);
		return instr==null ? -1 : instr.getOperandReadCycleOffset(pageCrossed);
	}

	public static MicroOp operandReadMicroOp(Opcode opcode) {
		OpcodeMicroInstr instr = microInstrForOpcode(opcode);
		return instr==null ? null : instr.getMicroOp();
	}
}
