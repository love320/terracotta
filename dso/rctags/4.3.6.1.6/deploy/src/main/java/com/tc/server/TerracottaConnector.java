/*
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.server;

import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.LocalConnector;
import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PushbackInputStream;
import java.io.StringWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A Jetty connector that is handed sockets from the DSO listen port once they are identified as HTTP requests
 */
public class TerracottaConnector extends LocalConnector {

  private static final Logger LOGGER = LoggerFactory.getLogger(TerracottaConnector.class);

  public TerracottaConnector(Server server, HttpConnectionFactory httpConnectionFactory) {
    super(server, httpConnectionFactory);
    // to avoid idle timeout warnings in the logs
    this.setIdleTimeout(-1);
    this.connect();
  }

  public void handleSocketFromDSO(Socket socket, byte[] data) throws IOException {

    //PushbackInputStream allows us to retrieve the original http request
    PushbackInputStream pis = new PushbackInputStream(socket.getInputStream(), data.length);
    pis.unread(data);
    InputStreamReader inputStreamReader = new InputStreamReader(pis, "ISO-8859-1");
    BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

    String readLineFromBuffer = bufferedReader.readLine();
    StringWriter writer = new StringWriter();
    do {
      writer.write(readLineFromBuffer);
      writer.write("\r\n");
      readLineFromBuffer = bufferedReader.readLine();
    }
    while (!readLineFromBuffer.equals(""));
    writer.write("\r\n");

    String requestAsString = writer.toString();

    try {
      String response = this.getResponse(requestAsString);
      String[] splitResponse = response.split("\n");
      List<String> responseLines = new ArrayList<>(Arrays.asList(splitResponse));
      responseLines.add(1, "Connection: close\r");
      String responseWithConnectionClose = String.join("\n", responseLines) + "\n";
      socket.getOutputStream().write(responseWithConnectionClose.getBytes("ISO-8859-1"));
    } catch (Exception e) {
      LOGGER.error("Exception while retrieving the HTTP response", e);
    } finally {
      socket.close();
    }

  }

}
