package org.github.socket.client.handler;
/**
 * 日志接口
 * @author 林炳忠
 *
 */
public abstract class ILogger {
	public static ILogger Instance = new ILogger(){

		@Override
		public void logger(String str) {
			System.out.println(str);
		}
		
	};
	public abstract void logger(String str);
}
