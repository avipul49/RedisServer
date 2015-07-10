package com.redisclient;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.zip.DataFormatException;

public class Analyser {

	public void analyse(String userName, byte[] bs) {
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(userName + ".txt", true));
			writer.write(fromStringToFloat(bs));
			writer.flush();
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	String fromStringToFloat(byte[] data) throws IOException,
			DataFormatException {
		StringBuffer floatString = new StringBuffer();

		for (int i = 0; i < data.length; i += 4) {
			int asInt = (data[i] & 0xFF) | ((data[i + 1] & 0xFF) << 8)
					| ((data[i + 2] & 0xFF) << 16)
					| ((data[i + 3] & 0xFF) << 24);
			floatString.append(Float.intBitsToFloat(asInt)).append(" ");
		}

		return floatString.toString();
	}

}
