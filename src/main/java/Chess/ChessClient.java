package Chess;

import java.io.*;
import java.net.*;
import java.util.Scanner;
//client chạy trên console (dành cho test)

public class ChessClient {

    public static void main(String[] args) {
        String host = "localhost";
        int port = 12345;

        try (Socket socket = new Socket(host, port); BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream())); PrintWriter out = new PrintWriter(socket.getOutputStream(), true); Scanner sc = new Scanner(System.in)) {

            System.out.println("Connected to Chess Server.");
            Thread readerThread = new Thread(() -> {
                try {
                    String serverMsg;
                    while ((serverMsg = in.readLine()) != null) {
                        System.out.println(serverMsg);
                    }
                } catch (IOException e) {
                    System.out.println("Disconnected from server.");
                }
            });
            readerThread.start();

            while (true) {
                String userInput = sc.nextLine();
                if (userInput.equalsIgnoreCase("exit")) {
                    break;
                }

                if (userInput.toLowerCase().startsWith("chat ")) {
                    out.println("CHAT " + userInput.substring(5));
                } else {
                    out.println("MOVE " + userInput);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
