package com.riverflows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;

/**
 * Created by robin on 1/16/16.
 */
public class FileUtils {

	public static byte[] getFileData(String filePath) throws Exception {

		ByteArrayOutputStream sink = new ByteArrayOutputStream();

		FileInputStream source  = new FileInputStream(filePath);

		int readCount;
		byte[] buffer = new byte[8192];

		while((readCount = source.read(buffer)) != -1) {
			sink.write(buffer, 0, readCount);
		}

		return sink.toByteArray();
	}
}
