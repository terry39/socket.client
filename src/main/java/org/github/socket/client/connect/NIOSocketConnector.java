package org.github.socket.client.connect;

import org.github.socket.client.handler.IProtoDecoder;
import org.github.socket.client.handler.IProtoEncoder;
import org.github.socket.client.handler.ISocketClientHandler;

/**
 * NIO socket client connector
 * @author 林炳忠
 *
 */
public class NIOSocketConnector implements ISocketConnector {
	private int connectTimeout;
	private ISocketClientHandler socketClientHander;//网络事件接收接口
	@SuppressWarnings("rawtypes")
	private IProtoDecoder decoder;
	@SuppressWarnings("rawtypes")
	private IProtoEncoder encoder;
	
	public NIOSocketConnector(int connectTimeout, ISocketClientHandler socketClientHander,
			IProtoEncoder<?> encoder, IProtoDecoder<?> decoder) {
		this.connectTimeout = connectTimeout;
		this.socketClientHander = socketClientHander;
		this.encoder = encoder;
		this.decoder = decoder;
	}
	
	@Override
	public boolean connect(String ip, int port) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void closeAndReconnect() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean send(Object data) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isClosed() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isBeClosing() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isConnecting() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onTryConnectFail() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSendFail(String sendId) {
		// TODO Auto-generated method stub
		
	}

}
