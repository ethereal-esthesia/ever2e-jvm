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

	private static final class DecExpect {
		final int opcode;
		final MicroOp[] script;

		DecExpect(int opcode, MicroOp[] script) {
			this.opcode = opcode;
			this.script = script;
		}
	}

	private static final class AslExpect {
		final int opcode;
		final MicroOp[] script;
		final Cpu65c02Microcode.AccessType accessType;

		AslExpect(int opcode, Cpu65c02Microcode.AccessType accessType, MicroOp[] script) {
			this.opcode = opcode;
			this.accessType = accessType;
			this.script = script;
		}
	}

	private static final class LsrExpect {
		final int opcode;
		final MicroOp[] script;
		final Cpu65c02Microcode.AccessType accessType;

		LsrExpect(int opcode, Cpu65c02Microcode.AccessType accessType, MicroOp[] script) {
			this.opcode = opcode;
			this.accessType = accessType;
			this.script = script;
		}
	}

	private static final class RolExpect {
		final int opcode;
		final MicroOp[] script;
		final Cpu65c02Microcode.AccessType accessType;

		RolExpect(int opcode, Cpu65c02Microcode.AccessType accessType, MicroOp[] script) {
			this.opcode = opcode;
			this.accessType = accessType;
			this.script = script;
		}
	}

	private static final class RorExpect {
		final int opcode;
		final MicroOp[] script;
		final Cpu65c02Microcode.AccessType accessType;

		RorExpect(int opcode, Cpu65c02Microcode.AccessType accessType, MicroOp[] script) {
			this.opcode = opcode;
			this.accessType = accessType;
			this.script = script;
		}
	}

	private static final class OraExpect {
		final int opcode;
		final MicroOp[] noCross;
		final MicroOp[] cross;
		final int readOffsetNoCross;
		final int readOffsetCross;

		OraExpect(int opcode, MicroOp[] noCross, MicroOp[] cross, int readOffsetNoCross, int readOffsetCross) {
			this.opcode = opcode;
			this.noCross = noCross;
			this.cross = cross;
			this.readOffsetNoCross = readOffsetNoCross;
			this.readOffsetCross = readOffsetCross;
		}
	}

	private static final class AndExpect {
		final int opcode;
		final MicroOp[] noCross;
		final MicroOp[] cross;
		final int readOffsetNoCross;
		final int readOffsetCross;

		AndExpect(int opcode, MicroOp[] noCross, MicroOp[] cross, int readOffsetNoCross, int readOffsetCross) {
			this.opcode = opcode;
			this.noCross = noCross;
			this.cross = cross;
			this.readOffsetNoCross = readOffsetNoCross;
			this.readOffsetCross = readOffsetCross;
		}
	}

	private static final class EorExpect {
		final int opcode;
		final MicroOp[] noCross;
		final MicroOp[] cross;
		final int readOffsetNoCross;
		final int readOffsetCross;

		EorExpect(int opcode, MicroOp[] noCross, MicroOp[] cross, int readOffsetNoCross, int readOffsetCross) {
			this.opcode = opcode;
			this.noCross = noCross;
			this.cross = cross;
			this.readOffsetNoCross = readOffsetNoCross;
			this.readOffsetCross = readOffsetCross;
		}
	}

	private static final class AdcExpect {
		final int opcode;
		final MicroOp[] noCross;
		final MicroOp[] cross;
		final int readOffsetNoCross;
		final int readOffsetCross;

		AdcExpect(int opcode, MicroOp[] noCross, MicroOp[] cross, int readOffsetNoCross, int readOffsetCross) {
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

	private static final DecExpect[] DEC_EXPECTATIONS = new DecExpect[] {
			new DecExpect(0xC6, new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_EA, MicroOp.M_WRITE_EA_DUMMY, MicroOp.M_WRITE_EA }),
			new DecExpect(0xD6, new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_DUMMY, MicroOp.M_READ_EA, MicroOp.M_WRITE_EA_DUMMY, MicroOp.M_WRITE_EA }),
			new DecExpect(0xCE, new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_FETCH_OPERAND_HI, MicroOp.M_READ_EA, MicroOp.M_WRITE_EA_DUMMY, MicroOp.M_WRITE_EA }),
			new DecExpect(0xDE, new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_FETCH_OPERAND_HI, MicroOp.M_READ_DUMMY, MicroOp.M_READ_EA, MicroOp.M_WRITE_EA_DUMMY, MicroOp.M_WRITE_EA }),
	};

	private static final AslExpect[] ASL_EXPECTATIONS = new AslExpect[] {
			new AslExpect(0x0A, Cpu65c02Microcode.AccessType.AT_NONE, new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_INTERNAL }),
			new AslExpect(0x06, Cpu65c02Microcode.AccessType.AT_RMW, new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_EA, MicroOp.M_WRITE_EA_DUMMY, MicroOp.M_WRITE_EA }),
			new AslExpect(0x16, Cpu65c02Microcode.AccessType.AT_RMW, new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_DUMMY, MicroOp.M_READ_EA, MicroOp.M_WRITE_EA_DUMMY, MicroOp.M_WRITE_EA }),
			new AslExpect(0x0E, Cpu65c02Microcode.AccessType.AT_RMW, new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_FETCH_OPERAND_HI, MicroOp.M_READ_EA, MicroOp.M_WRITE_EA_DUMMY, MicroOp.M_WRITE_EA }),
			new AslExpect(0x1E, Cpu65c02Microcode.AccessType.AT_RMW, new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_FETCH_OPERAND_HI, MicroOp.M_READ_DUMMY, MicroOp.M_READ_EA, MicroOp.M_WRITE_EA_DUMMY, MicroOp.M_WRITE_EA }),
	};

	private static final LsrExpect[] LSR_EXPECTATIONS = new LsrExpect[] {
			new LsrExpect(0x4A, Cpu65c02Microcode.AccessType.AT_NONE, new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_INTERNAL }),
			new LsrExpect(0x46, Cpu65c02Microcode.AccessType.AT_RMW, new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_EA, MicroOp.M_WRITE_EA_DUMMY, MicroOp.M_WRITE_EA }),
			new LsrExpect(0x56, Cpu65c02Microcode.AccessType.AT_RMW, new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_DUMMY, MicroOp.M_READ_EA, MicroOp.M_WRITE_EA_DUMMY, MicroOp.M_WRITE_EA }),
			new LsrExpect(0x4E, Cpu65c02Microcode.AccessType.AT_RMW, new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_FETCH_OPERAND_HI, MicroOp.M_READ_EA, MicroOp.M_WRITE_EA_DUMMY, MicroOp.M_WRITE_EA }),
			new LsrExpect(0x5E, Cpu65c02Microcode.AccessType.AT_RMW, new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_FETCH_OPERAND_HI, MicroOp.M_READ_DUMMY, MicroOp.M_READ_EA, MicroOp.M_WRITE_EA_DUMMY, MicroOp.M_WRITE_EA }),
	};

	private static final RolExpect[] ROL_EXPECTATIONS = new RolExpect[] {
			new RolExpect(0x2A, Cpu65c02Microcode.AccessType.AT_NONE, new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_INTERNAL }),
			new RolExpect(0x26, Cpu65c02Microcode.AccessType.AT_RMW, new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_EA, MicroOp.M_WRITE_EA_DUMMY, MicroOp.M_WRITE_EA }),
			new RolExpect(0x36, Cpu65c02Microcode.AccessType.AT_RMW, new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_DUMMY, MicroOp.M_READ_EA, MicroOp.M_WRITE_EA_DUMMY, MicroOp.M_WRITE_EA }),
			new RolExpect(0x2E, Cpu65c02Microcode.AccessType.AT_RMW, new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_FETCH_OPERAND_HI, MicroOp.M_READ_EA, MicroOp.M_WRITE_EA_DUMMY, MicroOp.M_WRITE_EA }),
			new RolExpect(0x3E, Cpu65c02Microcode.AccessType.AT_RMW, new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_FETCH_OPERAND_HI, MicroOp.M_READ_DUMMY, MicroOp.M_READ_EA, MicroOp.M_WRITE_EA_DUMMY, MicroOp.M_WRITE_EA }),
	};

	private static final RorExpect[] ROR_EXPECTATIONS = new RorExpect[] {
			new RorExpect(0x6A, Cpu65c02Microcode.AccessType.AT_NONE, new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_INTERNAL }),
			new RorExpect(0x66, Cpu65c02Microcode.AccessType.AT_RMW, new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_EA, MicroOp.M_WRITE_EA_DUMMY, MicroOp.M_WRITE_EA }),
			new RorExpect(0x76, Cpu65c02Microcode.AccessType.AT_RMW, new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_DUMMY, MicroOp.M_READ_EA, MicroOp.M_WRITE_EA_DUMMY, MicroOp.M_WRITE_EA }),
			new RorExpect(0x6E, Cpu65c02Microcode.AccessType.AT_RMW, new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_FETCH_OPERAND_HI, MicroOp.M_READ_EA, MicroOp.M_WRITE_EA_DUMMY, MicroOp.M_WRITE_EA }),
			new RorExpect(0x7E, Cpu65c02Microcode.AccessType.AT_RMW, new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_FETCH_OPERAND_HI, MicroOp.M_READ_DUMMY, MicroOp.M_READ_EA, MicroOp.M_WRITE_EA_DUMMY, MicroOp.M_WRITE_EA }),
	};

	private static final OraExpect[] ORA_EXPECTATIONS = new OraExpect[] {
			new OraExpect(0x09,
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_READ_IMM_DATA },
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_READ_IMM_DATA },
					1, 1),
			new OraExpect(0x05,
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_EA },
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_EA },
					2, 2),
			new OraExpect(0x15,
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_DUMMY, MicroOp.M_READ_EA },
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_DUMMY, MicroOp.M_READ_EA },
					3, 3),
			new OraExpect(0x0D,
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_FETCH_OPERAND_HI, MicroOp.M_READ_EA },
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_FETCH_OPERAND_HI, MicroOp.M_READ_EA },
					3, 3),
			new OraExpect(0x1D,
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_FETCH_OPERAND_HI, MicroOp.M_READ_EA },
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_FETCH_OPERAND_HI, MicroOp.M_READ_DUMMY, MicroOp.M_READ_EA },
					3, 4),
			new OraExpect(0x19,
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_FETCH_OPERAND_HI, MicroOp.M_READ_EA },
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_FETCH_OPERAND_HI, MicroOp.M_READ_DUMMY, MicroOp.M_READ_EA },
					3, 4),
			new OraExpect(0x01,
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_DUMMY, MicroOp.M_READ_ZP_PTR_LO, MicroOp.M_READ_ZP_PTR_HI, MicroOp.M_READ_EA },
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_DUMMY, MicroOp.M_READ_ZP_PTR_LO, MicroOp.M_READ_ZP_PTR_HI, MicroOp.M_READ_EA },
					5, 5),
			new OraExpect(0x11,
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_ZP_PTR_LO, MicroOp.M_READ_ZP_PTR_HI, MicroOp.M_READ_EA },
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_ZP_PTR_LO, MicroOp.M_READ_ZP_PTR_HI, MicroOp.M_READ_DUMMY, MicroOp.M_READ_EA },
					4, 5),
			new OraExpect(0x12,
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_ZP_PTR_LO, MicroOp.M_READ_ZP_PTR_HI, MicroOp.M_READ_EA },
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_ZP_PTR_LO, MicroOp.M_READ_ZP_PTR_HI, MicroOp.M_READ_EA },
					4, 4),
	};

	private static final AndExpect[] AND_EXPECTATIONS = new AndExpect[] {
			new AndExpect(0x29,
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_READ_IMM_DATA },
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_READ_IMM_DATA },
					1, 1),
			new AndExpect(0x25,
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_EA },
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_EA },
					2, 2),
			new AndExpect(0x35,
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_DUMMY, MicroOp.M_READ_EA },
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_DUMMY, MicroOp.M_READ_EA },
					3, 3),
			new AndExpect(0x2D,
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_FETCH_OPERAND_HI, MicroOp.M_READ_EA },
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_FETCH_OPERAND_HI, MicroOp.M_READ_EA },
					3, 3),
			new AndExpect(0x3D,
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_FETCH_OPERAND_HI, MicroOp.M_READ_EA },
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_FETCH_OPERAND_HI, MicroOp.M_READ_DUMMY, MicroOp.M_READ_EA },
					3, 4),
			new AndExpect(0x39,
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_FETCH_OPERAND_HI, MicroOp.M_READ_EA },
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_FETCH_OPERAND_HI, MicroOp.M_READ_DUMMY, MicroOp.M_READ_EA },
					3, 4),
			new AndExpect(0x21,
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_DUMMY, MicroOp.M_READ_ZP_PTR_LO, MicroOp.M_READ_ZP_PTR_HI, MicroOp.M_READ_EA },
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_DUMMY, MicroOp.M_READ_ZP_PTR_LO, MicroOp.M_READ_ZP_PTR_HI, MicroOp.M_READ_EA },
					5, 5),
			new AndExpect(0x31,
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_ZP_PTR_LO, MicroOp.M_READ_ZP_PTR_HI, MicroOp.M_READ_EA },
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_ZP_PTR_LO, MicroOp.M_READ_ZP_PTR_HI, MicroOp.M_READ_DUMMY, MicroOp.M_READ_EA },
					4, 5),
			new AndExpect(0x32,
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_ZP_PTR_LO, MicroOp.M_READ_ZP_PTR_HI, MicroOp.M_READ_EA },
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_ZP_PTR_LO, MicroOp.M_READ_ZP_PTR_HI, MicroOp.M_READ_EA },
					4, 4),
	};

	private static final EorExpect[] EOR_EXPECTATIONS = new EorExpect[] {
			new EorExpect(0x49,
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_READ_IMM_DATA },
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_READ_IMM_DATA },
					1, 1),
			new EorExpect(0x45,
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_EA },
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_EA },
					2, 2),
			new EorExpect(0x55,
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_DUMMY, MicroOp.M_READ_EA },
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_DUMMY, MicroOp.M_READ_EA },
					3, 3),
			new EorExpect(0x4D,
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_FETCH_OPERAND_HI, MicroOp.M_READ_EA },
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_FETCH_OPERAND_HI, MicroOp.M_READ_EA },
					3, 3),
			new EorExpect(0x5D,
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_FETCH_OPERAND_HI, MicroOp.M_READ_EA },
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_FETCH_OPERAND_HI, MicroOp.M_READ_DUMMY, MicroOp.M_READ_EA },
					3, 4),
			new EorExpect(0x59,
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_FETCH_OPERAND_HI, MicroOp.M_READ_EA },
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_FETCH_OPERAND_HI, MicroOp.M_READ_DUMMY, MicroOp.M_READ_EA },
					3, 4),
			new EorExpect(0x41,
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_DUMMY, MicroOp.M_READ_ZP_PTR_LO, MicroOp.M_READ_ZP_PTR_HI, MicroOp.M_READ_EA },
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_DUMMY, MicroOp.M_READ_ZP_PTR_LO, MicroOp.M_READ_ZP_PTR_HI, MicroOp.M_READ_EA },
					5, 5),
			new EorExpect(0x51,
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_ZP_PTR_LO, MicroOp.M_READ_ZP_PTR_HI, MicroOp.M_READ_EA },
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_ZP_PTR_LO, MicroOp.M_READ_ZP_PTR_HI, MicroOp.M_READ_DUMMY, MicroOp.M_READ_EA },
					4, 5),
			new EorExpect(0x52,
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_ZP_PTR_LO, MicroOp.M_READ_ZP_PTR_HI, MicroOp.M_READ_EA },
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_ZP_PTR_LO, MicroOp.M_READ_ZP_PTR_HI, MicroOp.M_READ_EA },
					4, 4),
	};

	private static final AdcExpect[] ADC_EXPECTATIONS = new AdcExpect[] {
			new AdcExpect(0x69,
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_READ_IMM_DATA },
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_READ_IMM_DATA },
					1, 1),
			new AdcExpect(0x65,
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_EA },
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_EA },
					2, 2),
			new AdcExpect(0x75,
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_DUMMY, MicroOp.M_READ_EA },
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_DUMMY, MicroOp.M_READ_EA },
					3, 3),
			new AdcExpect(0x6D,
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_FETCH_OPERAND_HI, MicroOp.M_READ_EA },
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_FETCH_OPERAND_HI, MicroOp.M_READ_EA },
					3, 3),
			new AdcExpect(0x7D,
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_FETCH_OPERAND_HI, MicroOp.M_READ_EA },
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_FETCH_OPERAND_HI, MicroOp.M_READ_DUMMY, MicroOp.M_READ_EA },
					3, 4),
			new AdcExpect(0x79,
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_FETCH_OPERAND_HI, MicroOp.M_READ_EA },
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_FETCH_OPERAND_HI, MicroOp.M_READ_DUMMY, MicroOp.M_READ_EA },
					3, 4),
			new AdcExpect(0x61,
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_DUMMY, MicroOp.M_READ_ZP_PTR_LO, MicroOp.M_READ_ZP_PTR_HI, MicroOp.M_READ_EA },
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_DUMMY, MicroOp.M_READ_ZP_PTR_LO, MicroOp.M_READ_ZP_PTR_HI, MicroOp.M_READ_EA },
					5, 5),
			new AdcExpect(0x71,
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_ZP_PTR_LO, MicroOp.M_READ_ZP_PTR_HI, MicroOp.M_READ_EA },
					new MicroOp[] { MicroOp.M_FETCH_OPCODE, MicroOp.M_FETCH_OPERAND_LO, MicroOp.M_READ_ZP_PTR_LO, MicroOp.M_READ_ZP_PTR_HI, MicroOp.M_READ_DUMMY, MicroOp.M_READ_EA },
					4, 5),
			new AdcExpect(0x72,
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
	public void decOpcodeEnumMatchesOpcodeByteList() {
		Cpu65c02Opcode[] decOps = Cpu65c02Opcode.decFamily().toArray(new Cpu65c02Opcode[0]);
		int[] decBytes = Cpu65c02Opcode.decOpcodeBytes();
		assertEquals(decOps.length, decBytes.length);
		for( int i = 0; i<decOps.length; i++ )
			assertEquals(decOps[i].opcodeByte(), decBytes[i]);
	}

	@Test
	public void decOpcodeEnumProgramsDriveResolvedMicrocode() {
		for( Cpu65c02Opcode dec : Cpu65c02Opcode.decFamily() ) {
			Cpu65c02OpcodeView entry = Cpu65c02Microcode.opcodeForByte(dec.opcodeByte());
			assertEquals(dec.microcode().accessType(), entry.getAccessType());
			assertArrayEquals(dec.microcode().noCrossScript(), entry.getExpectedMnemonicOrder(false));
			assertArrayEquals(dec.microcode().crossScript(), entry.getExpectedMnemonicOrder(true));
		}
	}

	@Test
	public void aslOpcodeEnumMatchesOpcodeByteList() {
		Cpu65c02Opcode[] aslOps = Cpu65c02Opcode.aslFamily().toArray(new Cpu65c02Opcode[0]);
		int[] aslBytes = Cpu65c02Opcode.aslOpcodeBytes();
		assertEquals(aslOps.length, aslBytes.length);
		for( int i = 0; i<aslOps.length; i++ )
			assertEquals(aslOps[i].opcodeByte(), aslBytes[i]);
	}

	@Test
	public void aslOpcodeEnumProgramsDriveResolvedMicrocode() {
		for( Cpu65c02Opcode asl : Cpu65c02Opcode.aslFamily() ) {
			Cpu65c02OpcodeView entry = Cpu65c02Microcode.opcodeForByte(asl.opcodeByte());
			assertEquals(asl.microcode().accessType(), entry.getAccessType());
			assertArrayEquals(asl.microcode().noCrossScript(), entry.getExpectedMnemonicOrder(false));
			assertArrayEquals(asl.microcode().crossScript(), entry.getExpectedMnemonicOrder(true));
		}
	}

	@Test
	public void lsrOpcodeEnumMatchesOpcodeByteList() {
		Cpu65c02Opcode[] lsrOps = Cpu65c02Opcode.lsrFamily().toArray(new Cpu65c02Opcode[0]);
		int[] lsrBytes = Cpu65c02Opcode.lsrOpcodeBytes();
		assertEquals(lsrOps.length, lsrBytes.length);
		for( int i = 0; i<lsrOps.length; i++ )
			assertEquals(lsrOps[i].opcodeByte(), lsrBytes[i]);
	}

	@Test
	public void lsrOpcodeEnumProgramsDriveResolvedMicrocode() {
		for( Cpu65c02Opcode lsr : Cpu65c02Opcode.lsrFamily() ) {
			Cpu65c02OpcodeView entry = Cpu65c02Microcode.opcodeForByte(lsr.opcodeByte());
			assertEquals(lsr.microcode().accessType(), entry.getAccessType());
			assertArrayEquals(lsr.microcode().noCrossScript(), entry.getExpectedMnemonicOrder(false));
			assertArrayEquals(lsr.microcode().crossScript(), entry.getExpectedMnemonicOrder(true));
		}
	}

	@Test
	public void rolOpcodeEnumMatchesOpcodeByteList() {
		Cpu65c02Opcode[] rolOps = Cpu65c02Opcode.rolFamily().toArray(new Cpu65c02Opcode[0]);
		int[] rolBytes = Cpu65c02Opcode.rolOpcodeBytes();
		assertEquals(rolOps.length, rolBytes.length);
		for( int i = 0; i<rolOps.length; i++ )
			assertEquals(rolOps[i].opcodeByte(), rolBytes[i]);
	}

	@Test
	public void rolOpcodeEnumProgramsDriveResolvedMicrocode() {
		for( Cpu65c02Opcode rol : Cpu65c02Opcode.rolFamily() ) {
			Cpu65c02OpcodeView entry = Cpu65c02Microcode.opcodeForByte(rol.opcodeByte());
			assertEquals(rol.microcode().accessType(), entry.getAccessType());
			assertArrayEquals(rol.microcode().noCrossScript(), entry.getExpectedMnemonicOrder(false));
			assertArrayEquals(rol.microcode().crossScript(), entry.getExpectedMnemonicOrder(true));
		}
	}

	@Test
	public void rorOpcodeEnumMatchesOpcodeByteList() {
		Cpu65c02Opcode[] rorOps = Cpu65c02Opcode.rorFamily().toArray(new Cpu65c02Opcode[0]);
		int[] rorBytes = Cpu65c02Opcode.rorOpcodeBytes();
		assertEquals(rorOps.length, rorBytes.length);
		for( int i = 0; i<rorOps.length; i++ )
			assertEquals(rorOps[i].opcodeByte(), rorBytes[i]);
	}

	@Test
	public void rorOpcodeEnumProgramsDriveResolvedMicrocode() {
		for( Cpu65c02Opcode ror : Cpu65c02Opcode.rorFamily() ) {
			Cpu65c02OpcodeView entry = Cpu65c02Microcode.opcodeForByte(ror.opcodeByte());
			assertEquals(ror.microcode().accessType(), entry.getAccessType());
			assertArrayEquals(ror.microcode().noCrossScript(), entry.getExpectedMnemonicOrder(false));
			assertArrayEquals(ror.microcode().crossScript(), entry.getExpectedMnemonicOrder(true));
		}
	}

	@Test
	public void oraOpcodeEnumMatchesOpcodeByteList() {
		Cpu65c02Opcode[] oraOps = Cpu65c02Opcode.oraFamily().toArray(new Cpu65c02Opcode[0]);
		int[] oraBytes = Cpu65c02Opcode.oraOpcodeBytes();
		assertEquals(oraOps.length, oraBytes.length);
		for( int i = 0; i<oraOps.length; i++ )
			assertEquals(oraOps[i].opcodeByte(), oraBytes[i]);
	}

	@Test
	public void oraOpcodeEnumProgramsDriveResolvedMicrocode() {
		for( Cpu65c02Opcode ora : Cpu65c02Opcode.oraFamily() ) {
			Cpu65c02OpcodeView entry = Cpu65c02Microcode.opcodeForByte(ora.opcodeByte());
			assertEquals(ora.microcode().accessType(), entry.getAccessType());
			assertArrayEquals(ora.microcode().noCrossScript(), entry.getExpectedMnemonicOrder(false));
			assertArrayEquals(ora.microcode().crossScript(), entry.getExpectedMnemonicOrder(true));
		}
	}

	@Test
	public void andOpcodeEnumMatchesOpcodeByteList() {
		Cpu65c02Opcode[] andOps = Cpu65c02Opcode.andFamily().toArray(new Cpu65c02Opcode[0]);
		int[] andBytes = Cpu65c02Opcode.andOpcodeBytes();
		assertEquals(andOps.length, andBytes.length);
		for( int i = 0; i<andOps.length; i++ )
			assertEquals(andOps[i].opcodeByte(), andBytes[i]);
	}

	@Test
	public void andOpcodeEnumProgramsDriveResolvedMicrocode() {
		for( Cpu65c02Opcode and : Cpu65c02Opcode.andFamily() ) {
			Cpu65c02OpcodeView entry = Cpu65c02Microcode.opcodeForByte(and.opcodeByte());
			assertEquals(and.microcode().accessType(), entry.getAccessType());
			assertArrayEquals(and.microcode().noCrossScript(), entry.getExpectedMnemonicOrder(false));
			assertArrayEquals(and.microcode().crossScript(), entry.getExpectedMnemonicOrder(true));
		}
	}

	@Test
	public void eorOpcodeEnumMatchesOpcodeByteList() {
		Cpu65c02Opcode[] eorOps = Cpu65c02Opcode.eorFamily().toArray(new Cpu65c02Opcode[0]);
		int[] eorBytes = Cpu65c02Opcode.eorOpcodeBytes();
		assertEquals(eorOps.length, eorBytes.length);
		for( int i = 0; i<eorOps.length; i++ )
			assertEquals(eorOps[i].opcodeByte(), eorBytes[i]);
	}

	@Test
	public void eorOpcodeEnumProgramsDriveResolvedMicrocode() {
		for( Cpu65c02Opcode eor : Cpu65c02Opcode.eorFamily() ) {
			Cpu65c02OpcodeView entry = Cpu65c02Microcode.opcodeForByte(eor.opcodeByte());
			assertEquals(eor.microcode().accessType(), entry.getAccessType());
			assertArrayEquals(eor.microcode().noCrossScript(), entry.getExpectedMnemonicOrder(false));
			assertArrayEquals(eor.microcode().crossScript(), entry.getExpectedMnemonicOrder(true));
		}
	}

	@Test
	public void adcOpcodeEnumMatchesOpcodeByteList() {
		Cpu65c02Opcode[] adcOps = Cpu65c02Opcode.adcFamily().toArray(new Cpu65c02Opcode[0]);
		int[] adcBytes = Cpu65c02Opcode.adcOpcodeBytes();
		assertEquals(adcOps.length, adcBytes.length);
		for( int i = 0; i<adcOps.length; i++ )
			assertEquals(adcOps[i].opcodeByte(), adcBytes[i]);
	}

	@Test
	public void adcOpcodeEnumProgramsDriveResolvedMicrocode() {
		for( Cpu65c02Opcode adc : Cpu65c02Opcode.adcFamily() ) {
			Cpu65c02OpcodeView entry = Cpu65c02Microcode.opcodeForByte(adc.opcodeByte());
			assertEquals(adc.microcode().accessType(), entry.getAccessType());
			assertArrayEquals(adc.microcode().noCrossScript(), entry.getExpectedMnemonicOrder(false));
			assertArrayEquals(adc.microcode().crossScript(), entry.getExpectedMnemonicOrder(true));
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
		for( Cpu65c02Opcode dec : Cpu65c02Opcode.decFamily() )
			assertEquals(dec, Cpu65c02Opcode.fromOpcodeByte(dec.opcodeByte()));
		for( Cpu65c02Opcode asl : Cpu65c02Opcode.aslFamily() )
			assertEquals(asl, Cpu65c02Opcode.fromOpcodeByte(asl.opcodeByte()));
		for( Cpu65c02Opcode lsr : Cpu65c02Opcode.lsrFamily() )
			assertEquals(lsr, Cpu65c02Opcode.fromOpcodeByte(lsr.opcodeByte()));
		for( Cpu65c02Opcode rol : Cpu65c02Opcode.rolFamily() )
			assertEquals(rol, Cpu65c02Opcode.fromOpcodeByte(rol.opcodeByte()));
		for( Cpu65c02Opcode ror : Cpu65c02Opcode.rorFamily() )
			assertEquals(ror, Cpu65c02Opcode.fromOpcodeByte(ror.opcodeByte()));
		for( Cpu65c02Opcode ora : Cpu65c02Opcode.oraFamily() )
			assertEquals(ora, Cpu65c02Opcode.fromOpcodeByte(ora.opcodeByte()));
		for( Cpu65c02Opcode and : Cpu65c02Opcode.andFamily() )
			assertEquals(and, Cpu65c02Opcode.fromOpcodeByte(and.opcodeByte()));
		for( Cpu65c02Opcode eor : Cpu65c02Opcode.eorFamily() )
			assertEquals(eor, Cpu65c02Opcode.fromOpcodeByte(eor.opcodeByte()));
		for( Cpu65c02Opcode adc : Cpu65c02Opcode.adcFamily() )
			assertEquals(adc, Cpu65c02Opcode.fromOpcodeByte(adc.opcodeByte()));
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
	public void allOraOpcodesHaveExpectedMicrocodeOrder() {
		for( OraExpect expect : ORA_EXPECTATIONS ) {
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
	public void allAndOpcodesHaveExpectedMicrocodeOrder() {
		for( AndExpect expect : AND_EXPECTATIONS ) {
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
	public void allEorOpcodesHaveExpectedMicrocodeOrder() {
		for( EorExpect expect : EOR_EXPECTATIONS ) {
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
	public void allAdcOpcodesHaveExpectedMicrocodeOrder() {
		for( AdcExpect expect : ADC_EXPECTATIONS ) {
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
	public void allDecOpcodesHaveExpectedMicrocodeOrder() {
		for( DecExpect expect : DEC_EXPECTATIONS ) {
			Cpu65c02OpcodeView entry = Cpu65c02Microcode.opcodeForByte(expect.opcode);
			assertEquals(expect.opcode, entry.getOpcodeByte());
			assertEquals(expect.script.length, entry.getExpectedMnemonicOrder(false).length);
			assertEquals(expect.script.length, entry.getExpectedMnemonicOrder(true).length);
			assertArrayEquals(expect.script, entry.getExpectedMnemonicOrder(false));
			assertArrayEquals(expect.script, entry.getExpectedMnemonicOrder(true));
		}
	}

	@Test
	public void allAslOpcodesHaveExpectedMicrocodeOrder() {
		for( AslExpect expect : ASL_EXPECTATIONS ) {
			Cpu65c02OpcodeView entry = Cpu65c02Microcode.opcodeForByte(expect.opcode);
			assertEquals(expect.opcode, entry.getOpcodeByte());
			assertEquals(expect.accessType, entry.getAccessType());
			assertEquals(expect.script.length, entry.getExpectedMnemonicOrder(false).length);
			assertEquals(expect.script.length, entry.getExpectedMnemonicOrder(true).length);
			assertArrayEquals(expect.script, entry.getExpectedMnemonicOrder(false));
			assertArrayEquals(expect.script, entry.getExpectedMnemonicOrder(true));
		}
	}

	@Test
	public void allLsrOpcodesHaveExpectedMicrocodeOrder() {
		for( LsrExpect expect : LSR_EXPECTATIONS ) {
			Cpu65c02OpcodeView entry = Cpu65c02Microcode.opcodeForByte(expect.opcode);
			assertEquals(expect.opcode, entry.getOpcodeByte());
			assertEquals(expect.accessType, entry.getAccessType());
			assertEquals(expect.script.length, entry.getExpectedMnemonicOrder(false).length);
			assertEquals(expect.script.length, entry.getExpectedMnemonicOrder(true).length);
			assertArrayEquals(expect.script, entry.getExpectedMnemonicOrder(false));
			assertArrayEquals(expect.script, entry.getExpectedMnemonicOrder(true));
		}
	}

	@Test
	public void allRolOpcodesHaveExpectedMicrocodeOrder() {
		for( RolExpect expect : ROL_EXPECTATIONS ) {
			Cpu65c02OpcodeView entry = Cpu65c02Microcode.opcodeForByte(expect.opcode);
			assertEquals(expect.opcode, entry.getOpcodeByte());
			assertEquals(expect.accessType, entry.getAccessType());
			assertEquals(expect.script.length, entry.getExpectedMnemonicOrder(false).length);
			assertEquals(expect.script.length, entry.getExpectedMnemonicOrder(true).length);
			assertArrayEquals(expect.script, entry.getExpectedMnemonicOrder(false));
			assertArrayEquals(expect.script, entry.getExpectedMnemonicOrder(true));
		}
	}

	@Test
	public void allRorOpcodesHaveExpectedMicrocodeOrder() {
		for( RorExpect expect : ROR_EXPECTATIONS ) {
			Cpu65c02OpcodeView entry = Cpu65c02Microcode.opcodeForByte(expect.opcode);
			assertEquals(expect.opcode, entry.getOpcodeByte());
			assertEquals(expect.accessType, entry.getAccessType());
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
