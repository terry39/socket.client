package org.github.socket.client.handler;

import java.util.Observable;
import java.util.Observer;
/**
 * 以装饰的方式对ISocketClientHandler的实现进行封装,并以观察者的方式对网络事件进行广播,提供给网络连接使用<br/>
 * 目的在于解耦连接器／重连器／心跳器／SocketClientHandler等类之间互相引用
 * @author 林炳忠
 *
 */
public class SocketClientHandlerObservable extends Observable implements ISocketClientHandler{
	//观察者事件类型
	public static enum ObserverEventType {
		Connect,
		Disconnect,
		Receive,
		Exception,
		TryConnectGiveup,
		SendFail,
		;
	}
	public static class ObserverEvent {
		ObserverEventType type;
		Object data;
		public ObserverEvent(ObserverEventType type, Object data) {
			this.type = type;
			this.data = data;
		}
	}
	public void addObserver(Observer observer) {
		super.addObserver(observer);
	}

	@Override
	public void onConnect() {
		this.setChanged();
		this.notifyObservers(new ObserverEvent(ObserverEventType.Connect, null));
	}

	@Override
	public void onDisconnect() {
		this.setChanged();
		this.notifyObservers(new ObserverEvent(ObserverEventType.Disconnect, null));
	}

	@Override
	public void onReceive(Object data) {
		this.setChanged();
		this.notifyObservers(new ObserverEvent(ObserverEventType.Receive, data));
	}

	@Override
	public void onException(Throwable throwable) {
		this.setChanged();
		this.notifyObservers(new ObserverEvent(ObserverEventType.Exception, throwable));
	}
	@Override
	public void onTryConnectGiveup() {
		this.setChanged();
		this.notifyObservers(new ObserverEvent(ObserverEventType.TryConnectGiveup, null));
	}
	@Override
	public void onSendFail(String sendId) {
		this.setChanged();
		this.notifyObservers(new ObserverEvent(ObserverEventType.SendFail, sendId));
	}
}
