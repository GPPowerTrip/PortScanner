package org.powertrip.excalibot.common.plugins.portscanner;

import org.powertrip.excalibot.common.com.SubTask;
import org.powertrip.excalibot.common.plugins.KnightPlug;
import org.powertrip.excalibot.common.plugins.interfaces.knight.ResultManagerInterface;

import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Created by Jaime on 20/01/2016.
 * 03:37
 */
public class Bot extends KnightPlug{
	public Bot(ResultManagerInterface resultManager) {
		super(resultManager);
	}

	@Override
	public boolean run(SubTask subTask) {
		String address = subTask.getParameter("address");
		int start = Integer.valueOf(subTask.getParameter("start"));
		int step = Integer.valueOf(subTask.getParameter("step"));
		int end = start + step - 1;

		for(int port = start; port<=end; port++){
			if(Thread.currentThread().isInterrupted() || !Thread.currentThread().isAlive()) return false;
			if(portIsOpen(address, port, 200)){
				try {
					resultManager.returnResult(
							subTask.createResult()
									.setSuccessful(true)
									.setResponse("port", String.valueOf(port))
					);
				} catch (InterruptedException e) {
					return false;
				}
			}
		}

		return true;
	}

	boolean portIsOpen(final String ip, final int port, final int timeout) {
		try {
			Socket socket = new Socket();
			socket.connect(new InetSocketAddress(ip, port), timeout);
			socket.close();
			return true;
		} catch (Exception ex) {
			return false;
		}
	}
}
