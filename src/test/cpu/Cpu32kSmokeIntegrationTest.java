package test.cpu;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

import core.emulator.machine.machine8.Emulator8Coordinator;

import static org.junit.Assert.assertTrue;

public class Cpu32kSmokeIntegrationTest {

	@Test
	public void opcodeSmoke32kReachesPassLoop() throws Exception {
		String emuFile = System.getProperty("ever2e.smoke32k.emu", "ROMS/Apple2e.emu");
		String pasteFile = System.getProperty(
				"ever2e.smoke32k.pasteFile",
				"ROMS/opcode_smoke_loader_hgr_mem_32k.mon");
		String steps = System.getProperty("ever2e.smoke32k.steps", "80000000");
		String haltExecution = System.getProperty("ever2e.smoke32k.haltExecution", "0x6A45,0x6A33");

		ByteArrayOutputStream output = new ByteArrayOutputStream();
		PrintStream originalOut = System.out;
		String previousHeadless = System.getProperty("java.awt.headless");
		try {
			System.setProperty("java.awt.headless", "true");
			System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
			Emulator8Coordinator.main(new String[] {
					emuFile,
					"--steps", steps,
					"--text-console",
					"--paste-file", pasteFile,
					"--halt-execution", haltExecution,
					"--print-cpu-state-at-exit",
					"--print-text-at-exit",
					"--no-sound",
			});
		}
		finally {
			System.setOut(originalOut);
			if( previousHeadless==null )
				System.clearProperty("java.awt.headless");
			else
				System.setProperty("java.awt.headless", previousHeadless);
		}

		String stdout = output.toString(StandardCharsets.UTF_8);
		assertTrue("32k smoke should stop at pass loop 0x6A45.\nOutput:\n"+stdout,
				stdout.contains("Stopped at PC=6A45"));
		assertTrue("32k smoke should not stop at fail loop 0x6A33.\nOutput:\n"+stdout,
				!stdout.contains("Stopped at PC=6A33"));
	}
}
