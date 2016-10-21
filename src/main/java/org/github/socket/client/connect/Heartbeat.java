package org.github.socket.client.connect;


import java.util.Timer;
import java.util.TimerTask;

import org.github.socket.client.handler.IHearbeatDataHandler;
import org.github.socket.client.handler.ILogger;
/**
 * 心跳
 * @author 林炳忠
 *
 */
public class Heartbeat {
	private ISocketConnector connect;
	private IHearbeatDataHandler heartbeatDataHandler;//心跳数据接口
	private long internalMills;
	private long beatTimeoutMills;
	private Timer thread;
	private volatile boolean sendHeartbeat = false;//是否发出心跳，又还未回复心跳包
	private volatile long sendHeartbeatTime = 0L;
	/**
	 * 
	 * @param connect
	 * @param internalMills 间隔，单位为毫秒
	 * @param beatTimeoutMills 心跳包返回超时时间，毫秒
	 */
	public Heartbeat(ISocketConnector connect, IHearbeatDataHandler heartbeatDataHandler , long internalMills, long beatTimeoutMills) {
		this.connect = connect;
		this.heartbeatDataHandler = heartbeatDataHandler;
		this.internalMills = internalMills;
		this.beatTimeoutMills = beatTimeoutMills;
	}
	public void start() {
		this.thread = new Timer("heartbeat-timer", true);
		this.thread.scheduleAtFixedRate(new TimerTask() {
			public void run() {
				try {
					if(connect.isConnecting()) {
						long late = System.currentTimeMillis() - sendHeartbeatTime;
						if(late >= internalMills) {
							ILogger.Instance.logger("it is time to execute heartbeat. late="+ late);
							Heartbeat.this.doBeat();
						}
						else {
							if(sendHeartbeat && late > beatTimeoutMills) {
								ILogger.Instance.logger("it is heartbeat is timeout and then close the connection.late="+late +",beatTimeoutMills="+ beatTimeoutMills);
								sendHeartbeat = false;
								connect.closeAndReconnect();
							}
						}
					}
					else {
						sendHeartbeat = false;
					}
				}
				catch(Exception e) {
					if(e instanceof InterruptedException) {
						Thread.currentThread().interrupt();
					}
					e.printStackTrace();
				}
			}
		}, 3000L, this.beatTimeoutMills);
	}
	private void doBeat() {
		if(this.connect != null && this.connect.isConnecting()) {
			Object data = heartbeatDataHandler.sendData();
			this.connect.send(data);
			this.sendHeartbeat = true;
			this.sendHeartbeatTime = System.currentTimeMillis();
		}
	}
	/**
	 * 心跳包返回的时候需要调用这个接口，否则超时的时候则会认为连接不可用
	 */
	public void onBeatResponseOk() {
		this.sendHeartbeat = false;
	}
	public void shutdown() {
		this.sendHeartbeat = false;
		if(thread == null) {
			return;
		}
		this.thread.cancel();
		this.thread = null;
	}
}
