package com.MatrixWorker;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class MatrixWorker {
	
	private BufferedReader br;
	private FileReader fr;
	
	private String readerFileName;
	
	private JedisPoolConfig conf;
	private JedisPool pool;
	private String redisIP;
	private Jedis uploader;
	private Jedis loader;
	
	private String listName;
	
	int rowSize;
	
	public static void main(String[] args){
		MatrixWorker mw = new MatrixWorker();
		mw.start();
	} 
	
	void start(){
		init();
		readFile(br,rowSize);
		//loadRedis(listName,0);
		
	}
	
	private void init(){
		paraInit();
		redisInit();
		readerInit(); 
		
		System.out.println("initialization ready");
	}
	
	private void paraInit(){
		readerFileName = "D:\\Dropbox\\Developer\\MATLAB\\MATLAB-repos\\matlab-guoguo\\data\\NEB406mov0606-video2.txt";
		rowSize = 3454;
		listName = "NEB406mov0606-video2new3";
	}
	
	private void redisInit() {
		try{
		redisIP = "10.227.80.244";

		conf = new JedisPoolConfig();
		conf.setMaxActive(10000);
		conf.setMaxIdle(5000);
		conf.setMaxWait(10000);
		conf.setTestOnBorrow(true);

		pool = new JedisPool(conf, redisIP);
		uploader = pool.getResource();
		loader = pool.getResource();
		System.out.println("redis initialization ready");
		}catch(Exception e){
			System.out.println("redis init error");
		}

	}
	
	
	private void readerInit(){
		try {
			fr = new FileReader(readerFileName);
			br = new BufferedReader(fr);
			System.out.println("readers init success");
		} catch (FileNotFoundException e) {
			System.out.println("readers init error");
			e.printStackTrace();
		}
		
	}

	
	public void readFile(BufferedReader _br, int _size){
		String currenline = "";
		String toUpload = "";
		int counter = 0;
		int size = _size;
		
		try {
			while( (currenline = br.readLine())!=null){
				currenline += " ";
				if(counter<size){
					toUpload += currenline;
					counter++;
					System.out.println(currenline);
				}else{
					System.out.println("one package ready to go");
					counter = 0;
					uploadRedis(listName,toUpload);
					toUpload = "";
					toUpload += currenline;
					counter++;
					System.out.println(currenline);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void uploadRedis(String _listName,String _toUpload){
		uploader.rpush(_listName, _toUpload);
		System.out.println("package sent");
		
	}
	
	public void loadRedis(String _listName,int _index){
		float[] toTest = getRow(_listName,_index);
		for(int j = 0;j<toTest.length;j++){
			System.out.println(toTest[j]);
		}
	}
	
	float[] getRow(String _listName,int _index){
		
		String result_str = loader.lindex(_listName, _index);
		String[] results_str = result_str.split(" ");
		int ll = results_str.length;
		float[] result  = new float[ll];
		for(int i =0;i<ll;i++){
				result[i] = Float.valueOf(results_str[i]); 
		}
		
		return result;
	}

}
