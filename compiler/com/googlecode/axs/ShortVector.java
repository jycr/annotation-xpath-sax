/*
 * This file is part of AXS, Annotation-XPath for SAX.
 * 
 * Copyright (c) 2013 Benjamin K. Stuhl
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

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
	
	public short top() {
		if (length == 0)
			throw new ArrayIndexOutOfBoundsException(0);
		return buffer[length - 1];
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
	
	public String toString() {
		StringBuffer sb = new StringBuffer("ShortVector(");
		sb.append(length);
		sb.append(", [");
		for (int i = 0; i < length; i++) {
			sb.append(buffer[i]);
			if (i != length-1)
				sb.append(", ");
		}
		sb.append("])");
		
		return sb.toString();
	}
}
