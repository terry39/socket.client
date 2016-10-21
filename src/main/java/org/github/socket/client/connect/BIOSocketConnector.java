package org.github.socket.client.connect;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

import org.github.socket.client.handler.ILogger;
import org.github.socket.client.handler.IProtoDecoder;
import org.github.socket.client.handler.IProtoEncoder;
import org.github.socket.client.handler.ISocketClientHandler;
/**
 * 客户端连接，阻塞模式
 * @author 林炳忠
 *
 */
public class BIOSocketConnector implements ISocketConnector {
	private String ip;
	private int port;
	private int connectTimeout;
	private int receiveBufferSize = 1024 * 64;
	private int sendBufferSize = 1024 * 64;
	private Socket socket = null;//网络句柄以及输入输出流
	private OutputStream outputStream = null;
	private InputStream inputStream = null;
	private ISocketClientHandler socketClientHander;//网络事件接收接口
	@SuppressWarnings("rawtypes")
	private IProtoDecoder decoder;
	@SuppressWarnings("rawtypes")
	private IProtoEncoder encoder;

	private volatile boolean isConnect = false;//是否连接
	private AtomicBoolean closedSignal = new AtomicBoolean(false);//是否强制关闭
	
	private ByteBuffer receiveBufferCached;//buffer包是从netty3拿过来的代码
	private Thread receiveThread;
	
	public BIOSocketConnector(int connectTimeout, ISocketClientHandler socketClientHander,
			IProtoEncoder<?> encoder, IProtoDecoder<?> decoder) {
		this.connectTimeout = connectTimeout;
		this.socketClientHander = socketClientHander;
		this.encoder = encoder;
		this.decoder = decoder;
		this.receiveBufferCached = ByteBuffer.allocate(128); 
	}
	/**
	 * 连接
	 */
	@Override
	public synchronized boolean connect(String ip, int port) {
		if(this.isConnect) return true;
		this.ip = ip;
		this.port = port;
        try {
        	//连接
            socket = new Socket();
            socket.setTcpNoDelay(true);
            socket.setSoTimeout(0);//inputstream.read()阻塞读
            socket.setKeepAlive(true);
            socket.setReceiveBufferSize(this.receiveBufferSize);
            socket.setSendBufferSize(this.sendBufferSize);
            socket.connect(new InetSocketAddress(this.ip, this.port), this.connectTimeout);
            this.inputStream = socket.getInputStream(); 
            this.outputStream = socket.getOutputStream();
    		this.receiveBufferCached = ByteBuffer.allocate(128);
        	this.closedSignal.set(false);
        	
            this.receive();
            
            this.isConnect = true;
            this.socketClientHander.onConnect();
            return true;
        }
        catch(Exception e) {
        	e.printStackTrace();
        	this.socketClientHander.onException(e);
        	this.onClose();
        	return false;
        }
	}
	//关闭后资源释放
	private void onClose() {
    	if(socket != null) {
			try {
				socket.close();
			} catch (IOException e) {
			}
    	}
    	if(this.outputStream != null) {
			try {
				this.outputStream.close();
			} catch (IOException e) {
			}
    	}
    	if(this.inputStream != null) {
			try {
				this.inputStream.close();
			} catch (IOException e) {
			}
    	}
    	this.isConnect = false;
	}
	//接收线程
	private void receive() {
		this.receiveThread = new Thread(new Runnable() {
			public void run() {
				ByteBuffer receiveBufferCached = BIOSocketConnector.this.receiveBufferCached;
	            ISocketClientHandler socketClientHander = BIOSocketConnector.this.socketClientHander;
	            @SuppressWarnings("rawtypes")
				IProtoDecoder decoder = BIOSocketConnector.this.decoder;
				try {
		            int receiveBytes = 0;  
		            byte[] receiveBuffer = new byte[128];
		            
		            //收到长度为-1的数据，或者收到关闭标志，或者线程中断，则退出接收数据，连接结束
					while((receiveBytes = BIOSocketConnector.this.inputStream.read(receiveBuffer, 0, 128)) != -1) {
						receiveBufferCached.put(receiveBuffer, 0, receiveBytes);
						while(true) {
							Object data = decoder.decoder(receiveBufferCached);
							if(data != null) {
								socketClientHander.onReceive(data);
							}
							else break;
						}
						
			    		if(BIOSocketConnector.this.isBeClosing()) {
			    			break;
			            }
					}
					ILogger.Instance.logger("receiveBytes="+receiveBytes);
					ILogger.Instance.logger("isBeClosing="+BIOSocketConnector.this.isBeClosing());
				} catch (Exception e) {
					e.printStackTrace();
					if(e instanceof InterruptedException) {
						Thread.currentThread().interrupt();
					}
					else {
						socketClientHander.onException(e);
					}
				}
				finally {
					BIOSocketConnector.this.onClose();
					//主动请求关闭，不触发关闭事件
					if( ! BIOSocketConnector.this.isBeClosing()) {
						socketClientHander.onDisconnect();
					}
				}
			}
		}, "bio-client-receiver");
		this.receiveThread.start();
	}
	/**
	 * 关闭
	 */
	@Override
	public void close() {
		this.closedSignal.set(true);
		this.receiveThread.interrupt();//中断一下
		try {
			this.socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	@Override
	public void closeAndReconnect() {
		this.receiveThread.interrupt();//中断一下
		try {
			this.socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	/**
	 * 发送
	 */
	@SuppressWarnings("unchecked")
	@Override
	public boolean send(Object data) {
		try {
			if(this.isConnecting() && this.outputStream != null) {
				byte bytes [] = this.encoder.encode(data);
				this.outputStream.write(bytes);
				this.outputStream.flush();
				return true;
			}
		}
		catch(Exception e) {
			this.socketClientHander.onException(e);
		}
		return false;
	}
	@Override
	public boolean isClosed() {
		return  ! this.isConnect;
	}
	/**
	 * 是否被主动调用close()
	 * @return
	 */
	@Override
	public boolean isBeClosing() {
		return this.closedSignal.get();
	}
	/**
	 * 是否连接成功
	 */
	@Override
	public boolean isConnecting() {
		return this.isConnect;
	}
	@Override
	public void onTryConnectFail() {
		this.socketClientHander.onTryConnectGiveup();
	}
	@Override
	public void onSendFail(String sendId) {
		this.socketClientHander.onSendFail(sendId);
	}
	
}  