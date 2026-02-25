package core.cpu.cpu8;

import java.util.Arrays;

/**
 * Byte-indexed microcode data store.
 *
 * This class intentionally avoids Opcode-class dependencies; it only exposes
 * per-opcode-byte micro-instruction descriptors.
 */
public final class Cpu65c02Microcode {

	public static final class CpuRegs {
		public int a;
		public int x;
		public int y;
		public int s;
		public int p;
		public int pc;
	}

	public static final class InternalRegs {
		public int operandLo;
		public int operandHi;
		public int effectiveAddress;
		public int zpPtrLo;
		public int zpPtrHi;
		public int temp;
		public int dataLatch;
		public int cycleIndex;
		public boolean pageCrossed;
	}

	public static final class MicroContext {
		public final CpuRegs cpu = new CpuRegs();
		public final InternalRegs internal = new InternalRegs();
	}

	public enum MicroOp {
		M_FETCH_OPCODE(0),
		M_FETCH_OPERAND_LO(1),
		M_FETCH_OPERAND_HI(2),
		M_READ_IMM_DATA(3),
		M_READ_ZP_PTR_LO(4),
		M_READ_ZP_PTR_HI(5),
		M_READ_DUMMY(6),
		M_READ_EA(7),
		M_WRITE_EA_DUMMY(8),
		M_WRITE_EA(9),
		M_INTERNAL(10);

		private final int code;

		MicroOp(int code) {
			this.code = code;
		}

		public int code() {
			return code;
		}
	}

	public enum AccessType {
		AT_NONE,
		AT_READ,
		AT_WRITE,
		AT_RMW
	}

	static final class MicroInstr {
		private final AccessType accessType;
		private final MicroOp[] noCrossScript;
		private final MicroOp[] crossScript;

		private MicroInstr(AccessType accessType, MicroOp[] noCrossScript, MicroOp[] crossScript) {
			this.accessType = accessType;
			this.noCrossScript = noCrossScript;
			this.crossScript = crossScript;
		}

		AccessType getAccessType() {
			return accessType;
		}

		MicroOp[] getCycleScript(boolean pageCrossed) {
			MicroOp[] src = pageCrossed ? crossScript : noCrossScript;
			return Arrays.copyOf(src, src.length);
		}

		boolean usesMemoryDataRead() {
			return indexOfFirstReadDataCycle(false)>=0 || indexOfFirstReadDataCycle(true)>=0;
		}

		int getOperandReadCycleOffset(boolean pageCrossed) {
			return indexOfFirstReadDataCycle(pageCrossed);
		}

		private int indexOfFirstReadDataCycle(boolean pageCrossed) {
			MicroOp[] script = pageCrossed ? crossScript : noCrossScript;
			for( int i = 0; i<script.length; i++ ) {
				MicroOp op = script[i];
				if( op==MicroOp.M_READ_IMM_DATA || op==MicroOp.M_READ_EA )
					return i;
			}
			return -1;
		}
	}

	private static final MicroInstr[] TABLE = buildTable();

	private Cpu65c02Microcode() {
	}

	private static MicroInstr[] buildTable() {
		MicroInstr[] table = new MicroInstr[256];
		MicroInstr defaultInstr = new MicroInstr(AccessType.AT_NONE,
				script(MicroOp.M_FETCH_OPCODE),
				script(MicroOp.M_FETCH_OPCODE));
		for( int i = 0; i<table.length; i++ )
			table[i] = defaultInstr;

		// LDA family from enum-owned microcode programs.
		for( Cpu65c02Opcode lda : Cpu65c02Opcode.ldaFamily() ) {
			Cpu65c02Opcode.MicroCycleProgram program = lda.microcode();
			set(table, lda.opcodeByte(), program.accessType(), program.noCrossScript(), program.crossScript());
		}
		for( Cpu65c02Opcode sta : Cpu65c02Opcode.staFamily() ) {
			Cpu65c02Opcode.MicroCycleProgram program = sta.microcode();
			set(table, sta.opcodeByte(), program.accessType(), program.noCrossScript(), program.crossScript());
		}

		// Representative RMW entry used by tests.
		set(table, 0xE6, AccessType.AT_RMW,
				script(MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_EA, MicroOp.M_WRITE_EA_DUMMY, MicroOp.M_WRITE_EA),
				script(MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_EA, MicroOp.M_WRITE_EA_DUMMY, MicroOp.M_WRITE_EA));

		return table;
	}

	private static void set(MicroInstr[] table, int opcodeByte, AccessType accessType, MicroOp[] noCross, MicroOp[] cross) {
		table[opcodeByte & 0xff] = new MicroInstr(accessType, noCross, cross);
	}

	private static MicroOp[] script(MicroOp... ops) {
		return ops;
	}

	static MicroInstr microInstrForByte(int opcodeByte) {
		return TABLE[opcodeByte & 0xff];
	}

	public static Cpu65c02OpcodeView opcodeForByte(int opcodeByte) {
		return new Cpu65c02OpcodeView(opcodeByte & 0xff);
	}

	public static MicroContext newContext() {
		return new MicroContext();
	}
}
