package org.github.socket.client.connect;

import java.util.List;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;

import org.github.socket.client.connect.INetAddressService.NetAddress;
import org.github.socket.client.handler.ILogger;
/**
 * 重试连接
 * @author 林炳忠
 *
 */
public class RetryConnector {
	private INetAddressService addressService;
	private ISocketConnector connect;
	private int internalMills;
	private int internalIncreaseMills;
	private int maxInternalMills;
	private int maxCount;
	private int tryCnt = 0;
	private ReentrantLock lock = new ReentrantLock();
	/**
	 * 
	 * @param addressService ip列表服务
	 * @param connect	连接处理
	 * @param internalMills	间隔毫秒
	 * @param internalIncreaseMills	间隔自增
	 * @param maxInternalMills	间隔上限
	 * @param maxCount			重试最大次数
	 */
	public RetryConnector(INetAddressService addressService, ISocketConnector connect, int internalMills,
			int internalIncreaseMills, int maxInternalMills, int maxCount) {
		this.addressService = addressService;
		this.connect = connect;
		this.internalMills = internalMills;
		this.internalIncreaseMills = internalIncreaseMills;
		this.maxInternalMills = maxInternalMills;
		this.maxCount = maxCount;
	}
	
	private List<NetAddress> getNetAddresses() {
		try {
			List<NetAddress> addrs = this.addressService.getAddresses();
			return addrs;
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	private void init() {
		this.tryCnt = 0;
	}
	private Random rand = new Random();
	/**
	 * 间隔时间计算
	 * @return
	 */
	private int internal() {
		if(this.maxCount > 0 && this.tryCnt > this.maxCount) {
			return 0;
		}
		int min = this.internalMills + this.tryCnt * this.internalIncreaseMills;
		int max = this.maxInternalMills;
		int internal = rand.nextInt(max) % (max - min + 1) + min;
		this.tryCnt++;
		return internal;
	}
	public boolean doConnect() {
		if(this.connect.isConnecting() || lock.isLocked()) {
			return true;
		}
		boolean flag = false;
		try {
			lock.lock();
			this.init();
			List<NetAddress> addrs = this.getNetAddresses();
			while(true) {
				try {
					if(addrs == null || addrs.isEmpty()) {
						Thread.sleep(500L);
						addrs = this.getNetAddresses();
						if(addrs == null || addrs.isEmpty()) {
							continue;
						}
					}
					for(NetAddress addr: addrs) {
						String ip = addr.getIp();
						int port = addr.getPort();;
						boolean ok = this.connect.connect(ip, port);
						if( ! ok) {
							long internal = this.internal();
							if(internal > 0) {
								try {
									Thread.sleep(internal);
								} catch (InterruptedException e) {
								}
							}
							else {
								ILogger.Instance.logger("it giveup to retry connect");
								flag = true;
								break;
							}
						}
						else {
							ILogger.Instance.logger("retry connect ok");
							flag = true;
							break;
						}
					}
					if(flag) break;
				}
				catch(Exception e) {
					e.printStackTrace();
					if(e instanceof InterruptedException) {
						break;
					}
					else {
						try {
							Thread.sleep(1000L);
						} catch (InterruptedException e1) {
							e1.printStackTrace();
						}
					}
				}
			}
		}
		finally {
			lock.unlock();
			if( ! flag) {
				this.connect.onTryConnectFail();
			}
		}
		return this.connect.isConnecting();
	}
}
