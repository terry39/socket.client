package org.github.socket.client;

import org.github.socket.client.connect.BIOSocketConnector;
import org.github.socket.client.connect.Heartbeat;
import org.github.socket.client.connect.INetAddressService;
import org.github.socket.client.connect.ISocketConnector;
import org.github.socket.client.connect.RetryConnector;
import org.github.socket.client.handler.IHearbeatDataHandler;
import org.github.socket.client.handler.ILogger;
import org.github.socket.client.handler.IProtoDecoder;
import org.github.socket.client.handler.IProtoEncoder;
import org.github.socket.client.handler.ISocketClientHandler;
import org.github.socket.client.handler.SenderManager;
import org.github.socket.client.handler.SocketClientHandlerDecorator;
import org.github.socket.client.handler.SocketClientHandlerObservable;

/**
 * socket client统一入口
 * @author 林炳忠
 *
 */
public final class SocketClientEngine {
	public static SocketClientEngine Instance = new SocketClientEngine();
	private ISocketClientHandler socketClientHandler;
	private int connectTimeoutMills = 1000 * 5;
	private int heartbeatTimeoutMills = 1000 * 5;
	private int heartbeatInternalMills = 1000 * 60 * 3;
	private IHearbeatDataHandler heartbeatData;
	private int reconnectInternalMills = 1000 * 2;
	private int reconnectInternalIncreaseMills = 1000 * 2;
	private int reconnectMaxInternalMills = 1000 * 10;
	private int reconnectMaxCount = 20;
	@SuppressWarnings("rawtypes")
	private IProtoDecoder decoder;
	@SuppressWarnings("rawtypes")
	private IProtoEncoder encoder;
	private ILogger logger;
	private INetAddressService addressService;
	private Heartbeat heartbeat;
	private ISocketConnector connect;
	private SenderManager senderManager;
	private RetryConnector retryConn;
	private boolean started = false;
	/**
	 * ip端口地址服务
	 * @param addressService
	 * @return
	 */
	public SocketClientEngine withNetAddressService(INetAddressService addressService) {
		this.addressService = addressService;
		return this;
	}
	/**
	 * 连接事件处理句柄
	 * @param socketClientHandler
	 */
	public SocketClientEngine withSocketClientHandler(ISocketClientHandler socketClientHandler) {
		this.socketClientHandler = socketClientHandler;
		return this;
	}
	/**
	 * 连接超时事件，毫秒，默认5秒
	 * @param connectTimeoutMills
	 */
	public SocketClientEngine withConnectTimeoutMills(int connectTimeoutMills) {
		this.connectTimeoutMills = connectTimeoutMills;
		return this;
	}
	/**
	 * 心跳超时，毫秒，默认15秒
	 * @param heartbeatTimeoutMills
	 * @return
	 */
	public SocketClientEngine withHeartbeatTimeoutMills(int heartbeatTimeoutMills) {
		this.heartbeatTimeoutMills = heartbeatTimeoutMills;
		return this;
	}
	/**
	 * 心跳间隔，毫秒，默认3分钟
	 * @param heartbeatInternalMills
	 */
	public SocketClientEngine withHeartbeatInternalMills(int heartbeatInternalMills) {
		this.heartbeatInternalMills = heartbeatInternalMills;
		return this;
	}
	/**
	 * 心跳数据句柄
	 * @param heartbeatData
	 */
	public SocketClientEngine withHeartbeatData(IHearbeatDataHandler heartbeatData) {
		this.heartbeatData = heartbeatData;
		return this;
	}
	/**
	 * 重连间隔，毫秒，默认2秒
	 * @param reconnectInternalMills
	 */
	public SocketClientEngine withReconnectInternalMills(int reconnectInternalMills) {
		this.reconnectInternalMills = reconnectInternalMills;
		return this;
	}
	/**
	 * 重连间隔递增，毫秒，默认2秒
	 * @param reconnectInternalIncreaseMills
	 */
	public SocketClientEngine withReconnectInternalIncreaseMills(int reconnectInternalIncreaseMills) {
		this.reconnectInternalIncreaseMills = reconnectInternalIncreaseMills;
		return this;
	}
	/**
	 * 重连间隔上限，毫秒，默认10秒
	 * @param reconnectMaxInternalMills
	 */
	public SocketClientEngine withReconnectMaxInternalMills(int reconnectMaxInternalMills) {
		this.reconnectMaxInternalMills = reconnectMaxInternalMills;
		return this;
	}
	/**
	 * 重连次数上限，<=0表示不限制次数，默认20次
	 * @param reconnectMaxCount
	 */
	public SocketClientEngine withReconnectMaxCount(int reconnectMaxCount) {
		this.reconnectMaxCount = reconnectMaxCount;
		return this;
	}
	/**
	 * 解码器
	 * @param decoder
	 * @return
	 */
	public SocketClientEngine withDecoder(IProtoDecoder<?> decoder) {
		this.decoder = decoder;
		return this;
	}
	/**
	 * 编码器
	 * @param encoder
	 * @return
	 */
	public SocketClientEngine withEncoder(IProtoEncoder<?> encoder) {
		this.encoder = encoder;
		return this;
	}
	public SocketClientEngine withLogger(ILogger logger) {
		this.logger = logger;
		return this;
	}
	/**
	 * 创建相关资源，并开始连接
	 */
	public void build() {
		if(started) {
			if(this.connect.isClosed()) {
				this.reconnect();
			}
			return;
		}
		if(this.socketClientHandler == null) {
			throw new NullPointerException("the socket event handler can not be null");
		}
		if(this.decoder == null) {
			throw new NullPointerException("the decoder cannt be null");
		}
		if(this.encoder == null) {
			throw new NullPointerException("the encode cannt be null");
		}
		if(this.heartbeatData == null) {
			throw new NullPointerException("the heartbeat data handler cannt be null");
		}
		if(this.addressService == null) {
			throw new NullPointerException("the address service cannt be null");
		}
		if(logger != null) {
			ILogger.Instance = logger;
		}
		SocketClientHandlerObservable observable = new SocketClientHandlerObservable();
		this.connect = new BIOSocketConnector(connectTimeoutMills, observable, this.encoder, this.decoder);
		this.retryConn = new RetryConnector(addressService, connect, reconnectInternalMills, 
				reconnectInternalIncreaseMills, reconnectMaxInternalMills, reconnectMaxCount);
		
		this.heartbeat = new Heartbeat(connect, heartbeatData, heartbeatInternalMills, heartbeatTimeoutMills);
		
		SocketClientHandlerDecorator decorator = new SocketClientHandlerDecorator(this.socketClientHandler,
				this.retryConn, this.heartbeat);
		observable.addObserver(decorator);
		
		this.senderManager = new SenderManager(connect, 1024);
		this.senderManager.start();
		
		this.retryConn.doConnect();
		this.started = true;
	}
	/**
	 * 会清发送队列缓冲和停止心跳
	 */
	public void destroy() {
		this.senderManager.clear();
		this.heartbeat.shutdown();
		this.connect.close();
	}
	/**
	 * 直接发送
	 * @param data
	 */
	public boolean sendWithoutQueue(Object data) {
		return this.connect.send(data);
	}
	/**
	 * 使用队列发送
	 * @param sendId 发送id，用于标志发送协议，在发送失败时会有回调
	 * @param data
	 */
	public boolean send(String sendId, Object data) {
		return this.senderManager.send(sendId, data);
	}
	/**
	 * 心跳包返回时，必须调用，否则会认为心跳包超时连接无效，导致重连
	 */
	public void onHearbeatResponseOk() {
		this.heartbeat.onBeatResponseOk();
	}
	/**
	 * 是否连接
	 * @return
	 */
	public boolean isConnecting() {
		return this.connect.isConnecting();
	}
	/**
	 * 是否关闭
	 * @return
	 */
	public boolean isClosed() {
		return this.connect.isClosed();
	}
	/**
	 * 放弃连接之后，手动连接
	 */
	public void reconnect() {
		this.retryConn.doConnect();
	}
}
