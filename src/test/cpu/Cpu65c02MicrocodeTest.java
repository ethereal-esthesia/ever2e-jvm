package test.cpu;

import org.junit.Test;

import core.cpu.cpu8.Cpu65c02Microcode;
import core.cpu.cpu8.Cpu65c02Microcode.MicroOp;
import core.cpu.cpu8.Cpu65c02Microcode.OpcodeMicroInstr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class Cpu65c02MicrocodeTest {

	@Test
	public void microOpNamesUseMPrefix() {
		for( MicroOp op : MicroOp.values() )
			assertTrue(op.name().startsWith("M_"));
	}

	@Test
	public void ldaAbsoluteCycleScript() {
		OpcodeMicroInstr instr = Cpu65c02Microcode.microInstrForOpcodeByte(0xAD); // LDA abs
		assertEquals(MicroOp.M_FETCH_OPCODE, instr.getCycleScript(false)[0]);
		assertEquals(MicroOp.M_FETCH_OPERAND_LO, instr.getCycleScript(false)[1]);
		assertEquals(MicroOp.M_FETCH_OPERAND_HI, instr.getCycleScript(false)[2]);
		assertEquals(MicroOp.M_READ_EA, instr.getCycleScript(false)[3]);
		assertEquals(3, instr.getOperandReadCycleOffset(false));
	}

	@Test
	public void ldaAbsoluteXCrossAddsDummyRead() {
		OpcodeMicroInstr instr = Cpu65c02Microcode.microInstrForOpcodeByte(0xBD); // LDA abs,X
		MicroOp[] noCross = instr.getCycleScript(false);
		MicroOp[] cross = instr.getCycleScript(true);
		assertEquals(4, noCross.length);
		assertEquals(5, cross.length);
		assertEquals(MicroOp.M_READ_EA, noCross[3]);
		assertEquals(MicroOp.M_READ_DUMMY, cross[3]);
		assertEquals(MicroOp.M_READ_EA, cross[4]);
		assertEquals(3, instr.getOperandReadCycleOffset(false));
		assertEquals(4, instr.getOperandReadCycleOffset(true));
	}

	@Test
	public void staAbsoluteHasWriteCycle() {
		OpcodeMicroInstr instr = Cpu65c02Microcode.microInstrForOpcodeByte(0x8D); // STA abs
		MicroOp[] script = instr.getCycleScript(false);
		assertEquals(MicroOp.M_FETCH_OPCODE, script[0]);
		assertEquals(MicroOp.M_FETCH_OPERAND_LO, script[1]);
		assertEquals(MicroOp.M_FETCH_OPERAND_HI, script[2]);
		assertEquals(MicroOp.M_WRITE_EA, script[3]);
		assertEquals(-1, instr.getOperandReadCycleOffset(false));
	}

	@Test
	public void incZeroPageIsReadModifyWrite() {
		OpcodeMicroInstr instr = Cpu65c02Microcode.microInstrForOpcodeByte(0xE6); // INC zpg
		MicroOp[] script = instr.getCycleScript(false);
		assertEquals(MicroOp.M_READ_EA, script[2]);
		assertEquals(MicroOp.M_WRITE_EA_DUMMY, script[3]);
		assertEquals(MicroOp.M_WRITE_EA, script[4]);
	}

	@Test
	public void everyOpcodeHasMicroInstrEntry() {
		for( int op = 0; op<256; op++ ) {
			OpcodeMicroInstr instr = Cpu65c02Microcode.microInstrForOpcodeByte(op);
			assertTrue(instr!=null);
			assertEquals(op, instr.getOpcodeByte());
			assertTrue(instr.getCycleScript(false).length>=1);
			assertEquals(MicroOp.M_FETCH_OPCODE, instr.getCycleScript(false)[0]);
		}
	}
}
