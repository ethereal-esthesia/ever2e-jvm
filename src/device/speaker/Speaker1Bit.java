package device.speaker;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import core.exception.HardwareException;
import core.memory.memory8.MemoryBusIIe;
import core.emulator.HardwareManager;

public class Speaker1Bit extends HardwareManager  {

	private static final int SAMPLE_BUFFER_SAMPLES = 1024;   // Lag of 1/40th to 1/20th of a second at 22050Hz
	private static final float MAX_SOUND_WORD = 32767f;   // Min and max limits on sound resolution
	private static final float MIN_SOUND_WORD = -32768f;
	private static final float SAMPLE_RATE = 22050;       // Sound samples per second
	private static final int SAMPLE_DURATION = (int) (1000000000/SAMPLE_RATE);    // Sample length in nanoseconds

	private static final int SKIP_CYCLES = 4;   // Granularity of physics simulation (speaker sensitivity)
	                                            // A value of 1 checks for changes every cycle, 4 every 4 cycles, etc.
	
	private static final float FRICTION = .01f;                                 // 1% acceleration loss per unit velocity
	private static final int CHARGE_DURATION = 20/SKIP_CYCLES;                  // 20 cycles
	private static final float MAGNET_FORCE = 11f*SKIP_CYCLES*SKIP_CYCLES;      // Magnet acceleration in units distance per increment
	private static final float SPRING_FORCE = .00049f*SKIP_CYCLES*SKIP_CYCLES;  // Units reverse spring acceleration per unit distance per increment

/*
	// Remove typical Apple IIe speaker static
	private static final float FRICTION = .02;        
	private static final Sint32 CHARGE_DURATION = 10/SKIP_CYCLES; 
	private static final float MAGNET_FORCE = 34*SKIP_CYCLES*SKIP_CYCLES;
	private static final float SPRING_FORCE = .0002*SKIP_CYCLES*SKIP_CYCLES;
*/

	private MemoryBusIIe bus;

	private byte [] buffer;
	private AudioFormat audioFormat;
	private int bytesPerSample;
	private SourceDataLine sdl;
	private int bufferIndex;
	private boolean closed;
	private volatile long muteUntilNs;

	private float pos;
	private float vel;

	private boolean toggleChargeNegative;
	private float charge;
	private int chargeDur;
	
	private float sampleSum;
	private int sampleTotal;
	private double sampleLength;
	private double durationInc;
	
	public Speaker1Bit( MemoryBusIIe bus, long unitsPerCycle, long bitGranularity ) throws LineUnavailableException {

		super(unitsPerCycle);
		durationInc = 1000000000d*(unitsPerCycle/Math.pow(2d, bitGranularity)/1000d)*SKIP_CYCLES;
		this.bus = bus;
		
			initializeAudioLine();
			bufferIndex = 0;
			closed = false;
			muteUntilNs = 0L;
        
		pos = 0f;
		vel = 0f;
		toggleChargeNegative = false;
		charge = 0f;
		chargeDur = 0;
		
		sampleSum = 0f;
		sampleTotal = 0;
		sampleLength = 0;
	
			open();
			Runtime.getRuntime().addShutdownHook(new Thread(this::close));
		
	}
	
	public void toggle()
	{
		chargeDur = CHARGE_DURATION;
		charge = toggleChargeNegative ? -MAGNET_FORCE : MAGNET_FORCE;
		toggleChargeNegative = !toggleChargeNegative;
	}

	public static int getSkipCycles() {
		return SKIP_CYCLES;
	}

	@Override
	public void cycle() throws HardwareException {

		if( bus.isSpeakerToggle() ) {
			toggle();
			bus.setSpeakerToggle(false);
		}
		
		super.incSleepCycles(SKIP_CYCLES);
		advanceSimulation(true);
		
	}

	public synchronized void warmupIterations(int iterations) {
		if( iterations<=0 )
			return;
		boolean savedBusToggle = bus.isSpeakerToggle();
		float savedPos = pos;
		float savedVel = vel;
		boolean savedToggleChargeNegative = toggleChargeNegative;
		float savedCharge = charge;
		int savedChargeDur = chargeDur;
		float savedSampleSum = sampleSum;
		int savedSampleTotal = sampleTotal;
		double savedSampleLength = sampleLength;
		int savedBufferIndex = bufferIndex;

		// Phase 1: touch toggle/charge code paths without emitting audio samples.
		int branchWarmupIterations = Math.max(CHARGE_DURATION+2, Math.min(iterations, 2048));
		for( int i = 0; i<branchWarmupIterations; i++ ) {
			if( (i & 0x1f)==0 )
				bus.setSpeakerToggle(true);
			if( bus.isSpeakerToggle() ) {
				toggle();
				bus.setSpeakerToggle(false);
			}
			advanceSimulation(false);
		}

		// Phase 2: exercise sample packing and line writes with silent samples.
		pos = 0f;
		vel = 0f;
		toggleChargeNegative = false;
		charge = 0f;
		chargeDur = 0;
		sampleSum = 0f;
		sampleTotal = 0;
		sampleLength = 0d;
		int audioWarmupIterations = Math.max(iterations, SAMPLE_BUFFER_SAMPLES * 2);
		for( int i = 0; i<audioWarmupIterations; i++ )
			advanceSimulation(false);

		int silentWriteIterations = Math.max(SAMPLE_BUFFER_SAMPLES * 4, iterations);
		for( int i = 0; i<silentWriteIterations; i++ )
			advanceSimulation(true);

		bus.setSpeakerToggle(savedBusToggle);
		pos = savedPos;
		vel = savedVel;
		toggleChargeNegative = savedToggleChargeNegative;
		charge = savedCharge;
		chargeDur = savedChargeDur;
		sampleSum = savedSampleSum;
		sampleTotal = savedSampleTotal;
		sampleLength = savedSampleLength;
		bufferIndex = savedBufferIndex;
	}

	private void advanceSimulation(boolean emitAudio) {
		sampleLength += durationInc;
	
		// Check for underflow and set variables to 0 as needed to speed up math routines in idle state
		/// It may be slightly faster to use fixed point integers ///
		if( pos>-1f/32768f && pos<1f/32767f )
			pos = 0f;
		if( vel>-1f/32768f && vel<1f/32767f )
			vel = 0f;
	
		// Move diaphragm
		float accel = -SPRING_FORCE*pos;
	
		accel += charge;
		accel -= vel*FRICTION;
		vel += accel;
		pos += vel;
	
		// Check magnet-charge duration expiration
		if( chargeDur>0 ) {
			chargeDur--;
			if( chargeDur==0 )
				charge = 0f;
		}
	
		// Apply diaphragm position to average for sampling and commit sample when necessary
		sampleTotal++;
		sampleSum += pos;
	
		if( sampleLength >= SAMPLE_DURATION ) {
	
			sampleSum /= (float) sampleTotal;
			sampleSum = sampleSum>MAX_SOUND_WORD ? MAX_SOUND_WORD:sampleSum;
			sampleSum = sampleSum<MIN_SOUND_WORD ? MIN_SOUND_WORD:sampleSum;
	
			if( emitAudio )
				writeSample(sampleSum);
			sampleLength -= SAMPLE_DURATION;
			
			sampleSum = 0f;
			sampleTotal = 0;
		}
	}
	
    private void initializeAudioLine() throws LineUnavailableException {
		AudioFormat[] candidates = new AudioFormat[] {
			new AudioFormat(SAMPLE_RATE, 16, 1, true, false),
			new AudioFormat(SAMPLE_RATE, 8, 1, true, false)
		};
		for( AudioFormat candidate : candidates ) {
			DataLine.Info info = new DataLine.Info(SourceDataLine.class, candidate);
			if( !AudioSystem.isLineSupported(info) )
				continue;
			sdl = AudioSystem.getSourceDataLine(candidate);
			audioFormat = candidate;
			bytesPerSample = candidate.getSampleSizeInBits() / 8;
			buffer = new byte[SAMPLE_BUFFER_SAMPLES * bytesPerSample];
			return;
		}
		throw new LineUnavailableException("No supported SourceDataLine for mono PCM at " + SAMPLE_RATE + "Hz");
	}

	    private void writeSample(float sampleValue) {
		if( muteUntilNs>0L && System.nanoTime()<muteUntilNs )
			return;
		int sampleInt = Math.round(sampleValue);
		if( bytesPerSample==2 ) {
			short sampleShort = (short) sampleInt;
			buffer[bufferIndex++] = (byte) (sampleShort & 0x00ff);
			buffer[bufferIndex++] = (byte) ((sampleShort >> 8) & 0x00ff);
		}
		else {
			buffer[bufferIndex++] = (byte) (sampleInt / 256);
		}
		if( bufferIndex>=buffer.length ) {
			sdl.write(buffer, 0, bufferIndex);
			bufferIndex = 0;
		}
	}

	    public void open() throws LineUnavailableException {
		sdl.open(audioFormat, buffer.length);
		sdl.start();
	}

	public void setStartupMuteMs(int muteMs) {
		if( muteMs<=0 )
			muteUntilNs = 0L;
		else
			muteUntilNs = System.nanoTime() + (muteMs * 1_000_000L);
	}

    public synchronized void close() {
    	if( closed || sdl==null )
    		return;
    	if( bufferIndex>0 ) {
    		try {
    			sdl.write(buffer, 0, bufferIndex);
    		}
    		catch( Exception e ) {
    			// Ignore write failures during shutdown.
    		}
    		bufferIndex = 0;
    	}
    	// drain() can block on some backends during JVM/window shutdown; flush avoids hangs.
    	sdl.flush();
    	sdl.stop();
    	sdl.close();
    	closed = true;
	}

	@Override
	public void coldReset() throws HardwareException {
		pos = 0f;
		vel = 0f;
		toggleChargeNegative = false;
		charge = 0f;
		chargeDur = 0;
		sampleSum = 0f;
		sampleTotal = 0;
		sampleLength = 0d;
		bufferIndex = 0;
		muteUntilNs = 0L;
		if( sdl!=null ) {
			try {
				sdl.flush();
			}
			catch( Exception e ) {
				// Ignore backend-specific flush issues during reset.
			}
		}
	}

}
