package test.cpu;

import core.cpu.cpu8.Cpu65c02;
import core.cpu.cpu8.Cpu65c02Microcode;
import core.cpu.cpu8.Opcode;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class Cpu65c02MicrocodeTest {

	@Test
	public void absReadCycleOffset() {
		Opcode ldaAbs = Cpu65c02.OPCODE[0xAD];
		assertEquals(3, Cpu65c02Microcode.operandReadCycleOffset(ldaAbs, false));
		assertEquals(3, Cpu65c02Microcode.operandReadCycleOffset(ldaAbs, true));
	}

	@Test
	public void absIndexedReadCycleOffset() {
		Opcode ldaAbsX = Cpu65c02.OPCODE[0xBD];
		assertEquals(3, Cpu65c02Microcode.operandReadCycleOffset(ldaAbsX, false));
		assertEquals(4, Cpu65c02Microcode.operandReadCycleOffset(ldaAbsX, true));

		Opcode ldaAbsY = Cpu65c02.OPCODE[0xB9];
		assertEquals(3, Cpu65c02Microcode.operandReadCycleOffset(ldaAbsY, false));
		assertEquals(4, Cpu65c02Microcode.operandReadCycleOffset(ldaAbsY, true));
	}

	@Test
	public void indirectReadCycleOffset() {
		Opcode ldaIndX = Cpu65c02.OPCODE[0xA1];
		assertEquals(5, Cpu65c02Microcode.operandReadCycleOffset(ldaIndX, false));
		assertEquals(5, Cpu65c02Microcode.operandReadCycleOffset(ldaIndX, true));

		Opcode ldaIndY = Cpu65c02.OPCODE[0xB1];
		assertEquals(4, Cpu65c02Microcode.operandReadCycleOffset(ldaIndY, false));
		assertEquals(5, Cpu65c02Microcode.operandReadCycleOffset(ldaIndY, true));
	}

	@Test
	public void readMnemonicsClassification() {
		assertTrue(Cpu65c02Microcode.usesMemoryDataRead(Cpu65c02.OpcodeMnemonic.LDA));
		assertTrue(Cpu65c02Microcode.usesMemoryDataRead(Cpu65c02.OpcodeMnemonic.BIT));
		assertFalse(Cpu65c02Microcode.usesMemoryDataRead(Cpu65c02.OpcodeMnemonic.STA));
		assertFalse(Cpu65c02Microcode.usesMemoryDataRead(Cpu65c02.OpcodeMnemonic.BNE));
	}
}
