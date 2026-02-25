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
		M_READ_IMM,
		M_READ_ZPG,
		M_READ_ZPG_XY,
		M_READ_ABS,
		M_READ_ABS_XY,
		M_READ_IND_X,
		M_READ_IND_Y,
		M_READ_ZPG_IND
	}

	private Cpu65c02Microcode() {
	}

	public static boolean usesMemoryDataRead(OpcodeMnemonic mnemonic) {
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

	public static int operandReadCycleOffset(Opcode opcode, boolean pageCrossed) {
		if( opcode==null || opcode.getMnemonic()==null )
			return -1;
		AddressMode mode = opcode.getAddressMode();
		if( mode==null )
			return -1;

		switch( mode ) {
			case IMM:
				return 1;
			case ZPG:
				return 2;
			case ZPG_X:
			case ZPG_Y:
				return 3;
			case ABS:
				return 3;
			case ABS_X:
			case ABS_Y:
				return pageCrossed ? 4 : 3;
			case IND_X:
				return 5;
			case IND_Y:
				return pageCrossed ? 5 : 4;
			case ZPG_IND:
				return 4;
			default:
				return -1;
		}
	}

	public static MicroOp operandReadMicroOp(Opcode opcode) {
		if( opcode==null )
			return null;
		AddressMode mode = opcode.getAddressMode();
		if( mode==null )
			return null;
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
				return null;
		}
	}
}
