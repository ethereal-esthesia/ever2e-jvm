package core.cpu.cpu8;

import java.util.Arrays;

import core.cpu.cpu8.Cpu65c02Microcode.AccessType;
import core.cpu.cpu8.Cpu65c02Microcode.MicroOp;

/**
 * Enum-backed opcode definitions for explicit microcoded op families.
 */
public enum Cpu65c02Opcode {

	LDA_IMM(0xA9, MicroCycleProgram.readShared(cycles(MicroOp.M_FETCH_OPCODE, MicroOp.M_READ_IMM_DATA))),
	LDA_ZPG(0xA5, MicroCycleProgram.readShared(cycles(MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_EA))),
	LDA_ZPG_X(0xB5, MicroCycleProgram.readShared(cycles(MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_DUMMY, MicroOp.M_READ_EA))),
	LDA_ABS(0xAD, MicroCycleProgram.readShared(cycles(MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_FETCH_OPERAND_HI, MicroOp.M_READ_EA))),
	LDA_ABS_X(0xBD, MicroCycleProgram.readSplit(
			cycles(MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_FETCH_OPERAND_HI, MicroOp.M_READ_EA),
			cycles(MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_FETCH_OPERAND_HI, MicroOp.M_READ_DUMMY, MicroOp.M_READ_EA))),
	LDA_ABS_Y(0xB9, MicroCycleProgram.readSplit(
			cycles(MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_FETCH_OPERAND_HI, MicroOp.M_READ_EA),
			cycles(MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_FETCH_OPERAND_HI, MicroOp.M_READ_DUMMY, MicroOp.M_READ_EA))),
	LDA_IND_X(0xA1, MicroCycleProgram.readShared(cycles(MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_DUMMY, MicroOp.M_READ_ZP_PTR_LO, MicroOp.M_READ_ZP_PTR_HI, MicroOp.M_READ_EA))),
	LDA_IND_Y(0xB1, MicroCycleProgram.readSplit(
			cycles(MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_ZP_PTR_LO, MicroOp.M_READ_ZP_PTR_HI, MicroOp.M_READ_EA),
			cycles(MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_ZP_PTR_LO, MicroOp.M_READ_ZP_PTR_HI, MicroOp.M_READ_DUMMY, MicroOp.M_READ_EA))),
	LDA_IND(0xB2, MicroCycleProgram.readShared(cycles(MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_ZP_PTR_LO, MicroOp.M_READ_ZP_PTR_HI, MicroOp.M_READ_EA)));

	private final int opcodeByte;
	private final MicroCycleProgram microcode;

	Cpu65c02Opcode(int opcodeByte, MicroCycleProgram microcode) {
		this.opcodeByte = opcodeByte & 0xff;
		this.microcode = microcode;
	}

	public int opcodeByte() {
		return opcodeByte;
	}

	public MicroCycleProgram microcode() {
		return microcode;
	}

	public static int[] ldaOpcodeBytes() {
		return buildLdaOpcodeBytes();
	}

	private static int[] buildLdaOpcodeBytes() {
		Cpu65c02Opcode[] ops = Cpu65c02Opcode.values();
		int[] bytes = new int[ops.length];
		for( int i = 0; i<ops.length; i++ )
			bytes[i] = ops[i].opcodeByte();
		return bytes;
	}

	private static MicroOp[] cycles(MicroOp... ops) {
		return ops;
	}

	public static final class MicroCycleProgram {
		private final AccessType accessType;
		private final MicroOp[] noCrossScript;
		private final MicroOp[] crossScript;

		private MicroCycleProgram(AccessType accessType, MicroOp[] noCrossScript, MicroOp[] crossScript) {
			this.accessType = accessType;
			this.noCrossScript = Arrays.copyOf(noCrossScript, noCrossScript.length);
			this.crossScript = Arrays.copyOf(crossScript, crossScript.length);
		}

		public static MicroCycleProgram readShared(MicroOp... script) {
			return new MicroCycleProgram(AccessType.AT_READ, script, script);
		}

		public static MicroCycleProgram readSplit(MicroOp[] noCrossScript, MicroOp[] crossScript) {
			return new MicroCycleProgram(AccessType.AT_READ, noCrossScript, crossScript);
		}

		public AccessType accessType() {
			return accessType;
		}

		public MicroOp[] noCrossScript() {
			return Arrays.copyOf(noCrossScript, noCrossScript.length);
		}

		public MicroOp[] crossScript() {
			return Arrays.copyOf(crossScript, crossScript.length);
		}
	}
}
