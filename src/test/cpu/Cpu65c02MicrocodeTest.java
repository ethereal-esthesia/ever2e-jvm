package test.cpu;

import org.junit.Test;

import core.cpu.cpu8.Cpu65c02Opcode;
import core.cpu.cpu8.Cpu65c02OpcodeView;
import core.cpu.cpu8.Cpu65c02Microcode;
import core.cpu.cpu8.Cpu65c02Microcode.MicroOp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;
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

	private static final class StaExpect {
		final int opcode;
		final MicroOp[] script;

		StaExpect(int opcode, MicroOp[] script) {
			this.opcode = opcode;
			this.script = script;
		}
	}

	private static final class IncExpect {
		final int opcode;
		final MicroOp[] script;

		IncExpect(int opcode, MicroOp[] script) {
			this.opcode = opcode;
			this.script = script;
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

	private static final StaExpect[] STA_EXPECTATIONS = new StaExpect[] {
			new StaExpect(0x85, new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_WRITE_EA }),
			new StaExpect(0x95, new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_DUMMY, MicroOp.M_WRITE_EA }),
			new StaExpect(0x8D, new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_FETCH_OPERAND_HI, MicroOp.M_WRITE_EA }),
			new StaExpect(0x9D, new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_FETCH_OPERAND_HI, MicroOp.M_READ_DUMMY, MicroOp.M_WRITE_EA }),
			new StaExpect(0x99, new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_FETCH_OPERAND_HI, MicroOp.M_READ_DUMMY, MicroOp.M_WRITE_EA }),
			new StaExpect(0x81, new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_DUMMY, MicroOp.M_READ_ZP_PTR_LO, MicroOp.M_READ_ZP_PTR_HI, MicroOp.M_WRITE_EA }),
			new StaExpect(0x91, new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_ZP_PTR_LO, MicroOp.M_READ_ZP_PTR_HI, MicroOp.M_READ_DUMMY, MicroOp.M_WRITE_EA }),
			new StaExpect(0x92, new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_ZP_PTR_LO, MicroOp.M_READ_ZP_PTR_HI, MicroOp.M_WRITE_EA }),
	};

	private static final IncExpect[] INC_EXPECTATIONS = new IncExpect[] {
			new IncExpect(0xE6, new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_EA, MicroOp.M_WRITE_EA_DUMMY, MicroOp.M_WRITE_EA }),
			new IncExpect(0xF6, new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_DUMMY, MicroOp.M_READ_EA, MicroOp.M_WRITE_EA_DUMMY, MicroOp.M_WRITE_EA }),
			new IncExpect(0xEE, new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_FETCH_OPERAND_HI, MicroOp.M_READ_EA, MicroOp.M_WRITE_EA_DUMMY, MicroOp.M_WRITE_EA }),
			new IncExpect(0xFE, new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_FETCH_OPERAND_HI, MicroOp.M_READ_DUMMY, MicroOp.M_READ_EA, MicroOp.M_WRITE_EA_DUMMY, MicroOp.M_WRITE_EA }),
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
	public void ldaOpcodeEnumMatchesOpcodeByteList() {
		Cpu65c02Opcode[] ldaOps = Cpu65c02Opcode.ldaFamily().toArray(new Cpu65c02Opcode[0]);
		int[] ldaBytes = Cpu65c02Opcode.ldaOpcodeBytes();
		assertEquals(ldaOps.length, ldaBytes.length);
		for( int i = 0; i<ldaOps.length; i++ )
			assertEquals(ldaOps[i].opcodeByte(), ldaBytes[i]);
	}

	@Test
	public void ldaOpcodeEnumProgramsDriveResolvedMicrocode() {
		for( Cpu65c02Opcode lda : Cpu65c02Opcode.ldaFamily() ) {
			Cpu65c02OpcodeView entry = Cpu65c02Microcode.opcodeForByte(lda.opcodeByte());
			assertEquals(lda.microcode().accessType(), entry.getAccessType());
			assertArrayEquals(lda.microcode().noCrossScript(), entry.getExpectedMnemonicOrder(false));
			assertArrayEquals(lda.microcode().crossScript(), entry.getExpectedMnemonicOrder(true));
		}
	}

	@Test
	public void staOpcodeEnumMatchesOpcodeByteList() {
		Cpu65c02Opcode[] staOps = Cpu65c02Opcode.staFamily().toArray(new Cpu65c02Opcode[0]);
		int[] staBytes = Cpu65c02Opcode.staOpcodeBytes();
		assertEquals(staOps.length, staBytes.length);
		for( int i = 0; i<staOps.length; i++ )
			assertEquals(staOps[i].opcodeByte(), staBytes[i]);
	}

	@Test
	public void staOpcodeEnumProgramsDriveResolvedMicrocode() {
		for( Cpu65c02Opcode sta : Cpu65c02Opcode.staFamily() ) {
			Cpu65c02OpcodeView entry = Cpu65c02Microcode.opcodeForByte(sta.opcodeByte());
			assertEquals(sta.microcode().accessType(), entry.getAccessType());
			assertArrayEquals(sta.microcode().noCrossScript(), entry.getExpectedMnemonicOrder(false));
			assertArrayEquals(sta.microcode().crossScript(), entry.getExpectedMnemonicOrder(true));
		}
	}

	@Test
	public void incOpcodeEnumMatchesOpcodeByteList() {
		Cpu65c02Opcode[] incOps = Cpu65c02Opcode.incFamily().toArray(new Cpu65c02Opcode[0]);
		int[] incBytes = Cpu65c02Opcode.incOpcodeBytes();
		assertEquals(incOps.length, incBytes.length);
		for( int i = 0; i<incOps.length; i++ )
			assertEquals(incOps[i].opcodeByte(), incBytes[i]);
	}

	@Test
	public void incOpcodeEnumProgramsDriveResolvedMicrocode() {
		for( Cpu65c02Opcode inc : Cpu65c02Opcode.incFamily() ) {
			Cpu65c02OpcodeView entry = Cpu65c02Microcode.opcodeForByte(inc.opcodeByte());
			assertEquals(inc.microcode().accessType(), entry.getAccessType());
			assertArrayEquals(inc.microcode().noCrossScript(), entry.getExpectedMnemonicOrder(false));
			assertArrayEquals(inc.microcode().crossScript(), entry.getExpectedMnemonicOrder(true));
		}
	}

	@Test
	public void opcodeByteRoundTripsToEnum() {
		for( Cpu65c02Opcode lda : Cpu65c02Opcode.ldaFamily() )
			assertEquals(lda, Cpu65c02Opcode.fromOpcodeByte(lda.opcodeByte()));
		for( Cpu65c02Opcode sta : Cpu65c02Opcode.staFamily() )
			assertEquals(sta, Cpu65c02Opcode.fromOpcodeByte(sta.opcodeByte()));
		for( Cpu65c02Opcode inc : Cpu65c02Opcode.incFamily() )
			assertEquals(inc, Cpu65c02Opcode.fromOpcodeByte(inc.opcodeByte()));
	}

	@Test
	public void ldaAbsoluteCycleScript() {
		Cpu65c02OpcodeView instr = Cpu65c02Microcode.opcodeForByte(0xAD); // LDA abs
		assertEquals(MicroOp.M_FETCH_OPCODE, instr.getExpectedMnemonicOrder(false)[0]);
		assertEquals(MicroOp.M_FETCH_OPERAND_LO, instr.getExpectedMnemonicOrder(false)[1]);
		assertEquals(MicroOp.M_FETCH_OPERAND_HI, instr.getExpectedMnemonicOrder(false)[2]);
		assertEquals(MicroOp.M_READ_EA, instr.getExpectedMnemonicOrder(false)[3]);
		assertEquals(3, instr.getOperandReadCycleOffset(false));
	}

	@Test
	public void ldaAbsoluteXCrossAddsDummyRead() {
		Cpu65c02OpcodeView instr = Cpu65c02Microcode.opcodeForByte(0xBD); // LDA abs,X
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
		Cpu65c02OpcodeView instr = Cpu65c02Microcode.opcodeForByte(0x8D); // STA abs
		MicroOp[] script = instr.getExpectedMnemonicOrder(false);
		assertEquals(MicroOp.M_FETCH_OPCODE, script[0]);
		assertEquals(MicroOp.M_FETCH_OPERAND_LO, script[1]);
		assertEquals(MicroOp.M_FETCH_OPERAND_HI, script[2]);
		assertEquals(MicroOp.M_WRITE_EA, script[3]);
		assertEquals(-1, instr.getOperandReadCycleOffset(false));
	}

	@Test
	public void incZeroPageIsReadModifyWrite() {
		Cpu65c02OpcodeView instr = Cpu65c02Microcode.opcodeForByte(0xE6); // INC zpg
		MicroOp[] script = instr.getExpectedMnemonicOrder(false);
		assertEquals(Cpu65c02Microcode.AccessType.AT_RMW, instr.getAccessType());
		assertEquals(MicroOp.M_READ_EA, script[2]);
		assertEquals(MicroOp.M_WRITE_EA_DUMMY, script[3]);
		assertEquals(MicroOp.M_WRITE_EA, script[4]);
	}

	@Test
	public void everyOpcodeHasMicroInstrEntry() {
		for( int op = 0; op<256; op++ ) {
			Cpu65c02OpcodeView instr = Cpu65c02Microcode.opcodeForByte(op);
			assertTrue(instr!=null);
			assertEquals(op, instr.getOpcodeByte());
			assertTrue(instr.getExpectedMnemonicOrder(false).length>=1);
			assertEquals(MicroOp.M_FETCH_OPCODE, instr.getExpectedMnemonicOrder(false)[0]);
		}
	}

	@Test
	public void allLdaOpcodesHaveExpectedMicrocodeOrder() {
		for( LdaExpect expect : LDA_EXPECTATIONS ) {
			Cpu65c02OpcodeView entry = Cpu65c02Microcode.opcodeForByte(expect.opcode);
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
	public void allStaOpcodesHaveExpectedMicrocodeOrder() {
		for( StaExpect expect : STA_EXPECTATIONS ) {
			Cpu65c02OpcodeView entry = Cpu65c02Microcode.opcodeForByte(expect.opcode);
			assertEquals(expect.opcode, entry.getOpcodeByte());
			assertEquals(expect.script.length, entry.getExpectedMnemonicOrder(false).length);
			assertEquals(expect.script.length, entry.getExpectedMnemonicOrder(true).length);
			assertArrayEquals(expect.script, entry.getExpectedMnemonicOrder(false));
			assertArrayEquals(expect.script, entry.getExpectedMnemonicOrder(true));
			assertEquals(-1, entry.getOperandReadCycleOffset(false));
		}
	}

	@Test
	public void allIncOpcodesHaveExpectedMicrocodeOrder() {
		for( IncExpect expect : INC_EXPECTATIONS ) {
			Cpu65c02OpcodeView entry = Cpu65c02Microcode.opcodeForByte(expect.opcode);
			assertEquals(expect.opcode, entry.getOpcodeByte());
			assertEquals(expect.script.length, entry.getExpectedMnemonicOrder(false).length);
			assertEquals(expect.script.length, entry.getExpectedMnemonicOrder(true).length);
			assertArrayEquals(expect.script, entry.getExpectedMnemonicOrder(false));
			assertArrayEquals(expect.script, entry.getExpectedMnemonicOrder(true));
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
