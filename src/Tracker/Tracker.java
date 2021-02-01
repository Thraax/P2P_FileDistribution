package Tracker;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;


public class Tracker extends Thread {

    Socket trackerRequestSocket;

  static private List<User> usersList = new ArrayList<>();

    public Tracker(Socket acceptationSocket) throws IOException {

        trackerRequestSocket = acceptationSocket;

    }

    synchronized public boolean signUp(String userName, String password) {

        if (checkExistsUser(userName) && usersList.size() <= 4) {

            return false;
        } else {

            User newUser = new User();
            newUser.setUserName(userName);
            newUser.setPassword(password);
            newUser.setActive();
            usersList.add(newUser);

            return true;

        }


    }

    synchronized public boolean login(String userName, String password) {

        for (int i = 0; i < usersList.size(); i++) {

            if (usersList.get(i).getUserName().equals(userName) && usersList.get(i).getPassword().equals(password)
                &&!usersList.get(i).getActive()) {
                return true;
            }

        }

        return false;
    }

    synchronized public boolean checkExistsUser(String userName) {

        for (int i = 0; i < usersList.size(); i++) {

            if (usersList.get(i).getUserName().equals(userName)) {

                return true;
            }
        }

        return false;

    }

    synchronized public void clearUser(String activeUser) {

        for (int i = 0; i < usersList.size(); i++) {

            if (usersList.get(i).getUserName().equals(activeUser)) {

                System.out.println("The user "+usersList.get(i).getUserName()+" has been deleted");
                usersList.remove(i);
                return;
            }

        }

        System.out.println("The user that end the connection didn't sign");

    }

    @Override
    public void run() {

        System.out.println("Peer connected successfully | 200 OK");

        while (true) {

            try {

                DataOutputStream sendDataToPeer = new DataOutputStream(trackerRequestSocket.getOutputStream());
                DataInputStream receiveDataFromPeer = new DataInputStream(trackerRequestSocket.getInputStream());
                String peerRequest = receiveDataFromPeer.readUTF();


                if (peerRequest.equals("login")) {

                    System.out.println("Peer will make login..");
                    String userName = receiveDataFromPeer.readUTF();
                    String password = receiveDataFromPeer.readUTF();

                    synchronized (usersList) {
                        if (login(userName, password)) {
                            sendDataToPeer.writeUTF("200 OK");
                            System.out.println("User '" + userName + "' logged in successfully");

                        } else {
                            sendDataToPeer.writeUTF("400 Error");
                            System.out.println("User '" + userName + "' does not exists");
                        }


                    }


                } else if (peerRequest.equals("sign")) {

                    String userName = receiveDataFromPeer.readUTF();
                    String password = receiveDataFromPeer.readUTF();

                    synchronized (usersList) {
                        if (signUp(userName, password)) {
                            sendDataToPeer.writeUTF("200 OK");
                            System.out.println("User '" + userName + "' signed successfully");
                        } else {
                            sendDataToPeer.writeUTF("400 Error");
                            System.out.println("User '" + userName + "' is already exists or maximum number is achieved");
                        }


                    }


                } else if (peerRequest.equals("exit")) {

                    String userName = receiveDataFromPeer.readUTF();

                    synchronized (usersList) {
                        clearUser(userName);
                    }

                }


            } catch (EOFException eof){
                System.out.println(eof.toString() + " peer closed connection");
                break;
            } catch (IOException e) {
                System.out.println(e.toString()+" Failed to read data | 400 Error");
                break;
            }
        }
    }


    public static void main(String[] args) throws IOException {
        System.out.println("Waiting for peers requests...");
        ServerSocket serverSocket = new ServerSocket(1030);
        while (true) {

            Socket acceptationSocket = serverSocket.accept();
            Thread runTracker = new Tracker(acceptationSocket);
            runTracker.start();


        }


    }


}


class User {


    private String userName;
    private String password;
    private boolean isActive;

    public User() {

        userName = "";
        password = "";
        isActive = false;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setActive(){
        isActive = true;
    }

    public boolean getActive(){
        return isActive;
    }

}
