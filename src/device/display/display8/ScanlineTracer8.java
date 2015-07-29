package device.display.display8;

import core.emulator.HardwareComponent;

public class ScanlineTracer8 implements HardwareComponent {

	private TraceMap8 traceMap;
	private int hStart;
	private int vStart;
	private int hSize;
	private int vSize;
	private int page;

	private int hScan;
	private int vScan;
	private int wordLow;
	private int wordHigh;

	private int wordHighStartBit;
	private int scanWordMask;

	@Override
	public void coldReset() {
		hScan = hSize-1;
		vScan = vSize-1;
	}

	public void setTraceMap( TraceMap8 traceMap ) {
		if( this.traceMap!=traceMap ) {
			this.traceMap = traceMap;
			int scanPair = traceMap.getScan(vScan, page);
			wordHigh = scanPair&~scanWordMask;
			wordLow = (scanPair+hScan)&scanWordMask;
			wordHighStartBit = traceMap.getWordHighStartBit();
			scanWordMask = (1<<wordHighStartBit)-1;
		}
	}

	public void setScanStart( int hStart, int vStart ) {
		this.hStart = hStart;
		this.vStart = vStart;
		coldReset();
	}

	public void setScanSize( int hSize, int vSize ) {
		this.hSize = hSize;
		this.vSize = vSize;
		coldReset();
	}

	public void setPage( int page ) {
		if( page!=this.page && traceMap!=null ) {
			this.page = page;
			int scanPair = traceMap.getScan(vScan, page);
			wordHigh = scanPair&~scanWordMask;
			wordLow = (scanPair+hScan)&scanWordMask;
		}
	}

	public void cycle() {
		hScan++;
		if( hScan!=hSize )
			wordLow++;
		else {
			vScan++;
			if( vScan==vSize )
				vScan = 0;
			int scanPair = traceMap.getScan(vScan, page);
			wordHigh = scanPair&~scanWordMask;
			wordLow = scanPair&scanWordMask;
			hScan = 0;
		}
	}

	public int getAddress() {
		return wordHigh|(wordLow&scanWordMask);
	}

	public boolean isHbl() {
		return hScan<hStart;
	}

	public boolean isVbl() {
		return vScan<vStart;
	}

	public boolean isBlank() {
		return isHbl()||isVbl();
	}

	public int getHScan() {
		return hScan;
	}

	public int getVScan() {
		return vScan;
	}

}
