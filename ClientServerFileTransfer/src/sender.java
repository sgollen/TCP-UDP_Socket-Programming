import java.io.*;
import java.net.*;
import java.util.*;

public class sender {
    public static void main(String argv[]) {
        if (argv.length != 4) {
            System.out.println("Incorrect number of command line arguments");
            System.out.println("Usage: java sender <host address> <udp port used by the emulator for data> <udp port used by the sender for acks> <name of the file to be transmitted> ");
            return;
        }

        String hostAddress = argv[0];

        // try parse the port number, if invalid, exit the program and print out the usage
        int emulatorPortNumber = 0;
        int ackPortNumber = 0;
        try {
            emulatorPortNumber = Integer.parseInt(argv[1]);
            ackPortNumber = Integer.parseInt(argv[2]);
        } catch (Exception e) {
            System.out.println(e);
            System.out.println("Invalid port number, both port numbers must be integers");
            System.out.println("Usage: java sender <host address> <udp port used by the emulator for data> <udp port used by the sender for acks> <name of the file to be transmitted> ");
            return;
        }

        /*
         * reading input from the file, if file does not exist, exit and print error message
         */
        String filePath = argv[3];
        FileInputStream fis;
        try {
            fis = new FileInputStream(filePath);
        } catch (Exception e) {
            System.out.println(e);
            System.out.println("The file path does exsit or it is unreadable. please check your input file and try again");
            return;
        }

        int BUFFERSIZE = 200; // buffer size for reading input from the file
        ArrayList<packet> al = new ArrayList<packet>();
        int seqNum = 0;
        byte[] buffer = new byte[BUFFERSIZE];


        // read in a line by line fashion
        try (Scanner scanner = new Scanner(new File(argv[3]))) {
            while (scanner.hasNextLine()){
                String line = scanner.nextLine();
                if (line.length() > 500) {
                    System.out.println("line in the file longer than 500");
                    // break into 3 packets
                    for (int i = 0; i <= line.length() / 500; i++) {
                        String temp = line.substring(i * 500, Math.min((i + 1) *  500, line.length()));
                        packet a = packet.createPacket(seqNum, temp);
                        al.add(a);
                        seqNum++;
                    }
                } else {
                    packet a = packet.createPacket(seqNum, line);
                    al.add(a);
                    seqNum++;
                }
            }
            scanner.close();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("error reading file");
        }

        /*
         * start sending the file and the last eof, after that, will close the connection
         */

        // converting the hostname to ip
        InetAddress IPAddress;
        try {
            IPAddress = InetAddress.getByName(hostAddress);
        } catch(Exception e) {
            System.out.println(e);
            System.out.println("server address is invalid, please try again");
            return;
        }

        // sending the file
        try {
            sendFile(seqNum, al, IPAddress, ackPortNumber, emulatorPortNumber);
        } catch(Exception e) {
            System.out.println(e);
            System.out.println("something is wrong when sending the file, please try again");
        }
    }

    public static void sendFile(int totalPacketNum, ArrayList<packet> al, InetAddress IPAddress, int listeningPort, int receiverPort) throws Exception {
        int N = 10; // window size
        int sendBase = 0;
        int nextSequenceNum = 0;
        int timer = 50; // timer value in msecs

        // create datagram socket
        DatagramSocket sendingSocket = new DatagramSocket(0);  // this socket sends packet to the simulator
        DatagramSocket listeningSocket;  // this socket listens to the acks from the simulator
        try {
            listeningSocket = new DatagramSocket(listeningPort);
        } catch(Exception e) {
            System.out.println(e);
            System.out.println("The specified sender port is not availiable, please try again");
            return;
        }

        PrintWriter writer = new PrintWriter("seqnum.log", "UTF-8"); // used for writing to the log file
        PrintWriter writer2 = new PrintWriter("ack.log", "UTF-8"); // used for writing to the log file
        while(true) {
            // terminate condition, recieves ack for the last packet
            if (sendBase == totalPacketNum) {
                /* totalPacketNum is the number of elements in the ArrayList.
                 * when sendBase equal to that, it is an indicator that the file has been transmitted completely
                 * we should break out the loop and proceed to send eot packet
                 */
                break;
            }

            // loop that sends packet from sendBase to sendBase + N, where N is the window size
            while (nextSequenceNum < sendBase + N && nextSequenceNum < totalPacketNum) {

                // send the current packet
                byte[] payload = al.get(nextSequenceNum).getUDPdata();
                DatagramPacket sendPacket =new DatagramPacket(payload, payload.length, IPAddress, receiverPort);
                sendingSocket.send(sendPacket);

                // writing to the seqnum.log
                writer.println(nextSequenceNum % 32);
                writer.flush(); // in case something fails half way, the flush will flush the content into the file

                // update the nextSequence number
                nextSequenceNum ++;
            }

            /* listening for the acks
             * setSoTimeout with a non-zero timeout, a call to receive() for this DatagramSocket will block for only this amount of time.
             * If the timeout expires, a java.net.SocketTimeoutException is raised
             */
            try {
                // set the time out limit
                listeningSocket.setSoTimeout(timer);

                // receive the packet
                byte[] ack = new byte[512];
                DatagramPacket m = new DatagramPacket(ack, ack.length);
                listeningSocket.receive(m);
                packet p = packet.parseUDPdata(m.getData());

                // write into log file
                int ackNum = p.getSeqNum();
                writer2.println(ackNum);
                writer2.flush();

                /* In the current implementation, sendBase and nextSequenceNum is the index in the ArrayList al
                 * However, all the seqNum in the packet is module 32, so we cannot directly use the seqNum to access
                 * the element in the ArrayList, as the index is not module 32
                 * So we need below relatively complex logic to determine how to update our sendBase correcly.
                 * Good thing is since 32 > 2 * N = 20,
                 * we will not encounter the case where we don't know if this is a delayed ack or we should move forward
                 */
                if (ackNum == sendBase % 32) {
                    sendBase ++;
                } else if (ackNum > sendBase % 32 && ((ackNum - sendBase % 32) < 10)) {
                    sendBase += (ackNum- sendBase % 32 + 1); // commulative ack, move the send base forward
                } else if (ackNum < 10 && ((ackNum + 32 - sendBase % 32) < 10)) {
                    sendBase += (ackNum + 32 - sendBase % 32 + 1); // commulative ack, move the send base forward
                }
            } catch (SocketTimeoutException e) {
                // time out, resent every unacked packets
                for (int i = sendBase; i < nextSequenceNum; i++) {
                    byte[] payload = al.get(i).getUDPdata();
                    DatagramPacket sendPacket = new DatagramPacket(payload, payload.length, IPAddress, receiverPort);
                    sendingSocket.send(sendPacket);
                }
            }
        }

        // now sending the last eot packet
        packet eot = packet.createEOT(totalPacketNum); // seqNum of the eot should be the next sequence number, which is the same as totaLPacketNum
        byte[] b = eot.getUDPdata();
        DatagramPacket e = new DatagramPacket(b, b.length, IPAddress, receiverPort);
        sendingSocket.send(e);

        // now listenting to the last eot
        // do not need implement resend here as the eot packet will not get lost
        while(true) {
            try {
                // listening to the eot
                byte[] temp = new byte[512];
                DatagramPacket t = new DatagramPacket(temp, temp.length);
                listeningSocket.receive(t);

                // read the packet
                packet p = packet.parseUDPdata(t.getData());
                if (p.getType() == 2) {
                    break;
                }
            } catch(SocketTimeoutException m) {
                // it is delayed, but eot will not be lost, it will come
                continue;
            }

        }


        // clean up
        listeningSocket.close();
        sendingSocket.close();
        writer.close();
        writer2.close();
    }
}

