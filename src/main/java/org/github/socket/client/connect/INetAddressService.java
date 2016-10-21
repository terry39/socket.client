package org.github.socket.client.connect;

import java.util.List;
/**
 * address list接口
 * @author 林炳忠
 *
 */
public interface INetAddressService {
	public List<NetAddress> getAddresses();
	public static class NetAddress {
		private String ip;
		private int port;
		public String getIp() {
			return ip;
		}
		public void setIp(String ip) {
			this.ip = ip;
		}
		public int getPort() {
			return port;
		}
		public void setPort(int port) {
			this.port = port;
		}
	}
}
