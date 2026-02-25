package core.cpu.cpu8;

import core.emulator.HardwareManager;
import core.emulator.machine.Emulator;
import core.exception.HardwareException;
import core.memory.memory8.Memory8;
import core.memory.memory8.MemoryBusIIe;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.PriorityQueue;

import static org.junit.Assert.assertTrue;

public class CpuLdaOpcodeBenchTest {

    private static final int MEM_SIZE = 0x20000;
    private static final int ROM_SIZE = 0x4000;
    private static final int PROG_PC = 0x0200;

    private static final class CpuEnv {
        final MemoryBusIIe bus;
        final Cpu65c02 cpu;
        final Emulator emulator;
        final byte[] rom;

        CpuEnv(MemoryBusIIe bus, Cpu65c02 cpu, Emulator emulator, byte[] rom) {
            this.bus = bus;
            this.cpu = cpu;
            this.emulator = emulator;
            this.rom = rom;
        }
    }

    private CpuEnv createEnv() throws HardwareException {
        Memory8 mem = new Memory8(MEM_SIZE);
        byte[] rom = new byte[ROM_SIZE];
        MemoryBusIIe bus = new MemoryBusIIe(mem, rom);
        Cpu65c02 cpu = new Cpu65c02(bus, 0);
        PriorityQueue<HardwareManager> queue = new PriorityQueue<HardwareManager>();
        queue.add(cpu);
        Emulator emulator = new Emulator(queue, 0);
        cpu.coldReset();
        int vec = 0xFFFC - 0xC000;
        rom[vec] = (byte) (PROG_PC & 0xFF);
        rom[vec + 1] = (byte) ((PROG_PC >> 8) & 0xFF);
        return new CpuEnv(bus, cpu, emulator, rom);
    }

    private void runInstruction(CpuEnv env) throws Exception {
        while (true) {
            boolean instructionEndsThisCycle = env.cpu.hasPendingInstructionEndEvent();
            env.emulator.startWithStepPhases(1, env.cpu, (step, manager, preCycle) -> true);
            if (instructionEndsThisCycle)
                return;
        }
    }

    private void prepareLdaProgram(CpuEnv env, int opByte) {
        Register reg = env.cpu.getRegister();
        reg.setX(0x01);
        reg.setY(0x01);

        env.bus.setByte(PROG_PC, opByte & 0xFF);

        switch (opByte & 0xFF) {
            case 0xA9: // IMM
                env.bus.setByte(PROG_PC + 1, 0x5A);
                break;
            case 0xA5: // ZPG
                env.bus.setByte(PROG_PC + 1, 0x10);
                env.bus.setByte(0x0010, 0x5A);
                break;
            case 0xB5: // ZPG,X
                env.bus.setByte(PROG_PC + 1, 0x10);
                env.bus.setByte(0x0011, 0x5A);
                break;
            case 0xAD: // ABS
                env.bus.setByte(PROG_PC + 1, 0x00);
                env.bus.setByte(PROG_PC + 2, 0x20);
                env.bus.setByte(0x2000, 0x5A);
                break;
            case 0xBD: // ABS,X
                env.bus.setByte(PROG_PC + 1, 0x00);
                env.bus.setByte(PROG_PC + 2, 0x20);
                env.bus.setByte(0x2001, 0x5A);
                break;
            case 0xB9: // ABS,Y
                env.bus.setByte(PROG_PC + 1, 0x00);
                env.bus.setByte(PROG_PC + 2, 0x20);
                env.bus.setByte(0x2001, 0x5A);
                break;
            case 0xA1: // (IND,X)
                env.bus.setByte(PROG_PC + 1, 0x20);
                env.bus.setByte(0x0021, 0x00);
                env.bus.setByte(0x0022, 0x21);
                env.bus.setByte(0x2100, 0x5A);
                break;
            case 0xB1: // (IND),Y
                env.bus.setByte(PROG_PC + 1, 0x30);
                env.bus.setByte(0x0030, 0x00);
                env.bus.setByte(0x0031, 0x21);
                env.bus.setByte(0x2101, 0x5A);
                break;
            case 0xB2: // (ZPG)
                env.bus.setByte(PROG_PC + 1, 0x40);
                env.bus.setByte(0x0040, 0x00);
                env.bus.setByte(0x0041, 0x21);
                env.bus.setByte(0x2100, 0x5A);
                break;
            default:
                throw new IllegalArgumentException("Unexpected LDA opcode " + Integer.toHexString(opByte));
        }
    }

    @Test
    public void quickBenchAllLdaOpcodes() throws Exception {
        int[] ldaOps = Cpu65c02Opcode.ldaOpcodeBytes();
        Map<Integer, Long> cpuNsByOp = new LinkedHashMap<Integer, Long>();
        Map<Integer, Long> plannerNsByOp = new LinkedHashMap<Integer, Long>();

        final int warmup = 100;
        final int iters = 1200;
        final int plannerIters = 200000;

        for (int op : ldaOps) {
            for (int i = 0; i < warmup; i++) {
                CpuEnv env = createEnv();
                prepareLdaProgram(env, op);
                runInstruction(env); // reset
                runInstruction(env); // LDA
            }

            long startCpu = System.nanoTime();
            for (int i = 0; i < iters; i++) {
                CpuEnv env = createEnv();
                prepareLdaProgram(env, op);
                runInstruction(env);
                runInstruction(env);
            }
            long cpuNs = System.nanoTime() - startCpu;
            cpuNsByOp.put(op, cpuNs);

            CpuEnv env = createEnv();
            prepareLdaProgram(env, op);
            CpuExecutionPlanner planner = new CpuExecutionPlanner(env.bus, env.cpu.getRegister());
            Opcode opcode = Cpu65c02.OPCODE[op & 0xFF];

            long startPlanner = System.nanoTime();
            int sum = 0;
            for (int i = 0; i < plannerIters; i++) {
                CpuExecutionPlanner.Plan plan = planner.buildPlan(opcode, PROG_PC);
                sum += plan.pendingCycles;
                sum += Cpu65c02CycleEstimator.predictInstructionCycles(env.bus, env.cpu.getRegister(), opcode, PROG_PC);
            }
            long plannerNs = System.nanoTime() - startPlanner;
            plannerNsByOp.put(op, plannerNs + (sum == 0 ? 0 : 0));
        }

        long cpuTotal = 0;
        long plannerTotal = 0;
        for (int op : ldaOps) {
            cpuTotal += cpuNsByOp.get(op);
            plannerTotal += plannerNsByOp.get(op);
            System.out.println(String.format(
                    "LDA %02X cpu=%.3fms planner=%.3fms",
                    op,
                    cpuNsByOp.get(op) / 1_000_000.0,
                    plannerNsByOp.get(op) / 1_000_000.0));
        }

        System.out.println(String.format(
                "LDA BENCH TOTAL cpu=%.3fms planner=%.3fms iters/op=%d plannerIters/op=%d",
                cpuTotal / 1_000_000.0,
                plannerTotal / 1_000_000.0,
                iters,
                plannerIters));

        assertTrue(cpuTotal > 0L);
        assertTrue(plannerTotal > 0L);
    }
}
