package com.guoguoredisserver;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.zip.DataFormatException;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;

import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;

public class GuoguoServer {

	JedisPoolConfig conf;
	JedisPool pool;
	String redisIP;
	private ConcurrentLinkedQueue<String> onlineUsers = new ConcurrentLinkedQueue<String>();

	public static void main(String[] args) {
		GuoguoServer server = new GuoguoServer();
		server.DataRecordStart();
	}

	void DataRecordStart() {
		redisInit();
		NotificationThread thread = new NotificationThread();
		thread.start();
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

	class HandlerThread extends Thread {

		private String channel_up;
		private LinkedBlockingDeque<String> receivedDataBuffer;
		private HandlerLisetner hl;
		private Jedis listenerJedis;
		private int receivedSize = 100;
		private int time = 0;

		private boolean isRunning = true;

		private String userName = "audio";

		BufferedWriter writerpos;

		Worker myWorker;

		public HandlerThread(String _userName) {
			this.channel_up = _userName + "_list";
			this.userName = _userName;
		}

		public void run() {
			init();
			myWorker.start();
			new Timer().start();
			listenerJedis.subscribe(hl, channel_up);
		}

		private void init() {
			try {
				writerpos = new BufferedWriter(new FileWriter(userName
						+ "pos.txt"));
			} catch (IOException e) {
				e.printStackTrace();
			}
			receivedDataBuffer = new LinkedBlockingDeque<String>(receivedSize);
			while (listenerJedis == null) {
				listenerJedis = pool.getResource();
			}
			System.out.println("Starting: " + userName);

			hl = new HandlerLisetner();
			myWorker = new Worker();
		}

		private class Timer extends Thread {

			public void run() {
				while (time < 5) {
					time++;
					System.out.println(time);
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				onlineUsers.remove(userName);
				System.out.println("Stopping: " + userName);
				isRunning = false;
				hl.unsubscribe(channel_up);
			}
		}

		private class HandlerLisetner extends JedisPubSub {
			@Override
			public void onMessage(String channel, String msg) {
				System.out.println("DataReceived: " + userName);
				receivedDataBuffer.add(msg);
				time = 0;
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
					writer = new BufferedWriter(new FileWriter(userName
							+ ".txt"));
				} catch (IOException e) {
					e.printStackTrace();
				}

				while (isRunning) {
					if (receivedDataBuffer.isEmpty() == false) {
						String toprocess = receivedDataBuffer.poll();
						datarecord(toprocess);
					}
				}

			}

			String fromStringToFloat(String in) throws IOException,
					DataFormatException {
				byte[] bytes = Base64.decode(in);
				byte[] originalData = bytes;// CompressionUtils.decompress(bytes);
				StringBuffer floatString = new StringBuffer();

				for (int i = 0; i < originalData.length; i += 4) {
					int asInt = (originalData[i] & 0xFF)
							| ((originalData[i + 1] & 0xFF) << 8)
							| ((originalData[i + 2] & 0xFF) << 16)
							| ((originalData[i + 3] & 0xFF) << 24);
					floatString.append(Float.intBitsToFloat(asInt)).append(" ");
				}

				return floatString.toString();
			}

			private void datarecord(String in) {
				try {
					writer.write(fromStringToFloat(in));
					writer.flush();
				} catch (Exception e) {
					e.printStackTrace();
				}

			}

		}

	}

	class NotificationThread extends Thread {

		private String channel_up;
		private HandlerLisetner hl;
		private Jedis listenerJedis;

		public NotificationThread() {
			this.channel_up = "notification";
		}

		public void run() {
			init();
			listenerJedis.subscribe(hl, channel_up);
		}

		private void init() {
			while (listenerJedis == null) {
				listenerJedis = pool.getResource();
			}
			hl = new HandlerLisetner();
		}

		private class HandlerLisetner extends JedisPubSub {
			@Override
			public void onMessage(String channel, String msg) {
				if (!onlineUsers.contains(msg)) {
					HandlerThread dataRecord = new HandlerThread(msg);
					dataRecord.start();
					onlineUsers.add(msg);
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

}
