package P2P_members;


import java.io.*;
import java.nio.file.Files;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Hashtable;


public class Sender extends Thread {

    static private Hashtable<String, Integer> filesHash;


    private DatagramSocket SenderSocket;
    String machineUserName;
    String mainPath;

    public Sender() throws IOException {

        SenderSocket = new DatagramSocket(5656);
        System.out.println("Waiting for peer receiver message...");
        filesHash = new Hashtable<>();
        byte[] sendData = new byte[1024]; // used for transfer file bytes
        byte[] receiveData = new byte[1024]; // used for receiving required file name

         machineUserName = System.getProperty("user.name");
         mainPath = "C:\\Users\\" + machineUserName + "\\Downloads\\";
        FileOutputStream trackerFile = new FileOutputStream(mainPath + "Tracker.txt");


        while (true) {

            try {

                // Receiving the establish connection from the receiving peer

                DatagramPacket receiveEstablish = new DatagramPacket(receiveData, receiveData.length);
                SenderSocket.receive(receiveEstablish);

                String establishCode = receiveEstablish(receiveEstablish); // Store the establish response in string


                byte[] receiverUserName = new byte[1024];
                DatagramPacket receiveUserName = new DatagramPacket(receiverUserName, receiverUserName.length);
                SenderSocket.receive(receiveUserName);

                String receiverName = new String(receiveUserName.getData(), receiveUserName.getOffset(), receiveUserName.getLength());


                System.out.println("Peer -> " + receiverName + " Connected | 200 Ok");


                // Option number '1' mean that the peer want to receive file from main directory
                if (establishCode.equals("1")) {


                    System.out.println(receiverName + " will receive a file from main directory");


                    listDir(mainPath, receiveEstablish.getAddress(), receiveEstablish.getPort(), SenderSocket);

                    // Peer will send a MSG contain the file name in the main directory
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

                    synchronized (receivePacket) {
                        SenderSocket.receive(receivePacket);
                    }

                    // Adding length parameters to fix bytes problem, Sometimes the string size maybe more than the bytes we decided
                    String requiredFile = new String(receivePacket.getData(), receivePacket.getOffset(), receivePacket.getLength());


                    // Check if the peer ended the session

                    if (requiredFile.equalsIgnoreCase("exit")) {
                        System.out.println("Peer ended the connection");

                    }


                    System.out.println("Required file is : " + requiredFile);   // Print user input to see how it look like as a 'UDP message'


                    File peerFile = new File(mainPath + requiredFile);


                    if (peerFile.exists()) {

                        sendData = new byte[(int) peerFile.length()];
                        sendData = Files.readAllBytes(peerFile.toPath());   // Fill the bytes with string data which contain the data for user
                        InetAddress clientIP = receivePacket.getAddress();
                        int clientPort = receivePacket.getPort();       // Initialize packet with the data length , client IP and his port
                        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clientIP, clientPort);
                        SenderSocket.send(sendPacket); // Send the data in the socket
                        System.out.println("File :" + requiredFile + ", Sent to the peer | 200 Ok");
                        trackerWrite(trackerFile, requiredFile, receiverName);

                    } else {


                        sendData[0] = -127;
                        InetAddress clientIP = receivePacket.getAddress();
                        int clientPort = receivePacket.getPort();       // Initialize packet with the data length , client IP and his port
                        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clientIP, clientPort);
                        SenderSocket.send(sendPacket); // Send the data in the socket
                        System.out.println("File :" + requiredFile + ", does not exists | 400 Error");

                    }

                }

                // The peer will send files to the main directory
                else if (establishCode.equals("2")) {

                    System.out.println("The peer will send file to the main directory...");
                    byte[] fileNameBytes = new byte[1024];      // Array byte for storing the file name and format
                    byte[] receiveFileBytes = new byte[6000];      // Array to store the actual file bytes


                    /* The user here will send two packets sequential

                        1- File name and will store in 'receiveFileName'
                        2- Actual file bytes
                        3- Will convert the bytes to file and write it into main directory

                     */

                    DatagramPacket receiveFileName = new DatagramPacket(fileNameBytes, fileNameBytes.length);
                    SenderSocket.receive(receiveFileName);
                    String fileName = new String(fileNameBytes);

                    DatagramPacket receiveFile = new DatagramPacket(receiveFileBytes, receiveFileBytes.length);
                    SenderSocket.receive(receiveFile);

                    writeFile(receiveFileBytes, fileName);

                }

                // Ending the connection
                else if (establishCode.equals("3")) {
                    System.out.println("The peer ended the session...");
                    break;
                }


            } catch (SocketException e) {
                System.out.println(e.toString() + " ... There is one sender already in the connection | 400 Error");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }


    // listDir is function to display all files in the main directory

    private void listDir(String path, InetAddress clientIP, int clientPort, DatagramSocket SenderSocket) throws IOException {

        String stringNames = "";       // String to hold all files names into it
        String[] filesNames;            // String array to load all names and then iterate on it
        byte[] dirFiles = new byte[2000];     // byte array to send it to peer and convert it there to string again
        File dir = new File(path);

        filesNames = dir.list();

        for (String names : filesNames) {

            stringNames += names + '\n';

        }

        dirFiles = stringNames.getBytes();

        DatagramPacket sendPacket = new DatagramPacket(dirFiles, dirFiles.length, clientIP, clientPort);

        synchronized (sendPacket) {
            SenderSocket.send(sendPacket);  // Send the data in the socket
        }


    }

    private String receiveEstablish(DatagramPacket datagramPacket) {

        String connectionRequest = new String(datagramPacket.getData(), datagramPacket.getOffset(), datagramPacket.getLength());

        return connectionRequest;

    }

    private void writeFile(byte[] fileBytes, String fileName) {

        // If the first index of bytes = -127, that means the array is empty according to wrong value
        if (fileBytes[0] == -127) {

            // Display for sender that the required file has wrong name
            System.out.println("There is no file contain that name -> " + fileName + " | 400 Error");

        } else {

            try {

                /*
                       Here will get the username of the person who using the machine
                       Then create a path pointing on the Downloads directory of the user
                       After that we will create a file with the array bytes "Cast bytes to file"
                       And finally write the new file in the main directory "Downloads"
                 */

                String machineUserName = System.getProperty("user.name");

                Path filePath = Paths.get("C:\\Users\\" + machineUserName + "\\Downloads\\", fileName.trim());


                Files.write(new File(String.valueOf(filePath)).toPath(), fileBytes);
                System.out.println("File received successfully | 200 Ok");
            } catch (IOException e) {
                System.out.println(e.toString() + " | 400 Error");
            }

        }
    }

    synchronized private void trackerWrite(FileOutputStream trackerBufferWriter, String fileName, String receiverName) {

        if (filesHash.containsKey(fileName)) {

            String trackerMSG = filesHash.get(fileName).toString() + " " + receiverName + " \n";

            byte[] fileNameBytes = trackerMSG.getBytes();

            try {
                trackerBufferWriter.write(fileNameBytes);
            } catch (IOException e) {
                System.out.println(e.toString() + " Failed to write to file | 400 Error");
            }


        } else {

            filesHash.put(fileName, fileName.hashCode() % 10 + 20);

            String trackerMSG = filesHash.get(fileName).toString() + " " + receiverName + "\n";

            byte[] fileNameBytes = trackerMSG.getBytes();

            try {
                trackerBufferWriter.write(fileNameBytes);
            } catch (IOException e) {
                System.out.println(e.toString() + " Failed to write to file | 400 Error");
            }


        }

    }


}
