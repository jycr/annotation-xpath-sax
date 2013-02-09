package com.googlecode.axs;

import java.util.Arrays;

/**
 * Internal support class which implements a Vector&lt;short&gt;
 * @author Ben
 *
 */
public class ShortVector {
	private short[] buffer;
	private int length = 0;
	
	public ShortVector() {
		buffer = new short[16];
	}
	
	public void push(short v) {
		if (length == buffer.length) {
			// reallocate
			int newsz = length * 3 / 2;
			short[] newBuffer = new short[newsz];
			System.arraycopy(buffer, 0, newBuffer, 0, buffer.length);
			buffer = newBuffer;
		}
		buffer[length++] = v;
	}
	
	public int size() {
		return length;
	}
	
	public short get(int index) {
		if (index >= length)
			throw new ArrayIndexOutOfBoundsException(index);
		return buffer[index];
	}
	
	public void put(int index, short v) {
		if (index >= length)
			throw new ArrayIndexOutOfBoundsException(index);
		buffer[index] = v;
	}
	
	public short[] result() {
		return Arrays.copyOf(buffer, length);
	}
}
