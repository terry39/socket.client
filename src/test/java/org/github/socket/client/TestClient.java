package org.github.socket.client;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.github.socket.client.connect.INetAddressService;
import org.github.socket.client.handler.IHearbeatDataHandler;
import org.github.socket.client.handler.ILogger;
import org.github.socket.client.handler.IProtoDecoder;
import org.github.socket.client.handler.IProtoEncoder;
import org.github.socket.client.handler.ISocketClientHandler;

public class TestClient {
	
	public static void start() {
		SocketClientEngine.Instance
			.withDecoder(new IProtoDecoder<String>() {
				@Override
				public String decoder(ByteBuffer buffer) {
					return new String(buffer.array()).trim();
				}
			})
			.withEncoder(new IProtoEncoder<String>() {
				@Override
				public byte[] encode(String data) {
					return data.getBytes();
				}
			})
			.withHeartbeatData(new IHearbeatDataHandler() {
				@Override
				public Object sendData() {
					return "heartbeat";
				}
			})
			.withNetAddressService(new INetAddressService(){
				@Override
				public List<NetAddress> getAddresses() {
					List<NetAddress> list = new ArrayList<>();
					NetAddress addr = new NetAddress();
					addr.setIp("127.0.0.1");
					addr.setPort(8001);
					list.add(addr);
					return list;
				}
			})
			.withLogger(new ILogger() {
				@Override
				public void logger(String str) {
					System.out.println("logger : " + str);
				}
			})
			.withSocketClientHandler(new ISocketClientHandler() {
				
				@Override
				public void onTryConnectGiveup() {
					System.out.println("onTryConnectGiveup");
				}
				
				@Override
				public void onSendFail(String sendId) {
					System.out.println("onSendFail "+sendId);
					
				}
				
				@Override
				public void onReceive(Object data) {
					System.out.println("onReceive = " + data);
					if("heartbeat".equals(data)) {
						SocketClientEngine.Instance.onHearbeatResponseOk();
					}
				}
				
				@Override
				public void onException(Throwable throwable) {
					throwable.printStackTrace();
					System.out.println("onException " + throwable.getMessage());
				}
				
				@Override
				public void onDisconnect() {
					System.out.println("onDisconnect");
				}
				
				@Override
				public void onConnect() {
					System.out.println("onConnect");
				}
			})
			.build();
			;
	}
	public static void main(String []args) {
		TestClient.start();
	}
}
