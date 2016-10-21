package org.github.socket.client.handler;

import java.nio.ByteBuffer;

public interface IProtoDecoder <E>{
	public E decoder(ByteBuffer buffer);
}
