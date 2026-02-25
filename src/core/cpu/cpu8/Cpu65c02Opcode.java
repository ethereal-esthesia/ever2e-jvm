package core.cpu.cpu8;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;

import core.cpu.cpu8.Cpu65c02Microcode.AccessType;
import core.cpu.cpu8.Cpu65c02Microcode.MicroOp;

/**
 * Enum-backed opcode definitions for explicit microcoded op families.
 */
public enum Cpu65c02Opcode {

	LDA_IMM(0xA9, MicroCycleProgram.readShared(cycles(MicroOp.M_FETCH_OPCODE, MicroOp.M_READ_IMM_DATA))),
	LDA_ZPG(0xA5, MicroCycleProgram.readShared(cycles(MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_EA))),
	LDA_ZPG_X(0xB5, MicroCycleProgram.readShared(cycles(MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_DUMMY, MicroOp.M_READ_EA))),
	LDA_ABS(0xAD, MicroCycleProgram.readShared(cycles(MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_FETCH_OPERAND_HI, MicroOp.M_READ_EA))),
	LDA_ABS_X(0xBD, MicroCycleProgram.readSplit(
			cycles(MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_FETCH_OPERAND_HI, MicroOp.M_READ_EA),
			cycles(MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_FETCH_OPERAND_HI, MicroOp.M_READ_DUMMY, MicroOp.M_READ_EA))),
	LDA_ABS_Y(0xB9, MicroCycleProgram.readSplit(
			cycles(MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_FETCH_OPERAND_HI, MicroOp.M_READ_EA),
			cycles(MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_FETCH_OPERAND_HI, MicroOp.M_READ_DUMMY, MicroOp.M_READ_EA))),
	LDA_IND_X(0xA1, MicroCycleProgram.readShared(cycles(MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_DUMMY, MicroOp.M_READ_ZP_PTR_LO, MicroOp.M_READ_ZP_PTR_HI, MicroOp.M_READ_EA))),
	LDA_IND_Y(0xB1, MicroCycleProgram.readSplit(
			cycles(MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_ZP_PTR_LO, MicroOp.M_READ_ZP_PTR_HI, MicroOp.M_READ_EA),
			cycles(MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_ZP_PTR_LO, MicroOp.M_READ_ZP_PTR_HI, MicroOp.M_READ_DUMMY, MicroOp.M_READ_EA))),
	LDA_IND(0xB2, MicroCycleProgram.readShared(cycles(MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_ZP_PTR_LO, MicroOp.M_READ_ZP_PTR_HI, MicroOp.M_READ_EA))),

	STA_ZPG(0x85, MicroCycleProgram.writeShared(cycles(MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_WRITE_EA))),
	STA_ZPG_X(0x95, MicroCycleProgram.writeShared(cycles(MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_DUMMY, MicroOp.M_WRITE_EA))),
	STA_ABS(0x8D, MicroCycleProgram.writeShared(cycles(MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_FETCH_OPERAND_HI, MicroOp.M_WRITE_EA))),
	STA_ABS_X(0x9D, MicroCycleProgram.writeShared(cycles(MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_FETCH_OPERAND_HI, MicroOp.M_READ_DUMMY, MicroOp.M_WRITE_EA))),
	STA_ABS_Y(0x99, MicroCycleProgram.writeShared(cycles(MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_FETCH_OPERAND_HI, MicroOp.M_READ_DUMMY, MicroOp.M_WRITE_EA))),
	STA_IND_X(0x81, MicroCycleProgram.writeShared(cycles(MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_DUMMY, MicroOp.M_READ_ZP_PTR_LO, MicroOp.M_READ_ZP_PTR_HI, MicroOp.M_WRITE_EA))),
	STA_IND_Y(0x91, MicroCycleProgram.writeShared(cycles(MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_ZP_PTR_LO, MicroOp.M_READ_ZP_PTR_HI, MicroOp.M_READ_DUMMY, MicroOp.M_WRITE_EA))),
	STA_IND(0x92, MicroCycleProgram.writeShared(cycles(MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_ZP_PTR_LO, MicroOp.M_READ_ZP_PTR_HI, MicroOp.M_WRITE_EA)));

	private final int opcodeByte;
	private final MicroCycleProgram microcode;
	private static final EnumMap<Cpu65c02Opcode, Integer> OPCODE_BYTES = buildOpcodeByteMap();
	private static final EnumMap<Cpu65c02Opcode, MicroCycleProgram> MICROCODE_PROGRAMS = buildMicrocodeProgramMap();
	private static final Cpu65c02Opcode[] BYTE_TO_ENUM = buildByteToEnumMap();
	private static final EnumSet<Cpu65c02Opcode> LDA_FAMILY = EnumSet.of(
			LDA_IMM, LDA_ZPG, LDA_ZPG_X, LDA_ABS, LDA_ABS_X, LDA_ABS_Y, LDA_IND_X, LDA_IND_Y, LDA_IND);
	private static final EnumSet<Cpu65c02Opcode> STA_FAMILY = EnumSet.of(
			STA_ZPG, STA_ZPG_X, STA_ABS, STA_ABS_X, STA_ABS_Y, STA_IND_X, STA_IND_Y, STA_IND);

	Cpu65c02Opcode(int opcodeByte, MicroCycleProgram microcode) {
		this.opcodeByte = opcodeByte & 0xff;
		this.microcode = microcode;
	}

	public int opcodeByte() {
		return OPCODE_BYTES.get(this).intValue();
	}

	public MicroCycleProgram microcode() {
		return MICROCODE_PROGRAMS.get(this);
	}

	public static int[] ldaOpcodeBytes() {
		return buildLdaOpcodeBytes();
	}

	public static EnumSet<Cpu65c02Opcode> ldaFamily() {
		return EnumSet.copyOf(LDA_FAMILY);
	}

	public static Cpu65c02Opcode fromOpcodeByte(int opcodeByte) {
		return BYTE_TO_ENUM[opcodeByte & 0xff];
	}

	public static EnumSet<Cpu65c02Opcode> staFamily() {
		return EnumSet.copyOf(STA_FAMILY);
	}

	public static int[] staOpcodeBytes() {
		return buildStaOpcodeBytes();
	}

	private static int[] buildLdaOpcodeBytes() {
		Cpu65c02Opcode[] ops = LDA_FAMILY.toArray(new Cpu65c02Opcode[0]);
		int[] bytes = new int[ops.length];
		for( int i = 0; i<ops.length; i++ )
			bytes[i] = ops[i].opcodeByte();
		return bytes;
	}

	private static int[] buildStaOpcodeBytes() {
		Cpu65c02Opcode[] ops = STA_FAMILY.toArray(new Cpu65c02Opcode[0]);
		int[] bytes = new int[ops.length];
		for( int i = 0; i<ops.length; i++ )
			bytes[i] = ops[i].opcodeByte();
		return bytes;
	}

	private static EnumMap<Cpu65c02Opcode, Integer> buildOpcodeByteMap() {
		EnumMap<Cpu65c02Opcode, Integer> map = new EnumMap<Cpu65c02Opcode, Integer>(Cpu65c02Opcode.class);
		for( Cpu65c02Opcode opcode : Cpu65c02Opcode.values() )
			map.put(opcode, Integer.valueOf(opcode.opcodeByte));
		return map;
	}

	private static EnumMap<Cpu65c02Opcode, MicroCycleProgram> buildMicrocodeProgramMap() {
		EnumMap<Cpu65c02Opcode, MicroCycleProgram> map = new EnumMap<Cpu65c02Opcode, MicroCycleProgram>(Cpu65c02Opcode.class);
		for( Cpu65c02Opcode opcode : Cpu65c02Opcode.values() )
			map.put(opcode, opcode.microcode);
		return map;
	}

	private static Cpu65c02Opcode[] buildByteToEnumMap() {
		Cpu65c02Opcode[] map = new Cpu65c02Opcode[256];
		for( Cpu65c02Opcode opcode : Cpu65c02Opcode.values() ) {
			int byteValue = opcode.opcodeByte;
			if( map[byteValue]!=null )
				throw new IllegalStateException("Duplicate opcode byte: " + byteValue);
			map[byteValue] = opcode;
		}
		return map;
	}

	private static MicroOp[] cycles(MicroOp... ops) {
		return ops;
	}

	public static final class MicroCycleProgram {
		private final AccessType accessType;
		private final MicroOp[] noCrossScript;
		private final MicroOp[] crossScript;

		private MicroCycleProgram(AccessType accessType, MicroOp[] noCrossScript, MicroOp[] crossScript) {
			this.accessType = accessType;
			this.noCrossScript = Arrays.copyOf(noCrossScript, noCrossScript.length);
			this.crossScript = Arrays.copyOf(crossScript, crossScript.length);
		}

		public static MicroCycleProgram readShared(MicroOp... script) {
			return new MicroCycleProgram(AccessType.AT_READ, script, script);
		}

		public static MicroCycleProgram readSplit(MicroOp[] noCrossScript, MicroOp[] crossScript) {
			return new MicroCycleProgram(AccessType.AT_READ, noCrossScript, crossScript);
		}

		public static MicroCycleProgram writeShared(MicroOp... script) {
			return new MicroCycleProgram(AccessType.AT_WRITE, script, script);
		}

		public AccessType accessType() {
			return accessType;
		}

		public MicroOp[] noCrossScript() {
			return Arrays.copyOf(noCrossScript, noCrossScript.length);
		}

		public MicroOp[] crossScript() {
			return Arrays.copyOf(crossScript, crossScript.length);
		}
	}
}
