package com.redisclient;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.zip.DataFormatException;

import javax.imageio.ImageIO;

import org.jcodec.codecs.h264.H264Decoder;
import org.jcodec.common.JCodecUtil;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;

import com.twilight.h264.player.H264Player;

public class Analyser {

	public void analyse(String userName, byte[] payload) throws IOException {

		// System.out.println(payload[10] + " " + payload[20] + " " +
		// payload[30]
		// + " " + payload[40]);
		// H264Player.getPlayer().play(payload);

		decodeBuffer(payload);
		// BufferedWriter writer = null;
		// try {
		// writer = new BufferedWriter(new FileWriter(userName + ".txt", true));
		// writer.write(fromStringToFloat(bs));
		// writer.flush();
		// writer.close();
		// } catch (Exception e) {
		// e.printStackTrace();
		// }
	}

	private static void avc2png(byte[] in) throws IOException {
		ByteBuffer bb = ByteBuffer.allocate(in.length);
		bb.put(in);
		H264Decoder decoder = new H264Decoder();
		Picture out = Picture.create(1920, 1088, ColorSpace.YUV420);
		Picture real = decoder.decodeFrame(bb, out.getData());
		BufferedImage bi = JCodecUtil.toBufferedImage(real);
		ImageIO.write(bi, "png", new File("out.png"));
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

	private Block findNext(byte[] payload, int i) {
		Block block = null;
		if (payload[i] == 0x19 && payload[i + 1] == 0x79) {
			block = new Block();
			block.type = 1;
			block.timeStamp = (payload[i + 3] << 8) + payload[i + 2];
			block.length = ((payload[i + 7] & 0XFF) << 24)
					+ ((payload[i + 6] & 0XFF) << 16)
					+ ((payload[i + 5] & 0XFF) << 8) + (payload[i + 4] & 0XFF);
			for (int j = i; j < i + 8; j++) {
				System.out.print(" {" + j + " " + payload[j] + "}");
			}

		}
		return block;

	}

	private void decodeBuffer(byte[] payload) throws IOException {
		// System.out.println(payload[10] + " " + payload[20] + " " +
		// payload[30]
		// + " " + payload[40]);
		// System.out.println(payload[60] + " " + payload[100] + " "
		// + payload[200] + " " + payload[400]);
		int i = 0;
		while (true) {
			if (payload.length - i <= 8) {
				// drop left data, because it is not a packet.
				break;
			}

			Block block = findNext(payload, i);
			if (block != null) {
				System.out.println(block.length);
				block.payload = new byte[block.length];
				System.arraycopy(payload, i + 8, block.payload, 0, block.length);
				// block.payload = Arrays.copyOfRange(payload, i + 8, i + 8
				// + block.length);
				H264Player.getPlayer().play(block.payload);
				// avc2png(block.payload);
				i = i + 8 + block.length;
			} else {
				break;
			}
		}

	}

	public static class Block {
		int type;
		int length;
		int timeStamp;
		byte[] payload;
	}
}
