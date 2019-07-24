package com.tc.util.io;

import com.tc.net.core.SecurityInfo;
import com.tc.util.Assert;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

import junit.framework.TestCase;

import static java.lang.Thread.sleep;

public class ServerURLTest extends TestCase {

  public void testServerURLTimeout() throws Exception {
    ServerSocket server;
    try {
      server = new ServerSocket(0);
      Thread t = new Thread(() -> {
        try {
          Socket socket = server.accept();
          BufferedWriter response = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
          response.write("connected to server");
          sleep(1500); //default timeout for the ServerURL for this test 1000ms so it will timeout after 1000ms.
          response.write("\n");
          response.flush();
          socket.close();
        } catch (Exception e) {
          throw new RuntimeException(e);
        } finally {
          try {
            server.close();
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        }
      });
      t.start();
      SecurityInfo securityInfo = new SecurityInfo();
      ServerURL serverURL = new ServerURL(server.getInetAddress()
          .getHostAddress(), server.getLocalPort(), "/groupidmap", 1000, securityInfo);
      serverURL.openStream();
      Assert.fail("Connection should have timed out");
    } catch (SocketTimeoutException e) {
      //pass
    }
  }
}