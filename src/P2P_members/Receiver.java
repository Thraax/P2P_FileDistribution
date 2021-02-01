package P2P_members;

import java.io.File;
import java.io.IOException;
import java.net.*;
import java.nio.file.Files;
import java.util.Scanner;

public class Receiver {

    private String userName;

    public Receiver(String userName) {

        try {

            this.userName = userName;
            DatagramSocket receiverSocket = new DatagramSocket((int) (Math.random() * 64000 + 1000));
            byte[] sendData = new byte[1024];  // Bytes array to hold string contain the required file name
            byte[] directoryFiles = new byte[2000];     // Bytes array to hold the main directory files names which the sender will send
            byte[] receiveData = new byte[6000];        // Bytes array to hold the bytes of the file which will come from main directory
            String machineUserName = System.getProperty("user.name");  // Initialize the current username of this PC
            String path = "C:\\Users\\" + machineUserName + "\\Desktop\\";      // Choose the Desktop to be the main path to receive files into it

            Scanner peerInput = new Scanner(System.in);


            while (true) {


                // Send the peer request for the sender

                String establishCode = establishConnection(receiverSocket); // The 'establishCode' Will hold a peer required option

                if (establishCode.equals("1")) {


                    // Send the user name for Sender server
                    byte [] userNameBytes  = userName.getBytes();
                    InetAddress senderIP = InetAddress.getByName("localhost");

                    DatagramPacket sendUserName = new DatagramPacket(userNameBytes, userNameBytes.length, senderIP, 5656);
                    receiverSocket.send(sendUserName);

                    // If the response code '1' it means the peer want to receive file from main directory

                    DatagramPacket receivePacket = new DatagramPacket(directoryFiles, directoryFiles.length);

                    synchronized (receivePacket){
                        receiverSocket.receive(receivePacket);          // Make the socket listen for Sender peer response
                        System.out.println("Files in directory:\n" + new String(directoryFiles));    // Print the files which stored in 'directoryFiles' array
                    }


                    System.out.print(">> ");
                    String stringSendData = peerInput.nextLine();        // This input will hold the required file name

                    // Sending message to peer that the receiver peer ended the connection
                    if (stringSendData.equalsIgnoreCase("exit") || stringSendData.equalsIgnoreCase("close")) {

                        sendData = stringSendData.getBytes();       // Send MSG to Sender contain to close the connection and stop listening
                        InetAddress serverIP = InetAddress.getByName("localhost");
                        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverIP, 5656);

                        receiverSocket.send(sendPacket);
                        break;
                    }

                    sendData = stringSendData.getBytes();       // Convert the user string input to bytes
                    InetAddress serverIP = InetAddress.getByName("localhost");  // Connect the packet to the local host
                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverIP, 5656);

                    synchronized (sendPacket){
                        receiverSocket.send(sendPacket);  // Send the file name across the socket to the sender

                    }

                    /*
                            - Receive the sender peer response
                            - Convert the bytes which came from Sender to File
                            - using writeFile function to write the file on the desktop
                     */

                    receivePacket = new DatagramPacket(receiveData, receiveData.length);

                    synchronized (receivePacket){
                        receiverSocket.receive(receivePacket);
                        receiveData = receivePacket.getData();
                    }



                    writeFile(receiveData, stringSendData);

                    System.out.println();


                } else if (establishCode.equals("2")) {


                    // If the response code was '2' that mean the receive peer want to send file to the main directory

                    byte[] chosenFileBytes = new byte[0]; // Bytes array to hold the file bytes ' 0 is only for initialization '

                    Scanner input = new Scanner(System.in);
                    System.out.println("Choose file from desktop directory to send: ");
                    listDir();   // listDir function list all files in the desktop directory for the sender
                    System.out.print(">> ");
                    String chosenFile = input.nextLine();   // Take the name of the file

                    File file = new File(path + chosenFile);   // Initialize the file to be ready to convert it to bytes

                    if (file.exists()) {        // Check first if the user entered a correct file name

                        byte[] fileNameBytes = new byte[1024];      // Bytes array to hold file name and extension to use it later in the Sender side
                        chosenFileBytes = new byte[(int) file.length()]; // Initialize the chosenFileBytes with the file size
                        chosenFileBytes = Files.readAllBytes(file.toPath());        // Convert the file to bytes and store it in chosenFileBytes

                        fileNameBytes = chosenFile.getBytes();
                        InetAddress clientIP = InetAddress.getLocalHost();
                        DatagramPacket sendNamePacket = new DatagramPacket(fileNameBytes, fileNameBytes.length, clientIP, 5656);
                        receiverSocket.send(sendNamePacket); // Send the file name and extension to the Sender

                        DatagramPacket sendFilePacket = new DatagramPacket(chosenFileBytes, chosenFileBytes.length, clientIP, 5656);
                        receiverSocket.send(sendFilePacket); // Send the file bytes to the Sender


                        System.out.println("File : " + chosenFile + ", Sent to the peer | 200 Ok");

                    } else {

                        /*
                            If the peer entered a wrong file name we will do the following:

                            - Initialize chosenFileBytes [0] = -127
                            - Send it to the Sender peer
                            - In the Sender side will check if the bytes array hold this value in the first index or not

                            if(chosenFileBytes[0] = -127){ He will know that this is wrong file name}
                            else { Will convert it to File and the write it to main directory }

                         */
                        chosenFileBytes[0] = -127;
                        InetAddress clientIP = InetAddress.getLocalHost();
                        DatagramPacket sendPacket = new DatagramPacket(chosenFileBytes, chosenFileBytes.length, clientIP, 5656);
                        receiverSocket.send(sendPacket); // Send the data in the socket
                        System.out.println("File : " + chosenFile + ", does not exists | 400 Error");
                    }

                } else if (establishCode.equals("3")) {

                    // Here we will miss this peer
                    System.out.println("The session will end....");
                    break;
                }

            }

            receiverSocket.close();


        } catch (SocketException | UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private void writeFile(byte[] fileBytes, String fileName) {


        if (fileBytes[0] == -127) {

            System.out.println("There is no file contain that name -> " + fileName + " | 400 Error");

        } else {

            try {
                String machineUserName = System.getProperty("user.name");
                Files.write(new File("C:\\Users\\" + machineUserName + "\\Desktop\\" + fileName).toPath(), fileBytes);
                System.out.println("File received successfully | 200 Ok");
            } catch (IOException e) {
                System.out.println(e.toString() + " | 400 Error");
            }

        }
    }

    private String establishConnection(DatagramSocket senderSocket) throws IOException {

        Scanner input = new Scanner(System.in);

        byte[] peerRole = new byte[1024];  // Byte array to hold peer choice ( receive , send)
        System.out.println("1- Receive a file");
        System.out.println("2- Send a file to main directory");
        System.out.println("3- Exit");
        String peerSendData = input.nextLine();

        peerRole = peerSendData.getBytes();
        InetAddress serverIP = InetAddress.getByName("localhost");
        DatagramPacket sendPacket = new DatagramPacket(peerRole, peerRole.length, serverIP, 5656);

        senderSocket.send(sendPacket);  // Send the request across the socket to the server

        return peerSendData;


    }

    private void listDir() {

        String machineUserName = System.getProperty("user.name");
        String path = "C:\\Users\\" + machineUserName + "\\Desktop\\";

        String[] filesNames;
        File dir = new File(path);

        filesNames = dir.list();

        for (String names : filesNames) {

            System.out.println(names);
        }

    }


}
