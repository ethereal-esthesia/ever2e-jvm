package test.cpu;

import org.junit.Test;

import core.cpu.cpu8.Cpu65c02Opcode;
import core.cpu.cpu8.Cpu65c02Microcode;
import core.cpu.cpu8.Cpu65c02Microcode.MicroOp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class Cpu65c02MicrocodeTest {

	private static final class LdaExpect {
		final int opcode;
		final MicroOp[] noCross;
		final MicroOp[] cross;
		final int readOffsetNoCross;
		final int readOffsetCross;

		LdaExpect(int opcode, MicroOp[] noCross, MicroOp[] cross, int readOffsetNoCross, int readOffsetCross) {
			this.opcode = opcode;
			this.noCross = noCross;
			this.cross = cross;
			this.readOffsetNoCross = readOffsetNoCross;
			this.readOffsetCross = readOffsetCross;
		}
	}

	private static final LdaExpect[] LDA_EXPECTATIONS = new LdaExpect[] {
			new LdaExpect(0xA9,
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_READ_IMM_DATA },
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_READ_IMM_DATA },
					1, 1),
			new LdaExpect(0xA5,
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_EA },
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_EA },
					2, 2),
			new LdaExpect(0xB5,
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_DUMMY, MicroOp.M_READ_EA },
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_DUMMY, MicroOp.M_READ_EA },
					3, 3),
			new LdaExpect(0xAD,
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_FETCH_OPERAND_HI, MicroOp.M_READ_EA },
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_FETCH_OPERAND_HI, MicroOp.M_READ_EA },
					3, 3),
			new LdaExpect(0xBD,
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_FETCH_OPERAND_HI, MicroOp.M_READ_EA },
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_FETCH_OPERAND_HI, MicroOp.M_READ_DUMMY, MicroOp.M_READ_EA },
					3, 4),
			new LdaExpect(0xB9,
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_FETCH_OPERAND_HI, MicroOp.M_READ_EA },
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_FETCH_OPERAND_HI, MicroOp.M_READ_DUMMY, MicroOp.M_READ_EA },
					3, 4),
			new LdaExpect(0xA1,
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_DUMMY, MicroOp.M_READ_ZP_PTR_LO, MicroOp.M_READ_ZP_PTR_HI, MicroOp.M_READ_EA },
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_DUMMY, MicroOp.M_READ_ZP_PTR_LO, MicroOp.M_READ_ZP_PTR_HI, MicroOp.M_READ_EA },
					5, 5),
			new LdaExpect(0xB1,
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_ZP_PTR_LO, MicroOp.M_READ_ZP_PTR_HI, MicroOp.M_READ_EA },
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_ZP_PTR_LO, MicroOp.M_READ_ZP_PTR_HI, MicroOp.M_READ_DUMMY, MicroOp.M_READ_EA },
					4, 5),
			new LdaExpect(0xB2,
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_ZP_PTR_LO, MicroOp.M_READ_ZP_PTR_HI, MicroOp.M_READ_EA },
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_ZP_PTR_LO, MicroOp.M_READ_ZP_PTR_HI, MicroOp.M_READ_EA },
					4, 4),
	};

	@Test
	public void microOpNamesUseMPrefix() {
		for( MicroOp op : MicroOp.values() )
			assertTrue(op.name().startsWith("M_"));
	}

	@Test
	public void microOpsHaveStableSequentialCodes() {
		MicroOp[] ops = MicroOp.values();
		for( int i = 0; i<ops.length; i++ )
			assertEquals(i, ops[i].code());
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
		for( LdaExpect expect : LDA_EXPECTATIONS ) {
			Cpu65c02Opcode entry = Cpu65c02Microcode.opcodeForByte(expect.opcode);
			assertTrue(entry.usesMemoryDataRead());
			assertEquals(expect.opcode, entry.getOpcodeByte());
			assertEquals(expect.noCross.length, entry.getExpectedMnemonicOrder(false).length);
			assertEquals(expect.cross.length, entry.getExpectedMnemonicOrder(true).length);
			for( int i = 0; i<expect.noCross.length; i++ )
				assertEquals(expect.noCross[i], entry.getExpectedMnemonicOrder(false)[i]);
			for( int i = 0; i<expect.cross.length; i++ )
				assertEquals(expect.cross[i], entry.getExpectedMnemonicOrder(true)[i]);
			assertEquals(expect.readOffsetNoCross, entry.getOperandReadCycleOffset(false));
			assertEquals(expect.readOffsetCross, entry.getOperandReadCycleOffset(true));
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
