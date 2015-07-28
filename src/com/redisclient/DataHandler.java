package com.redisclient;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.LinkedBlockingDeque;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;

class DataHandler implements Runnable {
	public static final int bufferSize = 1024 * 128 * 4;
	private static final int SecondsToExpire = 100;

	private String channel_up;
	private LinkedBlockingDeque<String> receivedDataBuffer;
	private HandlerLisetner jedisListener;
	private Jedis listenerJedis;
	private int receivedSize = 100;
	private int time = 0;

	private boolean isRunning = true;

	private String userName = "audio";

	Worker myWorker;
	JedisPool pool;

	private OnFinishListener finishListener;

	public DataHandler(String _userName, OnFinishListener finishListener,
			JedisPool pool) {
		this.channel_up = _userName + "_list";
		this.userName = _userName;
		this.finishListener = finishListener;
		this.pool = pool;
	}

	public void start() {
		new Thread(this).start();
	}

	public void run() {
		init();
		myWorker.start();
		new Timer().start();
		listenerJedis.subscribe(jedisListener, channel_up);
		System.out.println("END");
	}

	private void init() {
		receivedDataBuffer = new LinkedBlockingDeque<String>(receivedSize);
		while (listenerJedis == null) {
			listenerJedis = pool.getResource();
		}
		jedisListener = new HandlerLisetner();
		myWorker = new Worker();
		System.out.println("Starting: " + userName);
	}

	private class Timer extends Thread {

		public void run() {
			while (time < SecondsToExpire) {
				time++;
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			finishListener.onFinish(userName);
			System.out.println("Stopping: " + userName);
			isRunning = false;
			jedisListener.unsubscribe(channel_up);
		}
	}

	public interface OnFinishListener {
		void onFinish(String username);
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
		private ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
		private Analyser analyser = new Analyser();

		public void run() {
			buffer.order(ByteOrder.LITTLE_ENDIAN);
			while (isRunning) {
				if (receivedDataBuffer.isEmpty() == false) {
					String toprocess = receivedDataBuffer.poll();
					byte[] bytes = Base64.decode(toprocess);
					byte[] originalData = bytes;// CompressionUtils.decompress(bytes);
					buffer.clear();
					buffer.put(originalData);
					try {
						analyser.analyse(userName, originalData);
					} catch (IOException e) {
						e.printStackTrace();
					}

					// if (buffer.position() == buffer.capacity()) {
					// try {
					// analyser.analyse(userName, buffer.array());
					// } catch (IOException e) {
					// e.printStackTrace();
					// }
					// buffer.clear();
					// }
				}
			}

		}

	}
}
