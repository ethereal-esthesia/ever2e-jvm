package core.cpu.cpu8;

import java.util.Arrays;

import core.cpu.cpu8.Cpu65c02Microcode.AccessType;
import core.cpu.cpu8.Cpu65c02Microcode.MicroOp;

/**
 * Opcode view that consumes microcode descriptors.
 */
public final class Cpu65c02Opcode {

	private static final int[] LDA_OPCODE_BYTES = new int[] { 0xA9, 0xA5, 0xB5, 0xAD, 0xBD, 0xB9, 0xA1, 0xB1, 0xB2 };
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
}
