package org.github.socket.client.connect;

/**
 * 客户端连接
 * @author 林炳忠
 *
 */
public interface ISocketConnector {
	public boolean connect(String ip, int port);
	/**
	 * 强制关闭，不再重试连接
	 */
	public void close();
	/**
	 * 关闭之后在重连
	 */
	public void closeAndReconnect();
	public boolean send(Object data);
	/**
	 * 是否关闭状态
	 * @return
	 */
	public boolean isClosed();
	/**
	 * 是否被主动调用close()
	 * @return
	 */
	public boolean isBeClosing();
	public boolean isConnecting();
	/**
	 * 重试次数用完后调用
	 */
	public void onTryConnectFail();
	/**
	 * 发送失败
	 */
	public void onSendFail(String sendId);
}  