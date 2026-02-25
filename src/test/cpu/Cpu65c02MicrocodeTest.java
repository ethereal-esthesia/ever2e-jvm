package test.cpu;

import core.cpu.cpu8.Cpu65c02;
import core.cpu.cpu8.Cpu65c02Microcode;
import core.cpu.cpu8.Opcode;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
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
		assertTrue(Cpu65c02Microcode.usesMemoryDataRead(Cpu65c02.OPCODE[0xA9])); // LDA #imm
		assertTrue(Cpu65c02Microcode.usesMemoryDataRead(Cpu65c02.OPCODE[0x2C])); // BIT abs
		assertEquals(Cpu65c02Microcode.MicroOp.M_NONE, Cpu65c02Microcode.operandReadMicroOp(Cpu65c02.OPCODE[0x9D])); // STA abs,X
		assertEquals(Cpu65c02Microcode.MicroOp.M_NONE, Cpu65c02Microcode.operandReadMicroOp(Cpu65c02.OPCODE[0xD0])); // BNE rel
	}

	@Test
	public void microOpNamesUseMPrefix() {
		for( Cpu65c02Microcode.MicroOp op : Cpu65c02Microcode.MicroOp.values() )
			assertTrue(op.name().startsWith("M_"));
	}

	@Test
	public void everyOpcodeHasMicroInstrEntry() {
		for( int op = 0; op<256; op++ ) {
			Cpu65c02Microcode.OpcodeMicroInstr instr = Cpu65c02Microcode.microInstrForOpcodeByte(op);
			assertTrue(instr!=null);
			assertEquals(op, instr.getOpcodeByte());
			assertTrue(instr.getMicroOp()!=null);
		}
	}
}
