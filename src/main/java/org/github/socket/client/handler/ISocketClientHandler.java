package org.github.socket.client.handler;

/**
 * 客户端事件
 * @author 林炳忠
 *
 */
public interface ISocketClientHandler {
	public abstract void onConnect();
	public abstract void onDisconnect();
	public abstract void onReceive(Object data);
	/**
	 * 发生异常
	 * @param throwable
	 */
	public abstract void onException(Throwable throwable);
	/**
	 * 重试次数用完后调用
	 */
	public abstract void onTryConnectGiveup();
	/**
	 * 发送失败
	 */
	public abstract void onSendFail(String sendId);
}
