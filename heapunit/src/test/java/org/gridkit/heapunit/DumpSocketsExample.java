package org.gridkit.heapunit;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketImpl;

import org.junit.Test;

public class DumpSocketsExample {

	@Test
	public void printSockets() throws IOException {
		
		ServerSocket ss = new ServerSocket();
		ss.bind(sock(5000));
		
		Socket s1 = new Socket();
		Socket s2 = new Socket();
		
		s1.connect(sock(5000));
		s2.connect(sock(5000));
		
		ss.close();
		s1.close();
		// s2 remains unclosed
		
		HeapImage hi = HeapUnit.captureHeap();
		
		for(HeapInstance i: hi.instances(SocketImpl.class)) {
			// fd field in SocketImpl class is nullified when socket gets closed
			boolean open = i.value("fd") != null;
			System.out.println(i.rehydrate() + (open ? " - open" : " - closed"));
		}
	}

	private SocketAddress sock(int port) {
		return new InetSocketAddress("127.1.2.3", port);
	}
	
}
