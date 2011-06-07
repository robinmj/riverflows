/**
 * 
 */
package com.riverflows.wsclient;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

class CachingBufferedInputStream extends BufferedInputStream {

	private FileOutputStream cacheFile = null;
	
	public CachingBufferedInputStream(InputStream in, int size, File cacheFile) throws FileNotFoundException {
		super(in, size);
		
		this.cacheFile = new FileOutputStream(cacheFile);
	}
	
	@Override
	public synchronized int read() throws IOException {
		int ch = super.read();
		if(ch != -1) {
			cacheFile.write(ch);
		} else {
			cacheFile.close();
		}
		return ch;
	}
	
	@Override
	public int read(byte[] buffer) throws IOException {
		int count = super.read(buffer);
		if(count != -1) {
			cacheFile.write(buffer, 0, count);
		} else {
			cacheFile.close();
		}
		return count;
	}
	
	@Override
	public synchronized int read(byte[] buffer, int offset, int length)
			throws IOException {
		int count =  super.read(buffer, offset, length);
		if(count != -1) {
			cacheFile.write(buffer, offset, count);
		} else {
			cacheFile.close();
		}
		return count;
	}
}