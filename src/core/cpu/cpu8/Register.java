package core.cpu.cpu8;

public class Register {

	private int   A;   // Accumulator
	private int   Y;   // Index register y
	private int   X;   // Index register x
	private int   PC;  // Program counter (PCL:PCH)
	private int   S;   // Stack pointer (0x01NN)
	private int   P;   // Processor status

	enum StatusRegister
	{

		N (0x80),    // N  Negative flag            - 0: last compare negative, 1: otherwise
		V (0x40),    // V  Overflow flag            - 0: no overflow on last result, 1: otherwise
		R (0x20),    // 1  Unused                   - Always 1
		B (0x10),    // B  Break status             - 1: after BRK, 0: after IRQB
		D (0x08),    // D  Decimal mode             - 0: cleared by CLD, 1: set by SED
		I (0x04),    // I  Interrupt-disable flag   - 0: cleared by CLI, 1: set by SEI
		Z (0x02),    // Z  Zero flag                - 0: last result non-zero, 1: otherwise
		C (0x01);    // C  Carry bit                - 0: no carry on last result or CLC, 1: carry on last result or SEC

		public int value;
		
		StatusRegister( int value ) {
			this.value = value;
		}

	};

	public int getA() {
		return A;
	}
	public void setA(int A) {
		this.A = 0xff&A;
	}
	public int getY() {
		return Y;
	}
	public void setY(int Y) {
		this.Y = 0xff&Y;
	}
	public int getX() {
		return X;
	}
	public void setX(int X) {
		this.X = 0xff&X;
	}
	public int getPC() {
		return PC;
	}
	public int getPCL() {
		return PC&0xff;
	}
	public int getPCH() {
		return PC>>8;
	}
	public void setPC(int PC) {
		this.PC = 0xffff&PC;
	}
	public int getS() {
		return S;
	}
	public void setS(int S) {
		this.S = 0xff&S;
	}
	public int getP() {
		return P;
	}
	public boolean getP(StatusRegister statusRegister) {
		return (P&statusRegister.value)!=0;
	}
	public void setP(int P) {
		this.P = 0xff&P;
	}
	public void setP(StatusRegister statusRegister) {
		this.P |= statusRegister.value;
	}
	public void clearP(StatusRegister statusRegister) {
		this.P &= ~statusRegister.value;
	}
	public void testP(boolean setFlag, StatusRegister statusRegister) {
		if( setFlag )
			this.P |= statusRegister.value;
		else
			this.P &= ~statusRegister.value;
	}
	public void testPZ(int value) {
		if( (value&0xff)==0 )
			this.P |= StatusRegister.Z.value;
		else
			this.P &= ~StatusRegister.Z.value;

	}
	public void testPN(int value) {
		if( (value & 0x80)!=0 )
			this.P |= StatusRegister.N.value;
		else
			this.P &= ~StatusRegister.N.value;
	}
	public void testPC(int value) {
		if( (value & 0x100)!=0 )
			this.P |= StatusRegister.C.value;
		else
			this.P &= ~StatusRegister.C.value;
	}
	public void testPZN(int value) {
		if( (value&0xff)==0 )
			this.P |= StatusRegister.Z.value;
		else
			this.P &= ~StatusRegister.Z.value;
		if( (value & 0x80)!=0 )
			this.P |= StatusRegister.N.value;
		else
			this.P &= ~StatusRegister.N.value;
	}
	public void testPCZN(int value) {
		if( (value & 0x100)!=0 )
			this.P |= StatusRegister.C.value;
		else
			this.P &= ~StatusRegister.C.value;
		if( (value&0xff)==0 )
			this.P |= StatusRegister.Z.value;
		else
			this.P &= ~StatusRegister.Z.value;
		if( (value & 0x80)!=0 )
			this.P |= StatusRegister.N.value;
		else
			this.P &= ~StatusRegister.N.value;
	}
	public void testPC_ZN(int value) {
		if( value < 0 )
			this.P |= StatusRegister.C.value;
		else
			this.P &= ~StatusRegister.C.value;
		if( (value&0xff)==0 )
			this.P |= StatusRegister.Z.value;
		else
			this.P &= ~StatusRegister.Z.value;
		if( (value & 0x80)!=0 )
			this.P |= StatusRegister.N.value;
		else
			this.P &= ~StatusRegister.N.value;
	}
	
	public String toString() {
		
		StringBuffer out = new StringBuffer();

		out.append("A:"+Cpu65c02.getHexString(getA(), 2)+" ");
		out.append("X:"+Cpu65c02.getHexString(getX(), 2)+" ");
		out.append("Y:"+Cpu65c02.getHexString(getY(), 2)+" ");
		out.append("PC:"+Cpu65c02.getHexString(getPC(), 4)+" ");
		out.append("S:"+Cpu65c02.getHexString(getS(), 2)+" ");
		out.append("P:");
		out.append(getP(StatusRegister.N) ? 'N':'.');
		out.append(getP(StatusRegister.V) ? 'V':'.');
		out.append(getP(StatusRegister.R) ? 'R':'.');
		out.append(getP(StatusRegister.B) ? 'B':'.');
		out.append(getP(StatusRegister.D) ? 'D':'.');
		out.append(getP(StatusRegister.I) ? 'I':'.');
		out.append(getP(StatusRegister.Z) ? 'Z':'.');
		out.append(getP(StatusRegister.C) ? 'C':'.');
		
		return out.toString();

	}
	
}
