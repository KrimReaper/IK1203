import java.net.*;
import java.io.*;
import java.util.*;
import tcpclient.TCPClient;
import java.nio.charset.StandardCharsets;

public class HTTPAsk {

  /*
   * Usage: explain how to use the program, then exit with failure status
   */
  private static void usage() {
      System.err.println("Usage: HTTPAsk <server port number>");
      System.exit(1);
  }

  /*
   * Main program. Parse arguments on command line and in case of error,
   * tell user how to operate the program.
   */
  public static void main( String[] args) {
    int serverPort = 0;
    ServerSocket serverSocket = null;

    try {
      serverPort = Integer.parseInt(args[0]);
    } catch (Exception ex) {
      usage();
    }

    // Create the server socket with the specified port. Catch any socket errors
    try {
      serverSocket = new ServerSocket(serverPort);
    } catch(IOException ex) {
      System.err.println(ex);
      System.exit(1);
    }

    try {
      // Keep the server running indefinitely and serve one client at a time
      while (true) {

        // Connect
        Socket connectionSocket = serverSocket.accept();
        System.out.println("Client connected to the server...");
        System.out.println(connectionSocket.toString());
        System.out.println();

        // http://localhost:8888/ask?hostname=time.nist.gov&limit=1200&port=13
        // GET /ask?hostname=time.nist.gov&limit=1200&port=13 HTTP/1.1
        //URL url = new URL("http://localhost:" + serverPort + response[1]);

        DataOutputStream fromServer = new DataOutputStream(connectionSocket.getOutputStream());
        BufferedReader toServer = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
        String[] headersRaw = toServer.readLine().split("[\\?= &]");
        System.out.println("Getting input from socket...\n");

        // Initial query parameters
        String hostname = null;
        int port = 0;
        byte[] toServerBytes = null;
        boolean shutdown = false;
        Integer timeout = null;
        Integer limit = null;

        // Test headersRaw
        System.out.println("Testing headers");
        for (int i = 0; i < headersRaw.length; i++) {
            System.out.println(headersRaw[i]);
        }

        System.out.println("\nParsing HTTP request...");
        // Needs to handle more cases
        if (headersRaw[1].equals("/ask")) {
          try {
            // Bad loop, reimplement with arrayList for task 4?
            for (int i = 2; i < headersRaw.length; i++) {
                if (headersRaw[i].equals("hostname")) {
                    hostname = headersRaw[i+1];
                }
                if (headersRaw[i].equals("port")) {
                    port = Integer.parseInt(headersRaw[i+1]);
                }
                if (headersRaw[i].equals("string")) {
                    toServerBytes = headersRaw[i+1].getBytes(StandardCharsets.UTF_8);
                }
                if (headersRaw[i].equals("shutdown")) {
                    shutdown = Boolean.parseBoolean(headersRaw[i+1]);
                }
                if (headersRaw[i].equals("timeout")) {
                    timeout = Integer.valueOf(headersRaw[i+1]);
                }
                if (headersRaw[i].equals("limit")) {
                    limit = Integer.valueOf(headersRaw[i+1]);
                }
            }
            //Needs more catches
          } catch (Exception ex){
              System.out.println("Error in parsing URL: " + ex);
              connectionSocket.close();
          }

          // Test headers parsed
          System.out.println("\nTesting parsed headers");
          System.out.println(hostname);
          System.out.println(port);
          // for (int i = 0; i < toServerBytes.length; i++) {
          //     System.out.println(toServerBytes[i]);
          // }
          System.out.println(shutdown);
          System.out.println(timeout);
          System.out.println(limit);

          // Try opening the TCP client
          System.out.println("\nOpening TCP client...");
          try {
            TCPClient tcpClient = new TCPClient(shutdown, timeout, limit);
            byte[] serverResponse = tcpClient.askServer(hostname, port, toServerBytes);
            fromServer.writeBytes("HTTP/1.1 200 OK\r\n\r\n");

            System.out.println("\nAfter serverBytes");


            // For testing
            String serverResponseParsed = new String(serverResponse);
            System.out.println("\n******************************* DEBUG *******************************");
            System.out.println(serverResponseParsed);
            System.out.println("*************************** END OF DEBUG ****************************\n");

            fromServer.write(serverResponse);
            connectionSocket.close();

          } catch (IOException ex) {
            fromServer.writeBytes("HTTP/1.1 404 Not Found\r\n");
            System.out.println("Error in TCP connection: " + ex);
            connectionSocket.close();
          }
        }

        else {
          fromServer.writeBytes("HTTP/1.1 400 Bad Request\r\n");
          connectionSocket.close();
        }

        System.out.println("\nClient has been served...\n\n");
        connectionSocket.close();
      }

    } catch (Exception ex) {
      System.out.println("Server error: " + ex);
      System.exit(1);
    }
  }
}
