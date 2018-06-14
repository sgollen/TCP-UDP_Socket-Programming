import java.io.*;
import java.net.*;
import java.lang.*;

class client {
   public static void main(String args[]) throws Exception
   {
    String serverAddr;
    String req_code;
    String msg;
    int n_port;
    if(args.length != 4) {
      System.out.println("Incorrect number of arguments");
      System.exit(0);
    }
   // System.out.println("Incorrect number of arguments");
    serverAddr = args[0];
    n_port = Integer.parseInt(args[1]);
    req_code = args[2];
    msg = args[3];
    InetAddress IPAddr;
    IPAddr = InetAddress.getByName(serverAddr);

   // create udp socket and send the req-code using udp packet
    DatagramSocket clientSocket = new DatagramSocket();
    byte[] reqCodeMsg = new byte[1024];
    reqCodeMsg = req_code.getBytes();
    DatagramPacket sendCode = new DatagramPacket(reqCodeMsg, reqCodeMsg.length, IPAddr, n_port);
    clientSocket.send(sendCode);

    //recieve the r_port value from the server
    byte[] rPortCode = new byte[1024];
    DatagramPacket receivePortCode = new DatagramPacket(rPortCode, rPortCode.length);
    clientSocket.setSoTimeout(1000); // block for 1sec if no response from server then exit
    try {
        clientSocket.receive(receivePortCode);
    } catch (SocketTimeoutException e) {
        System.out.println("TIMEOUT:NO_RESPONSE_FROM_SERVER");
        System.exit(0);  
    }
    String answerPort = new String(receivePortCode.getData());
    String rPort = answerPort.trim();
    // send the r_port value back using the same udp socket
    DatagramPacket sendRPort = new DatagramPacket(rPortCode, rPortCode.length, IPAddr, n_port);
    clientSocket.send(sendRPort);
   
   // receieve ach from the server
    byte[] confirmCode = new byte[16];
    DatagramPacket receiveAch = new DatagramPacket(confirmCode, confirmCode.length);
    clientSocket.receive(receiveAch);
    String tempCode = new String(receiveAch.getData());
    String achCode = tempCode.trim();
   
    if (achCode.equals("no")) {
      System.out.println("Achknowledgment_from_server=DENIED");
      System.exit(0);
    }
    int r_port = Integer.parseInt(rPort);
    //udp connection closed
    clientSocket.close();
        
    // tcp connection socket
    Socket mainSocket = new Socket(IPAddr, r_port);
    //attach streams to the socket
    DataOutputStream outStream = new DataOutputStream (mainSocket.getOutputStream());
    BufferedReader inStream =  new BufferedReader(new InputStreamReader(mainSocket.getInputStream()));
    outStream.writeBytes(msg + '\n');
    String finalMsg = inStream.readLine();
    System.out.println("CLIENT_MSG=" + finalMsg);
    mainSocket.close();
   }
}
