package org.github.socket.client.handler;
/**
 * 编码器
 * @author 林炳忠
 *
 * @param <E>
 */
public interface IProtoEncoder<E> {
	public byte[] encode(E data);
}
