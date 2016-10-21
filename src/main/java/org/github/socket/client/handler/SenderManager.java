package org.github.socket.client.handler;

import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;

import org.github.socket.client.connect.ISocketConnector;

/**
 * 发送队列
 * @author 林炳忠
 *
 */
public class SenderManager {
	private ISocketConnector connect;//连接接口
	private ArrayBlockingQueue<SenderMsg> queue = null;
	private Thread thread = null;
	//标志是否开启上传数据，因为可能在业务层登录成功之后才能进行业务数据的上报，
	//否则在断连重试成功之后但又还未进行登录就开始上报遗留数据会导致验证不通过
	private volatile boolean startToSend = false;
	private static class SenderMsg {
		String sendId;
		Object data;
	}
	/**
	 * 
	 * @param connect
	 * @param queueSize 默认 1024
	 */
	public SenderManager(ISocketConnector connect, int queueSize) {
		this.connect = connect;
		if(queueSize <= 0) queueSize = 1024;
		this.queue = new ArrayBlockingQueue<>(queueSize);
		this.startToSend = false;
	}
	/**
	 * 如果队列满了，则发送失败
	 * @param data
	 * @return
	 */
	public boolean send(String sendId, Object data) {
		try{
			SenderMsg msg = new SenderMsg();
			msg.sendId = sendId;
			msg.data = data;
			this.queue.add(msg);
			if(this.connect.isClosed()) {
				return false;
			}
			return true;
		}
		catch(IllegalStateException e) {
			return false;
		}
	}
	public void clear() {
		this.queue.clear();
	}
	/**
	 * 待发信息个数
	 * @return
	 */
	public int size() {
		return this.queue.size();
	}
	public void startToSend() {
		this.startToSend = true;
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
						sender();
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
		}, "sendermanager");
		this.thread.start();
	}
	private void sender() throws Exception {
		int size = this.queue.size();
		if(size == 0 || ! this.startToSend) {//或空或未开始上报，休息1秒
			Thread.sleep((long)(1* 1000L));
			return;
		}
		if(this.connect.isClosed()) {
			this.startToSend = false;
			Iterator<SenderMsg> iter = this.queue.iterator();
			while(iter.hasNext()) {
				SenderMsg msg = iter.next();
				this.connect.onSendFail(msg.sendId);
			}
			return;
		}
		for(int i=0; i<size; i++) {
			SenderMsg msg = this.queue.peek();
			if(msg != null) {
				boolean flag = this.connect.send((byte[])msg.data);
				if( ! flag) {
					this.connect.onSendFail(msg.sendId);
					break;
				}
				else {
					this.queue.poll();
				}
			}
			else {
				this.queue.poll();
			}
		}
	}
}
