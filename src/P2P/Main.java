package P2P;


import P2P_members.Receiver;
import P2P_members.Sender;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;


class activeUser {

    String username;
    String password;

    activeUser() {
        username = "";
        password = "";
    }


}

public class Main {

    static private void startP2P(activeUser activeUser) throws IOException {

        Scanner input = new Scanner(System.in);
        System.out.println("\n\t\t\t\t----- Welcome '" + activeUser.username + "' to P2P system -----\t\t\t\t\n");

        while (true) {

            System.out.println("1- Sender");
            System.out.println("2- Peer (Send/Receive)");
            System.out.println("3- Back");
            System.out.print(">> ");
            String userRole = input.nextLine();

            if (userRole.equals("1")) {

                Sender p2pSender = new Sender();
            } else if (userRole.equals("2")) {

                Receiver p2pReceiver = new Receiver(activeUser.username);

            } else if (userRole.equals("3")) {
                break;

            } else {
                System.out.println("!!! Please enter correct input (1:3) !!!\n");
            }


        }

    }


    public static void main(String[] args) throws IOException {

        activeUser activeUser = new activeUser();
        Scanner input = new Scanner(System.in);
        Socket peerSocket = null;

        try {

            peerSocket = new Socket(InetAddress.getLocalHost(), 1030);
            DataInputStream receiveMsgFromServer = new DataInputStream(peerSocket.getInputStream()); // Buffer that will receive a MSG from server
            DataOutputStream sendMsgToServer = new DataOutputStream(peerSocket.getOutputStream());    // Buffer that will send a MSG to server

            while (true) {


                System.out.println("1- Login");
                System.out.println("2- Sign up");  // Display the operations for peer
                System.out.println("3- Exit");
                System.out.print(">> ");
                String operation = input.nextLine();

                if (operation.equals("1")) {

                    System.out.print("Enter your user name : ");
                    String userName = input.nextLine();
                    System.out.print("Enter your password : ");
                    String password = input.nextLine();

                    sendMsgToServer.writeUTF("login");
                    sendMsgToServer.writeUTF(userName);
                    sendMsgToServer.writeUTF(password);

                    String serverResponse = receiveMsgFromServer.readUTF();

                    if (serverResponse.equals("200 OK")) {
                        System.out.println("Login operation done successfully | 200 OK");
                        activeUser.username = userName;
                        startP2P(activeUser);

                    } else {
                        System.out.println("User does not exists or he is already signed | 400 Error");
                    }

                } else if (operation.equals("2")) {


                    System.out.print("Enter your user name : ");
                    String userName = input.nextLine();
                    System.out.print("Enter your password : ");
                    String password = input.nextLine();

                    sendMsgToServer.writeUTF("sign");
                    sendMsgToServer.writeUTF(userName);
                    sendMsgToServer.writeUTF(password);

                    String serverResponse = receiveMsgFromServer.readUTF();

                    if (serverResponse.equals("200 OK")) {
                        System.out.println("Sign up operation done successfully | 200 OK");
                        activeUser.username = userName;
                        startP2P(activeUser);
                    } else {
                        System.out.println("Sign up operation failed because user is already exists or maximum number is achieved | 400 Error");
                    }


                } else if (operation.equals("3")) {

                    sendMsgToServer.writeUTF("exit");
                    sendMsgToServer.writeUTF(activeUser.username);
                    System.out.println("The connection will end after sending a msg to Tracker");
                    sendMsgToServer.close();
                    receiveMsgFromServer.close();
                    peerSocket.close();

                    break;

                }


            }

        } catch (UnknownHostException e) {
            System.out.println(e.toString() + " | 400 Error");
        } catch (IOException e) {
            System.out.println(e.toString() + " Failed to connect to tracker server | 400 Error");
        }
    }
}

