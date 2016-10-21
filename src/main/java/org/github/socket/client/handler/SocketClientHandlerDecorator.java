package org.github.socket.client.handler;

import java.util.Observable;
import java.util.Observer;

import org.github.socket.client.connect.Heartbeat;
import org.github.socket.client.connect.RetryConnector;
import org.github.socket.client.handler.SocketClientHandlerObservable.ObserverEvent;
import org.github.socket.client.handler.SocketClientHandlerObservable.ObserverEventType;

/**
 * 客户端事件ISocketClientHandler观察者
 * 并以装饰的方式对第三方SocketClientHandler的实现进行封装
 * @author 林炳忠
 *
 */
public class SocketClientHandlerDecorator implements Observer, ISocketClientHandler {
	private ISocketClientHandler socketClientHandler;//客户端事件接口
	private ReceiveManager receiveManager;
	private RetryConnector retryConnector;
	private Heartbeat heartbeat;
	public SocketClientHandlerDecorator(ISocketClientHandler socketClientHandler, RetryConnector retryConnector, 
			Heartbeat heartbeat) {
		this.socketClientHandler = socketClientHandler;
		this.retryConnector = retryConnector;
		this.heartbeat = heartbeat;
		this.receiveManager = new ReceiveManager(socketClientHandler);
	}
	@Override
	public void onConnect() {
		this.heartbeat.start();
		this.receiveManager.start();
		
		this.socketClientHandler.onConnect();
	}
	@Override
	public void onDisconnect() {
		this.heartbeat.shutdown();
		this.retryConnector.doConnect();
		this.socketClientHandler.onDisconnect();
	}
	@Override
	public void onReceive(Object data) {
		if(data != null) {
			this.receiveManager.onReceive(data);
		}
	}
	@Override
	public void onException(Throwable throwable) {
		this.socketClientHandler.onException(throwable);
	}
	@Override
	public void onTryConnectGiveup() {
		this.socketClientHandler.onTryConnectGiveup();
	}
	@Override
	public void onSendFail(String sendId) {
		this.socketClientHandler.onSendFail(sendId);
	}
	@Override
	public void update(Observable o, Object arg) {
		if(arg instanceof ObserverEvent) {
			ObserverEvent event = (ObserverEvent) arg;
			ObserverEventType type = event.type;
			Object data = event.data;
			switch (type) {
			case Connect:
				this.onConnect();
				break;
			case Disconnect:
				this.onDisconnect();
				break;
			case Receive:
				this.onReceive(data);
				break;
			case Exception:
				this.onException((Throwable) data);
				break;
			case SendFail:
				this.onSendFail((String)data);
				break;
			case TryConnectGiveup:
				this.onTryConnectGiveup();
				break;
			default:
				break;
			}
		}
	}

}
