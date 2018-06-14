import java.io.*;
import java.net.*;
import java.lang.*;

class server {
  public static void main(String args[]) throws Exception
  {
    String req_code;
    if (args.length != 1) {
      System.out.println("Incorrect number of arguments");
      System.exit(0);
    }
    req_code = args[0];
    String randomPort = "1234";
    int n_port = Integer.parseInt(randomPort);
    DatagramSocket serverSocket;
   // tries to create a socket at the pre-determined n-port
   //if it fails increments the selected n_port
    while (true) {
      try {
          serverSocket = new DatagramSocket(n_port);
      } catch (SocketException e) {
         // System.out.println("FAILED PORT");
          n_port = n_port + 1;
          continue;
      }
      break;
    }
    String tempVal = Integer.toString(n_port);
    System.out.println("SERVER_PORT=" + tempVal);

// keep the server alive until terminated manually
    while(true) {
       // recieve the req_code from client
       byte[] reqMsg = new byte[1024];
       DatagramPacket receiveCode = new DatagramPacket(reqMsg, reqMsg.length);
       serverSocket.receive(receiveCode); 
       String tempCode = new String(receiveCode.getData());
       String requiredCode = tempCode.trim();  // required for string comparision
       // save the client port and address
       int clientPort = receiveCode.getPort();
       InetAddress IPaddr = receiveCode.getAddress();
       // checks if the client send the correct req_code, if yes then go forward else do nothing/continue
       if (requiredCode.equals(req_code)) {
       //create the serverSocket (listener) at designated port if fails increment as suggested in the A1 FAQs 
          ServerSocket mainSocket;
          int r_port = 2150;
          while (true) {
             try {
                mainSocket = new ServerSocket(r_port);
             } catch (SocketException e) {
              //  System.out.println("FAILED PORT");
                r_port = r_port + 1;
                continue;
             }
             break;
          }
          String tempVal1 = Integer.toString(r_port);
       
          // send the r_port to client as a packet using udp n_port (udp)
          byte[] rPortCode = new byte[1024]; 
          rPortCode = tempVal1.getBytes();
          DatagramPacket sendPort = new DatagramPacket(rPortCode, rPortCode.length, IPaddr, clientPort);
          serverSocket.send(sendPort);
        
          // recieve the confirmation r_port value from client
          byte[] confirmPort = new byte[1024];
          DatagramPacket receivePort = new DatagramPacket(confirmPort, confirmPort.length);
          serverSocket.receive(receivePort);
          String tempPort = new String(receivePort.getData());
          String checkPort = tempPort.trim();  // required for the srting comparison
          System.out.println("SERVER_TCP_PORT=" + checkPort);
        
          // send achknowledge code if r_port is same on both sides using Datagram socket (udp)
          byte[] achCode = new byte[1024];
          String achString;
          if(checkPort.equals(tempVal1)) {  //check tempVal1 = server r_port, checkPort = client sent r_port
              achString = "ok";
              achCode = achString.getBytes();       
          } else {
              achString = "no";
              achCode = achString.getBytes();         
          }
          DatagramPacket sendAch = new DatagramPacket(achCode, achCode.length, IPaddr, clientPort);
          serverSocket.send(sendAch);
          while (true) {
              Socket connectionSocket = mainSocket.accept();   //wait for message
              DataOutputStream outStream = new DataOutputStream (connectionSocket.getOutputStream());
             //attach input stream to the socket
              BufferedReader inStream =  new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
              String finalMsg = inStream.readLine();
             //message
              System.out.println("SERVER_RCV_MSG=" + finalMsg);
              StringBuffer sb = new StringBuffer(finalMsg);
              String reverseMsg = sb.reverse().toString(); // reverse the given string
              outStream.writeBytes(reverseMsg + '\n');
// close the tcp socket listener or continue listening, not clearified?
///// mainSocket.close();   // close the listener Serversocket. Not closed to check if randomizing r_port works 
              connectionSocket.close();
              break;
          }  
       } else {
          continue;  // if req_code is wrong then keep the server alive i.e loop
       }// req_code check ends
    }// server loop ends
  }// main ends
}// class ends
