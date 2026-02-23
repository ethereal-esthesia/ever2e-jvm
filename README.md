# Ever2e

JVM reference implementation and trace source for Apple IIe / 65C02 behavior.

## What this repo is for

- Run the JVM Apple IIe emulator from `.emu` machine configs.
- Generate per-step CPU traces.
- Compare behavior against the Python port and MAME traces.
- Experiment with soft-switch, slot ROM, and reset behavior.

## Repo layout

- `src/core/emulator/machine/machine8/Emulator8Coordinator.java`: main runner
- `src/core/cpu/cpu8/Cpu65c02.java`: 65C02 core
- `src/core/memory/memory8/MemoryBusIIe.java`: Apple IIe memory map + soft switches
- `ROMS/`: machine configs and ROM binaries

## Build

```bash
cd /Users/shane/Project/ever2e-jvm
./gradlew classes
```

## Run

Default machine:

```bash
cd /Users/shane/Project/ever2e-jvm
./gradlew runHeadless
```

Specific `.emu` file:

```bash
./gradlew runHeadless --args="ROMS/Apple2e.emu"
```

Windowed run (LWJGL):

```bash
./gradlew run
```

## CLI args

- `--steps N`
  - Max CPU steps to execute (required for bounded trace runs).
- `--trace-file <path>`
  - Write CPU trace CSV.
- `--trace-phase pre|post`
  - Snapshot phase for instruction rows.
- `--post`
  - Shortcut for `--trace-phase post`.
- `--text-console`
  - Run with console text display (no GUI window).
- `--print-text-at-exit`
  - Print the active 40x24 text page on exit (headless/text-console useful).
- `--show-fps`
  - Print windowed display FPS once per second to stderr.
- `--trace-start-pc <addr>`
  - Do not emit trace rows until this PC is reached (inclusive), then continue tracing normally.
- `--reset-pflag-value <value>`
  - Override reset-time `P` policy (hex `0x..` or decimal). Bits `0x20` and `0x10` stay asserted.
- `--halt-execution <addr>`
  - Stop execution when PC reaches the address (hex `0x....` or decimal).
- `--paste-file <path>`
  - Queue BASIC source text into the keyboard input queue at startup (same CR conversion as paste).
- `--no-sound`
  - Disable speaker initialization and run without audio output.
- `--debug`
  - Enable emulator stdout logging (logging is quiet by default).
- `--no-logging`
  - Force quiet mode (kept for compatibility).

Startup behavior:
- With sound enabled, the emulator performs an internal silent JIT-prime pass (300000 steps) on the same object graph before normal logging begins, to reduce startup audio jitter.

## Keyboard shortcuts

- `Insert`
  - Clear queued keyboard input.
- `Shift+Insert`
  - Paste clipboard text into queued keyboard input.
- `Ctrl+F12`
  - Trigger reset interrupt sequence.

## Trace output format

When `--trace-file` is used:

- Header:
  - `step,event_type,event,pc,opcode,a,x,y,p,s,mnemonic,mode`
- Reset row:
  - `event_type=event`, `event=RESET`
- Instruction rows:
  - `event_type=instr`, `event=` (empty)

Notes:

- `RESET` is a trace event, not a fetched opcode.
- Trace phase defaults to `pre`.
- Use `--trace-phase post` (or `--post`) for post-step rows.

## Examples

Generate a pre-phase trace:

```bash
./gradlew runHeadless --args="ROMS/Apple2eMemCheck.emu --steps 5000 --trace-file /tmp/jvm_trace.csv"
```

Generate a post-phase trace:

```bash
./gradlew runHeadless --args="ROMS/Apple2eMemCheck.emu --steps 5000 --trace-file /tmp/jvm_trace_post.csv --trace-phase post"
```

Generate a trace that starts only at a target PC:

```bash
./gradlew runHeadless --args="ROMS/Apple2e.emu --steps 200000 --trace-file /tmp/jvm_trace_basic.csv --trace-phase pre --trace-start-pc 0xE000"
```

Run with reset `P` policy and halt point:

```bash
./gradlew runHeadless --args="ROMS/Apple2eMemCheck.emu --steps 200000 --trace-phase pre --reset-pflag-value 0x36 --halt-execution 0xC70B --trace-file /tmp/jvm_halt_trace.csv"
```

Queue a BASIC program file for typed input:

```bash
./gradlew runHeadless --args="ROMS/Apple2e.emu --steps 200000 --paste-file samples/VBL_TEST.BAS"
```

Queue the combined 4-slice opcode monitor loader (generated from `python_port`) and execute:

```bash
./gradlew runHeadless --args="ROMS/Apple2e.emu --steps 30000000 --paste-file /Users/shane/Project/ever2e/python_port/tools/opcode_smoke_loader_4x.mon --print-text-at-exit"
```

## Known gaps

- Expansion ROM behavior is still under active parity work.
- Full peripheral fidelity and cycle-accurate parity are incomplete.


