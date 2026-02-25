package test.cpu;

import org.junit.Test;

import core.cpu.cpu8.Cpu65c02Opcode;
import core.cpu.cpu8.Cpu65c02Microcode;
import core.cpu.cpu8.Cpu65c02Microcode.MicroOp;

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
		Cpu65c02Opcode instr = Cpu65c02Microcode.opcodeForByte(0xAD); // LDA abs
		assertEquals(MicroOp.M_FETCH_OPCODE, instr.getExpectedMnemonicOrder(false)[0]);
		assertEquals(MicroOp.M_FETCH_OPERAND_LO, instr.getExpectedMnemonicOrder(false)[1]);
		assertEquals(MicroOp.M_FETCH_OPERAND_HI, instr.getExpectedMnemonicOrder(false)[2]);
		assertEquals(MicroOp.M_READ_EA, instr.getExpectedMnemonicOrder(false)[3]);
		assertEquals(3, instr.getOperandReadCycleOffset(false));
	}

	@Test
	public void ldaAbsoluteXCrossAddsDummyRead() {
		Cpu65c02Opcode instr = Cpu65c02Microcode.opcodeForByte(0xBD); // LDA abs,X
		MicroOp[] noCross = instr.getExpectedMnemonicOrder(false);
		MicroOp[] cross = instr.getExpectedMnemonicOrder(true);
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
		Cpu65c02Opcode instr = Cpu65c02Microcode.opcodeForByte(0x8D); // STA abs
		MicroOp[] script = instr.getExpectedMnemonicOrder(false);
		assertEquals(MicroOp.M_FETCH_OPCODE, script[0]);
		assertEquals(MicroOp.M_FETCH_OPERAND_LO, script[1]);
		assertEquals(MicroOp.M_FETCH_OPERAND_HI, script[2]);
		assertEquals(MicroOp.M_WRITE_EA, script[3]);
		assertEquals(-1, instr.getOperandReadCycleOffset(false));
	}

	@Test
	public void incZeroPageIsReadModifyWrite() {
		Cpu65c02Opcode instr = Cpu65c02Microcode.opcodeForByte(0xE6); // INC zpg
		MicroOp[] script = instr.getExpectedMnemonicOrder(false);
		assertEquals(MicroOp.M_READ_EA, script[2]);
		assertEquals(MicroOp.M_WRITE_EA_DUMMY, script[3]);
		assertEquals(MicroOp.M_WRITE_EA, script[4]);
	}

	@Test
	public void everyOpcodeHasMicroInstrEntry() {
		for( int op = 0; op<256; op++ ) {
			Cpu65c02Opcode instr = Cpu65c02Microcode.opcodeForByte(op);
			assertTrue(instr!=null);
			assertEquals(op, instr.getOpcodeByte());
			assertTrue(instr.getExpectedMnemonicOrder(false).length>=1);
			assertEquals(MicroOp.M_FETCH_OPCODE, instr.getExpectedMnemonicOrder(false)[0]);
		}
	}

	@Test
	public void allLdaOpcodesHaveExpectedMicrocodeOrder() {
		for( int op : Cpu65c02Microcode.ldaOpcodeBytes() ) {
			Cpu65c02Opcode entry = Cpu65c02Microcode.opcodeForByte(op);
			MicroOp[] noCross = entry.getExpectedMnemonicOrder(false);
			assertEquals(MicroOp.M_FETCH_OPCODE, noCross[0]);
			assertTrue(entry.usesMemoryDataRead());
			switch( op ) {
				case 0xA9: // LDA #imm
					assertEquals(MicroOp.M_READ_IMM_DATA, noCross[1]);
					assertEquals(1, entry.getOperandReadCycleOffset(false));
					break;
				case 0xA5: // LDA zpg
					assertEquals(MicroOp.M_FETCH_OPERAND_LO, noCross[1]);
					assertEquals(MicroOp.M_READ_EA, noCross[2]);
					assertEquals(2, entry.getOperandReadCycleOffset(false));
					break;
				case 0xB5: // LDA zpg,X
					assertEquals(MicroOp.M_READ_DUMMY, noCross[2]);
					assertEquals(MicroOp.M_READ_EA, noCross[3]);
					assertEquals(3, entry.getOperandReadCycleOffset(false));
					break;
				case 0xAD: // LDA abs
					assertEquals(MicroOp.M_FETCH_OPERAND_HI, noCross[2]);
					assertEquals(MicroOp.M_READ_EA, noCross[3]);
					assertEquals(3, entry.getOperandReadCycleOffset(false));
					break;
				case 0xBD: // LDA abs,X
				case 0xB9: // LDA abs,Y
					assertEquals(MicroOp.M_READ_EA, noCross[3]);
					assertEquals(3, entry.getOperandReadCycleOffset(false));
					assertEquals(MicroOp.M_READ_DUMMY, entry.getExpectedMnemonicOrder(true)[3]);
					assertEquals(MicroOp.M_READ_EA, entry.getExpectedMnemonicOrder(true)[4]);
					assertEquals(4, entry.getOperandReadCycleOffset(true));
					break;
				case 0xA1: // LDA (zpg,X)
					assertEquals(MicroOp.M_READ_ZP_PTR_LO, noCross[3]);
					assertEquals(MicroOp.M_READ_ZP_PTR_HI, noCross[4]);
					assertEquals(MicroOp.M_READ_EA, noCross[5]);
					assertEquals(5, entry.getOperandReadCycleOffset(false));
					break;
				case 0xB1: // LDA (zpg),Y
					assertEquals(MicroOp.M_READ_ZP_PTR_LO, noCross[2]);
					assertEquals(MicroOp.M_READ_ZP_PTR_HI, noCross[3]);
					assertEquals(MicroOp.M_READ_EA, noCross[4]);
					assertEquals(4, entry.getOperandReadCycleOffset(false));
					assertEquals(MicroOp.M_READ_DUMMY, entry.getExpectedMnemonicOrder(true)[4]);
					assertEquals(MicroOp.M_READ_EA, entry.getExpectedMnemonicOrder(true)[5]);
					assertEquals(5, entry.getOperandReadCycleOffset(true));
					break;
				case 0xB2: // LDA (zpg)
					assertEquals(MicroOp.M_READ_ZP_PTR_LO, noCross[2]);
					assertEquals(MicroOp.M_READ_ZP_PTR_HI, noCross[3]);
					assertEquals(MicroOp.M_READ_EA, noCross[4]);
					assertEquals(4, entry.getOperandReadCycleOffset(false));
					break;
				default:
					throw new AssertionError("Unexpected LDA opcode in table: " + op);
			}
		}
	}

	@Test
	public void microContextExposesCpuAndInternalRegisters() {
		Cpu65c02Microcode.MicroContext ctx = Cpu65c02Microcode.newContext();
		assertEquals(0, ctx.cpu.a);
		assertEquals(0, ctx.cpu.x);
		assertEquals(0, ctx.cpu.y);
		assertEquals(0, ctx.cpu.pc);
		assertEquals(0, ctx.internal.effectiveAddress);
		assertEquals(0, ctx.internal.cycleIndex);
		assertTrue(!ctx.internal.pageCrossed);
		ctx.cpu.a = 0x42;
		ctx.internal.effectiveAddress = 0xC054;
		ctx.internal.pageCrossed = true;
		assertEquals(0x42, ctx.cpu.a);
		assertEquals(0xC054, ctx.internal.effectiveAddress);
		assertTrue(ctx.internal.pageCrossed);
	}
}
