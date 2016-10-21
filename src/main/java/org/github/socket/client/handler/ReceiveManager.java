package org.github.socket.client.handler;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 接收管理
 * @author 林炳忠
 *
 */
public class ReceiveManager {
	private ISocketClientHandler handler;//客户端事件接口
	private BlockingQueue<Object> queue = null;
	private Thread thread = null;
	/**
	 * 
	 * @param handler 客户端事件接口
	 */
	public ReceiveManager(ISocketClientHandler handler) {
		this.handler = handler;
		this.queue = new LinkedBlockingQueue<>();
	}
	/**
	 * 接收消息入口
	 * @param data
	 */
	public void onReceive(Object data) {
		this.queue.add(data);
	}
	/**
	 * 接收信息个数
	 * @return
	 */
	public int size() {
		return this.queue.size();
	}
	public synchronized void start() {
		if(thread != null) {
			return;
		}
		this.thread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					while(true) {
						Object data = queue.take();
						handler.onReceive(data);
					}
				}
				catch(Exception e) {
					if(e instanceof InterruptedException) {
						Thread.currentThread().interrupt();
					}
					else {
						e.printStackTrace();
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e1) {
						}
					}
				}
			}
		}, "receiver-manager");
		this.thread.start();
	}

}
