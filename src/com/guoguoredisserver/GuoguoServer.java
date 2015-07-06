package com.guoguoredisserver;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.LinkedBlockingDeque;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;

import com.guoguoredisserver.Adpcm.AdpcmState;
import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;

public class GuoguoServer {

	JedisPoolConfig conf;
	JedisPool pool;
	String redisIP;

	public static void main(String[] args) {
		GuoguoServer server = new GuoguoServer();
		server.DataRecordStart();
	}

	void ServerStart() {
		redisInit();
		AdminThread admin = new AdminThread();
		admin.start();

		TestThread test = new TestThread();
		test.start();
	}

	void DataRecordStart() {
		redisInit();
		HandlerThread dataRecord = new HandlerThread("vv");
		dataRecord.start();

		// TestThread test = new TestThread();
		// test.start();
	}

	class TestThread extends Thread {
		public void run() {

			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			Jedis testJedis = pool.getResource();
			// testJedis.publish("GuoguoServer Admin Channel",
			// "testuser:start");
			// System.out.println("test message sent");

			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			int i = 0;
			while (i < 50) {
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				String toSend = "";
				for (int k = 0; k < 4; k++) {
					Random rand = new Random();
					toSend += String.valueOf(rand.nextFloat() + " ");
				}
				String toSendStr = toSend.toString();
				testJedis.publish("dataRecord_channel_up", toSendStr);
				System.out.println("data sent, which is " + toSendStr);
				i++;
			}

		}
	}

	void redisInit() {
		redisIP = "localhost";

		conf = new JedisPoolConfig();
		conf.setMaxActive(10000);
		conf.setMaxIdle(5000);
		conf.setMaxWait(10000);
		conf.setTestOnBorrow(true);

		pool = new JedisPool(conf, redisIP);
		System.out.println("redis initialization ready");

	}

	class AdminThread extends Thread {

		private Jedis listenerJedis;
		private String adminChannel;
		private NotificationLisetner nl;

		private void init() {
			while (listenerJedis == null) {
				listenerJedis = pool.getResource();
			}
			nl = new NotificationLisetner();
			adminChannel = "GuoguoServer Admin Channel";

			System.out.println("adminThread init ready");

		}

		public void run() {
			init();

			listenerJedis.subscribe(nl, adminChannel);
		}

		private class NotificationLisetner extends JedisPubSub {
			@Override
			public void onMessage(String channel, String msg) {
				System.out.println("notificationlistener msg received:" + msg);

				String userName = (msg.split(":"))[0];
				String command = (msg.split(":"))[1];

				if (command.equals("stop")) {
					System.out.println("HT stoped: " + userName);
				} else if (command.equals("start")) {
					HandlerThread ht = new HandlerThread(userName);
					ht.start();
					System.out.println("HT started: " + userName);
				} else {
					System.out.println("msg error!!!!!!!!!!!!!");
				}

			}

			@Override
			public void onPMessage(String arg0, String arg1, String arg2) {
			}

			@Override
			public void onPSubscribe(String arg0, int arg1) {
			}

			@Override
			public void onPUnsubscribe(String arg0, int arg1) {
			}

			@Override
			public void onSubscribe(String arg0, int arg1) {
			}

			@Override
			public void onUnsubscribe(String arg0, int arg1) {
			}
		}

	}

	class HandlerThread extends Thread {

		private String userName;
		private String channel_up;
		private String channel_down;
		private LinkedBlockingDeque<String> receivedDataBuffer;
		private MatrixBuffer resultBuffer;
		private HandlerLisetner hl;
		private Jedis listenerJedis;
		private int receivedSize = 100;
		private int columnSize = 4;
		private int rowSize = 100;

		private MatrixBuffer tempBuffer;
		private int startColumn = 4;
		private int endColumn = 9;
		private int tempBuffer_rows = 100;
		private int tempBuffer_columns = endColumn - startColumn + 1;

		// ******************** Guoguo algorithm related parameters
		private Jedis pushredis; // redis which to push location results to the
									// redis storage
		public String redisaddress = "10.227.80.244";// The address of the redis
														// server
		public String redispubid = "User0";// The user id of the redis pub
											// channel
		public String redislocationlist = "redisresult";// new add, the redis
														// list name that stored
														// all the estimated
														// location data.

		private int StationNum = 6;// Total number of anchor stations.
		// private double stations[][]={{105, 11.5, 110.5, 172.5, 257, 317, 338,
		// 254, 194},{0, 49, 85, 85, 87, 87, 2, 2, 2}};
		/*
		 * private double stations[][]={{795.284160000000, 880.445280000000,
		 * 846.521040000000, 284.835600000000, 267.614400000000,
		 * 369.234720000000}, {15.6057600000000, 235.122720000000,
		 * 363.169200000000, 366.918240000000, 113.477040000000,
		 * 6.61416000000000}};
		 */

		private double stations[][] = {
				{ 256.8854, 30.3886, 291.6631, 661.5074, 798.2102, 632.8867 },// x-coordinate
																				// in
																				// cm
				{ 0, 123.372, 210.0783, 205.7603, 203.2203, 3.81 } };// y-coordinate
																		// in cm

		// 3D coordinate
		private double stations3D[][] = {
				{ 256.8854, 30.3886, 291.6631, 661.5074, 798.2102, 632.8867 },// x-coordinate
																				// in
																				// cm
				{ 0, 123.372, 210.0783, 205.7603, 203.2203, 3.81 },
				{ 0, 0, 0, 0, 0, 0 } };// y-coordinate in cm

		private String storefilename = "audio";
		// private String redisaddress="10.227.80.244";//The address of the
		// redis server
		// private String redispubid="User0";//The user id of the redis pub
		// channel
		private int resultlen = StationNum * 4 + 2;
		private int numpos = 0;
		private int[] stationid;

		private double[][] rangingV;// Store the current ranging value
									// transmitted from App
		private double[][] rangingVNew;// Store the current ranging value
										// calculated from TBL
		private int rangingV_rows = 3;
		private int rangingV_cols = StationNum;

		private MatrixBuffer rangingIniV;// Store all ranging values transmitted
											// from App
		private double[][] rangingIniVdut;
		private int rangingIniV_rows = 150;// maximum value
		private int rangingIniV_cols = StationNum;

		private MatrixBuffer rangingVFit;// Store all the fitted ranging values
											// from rangingIniV
		private double[][] rangingVFitdut;
		private int rangingVFit_rows = 150;// maximum value
		private int rangingVFit_cols = StationNum;

		private int ranging_backrows = 100;
		private int pos_backrows = 100;

		// private int[] outliertimeoutvec =new int[StationNum];//time out
		// counter for the ranging results
		private int[] outliertimeoutvec = new int[StationNum];// time out
																// counter
		// for the ranging
		// results
		private double[] kalmanx = new double[StationNum];// zeros(2,Numstation);
		private double[] kalmanP = new double[StationNum];;// kalmanP=10.*ones(2,Numstation);
		private int[] kalmanzeroN = new int[StationNum];// =zeros(1,Numstation);
		private float[] LocP = new float[3];// [ref,Methodflag, estdelta]

		private int poslen = 4;
		double[] positionval = new double[poslen];
		double[] positionnew = new double[poslen];

		private MatrixBuffer PosVFit;
		private double[][] PosVFitdut;
		private MatrixBuffer PosTotal;
		private double[][] PosTotaldut;
		private int maxrows = 30;// maximum value
		// private int[] posoutliertimeout=new int[StationNum];//time out
		// counter for the location results
		private int[] posoutliertimeout = new int[2];// 0;
		// poskalmanx=zeros(1,2);
		// poskalmanP=10.*ones(1,2);
		private double[] poskalmanx = new double[2];
		private double[] poskalmanP = new double[2];

		String outputline = null;

		// GuoguoJavaWin.Java2MATLAB j2mInst; //matlab algorithm instance
		Object[] returnresult0 = null;// Define the object return by MATLAB
		Object[] returnresult1 = null;// Define the object return by MATLAB
		Object[] returnresult2 = null;// Define the object return by MATLAB

		BufferedWriter writerpos; // to save data in harddisk

		Worker myWorker;

		public void run() {
			init();
			myWorker.start();
			listenerJedis.subscribe(hl, channel_up);
		}

		private void init() {
			// Guoguo related init
			pushredis = new Jedis(redisaddress);

			stationid = new int[StationNum];

			rangingV = new double[rangingV_rows][rangingV_cols];
			rangingIniV = new MatrixBuffer(rangingIniV_rows, rangingIniV_cols);
			rangingVFit = new MatrixBuffer(rangingVFit_rows, rangingVFit_cols);

			PosVFit = new MatrixBuffer(maxrows, poslen);
			PosTotal = new MatrixBuffer(maxrows, poslen);

			for (int i = 0; i < StationNum; i++) {
				kalmanx[i] = 0;
				// kalmanx[1][i]=0;
				kalmanP[i] = 10;
				// kalmanP[1][i]=10;
				kalmanzeroN[i] = 0;
			}
			poskalmanx[0] = 0;
			poskalmanx[1] = 0;
			poskalmanP[0] = 10;
			poskalmanP[1] = 10;

			LocP[0] = 0;// ref
			LocP[1] = 1;// Methodflag
			LocP[2] = 0;// estdelta
			try {
				writerpos = new BufferedWriter(new FileWriter(storefilename
						+ "pos.txt"));
			} catch (IOException e) {
				e.printStackTrace();
			}
			// instanciate algorithm instance
			// try {
			// j2mInst=new GuoguoJavaWin.Java2MATLAB(); //initialize the
			// Java2MATLAB instance
			// } catch (MWException e) {
			// e.printStackTrace();
			// }
			// end here

			receivedDataBuffer = new LinkedBlockingDeque<String>(receivedSize);
			resultBuffer = new MatrixBuffer(rowSize, columnSize);
			tempBuffer = new MatrixBuffer(tempBuffer_rows, tempBuffer_columns);
			while (listenerJedis == null) {
				listenerJedis = pool.getResource();
			}
			hl = new HandlerLisetner();
			myWorker = new Worker();
			System.out.println("handler init ready");
		}

		public HandlerThread(String _userName) {
			this.userName = _userName;
			this.channel_up = _userName + "_list";
			this.channel_down = _userName + "_channel_down";
		}

		private class HandlerLisetner extends JedisPubSub {
			@Override
			public void onMessage(String channel, String msg) {
				// System.out.println("handlerlistener msg received:" + msg);
				receivedDataBuffer.add(msg);
			}

			@Override
			public void onPMessage(String arg0, String arg1, String arg2) {
			}

			@Override
			public void onPSubscribe(String arg0, int arg1) {
			}

			@Override
			public void onPUnsubscribe(String arg0, int arg1) {
			}

			@Override
			public void onSubscribe(String arg0, int arg1) {
			}

			@Override
			public void onUnsubscribe(String arg0, int arg1) {
			}
		}

		private class Worker extends Thread {
			BufferedWriter writer;

			public void run() {
				try {
					writer = new BufferedWriter(new FileWriter(storefilename
							+ ".txt"));
				} catch (IOException e) {
					e.printStackTrace();
				}

				while (true) {
					if (receivedDataBuffer.isEmpty() == false) {
						String toprocess = receivedDataBuffer.poll();
						datarecord(toprocess);
						// process_2(toprocess);
					}
				}

			}

			String fromStringToFloat(String in) {
				byte[] bytes = Base64.decode(in);
				// float[] floatValues = new float[bytes.length / 4];
				StringBuffer floatString = new StringBuffer();

				for (int i = 0; i < bytes.length; i += 4) {
					int asInt = (bytes[i] & 0xFF)
							| ((bytes[i + 1] & 0xFF) << 8)
							| ((bytes[i + 2] & 0xFF) << 16)
							| ((bytes[i + 3] & 0xFF) << 24);
					floatString.append(Float.intBitsToFloat(asInt)).append(" ");
				}

				return floatString.toString();
			}

			private String printvectorstr(double[] input, int num) {
				String res = "";
				for (int i = 0; i < num; i++) {
					res += input[i] + " ";
				}
				// System.out.println(res);
				return res;
			}

			private double[][] resultcast(double[] res, int row, int col) {
				double[][] result = new double[row][col];
				for (int i = 0; i < row; i++) {
					for (int j = 0; j < col; j++) {
						result[i][j] = res[i + j * row];
					}
				}
				return result;
			}

			private double[] getfirstrow(double[][] res, int row, int col) {
				double[] result = new double[col];
				for (int j = 0; j < col; j++) {
					result[j] = res[0][j] * res[1][j];
				}
				return result;
			}

			private MediaBlock findNext(byte[] payload, int i) {
				MediaBlock block = new MediaBlock();
				System.out.println(payload[0] + " " + payload[1]);

				if (payload[i] == 0x19 && payload[i + 1] == 0x79) {

					block.type = 1;
					block.timeStamp = (payload[i + 3] << 8) + payload[i + 2];
					block.length = (payload[i + 7] << 24)
							+ (payload[i + 6] << 16) + (payload[i + 5] << 8)
							+ payload[i + 4];
				} else if (payload[i] == 0x19 && payload[i + 1] == 0x82) {

					block.type = 2;
					block.timeStamp = (payload[i + 3] << 8) + payload[i + 2];
					block.length = (payload[i + 7] << 24)
							+ (payload[i + 6] << 16) + (payload[i + 5] << 8)
							+ payload[i + 4];
				}

				return block;
			}

			Adpcm adpcm = new Adpcm();

			private void datarecord(String in) {
				try {
					byte[] payload = Base64.decode(in);
					short[] decode = new short[payload.length * 2];
					AdpcmState state = adpcm.new AdpcmState();
					adpcm.initState(state);
					int s = adpcm.decode(state, payload, 0, payload.length,
							decode, 0);
					float[] fa = new float[s];
					System.out.println("Size---- " + s + " " + decode[0] + " "
							+ decode[1] + " " + decode[2] + " " + decode[3]);
					StringBuffer fs = new StringBuffer();
					for (int i = 0; i < s; i++) {
						fa[i] = (float) (decode[i] * 1.0 / 32768.0);
						fs.append(fa[i]).append(" ");
					}
					// int i = 0;
					// while (true) {
					// if (payload.length - i <= 8) {
					// break;
					// }
					//
					// MediaBlock block = findNext(payload, i);
					// if (block.type == 1) {
					// block.payload = new byte[block.length];
					// System.arraycopy(payload, 8, block.payload, 0,
					// block.length);
					// i = i + 8 + block.length;
					// } else if (block.type == 2) {
					// block.payload = new byte[block.length];
					// System.arraycopy(payload, 8, block.payload, 0,
					// block.length);
					// i = i + 8 + block.length;
					// StringBuffer floatString = new StringBuffer();
					//
					// for (int j = 0; j < block.payload.length; j += 4) {
					// int asInt = (block.payload[i] & 0xFF)
					// | ((block.payload[i + 1] & 0xFF) << 8)
					// | ((block.payload[i + 2] & 0xFF) << 16)
					// | ((block.payload[i + 3] & 0xFF) << 24);
					// floatString.append(Float.intBitsToFloat(asInt))
					// .append(" ");
					// }
					//
					//
					// System.out.println("data written");
					// } else {
					// break;
					// }
					// }
					System.out.println("data written");
					writer.write(fs.toString());
					writer.flush();
				} catch (IOException e) {
					e.printStackTrace();
				}

			}

			/*
			 * private void myprocess(String in){ float[] data =
			 * fromStringToFloat(in); float[] result = GuoguoAlgorithm(data);
			 * resultBuffer.addRow(result);
			 * System.out.println("resultBuffer added");
			 * 
			 * float[] verifyR = resultBuffer.getFirstRow();
			 * System.out.println("verify row:"); for(int j
			 * =0;j<verifyR.length;j++){ System.out.println(verifyR[j]); }
			 * 
			 * LinkedBlockingDeque<Float> verifyC = resultBuffer.getColunmn(1);
			 * System.out.println("verify column:"); Iterator<Float> myiterator
			 * = verifyC.iterator(); while (myiterator.hasNext()) {
			 * System.out.println(myiterator.next()); } }
			 */

			private float[] GuoguoAlgorithm(float[] in) {
				return in;
			}
		}

		/**
		 * �޶�����ľ���ʹ�����飩
		 * 
		 * @author zhangpeng
		 * @date 2013-5-28
		 */
		private class MatrixBuffer {
			/** �������� */
			List<double[]> matrix;
			/** ��������� */
			int number_rows;
			/** ��������� */
			int number_columns;

			public MatrixBuffer(int number_rows, int number_columns) {
				this.number_rows = number_rows;
				this.number_columns = number_columns;
				// ʹ��arrayList����LinkedList���ʹ�������������Ķ�ȡ�����϶࣬ʹ��ArrayList��
				// ����������϶�ʹ��LinkedList
				matrix = new ArrayList<double[]>();
				// matrix = new LinkedList<float[]>();
			}

			// ��ȡ��Ӧ����ķ�����ûд���Ҳ�֪������Ϊʲô��Ҫ�������
			// getColunmn(int index)

			/**
			 * ��ȡ��һ��
			 * 
			 * @author zhangpeng
			 * @date 2013-5-28
			 * @return
			 */
			public double[] getFirstRow() {
				return matrix.get(0);
			}

			public int getcurrentRows() {
				return matrix.size();
			}

			/**
			 * �����ݵ���һ�У������������ƣ��Ƴ����һ��
			 * 
			 * @author zhangpeng
			 * @date 2013-5-28
			 * @param resultArray
			 * @throws Exception
			 */
			public void addRow(double[] resultArray) {
				// ������鳤�ȴ��ھ��������ȡ����
				if (resultArray.length > number_columns) {
					double[] temp = new double[number_columns];
					System.arraycopy(resultArray, 0, temp, 0, number_columns);
					resultArray = temp;
				}

				matrix.add(0, resultArray);

				if (matrix.size() > number_rows) {
					matrix.remove(matrix.size() - 1);
				}
			}

			/**
			 * ��ȡǰ���е����
			 * 
			 * @author zhangpeng
			 * @date 2013-5-28
			 * @param row
			 * @return
			 */
			public double[][] getArray(int row) {
				// ���Ҫȡ��������ھ������еĳ��ȣ���ȡ���������
				row = row > matrix.size() ? matrix.size() : row;

				double[][] fs = new double[row][number_columns];

				for (int i = 0; i < row; i++) {
					fs[i] = matrix.get(i);
				}

				return fs;
			}

			/**
			 * ��ȡ���¼��е����
			 * 
			 * @author Kaikai Liu
			 * @date 2013-5-28
			 * @param row
			 * @return
			 */
			public double[][] getLatestArray(int row) {
				// ���Ҫȡ��������ھ������еĳ��ȣ���ȡ���������
				row = row > matrix.size() ? matrix.size() : row;

				// double[][] fs = new double[row+1][number_columns];
				double[][] fs = new double[row][number_columns];
				int j = 0;
				for (int i = row - 1; i >= 0; i--) {
					fs[j] = matrix.get(i);
					j++;
				}
				return fs;
			}
		}
	}

	public static class MediaBlock {
		public int type;
		public int timeStamp;
		public int length;
		public byte[] payload;
	}
}
