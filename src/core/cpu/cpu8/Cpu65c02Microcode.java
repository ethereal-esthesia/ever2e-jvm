package core.cpu.cpu8;

import java.util.Arrays;

import core.cpu.cpu8.Cpu65c02.AddressMode;
import core.cpu.cpu8.Cpu65c02.OpcodeMnemonic;

/**
 * Opcode-centric microcode descriptors.
 *
 * Source-of-truth for per-cycle expected mnemonic order.
 */
public final class Cpu65c02Microcode {

	public enum MicroOp {
		M_FETCH_OPCODE,
		M_FETCH_OPERAND_LO,
		M_FETCH_OPERAND_HI,
		M_READ_IMM_DATA,
		M_READ_ZP_PTR_LO,
		M_READ_ZP_PTR_HI,
		M_READ_DUMMY,
		M_READ_EA,
		M_WRITE_EA_DUMMY,
		M_WRITE_EA,
		M_INTERNAL
	}

	public enum AccessType {
		AT_NONE,
		AT_READ,
		AT_WRITE,
		AT_RMW
	}

	static final class OpcodeMicroInstr {
		private final int opcodeByte;
		private final AccessType accessType;
		private final MicroOp[] noCrossScript;
		private final MicroOp[] crossScript;

		private OpcodeMicroInstr(int opcodeByte, AccessType accessType, MicroOp[] noCrossScript, MicroOp[] crossScript) {
			this.opcodeByte = opcodeByte;
			this.accessType = accessType;
			this.noCrossScript = noCrossScript;
			this.crossScript = crossScript;
		}

		int getOpcodeByte() {
			return opcodeByte;
		}

		AccessType getAccessType() {
			return accessType;
		}

		MicroOp[] getCycleScript(boolean pageCrossed) {
			MicroOp[] src = pageCrossed ? crossScript : noCrossScript;
			return Arrays.copyOf(src, src.length);
		}

		boolean usesMemoryDataRead() {
			return indexOfFirstReadDataCycle(false)>=0 || indexOfFirstReadDataCycle(true)>=0;
		}

		int getOperandReadCycleOffset(boolean pageCrossed) {
			return indexOfFirstReadDataCycle(pageCrossed);
		}

		private int indexOfFirstReadDataCycle(boolean pageCrossed) {
			MicroOp[] script = pageCrossed ? crossScript : noCrossScript;
			for( int i = 0; i<script.length; i++ ) {
				MicroOp op = script[i];
				if( op==MicroOp.M_READ_IMM_DATA || op==MicroOp.M_READ_EA )
					return i;
			}
			return -1;
		}
	}

	private static final OpcodeMicroInstr[] OPCODE_MICROCODE = buildOpcodeTable();
	private static final int[] LDA_OPCODE_BYTES = new int[] { 0xA9, 0xA5, 0xB5, 0xAD, 0xBD, 0xB9, 0xA1, 0xB1, 0xB2 };

	private Cpu65c02Microcode() {
	}

	private static OpcodeMicroInstr[] buildOpcodeTable() {
		OpcodeMicroInstr[] table = new OpcodeMicroInstr[256];
		for( int op = 0; op<256; op++ ) {
			Opcode opcode = Cpu65c02.OPCODE[op];
			AccessType accessType = classifyAccessType(opcode);
			MicroOp[] noCross = buildCycleScript(opcode, accessType, false);
			MicroOp[] cross = buildCycleScript(opcode, accessType, true);
			table[op] = new OpcodeMicroInstr(op, accessType, noCross, cross);
		}
		return table;
	}

	private static AccessType classifyAccessType(Opcode opcode) {
		if( opcode==null || opcode.getMnemonic()==null )
			return AccessType.AT_NONE;
		switch( opcode.getMnemonic() ) {
			case ADC:
			case AND:
			case BIT:
			case CMP:
			case CPX:
			case CPY:
			case EOR:
			case LDA:
			case LDX:
			case LDY:
			case ORA:
			case SBC:
			case TRB:
			case TSB:
				return AccessType.AT_READ;
			case STA:
			case STX:
			case STY:
			case STZ:
				return AccessType.AT_WRITE;
			case ASL:
			case DEC:
			case INC:
			case LSR:
			case ROL:
			case ROR:
				return opcode.getAddressMode()==AddressMode.ACC ? AccessType.AT_NONE : AccessType.AT_RMW;
			default:
				return AccessType.AT_NONE;
		}
	}

	private static MicroOp[] script(MicroOp... ops) {
		return ops;
	}

	private static MicroOp[] buildCycleScript(Opcode opcode, AccessType accessType, boolean pageCrossed) {
		if( opcode==null || opcode.getAddressMode()==null )
			return script(MicroOp.M_FETCH_OPCODE);
		switch( opcode.getAddressMode() ) {
			case IMM:
				return accessType==AccessType.AT_READ
					? script(MicroOp.M_FETCH_OPCODE, MicroOp.M_READ_IMM_DATA)
					: script(MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO);
			case ZPG:
				return byAccess(accessType,
					script(MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_EA),
					script(MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_WRITE_EA),
					script(MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_EA, MicroOp.M_WRITE_EA_DUMMY, MicroOp.M_WRITE_EA));
			case ZPG_X:
			case ZPG_Y:
				return byAccess(accessType,
					script(MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_DUMMY, MicroOp.M_READ_EA),
					script(MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_DUMMY, MicroOp.M_WRITE_EA),
					script(MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_DUMMY, MicroOp.M_READ_EA, MicroOp.M_WRITE_EA_DUMMY, MicroOp.M_WRITE_EA));
			case ABS:
				return byAccess(accessType,
					script(MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_FETCH_OPERAND_HI, MicroOp.M_READ_EA),
					script(MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_FETCH_OPERAND_HI, MicroOp.M_WRITE_EA),
					script(MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_FETCH_OPERAND_HI, MicroOp.M_READ_EA, MicroOp.M_WRITE_EA_DUMMY, MicroOp.M_WRITE_EA));
			case ABS_X:
			case ABS_Y:
				if( accessType==AccessType.AT_READ ) {
					return pageCrossed
						? script(MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_FETCH_OPERAND_HI, MicroOp.M_READ_DUMMY, MicroOp.M_READ_EA)
						: script(MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_FETCH_OPERAND_HI, MicroOp.M_READ_EA);
				}
				if( accessType==AccessType.AT_WRITE ) {
					return script(MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_FETCH_OPERAND_HI, MicroOp.M_READ_DUMMY, MicroOp.M_WRITE_EA);
				}
				if( accessType==AccessType.AT_RMW ) {
					return script(MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_FETCH_OPERAND_HI, MicroOp.M_READ_DUMMY, MicroOp.M_READ_EA, MicroOp.M_WRITE_EA_DUMMY, MicroOp.M_WRITE_EA);
				}
				break;
			case IND_X:
				return byAccess(accessType,
					script(MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_DUMMY, MicroOp.M_READ_ZP_PTR_LO, MicroOp.M_READ_ZP_PTR_HI, MicroOp.M_READ_EA),
					script(MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_DUMMY, MicroOp.M_READ_ZP_PTR_LO, MicroOp.M_READ_ZP_PTR_HI, MicroOp.M_WRITE_EA),
					script(MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_DUMMY, MicroOp.M_READ_ZP_PTR_LO, MicroOp.M_READ_ZP_PTR_HI, MicroOp.M_READ_EA, MicroOp.M_WRITE_EA_DUMMY, MicroOp.M_WRITE_EA));
			case IND_Y:
				if( accessType==AccessType.AT_READ ) {
					return pageCrossed
						? script(MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_ZP_PTR_LO, MicroOp.M_READ_ZP_PTR_HI, MicroOp.M_READ_DUMMY, MicroOp.M_READ_EA)
						: script(MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_ZP_PTR_LO, MicroOp.M_READ_ZP_PTR_HI, MicroOp.M_READ_EA);
				}
				if( accessType==AccessType.AT_WRITE ) {
					return script(MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_ZP_PTR_LO, MicroOp.M_READ_ZP_PTR_HI, MicroOp.M_READ_DUMMY, MicroOp.M_WRITE_EA);
				}
				break;
			case ZPG_IND:
				return byAccess(accessType,
					script(MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_ZP_PTR_LO, MicroOp.M_READ_ZP_PTR_HI, MicroOp.M_READ_EA),
					script(MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_ZP_PTR_LO, MicroOp.M_READ_ZP_PTR_HI, MicroOp.M_WRITE_EA),
					script(MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_ZP_PTR_LO, MicroOp.M_READ_ZP_PTR_HI, MicroOp.M_READ_EA, MicroOp.M_WRITE_EA_DUMMY, MicroOp.M_WRITE_EA));
			default:
				break;
		}
		int cycles = opcode.getCycleTime();
		MicroOp[] generic = new MicroOp[Math.max(1, cycles)];
		generic[0] = MicroOp.M_FETCH_OPCODE;
		for( int i = 1; i<generic.length; i++ )
			generic[i] = MicroOp.M_INTERNAL;
		return generic;
	}

	private static MicroOp[] byAccess(AccessType accessType, MicroOp[] read, MicroOp[] write, MicroOp[] rmw) {
		switch( accessType ) {
			case AT_READ:
				return read;
			case AT_WRITE:
				return write;
			case AT_RMW:
				return rmw;
			default:
				return script(MicroOp.M_FETCH_OPCODE, MicroOp.M_INTERNAL);
		}
	}

	static OpcodeMicroInstr microInstrForOpcodeByte(int opcodeByte) {
		return OPCODE_MICROCODE[opcodeByte & 0xff];
	}

	static OpcodeMicroInstr microInstrForOpcode(Opcode opcode) {
		if( opcode==null || opcode.getMachineCode()==null )
			return null;
		return microInstrForOpcodeByte(opcode.getMachineCode());
	}

	public static Cpu65c02Opcode opcodeForByte(int opcodeByte) {
		return new Cpu65c02Opcode(opcodeByte & 0xff);
	}

	public static Cpu65c02Opcode opcodeFor(Opcode opcode) {
		if( opcode==null || opcode.getMachineCode()==null )
			return null;
		return opcodeForByte(opcode.getMachineCode());
	}

	public static boolean usesMemoryDataRead(Opcode opcode) {
		OpcodeMicroInstr instr = microInstrForOpcode(opcode);
		return instr!=null && instr.usesMemoryDataRead();
	}

	public static int operandReadCycleOffset(Opcode opcode, boolean pageCrossed) {
		OpcodeMicroInstr instr = microInstrForOpcode(opcode);
		return instr==null ? -1 : instr.getOperandReadCycleOffset(pageCrossed);
	}

	public static int[] ldaOpcodeBytes() {
		return Arrays.copyOf(LDA_OPCODE_BYTES, LDA_OPCODE_BYTES.length);
	}
}
