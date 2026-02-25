package core.cpu.cpu8;

import core.cpu.cpu8.Cpu65c02Microcode.AccessType;
import core.cpu.cpu8.Cpu65c02Microcode.MicroOp;

/**
 * Byte-addressable opcode view that resolves through microcode tables.
 */
public final class Cpu65c02OpcodeView {

	private final int opcodeByte;

	public Cpu65c02OpcodeView(int opcodeByte) {
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
}
