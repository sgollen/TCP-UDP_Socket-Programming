import java.io.*;
import java.net.*;
import java.util.*;

class receiver {

    public static void main(String argv[]) {
        if (argv.length != 4) {
            System.out.println("Incorrect number of parameters passed");
            System.out.println("Usage: java receiver <hostname for the network emulator> <UDP port number used by the emulator for acks> <UDP port number used to receive data> <name of the file>");
            return;
        }

        // parsing the port numbers
        int ackPort;
        int dataPort;
        try {
            ackPort = Integer.parseInt(argv[1]);
            dataPort = Integer.parseInt(argv[2]);
        } catch(Exception e) {
            System.out.println(e);
            System.out.println("Invalid command line input given, the port numbers must be integers");
            System.out.println("Usage: java receiver <hostname for the network emulator> <UDP port number used by the emulator for acks> <UDP port number used to receive data> <name of the file>");
            return;
        }

        // parsing the host address
        String hostAddress= argv[0];
        InetAddress IPAddress;
        try {
            IPAddress = InetAddress.getByName(hostAddress);
        } catch(Exception e) {
            System.out.println(e);
            System.out.println("server address is invalid, please try again");
            return;
        }


        /*
         * listening to the data and send acks
         */
        ArrayList<packet> data = new ArrayList<packet>();
        try {
            boolean result =receiveData(IPAddress, ackPort, dataPort, data);
            if (!result) {
                // something has went wrong in the receiveData function
                System.out.println("fail to receive data, now exiting the program");
                return;
            }
        } catch (Exception e) {
            System.out.println(e);
            System.out.println("something wrong when receiving data, please restart and try again");
            return;
        }

        /*
         * writing to the target file
         */
        try {
            PrintWriter writer = new PrintWriter(argv[3], "UTF-8");
            for (packet s: data) {
                writer.println(new String(s.getData()));
                writer.flush();
            }
            writer.close();
        } catch(Exception e) {
            System.out.println(e);
            System.out.println("something wrong happened during writing to the file, please restart and try again");
        }
    }

    /*
     * Receives the file data from send properly, creates and sends appropriate achs to the emulator/ sender.
     */
    public static boolean receiveData(InetAddress IPAddress, int ackPort, int dataPort, ArrayList<packet> data) throws Exception {
        DatagramSocket ackSocket;  // socket that used for sending acks
        DatagramSocket dataSocket;  // socket that used for receiving data packets
        PrintWriter writer = new PrintWriter("arrival.log", "UTF-8"); // writer for writing logs
        /*
         * create sockets
         */
        try {
            ackSocket = new DatagramSocket(0);
            dataSocket= new DatagramSocket(dataPort);
        } catch(Exception e) {
            System.out.println(e);
            System.out.println("the port provided are not available, please choose a different port number");
            return false;
        }

        /*
         * receiving data
         */
        int expectedSeq = 0;
        byte[] receiveData = new byte[512];
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

        // loop for receiving data
        while(true) {
            dataSocket.receive(receivePacket);

            packet dataPacket = packet.parseUDPdata(receivePacket.getData());  // from bytes to packet
            int seqNum = dataPacket.getSeqNum();
            int type = dataPacket.getType();

            if (expectedSeq == seqNum) {

                if (type == 2) {
                    /* receive an eot packet, end of transmission
                     * should send an eot packet, and then exit
                     * doing some cleanup before exsiting this function
                     */

                    // create and send eof packet
                    packet eot = packet.createEOT(seqNum);
                    byte[] e = eot.getUDPdata();
                    DatagramPacket ep = new DatagramPacket(e, e.length, IPAddress, ackPort);
                    ackSocket.send(ep);

                    // clean up
                    ackSocket.close();
                    dataSocket.close();
                    writer.close();
                    return true;
                }

                // not end of transmission, we should log it and check whether we should accept or drop this packet
                writer.println(seqNum);
                writer.flush();

                // append input to the data list
                data.add(dataPacket);

                // create new ack packet
                packet s = packet.createACK(seqNum);

                // send out the packet
                byte[] sendData = s.getUDPdata();
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, ackPort);
                ackSocket.send(sendPacket);
                expectedSeq ++;
                expectedSeq %= 32;
            } else {
                // send an ack for expectedSeq
                // create new ack packet
                packet s;
                if (expectedSeq == 0) {
                    s = packet.createACK(31);
                } else {
                    s = packet.createACK(expectedSeq - 1);
                }


                // send out the packet
                byte[] sendData = s.getUDPdata();
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, ackPort);
                ackSocket.send(sendPacket);

            }
        }
    }

}