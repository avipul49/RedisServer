package com.redisclient;

import java.util.concurrent.ConcurrentLinkedQueue;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;

public class Main implements DataHandler.OnFinishListener {
	JedisPoolConfig conf;
	JedisPool pool;
	String redisIP;
	private ConcurrentLinkedQueue<String> onlineUsers = new ConcurrentLinkedQueue<String>();

	public static void main(String[] args) {
		Main server = new Main();
		server.start();
	}

	void start() {
		redisInit();
		new DataHandler("vv", this, pool).start();
		// NotificationThread thread = new NotificationThread();
		// thread.start();
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
					DataHandler dataRecord = new DataHandler(msg, Main.this,
							pool);
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

	@Override
	public void onFinish(String username) {
		onlineUsers.remove(username);
	}

}
