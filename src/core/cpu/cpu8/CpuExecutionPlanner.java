package core.cpu.cpu8;

import java.util.EnumSet;

import core.memory.memory8.MemoryBusIIe;

final class CpuExecutionPlanner {

	static final class Plan {
		final int pendingCycles;
		final int totalCycles;

		Plan(int pendingCycles, int totalCycles) {
			this.pendingCycles = pendingCycles;
			this.totalCycles = totalCycles;
		}
	}

	private static final EnumSet<Cpu65c02Opcode> MICRO_QUEUED_FAMILY = Cpu65c02Opcode.ldaFamily();

	private final MemoryBusIIe memory;
	private final Register reg;

	CpuExecutionPlanner(MemoryBusIIe memory, Register reg) {
		this.memory = memory;
		this.reg = reg;
	}

	Plan buildPlan(Opcode op, int pc) {
		if( !isMicroQueued(op) )
			return new Plan(0, op.getCycleTime());
		int totalCycles = Cpu65c02CycleEstimator.predictInstructionCycles(memory, reg, op, pc);
		return new Plan(Math.max(0, totalCycles-1), totalCycles);
	}

	boolean isMicroQueued(Opcode op) {
		if( op==null || op.getMachineCode()==null )
			return false;
		Cpu65c02Opcode mapped = Cpu65c02Opcode.fromOpcodeByte(op.getMachineCode().intValue());
		return mapped!=null && MICRO_QUEUED_FAMILY.contains(mapped);
	}
}
