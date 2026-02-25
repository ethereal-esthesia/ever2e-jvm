package test.cpu;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;

import com.sun.management.OperatingSystemMXBean;
import org.junit.Test;

import core.cpu.cpu8.Cpu65c02;
import core.cpu.cpu8.Cpu65c02.AddressMode;
import core.cpu.cpu8.Cpu65c02.OpcodeMnemonic;
import core.cpu.cpu8.Opcode;
import core.cpu.cpu8.Register;
import core.emulator.HardwareManager;
import core.emulator.machine.Emulator;
import core.exception.HardwareException;
import core.memory.memory8.Memory8;
import core.memory.memory8.MemoryBusIIe;

/**
 * CPU-only randomized opcode validator.
 *
 * This runs one instruction per trial on Cpu65c02 and compares final CPU state
 * with an independent reference model using the same initial state and memory.
 */
public class Cpu65C02OpcodeLevelTest {

    private static final int MEM_SIZE = 0x20000;
    private static final int ROM_SIZE = 0x4000;
    private static final int PROG_PC = 0x2000;
    private static final int SAFE_ADDR_MIN = 0x0200;
    private static final int SAFE_ADDR_MAX = 0xBFFF;

    private static final int FLAG_N = 0x80;
    private static final int FLAG_V = 0x40;
    private static final int FLAG_B = 0x10;
    private static final int FLAG_D = 0x08;
    private static final int FLAG_I = 0x04;
    private static final int FLAG_Z = 0x02;
    private static final int FLAG_C = 0x01;

    /**
     * Fast deterministic RNG for test harness use.
     * Overrides Random.next(bits) so all inherited APIs use this core.
     */
    private static final class FastXorShiftRandom extends Random {
        private static final long serialVersionUID = 1L;
        private long state;

        FastXorShiftRandom(long seed) {
            setSeed(seed);
        }

        @Override
        public void setSeed(long seed) {
            long mixed = mix64(seed);
            state = mixed == 0L ? 0x9E3779B97F4A7C15L : mixed;
        }

        @Override
        protected int next(int bits) {
            long x = state;
            x ^= (x << 13);
            x ^= (x >>> 7);
            x ^= (x << 17);
            state = x;
            return (int) (x >>> (64 - bits));
        }

        private static long mix64(long z) {
            z = (z ^ (z >>> 33)) * 0xff51afd7ed558ccdL;
            z = (z ^ (z >>> 33)) * 0xc4ceb9fe1a85ec53L;
            return z ^ (z >>> 33);
        }
    }

    private static final class CpuEnv {
        final Memory8 memory;
        final MemoryBusIIe bus;
        final Cpu65c02 cpu;
        final Emulator emulator;
        final byte[] rom;

        CpuEnv(Memory8 memory, MemoryBusIIe bus, Cpu65c02 cpu, Emulator emulator, byte[] rom) {
            this.memory = memory;
            this.bus = bus;
            this.cpu = cpu;
            this.emulator = emulator;
            this.rom = rom;
        }
    }

    private static final class Setup {
        int opcodeByte;
        Opcode opcode;
        int operandPtr;
    }

    private static final class RefState {
        int a;
        int x;
        int y;
        int s;
        int p;
        int pc;

        RefState copy() {
            RefState c = new RefState();
            c.a = a;
            c.x = x;
            c.y = y;
            c.s = s;
            c.p = p;
            c.pc = pc;
            return c;
        }
    }

    private static final class TrialFailure {
        final int opcode;
        final int trial;
        final String reason;

        TrialFailure(int opcode, int trial, String reason) {
            this.opcode = opcode;
            this.trial = trial;
            this.reason = reason;
        }
    }

    private static final class RefMem {
        final MemoryBusIIe base;
        final Map<Integer, Integer> writes = new HashMap<Integer, Integer>();

        RefMem(MemoryBusIIe base) {
            this.base = base;
        }

        int getByte(int address) {
            int a = address & 0xFFFF;
            Integer v = writes.get(a);
            return v == null ? base.getByte(a) : v.intValue();
        }

        void setByte(int address, int value) {
            writes.put(address & 0xFFFF, value & 0xFF);
        }

        int getWord16LittleEndian(int address) {
            return getByte(address) | (getByte(address + 1) << 8);
        }

        int getWord16LittleEndian(int address, int mask) {
            return getByte(address & mask) | (getByte((address + 1) & mask) << 8);
        }
    }

    public static void main(String[] args) throws Exception {
        int trialsPerOpcode = 200;
        long baseSeed = 0x6502_2026L;
        Integer rngSelfTestCount = null;

        for (int i = 0; i < args.length; i++) {
            if ("--trials".equals(args[i]) && i + 1 < args.length) {
                trialsPerOpcode = Integer.parseInt(args[++i]);
            } else if ("--seed".equals(args[i]) && i + 1 < args.length) {
                baseSeed = Long.parseLong(args[++i]);
            } else if ("--rng-selftest".equals(args[i])) {
                if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                    rngSelfTestCount = Integer.parseInt(args[++i]);
                } else {
                    rngSelfTestCount = Integer.valueOf(1_000_000);
                }
            }
        }

        long cycleXor = estimateMacCpuCycleCount();
        long seed = baseSeed ^ cycleXor;
        Random rng = new FastXorShiftRandom(seed);

        if (rngSelfTestCount != null) {
            runRngSelfTest(rng, rngSelfTestCount.intValue(), baseSeed, cycleXor, seed);
            return;
        }
        Map<Integer, Integer> passByOpcode = new LinkedHashMap<Integer, Integer>();
        List<TrialFailure> failures = new ArrayList<TrialFailure>();
        int totalChecks = 0;
        int totalPass = 0;

        for (int opByte = 0; opByte <= 0xFF; opByte++) {
            Opcode op = Cpu65c02.OPCODE[opByte];
            int pass = 0;

            for (int t = 0; t < trialsPerOpcode; t++) {
                totalChecks++;
                try {
                    TrialFailure failure = runTrial(opByte, op, t, rng);
                    if (failure == null) {
                        pass++;
                        totalPass++;
                    } else {
                        failures.add(failure);
                    }
                } catch (Throwable ex) {
                    failures.add(new TrialFailure(opByte, t,
                            "exception " + ex.getClass().getSimpleName() + ": " + ex.getMessage()));
                }
            }
            passByOpcode.put(opByte, pass);
        }

        System.out.println("CPU RANDOMIZED OPCODE HARNESS");
        System.out.println("base_seed=" + baseSeed + " cycle_xor=" + cycleXor +
                " seed=" + seed + " trials_per_opcode=" + trialsPerOpcode + " rng=fast");
        System.out.println("total=" + totalPass + "/" + totalChecks + " passed");

        int failOpcodes = 0;
        for (Map.Entry<Integer, Integer> e : passByOpcode.entrySet()) {
            int op = e.getKey().intValue();
            int pass = e.getValue().intValue();
            if (pass != trialsPerOpcode) {
                failOpcodes++;
            }
            System.out.println(String.format("OP %02X %-3s %-8s pass=%3d/%3d",
                    op,
                    Cpu65c02.OPCODE[op].getMnemonic(),
                    Cpu65c02.OPCODE[op].getAddressMode(),
                    pass,
                    trialsPerOpcode));
        }

        System.out.println("failing_opcodes=" + failOpcodes);
        int printed = 0;
        for (TrialFailure f : failures) {
            if (printed >= 40) {
                System.out.println("... " + (failures.size() - printed) + " more failures");
                break;
            }
            System.out.println(String.format("FAIL op=%02X trial=%d %s", f.opcode, f.trial, f.reason));
            printed++;
        }

        if (!failures.isEmpty()) {
            System.exit(1);
        }
    }

    @Test
    public void randomizedOpcodeReferenceParity() throws Exception {
        String trials = System.getProperty("ever2e.cpu.randomized.trials", "8");
        String seed = System.getProperty("ever2e.cpu.randomized.seed", "6619714");
        main(new String[] {"--trials", trials, "--seed", seed});
    }

    private static void runRngSelfTest(Random rng, int count, long baseSeed, long cycleXor, long seed) {
        long sum = 0L;
        int min = 255;
        int max = 0;
        for (int i = 0; i < count; i++) {
            int v = rng.nextInt(256);
            sum += v;
            if (v < min) {
                min = v;
            }
            if (v > max) {
                max = v;
            }
        }
        double average = sum / (double) count;
        double expected = 127.5;
        double delta = average - expected;

        System.out.println("FAST RNG SELF TEST");
        System.out.println("base_seed=" + baseSeed + " cycle_xor=" + cycleXor + " seed=" + seed);
        System.out.println("samples=" + count + " min=" + min + " max=" + max);
        System.out.println(String.format("average=%.6f expected=%.6f delta=%.6f", average, expected, delta));
    }

    private static long estimateMacCpuCycleCount() {
        long cpuTimeNs = getProcessCpuTimeNs();
        long cpuHz = getMacCpuFrequencyHz();
        if (cpuTimeNs > 0 && cpuHz > 0) {
            double cycles = (cpuTimeNs / 1_000_000_000.0) * cpuHz;
            return (long) cycles;
        }
        return System.nanoTime();
    }

    private static long getProcessCpuTimeNs() {
        try {
            OperatingSystemMXBean os = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            return os.getProcessCpuTime();
        } catch (Throwable ignored) {
            return -1L;
        }
    }

    private static long getMacCpuFrequencyHz() {
        String osName = System.getProperty("os.name", "").toLowerCase();
        if (!osName.contains("mac")) {
            return -1L;
        }
        long hz = readSysctlLong("hw.cpufrequency");
        if (hz > 0) {
            return hz;
        }
        hz = readSysctlLong("hw.cpufrequency_max");
        if (hz > 0) {
            return hz;
        }
        return -1L;
    }

    private static long readSysctlLong(String key) {
        Process p = null;
        try {
            p = new ProcessBuilder("sysctl", "-n", key).start();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line = br.readLine();
                int rc = p.waitFor();
                if (rc == 0 && line != null) {
                    return Long.parseLong(line.trim());
                }
            }
        } catch (Throwable ignored) {
            // fall through
        } finally {
            if (p != null) {
                p.destroy();
            }
        }
        return -1L;
    }

    private static TrialFailure runTrial(int opByte, Opcode opcode, int trial, Random rng) throws Exception {
        CpuEnv env = createCpuEnv();
        Setup setup = new Setup();
        setup.opcodeByte = opByte;
        setup.opcode = opcode;

        RefState before = randomStateForTrial(opcode, rng);
        prepareInstructionBytes(env, setup, rng, before);
        runInstruction(env); // execute reset; next opcode is now fetched from PROG_PC

        applyCpuState(env.cpu.getRegister(), before);

        RefState expected = before.copy();
        RefMem refMem = new RefMem(env.bus);
        runReference(expected, setup, refMem, env.rom);

        runInstruction(env); // execute target opcode

        Register got = env.cpu.getRegister();
        if (got.getA() != expected.a) {
            return new TrialFailure(opByte, trial,
                    String.format("A exp=%02X got=%02X", expected.a, got.getA()));
        }
        if (got.getX() != expected.x) {
            return new TrialFailure(opByte, trial,
                    String.format("X exp=%02X got=%02X", expected.x, got.getX()));
        }
        if (got.getY() != expected.y) {
            return new TrialFailure(opByte, trial,
                    String.format("Y exp=%02X got=%02X", expected.y, got.getY()));
        }
        if (got.getS() != expected.s) {
            return new TrialFailure(opByte, trial,
                    String.format("S exp=%02X got=%02X", expected.s, got.getS()));
        }
        if ((got.getP() & 0xFF) != (expected.p & 0xFF)) {
            return new TrialFailure(opByte, trial,
                    String.format("P exp=%02X got=%02X", expected.p & 0xFF, got.getP() & 0xFF));
        }
        int gotNextPc = env.cpu.getPendingPC();
        if (gotNextPc != expected.pc) {
            return new TrialFailure(opByte, trial,
                    String.format("PC exp=%04X got=%04X", expected.pc, gotNextPc));
        }

        return null;
    }

    private static void runInstruction(CpuEnv env) throws Exception {
        while (true) {
            boolean instructionEndsThisCycle = env.cpu.hasPendingInstructionEndEvent();
            env.emulator.startWithStepPhases(1, env.cpu, (step, manager, preCycle) -> true);
            if (instructionEndsThisCycle) {
                return;
            }
        }
    }

    private static CpuEnv createCpuEnv() throws HardwareException {
        Memory8 mem = new Memory8(MEM_SIZE);
        byte[] rom = new byte[ROM_SIZE];
        MemoryBusIIe bus = new MemoryBusIIe(mem, rom);
        Cpu65c02 cpu = new Cpu65c02(bus, 0);
        PriorityQueue<HardwareManager> queue = new PriorityQueue<HardwareManager>();
        queue.add(cpu);
        Emulator emulator = new Emulator(queue, 0);
        cpu.coldReset();

        rom[0x3ffc] = (byte) (PROG_PC & 0xFF);
        rom[0x3ffd] = (byte) ((PROG_PC >> 8) & 0xFF);

        return new CpuEnv(mem, bus, cpu, emulator, rom);
    }

    private static RefState randomStateForTrial(Opcode opcode, Random rng) {
        RefState s = new RefState();
        s.a = rng.nextInt(256);
        s.x = rng.nextInt(256);
        s.y = rng.nextInt(256);
        s.s = rng.nextInt(256);
        s.p = rng.nextInt(256);

        if (opcode.getMnemonic() == OpcodeMnemonic.ADC || opcode.getMnemonic() == OpcodeMnemonic.SBC) {
            s.p &= ~FLAG_D;
        }

        s.pc = PROG_PC;
        return s;
    }

    private static void applyCpuState(Register reg, RefState s) {
        reg.setA(s.a);
        reg.setX(s.x);
        reg.setY(s.y);
        reg.setS(s.s);
        reg.setP(s.p);
    }

    private static void prepareInstructionBytes(CpuEnv env, Setup setup, Random rng, RefState state) {
        Opcode op = setup.opcode;
        int pc = PROG_PC;

        env.bus.setByte(pc, setup.opcodeByte);

        int lo;
        int hi;
        int zp;
        int ptr;
        int target;
        int offset;

        switch (op.getAddressMode()) {
            case IMM:
                env.bus.setByte(pc + 1, rng.nextInt(256));
                setup.operandPtr = pc + 1;
                break;
            case ABS:
                target = randomSafeAddress(rng);
                env.bus.setByte(pc + 1, target & 0xFF);
                env.bus.setByte(pc + 2, (target >> 8) & 0xFF);
                env.bus.setByte(target, rng.nextInt(256));
                setup.operandPtr = target;
                break;
            case ZPG:
                zp = rng.nextInt(256);
                env.bus.setByte(pc + 1, zp);
                env.bus.setByte(zp, rng.nextInt(256));
                setup.operandPtr = zp;
                break;
            case ACC:
            case IMP:
                setup.operandPtr = 0;
                break;
            case IND_X:
                zp = rng.nextInt(256);
                target = randomSafeAddress(rng);
                env.bus.setByte(pc + 1, zp);
                env.bus.setByte((zp + state.x) & 0xFF, target & 0xFF);
                env.bus.setByte((zp + state.x + 1) & 0xFF, (target >> 8) & 0xFF);
                env.bus.setByte(target, rng.nextInt(256));
                setup.operandPtr = target;
                break;
            case IND_Y:
                zp = rng.nextInt(256);
                target = randomSafeAddress(rng);
                ptr = (target - state.y) & 0xFFFF;
                env.bus.setByte(pc + 1, zp);
                env.bus.setByte(zp, ptr & 0xFF);
                env.bus.setByte((zp + 1) & 0xFF, (ptr >> 8) & 0xFF);
                env.bus.setByte(target, rng.nextInt(256));
                setup.operandPtr = target;
                break;
            case ZPG_X:
                zp = rng.nextInt(256);
                env.bus.setByte(pc + 1, zp);
                env.bus.setByte((zp + state.x) & 0xFF, rng.nextInt(256));
                setup.operandPtr = (zp + state.x) & 0xFF;
                break;
            case ZPG_Y:
                zp = rng.nextInt(256);
                env.bus.setByte(pc + 1, zp);
                env.bus.setByte((zp + state.y) & 0xFF, rng.nextInt(256));
                setup.operandPtr = (zp + state.y) & 0xFF;
                break;
            case ABS_X:
                ptr = randomSafeAddressBaseForIndex(rng, state.x);
                target = (ptr + state.x) & 0xFFFF;
                env.bus.setByte(pc + 1, ptr & 0xFF);
                env.bus.setByte(pc + 2, (ptr >> 8) & 0xFF);
                env.bus.setByte(target, rng.nextInt(256));
                setup.operandPtr = target;
                break;
            case ABS_Y:
                ptr = randomSafeAddressBaseForIndex(rng, state.y);
                target = (ptr + state.y) & 0xFFFF;
                env.bus.setByte(pc + 1, ptr & 0xFF);
                env.bus.setByte(pc + 2, (ptr >> 8) & 0xFF);
                env.bus.setByte(target, rng.nextInt(256));
                setup.operandPtr = target;
                break;
            case REL:
                offset = rng.nextInt(256);
                env.bus.setByte(pc + 1, offset);
                setup.operandPtr = pc + 1;
                break;
            case ABS_IND:
                ptr = randomSafeAddress(rng);
                target = randomSafeAddress(rng);
                env.bus.setByte(pc + 1, ptr & 0xFF);
                env.bus.setByte(pc + 2, (ptr >> 8) & 0xFF);
                env.bus.setByte(ptr, target & 0xFF);
                env.bus.setByte((ptr + 1) & 0xFFFF, (target >> 8) & 0xFF);
                setup.operandPtr = target;
                break;
            case ABS_IND_X:
                ptr = randomSafeAddressBaseForIndex(rng, state.x);
                target = randomSafeAddress(rng);
                env.bus.setByte(pc + 1, ptr & 0xFF);
                env.bus.setByte(pc + 2, (ptr >> 8) & 0xFF);
                lo = target & 0xFF;
                hi = (target >> 8) & 0xFF;
                env.bus.setByte((ptr + state.x) & 0xFFFF, lo);
                env.bus.setByte((ptr + state.x + 1) & 0xFFFF, hi);
                setup.operandPtr = target;
                break;
            case ZPG_IND:
                zp = rng.nextInt(256);
                target = randomSafeAddress(rng);
                env.bus.setByte(pc + 1, zp);
                env.bus.setByte(zp, target & 0xFF);
                env.bus.setByte((zp + 1) & 0xFF, (target >> 8) & 0xFF);
                env.bus.setByte(target, rng.nextInt(256));
                setup.operandPtr = target;
                break;
            default:
                throw new IllegalStateException("Unhandled mode " + op.getAddressMode());
        }

        if (op.getMnemonic() == OpcodeMnemonic.BRK) {
            int vec = randomSafeAddress(rng);
            env.rom[0x3ffe] = (byte) (vec & 0xFF);
            env.rom[0x3fff] = (byte) ((vec >> 8) & 0xFF);
        }
    }

    private static int randomSafeAddress(Random rng) {
        while (true) {
            int candidate = SAFE_ADDR_MIN + rng.nextInt(SAFE_ADDR_MAX - SAFE_ADDR_MIN + 1);
            if (candidate >= PROG_PC && candidate <= (PROG_PC + 2)) {
                continue;
            }
            return candidate;
        }
    }

    private static int randomSafeAddressBaseForIndex(Random rng, int index) {
        int maxBase = SAFE_ADDR_MAX - (index & 0xFF);
        if (maxBase < SAFE_ADDR_MIN) {
            maxBase = SAFE_ADDR_MIN;
        }
        while (true) {
            int candidate = SAFE_ADDR_MIN + rng.nextInt(maxBase - SAFE_ADDR_MIN + 1);
            if (candidate >= (PROG_PC - (index & 0xFF)) && candidate <= ((PROG_PC + 2) - (index & 0xFF))) {
                continue;
            }
            return candidate;
        }
    }

    private static int resolveOperandPtr(RefState s, Setup setup, RefMem mem) {
        int pc = s.pc;
        int operandCounter = (pc + 1) & 0xFFFF;

        switch (setup.opcode.getAddressMode()) {
            case IMM:
                return operandCounter;
            case ABS:
                return mem.getWord16LittleEndian(operandCounter);
            case ZPG:
                return mem.getByte(operandCounter);
            case ACC:
            case IMP:
                return 0;
            case IND_X: {
                int zp = (mem.getByte(operandCounter) + s.x) & 0xFF;
                return mem.getWord16LittleEndian(zp, 0xFF);
            }
            case IND_Y: {
                int zp = mem.getByte(operandCounter);
                int base = mem.getWord16LittleEndian(zp, 0xFF);
                return (base + s.y) & 0xFFFF;
            }
            case ZPG_X:
                return (mem.getByte(operandCounter) + s.x) & 0xFF;
            case ZPG_Y:
                return (mem.getByte(operandCounter) + s.y) & 0xFF;
            case ABS_X:
                return (mem.getWord16LittleEndian(operandCounter) + s.x) & 0xFFFF;
            case ABS_Y:
                return (mem.getWord16LittleEndian(operandCounter) + s.y) & 0xFFFF;
            case REL:
                return operandCounter;
            case ABS_IND: {
                int ptr = mem.getWord16LittleEndian(operandCounter);
                return mem.getWord16LittleEndian(ptr);
            }
            case ABS_IND_X: {
                int ptr = (mem.getWord16LittleEndian(operandCounter) + s.x) & 0xFFFF;
                return mem.getWord16LittleEndian(ptr);
            }
            case ZPG_IND: {
                int zp = mem.getByte(operandCounter);
                return mem.getWord16LittleEndian(zp, 0xFF);
            }
            default:
                throw new IllegalStateException("Unhandled mode " + setup.opcode.getAddressMode());
        }
    }

    private static void runReference(RefState s, Setup setup, RefMem mem, byte[] rom) {
        int instrSize = setup.opcode.getInstrSize() & 0xFF;
        int nextPc = (s.pc + instrSize) & 0xFFFF;
        int operandPtr = resolveOperandPtr(s, setup, mem);
        int value;

        switch (setup.opcode.getMnemonic()) {
            case ADC: {
                value = mem.getByte(operandPtr);
                int carryIn = flag(s.p, FLAG_C) ? 1 : 0;
                int a0 = s.a;
                int sum = a0 + value + carryIn;
                s.a = sum & 0xFF;
                setFlag(s, FLAG_C, (sum & 0x100) != 0);
                setZN(s, s.a);
                setFlag(s, FLAG_V, ((a0 ^ s.a) & (value ^ s.a) & 0x80) != 0);
                s.pc = nextPc;
                break;
            }
            case AND:
                s.a = s.a & mem.getByte(operandPtr);
                setZN(s, s.a);
                s.pc = nextPc;
                break;
            case ASL:
                if (setup.opcode.getAddressMode() == AddressMode.ACC) {
                    int out = (s.a << 1) & 0x1FF;
                    s.a = out & 0xFF;
                    setFlag(s, FLAG_C, (out & 0x100) != 0);
                    setZN(s, s.a);
                } else {
                    int out = (mem.getByte(operandPtr) << 1) & 0x1FF;
                    mem.setByte(operandPtr, out & 0xFF);
                    setFlag(s, FLAG_C, (out & 0x100) != 0);
                    setZN(s, out & 0xFF);
                }
                s.pc = nextPc;
                break;
            case BCC:
                s.pc = !flag(s.p, FLAG_C) ? branchTarget(nextPc, mem.getByte(operandPtr)) : nextPc;
                break;
            case BCS:
                s.pc = flag(s.p, FLAG_C) ? branchTarget(nextPc, mem.getByte(operandPtr)) : nextPc;
                break;
            case BEQ:
                s.pc = flag(s.p, FLAG_Z) ? branchTarget(nextPc, mem.getByte(operandPtr)) : nextPc;
                break;
            case BIT: {
                value = mem.getByte(operandPtr);
                if (setup.opcode.getAddressMode() != AddressMode.IMM) {
                    s.p = (s.p & 0x3F) | (value & 0xC0);
                }
                setFlag(s, FLAG_Z, ((s.a & value) & 0xFF) == 0);
                s.pc = nextPc;
                break;
            }
            case BMI:
                s.pc = flag(s.p, FLAG_N) ? branchTarget(nextPc, mem.getByte(operandPtr)) : nextPc;
                break;
            case BNE:
                s.pc = !flag(s.p, FLAG_Z) ? branchTarget(nextPc, mem.getByte(operandPtr)) : nextPc;
                break;
            case BPL:
                s.pc = !flag(s.p, FLAG_N) ? branchTarget(nextPc, mem.getByte(operandPtr)) : nextPc;
                break;
            case BRA:
                s.pc = branchTarget(nextPc, mem.getByte(operandPtr));
                break;
            case BRK:
                push(s, mem, (nextPc >> 8) & 0xFF);
                push(s, mem, nextPc & 0xFF);
                push(s, mem, s.p | FLAG_B);
                s.p |= FLAG_I;
                s.p &= ~FLAG_D;
                s.pc = ((rom[0x3fff] & 0xFF) << 8) | (rom[0x3ffe] & 0xFF);
                break;
            case BVC:
                s.pc = !flag(s.p, FLAG_V) ? branchTarget(nextPc, mem.getByte(operandPtr)) : nextPc;
                break;
            case BVS:
                s.pc = flag(s.p, FLAG_V) ? branchTarget(nextPc, mem.getByte(operandPtr)) : nextPc;
                break;
            case CLC:
                s.p &= ~FLAG_C;
                s.pc = nextPc;
                break;
            case CLD:
                s.p &= ~FLAG_D;
                s.pc = nextPc;
                break;
            case CLI:
                s.p &= ~FLAG_I;
                s.pc = nextPc;
                break;
            case CLV:
                s.p &= ~FLAG_V;
                s.pc = nextPc;
                break;
            case CMP:
                cmpSetFlags(s, s.a, mem.getByte(operandPtr));
                s.pc = nextPc;
                break;
            case CPX:
                cmpSetFlags(s, s.x, mem.getByte(operandPtr));
                s.pc = nextPc;
                break;
            case CPY:
                cmpSetFlags(s, s.y, mem.getByte(operandPtr));
                s.pc = nextPc;
                break;
            case DEA:
                s.a = (s.a - 1) & 0xFF;
                setZN(s, s.a);
                s.pc = nextPc;
                break;
            case DEC:
                value = (mem.getByte(operandPtr) - 1) & 0xFF;
                mem.setByte(operandPtr, value);
                setZN(s, value);
                s.pc = nextPc;
                break;
            case DEX:
                s.x = (s.x - 1) & 0xFF;
                setZN(s, s.x);
                s.pc = nextPc;
                break;
            case DEY:
                s.y = (s.y - 1) & 0xFF;
                setZN(s, s.y);
                s.pc = nextPc;
                break;
            case EOR:
                s.a = (s.a ^ mem.getByte(operandPtr)) & 0xFF;
                setZN(s, s.a);
                s.pc = nextPc;
                break;
            case INA:
                s.a = (s.a + 1) & 0xFF;
                setZN(s, s.a);
                s.pc = nextPc;
                break;
            case INC:
                value = (mem.getByte(operandPtr) + 1) & 0xFF;
                mem.setByte(operandPtr, value);
                setZN(s, value);
                s.pc = nextPc;
                break;
            case INX:
                s.x = (s.x + 1) & 0xFF;
                setZN(s, s.x);
                s.pc = nextPc;
                break;
            case INY:
                s.y = (s.y + 1) & 0xFF;
                setZN(s, s.y);
                s.pc = nextPc;
                break;
            case JMP:
                s.pc = operandPtr;
                break;
            case JSR:
                push(s, mem, ((nextPc - 1) >> 8) & 0xFF);
                push(s, mem, (nextPc - 1) & 0xFF);
                s.pc = operandPtr;
                break;
            case LDA:
                s.a = mem.getByte(operandPtr);
                setZN(s, s.a);
                s.pc = nextPc;
                break;
            case LDX:
                s.x = mem.getByte(operandPtr);
                setZN(s, s.x);
                s.pc = nextPc;
                break;
            case LDY:
                s.y = mem.getByte(operandPtr);
                setZN(s, s.y);
                s.pc = nextPc;
                break;
            case LSR:
                if (setup.opcode.getAddressMode() == AddressMode.ACC) {
                    setFlag(s, FLAG_C, (s.a & 0x01) != 0);
                    s.a = (s.a >> 1) & 0x7F;
                    setFlag(s, FLAG_N, false);
                    setFlag(s, FLAG_Z, s.a == 0);
                } else {
                    value = mem.getByte(operandPtr);
                    setFlag(s, FLAG_C, (value & 0x01) != 0);
                    value = (value >> 1) & 0x7F;
                    mem.setByte(operandPtr, value);
                    setFlag(s, FLAG_N, false);
                    setFlag(s, FLAG_Z, value == 0);
                }
                s.pc = nextPc;
                break;
            case NOP:
                s.pc = nextPc;
                break;
            case ORA:
                s.a = (s.a | mem.getByte(operandPtr)) & 0xFF;
                setZN(s, s.a);
                s.pc = nextPc;
                break;
            case PHA:
                push(s, mem, s.a);
                s.pc = nextPc;
                break;
            case PHP:
                push(s, mem, s.p);
                s.pc = nextPc;
                break;
            case PHX:
                push(s, mem, s.x);
                s.pc = nextPc;
                break;
            case PHY:
                push(s, mem, s.y);
                s.pc = nextPc;
                break;
            case PLA:
                s.a = pop(s, mem);
                setZN(s, s.a);
                s.pc = nextPc;
                break;
            case PLP:
                s.p = pop(s, mem) | FLAG_B;
                s.pc = nextPc;
                break;
            case PLX:
                s.x = pop(s, mem);
                setZN(s, s.x);
                s.pc = nextPc;
                break;
            case PLY:
                s.y = pop(s, mem);
                setZN(s, s.y);
                s.pc = nextPc;
                break;
            case ROL:
                if (setup.opcode.getAddressMode() == AddressMode.ACC) {
                    int carryIn = flag(s.p, FLAG_C) ? 1 : 0;
                    int out = ((s.a << 1) | carryIn) & 0x1FF;
                    setFlag(s, FLAG_C, (out & 0x100) != 0);
                    s.a = out & 0xFF;
                    setZN(s, s.a);
                } else {
                    int carryIn = flag(s.p, FLAG_C) ? 1 : 0;
                    int out = ((mem.getByte(operandPtr) << 1) | carryIn) & 0x1FF;
                    setFlag(s, FLAG_C, (out & 0x100) != 0);
                    out &= 0xFF;
                    mem.setByte(operandPtr, out);
                    setZN(s, out);
                }
                s.pc = nextPc;
                break;
            case ROR:
                if (setup.opcode.getAddressMode() == AddressMode.ACC) {
                    int in = s.a | (flag(s.p, FLAG_C) ? 0x100 : 0);
                    setFlag(s, FLAG_C, (in & 0x01) != 0);
                    s.a = (in >> 1) & 0xFF;
                    setZN(s, s.a);
                } else {
                    int in = mem.getByte(operandPtr) | (flag(s.p, FLAG_C) ? 0x100 : 0);
                    setFlag(s, FLAG_C, (in & 0x01) != 0);
                    int out = (in >> 1) & 0xFF;
                    mem.setByte(operandPtr, out);
                    setZN(s, out);
                }
                s.pc = nextPc;
                break;
            case RTI:
                s.p = pop(s, mem) | FLAG_B;
                s.pc = pop(s, mem) | (pop(s, mem) << 8);
                break;
            case RTS:
                s.pc = ((pop(s, mem) | (pop(s, mem) << 8)) + 1) & 0xFFFF;
                break;
            case SBC: {
                value = mem.getByte(operandPtr);
                int a0 = s.a;
                int inv = value ^ 0xFF;
                int sum = a0 + inv + (flag(s.p, FLAG_C) ? 1 : 0);
                s.a = sum & 0xFF;
                setFlag(s, FLAG_C, (sum & 0x100) != 0);
                setZN(s, s.a);
                setFlag(s, FLAG_V, ((a0 ^ s.a) & (inv ^ s.a) & 0x80) != 0);
                s.pc = nextPc;
                break;
            }
            case SEC:
                s.p |= FLAG_C;
                s.pc = nextPc;
                break;
            case SED:
                s.p |= FLAG_D;
                s.pc = nextPc;
                break;
            case SEI:
                s.p |= FLAG_I;
                s.pc = nextPc;
                break;
            case STA:
                mem.setByte(operandPtr, s.a);
                s.pc = nextPc;
                break;
            case STX:
                mem.setByte(operandPtr, s.x);
                s.pc = nextPc;
                break;
            case STY:
                mem.setByte(operandPtr, s.y);
                s.pc = nextPc;
                break;
            case STZ:
                mem.setByte(operandPtr, 0);
                s.pc = nextPc;
                break;
            case TAX:
                s.x = s.a;
                setZN(s, s.x);
                s.pc = nextPc;
                break;
            case TAY:
                s.y = s.a;
                setZN(s, s.y);
                s.pc = nextPc;
                break;
            case TRB:
                value = mem.getByte(operandPtr);
                setFlag(s, FLAG_Z, (s.a & value) == 0);
                mem.setByte(operandPtr, value & (~s.a));
                s.pc = nextPc;
                break;
            case TSB:
                value = mem.getByte(operandPtr);
                setFlag(s, FLAG_Z, (s.a & value) == 0);
                mem.setByte(operandPtr, value | s.a);
                s.pc = nextPc;
                break;
            case TSX:
                s.x = s.s;
                setZN(s, s.x);
                s.pc = nextPc;
                break;
            case TXA:
                s.a = s.x;
                setZN(s, s.a);
                s.pc = nextPc;
                break;
            case TXS:
                s.s = s.x;
                s.pc = nextPc;
                break;
            case TYA:
                s.a = s.y;
                setZN(s, s.a);
                s.pc = nextPc;
                break;
            default:
                throw new IllegalStateException("Unsupported mnemonic " + setup.opcode.getMnemonic());
        }
    }

    private static void cmpSetFlags(RefState s, int lhs, int rhs) {
        int result = (lhs - rhs) & 0x1FF;
        setFlag(s, FLAG_C, lhs >= rhs);
        setFlag(s, FLAG_Z, (result & 0xFF) == 0);
        setFlag(s, FLAG_N, (result & 0x80) != 0);
    }

    private static int branchTarget(int nextPc, int offsetByte) {
        int signed = (byte) (offsetByte & 0xFF);
        return (nextPc + signed) & 0xFFFF;
    }

    private static int pop(RefState s, RefMem mem) {
        s.s = (s.s + 1) & 0xFF;
        return mem.getByte(0x100 | s.s);
    }

    private static void push(RefState s, RefMem mem, int value) {
        mem.setByte(0x100 | s.s, value & 0xFF);
        s.s = (s.s - 1) & 0xFF;
    }

    private static boolean flag(int p, int mask) {
        return (p & mask) != 0;
    }

    private static void setFlag(RefState s, int mask, boolean on) {
        if (on) {
            s.p |= mask;
        } else {
            s.p &= ~mask;
        }
        s.p &= 0xFF;
    }

    private static void setZN(RefState s, int value) {
        value &= 0xFF;
        setFlag(s, FLAG_Z, value == 0);
        setFlag(s, FLAG_N, (value & 0x80) != 0);
    }
}
