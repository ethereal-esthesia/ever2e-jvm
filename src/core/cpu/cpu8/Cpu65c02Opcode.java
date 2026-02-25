package core.cpu.cpu8;

import java.util.Arrays;

import core.cpu.cpu8.Cpu65c02Microcode.AccessType;
import core.cpu.cpu8.Cpu65c02Microcode.MicroOp;

/**
 * Opcode view that consumes microcode descriptors.
 */
public final class Cpu65c02Opcode {

	public enum LdaOpcode {
		LDA_IMM(0xA9),
		LDA_ZPG(0xA5),
		LDA_ZPG_X(0xB5),
		LDA_ABS(0xAD),
		LDA_ABS_X(0xBD),
		LDA_ABS_Y(0xB9),
		LDA_IND_X(0xA1),
		LDA_IND_Y(0xB1),
		LDA_IND(0xB2);

		private final int opcodeByte;

		LdaOpcode(int opcodeByte) {
			this.opcodeByte = opcodeByte & 0xff;
		}

		public int opcodeByte() {
			return opcodeByte;
		}
	}

	private static final int[] LDA_OPCODE_BYTES = buildLdaOpcodeBytes();
	private final int opcodeByte;

	public Cpu65c02Opcode(int opcodeByte) {
		this.opcodeByte = opcodeByte & 0xff;
	}

	public int getOpcodeByte() {
		return opcodeByte;
	}

	public Opcode getOpcode() {
		return Cpu65c02.OPCODE[opcodeByte];
	}

	public AccessType getAccessType() {
		Cpu65c02Microcode.MicroInstr instr = Cpu65c02Microcode.microInstrForByte(opcodeByte);
		return instr.getAccessType();
	}

	public MicroOp[] getExpectedMnemonicOrder(boolean pageCrossed) {
		Cpu65c02Microcode.MicroInstr instr = Cpu65c02Microcode.microInstrForByte(opcodeByte);
		return instr.getCycleScript(pageCrossed);
	}

	public boolean usesMemoryDataRead() {
		Cpu65c02Microcode.MicroInstr instr = Cpu65c02Microcode.microInstrForByte(opcodeByte);
		return instr.usesMemoryDataRead();
	}

	public int getOperandReadCycleOffset(boolean pageCrossed) {
		Cpu65c02Microcode.MicroInstr instr = Cpu65c02Microcode.microInstrForByte(opcodeByte);
		return instr.getOperandReadCycleOffset(pageCrossed);
	}

	public static int[] ldaOpcodeBytes() {
		return Arrays.copyOf(LDA_OPCODE_BYTES, LDA_OPCODE_BYTES.length);
	}

	private static int[] buildLdaOpcodeBytes() {
		LdaOpcode[] ops = LdaOpcode.values();
		int[] bytes = new int[ops.length];
		for( int i = 0; i<ops.length; i++ )
			bytes[i] = ops[i].opcodeByte();
		return bytes;
	}
}
