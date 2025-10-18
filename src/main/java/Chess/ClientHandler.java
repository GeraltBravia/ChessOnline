/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Chess;

import java.io.IOException;
import java.io.*;
import java.net.*;
import java.util.Map;

/**
 * ClientHandler handles a single connected client socket.
 */
public class ClientHandler implements Runnable {

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String playerId;
    private Player player;
    
    public ClientHandler(Socket socket) {
        this.socket = socket;
        this.playerId = "Player_" + socket.getPort();
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
            closeConnection();
        }
    }

    public Player getPlayer() {
        return player;
    }

    public String getPlayerId() {
        return playerId;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    @Override
    public void run() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                System.out.println(playerId + " sent: " + message);
                processMessage(message);
            }
        } catch (IOException e) {
            System.out.println(playerId + " connection error: " + e.getMessage());
        } finally {
            closeConnection();
        }
    }

    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    private void processMessage(String message) {
        if (message.startsWith("MOVE ")) {
            String moveStr = message.substring(5);
            synchronized (ChessServer.games) {
                for (GameSession game : ChessServer.games) {
                    if (game.containsPlayer(this)) {
                        game.processMove(moveStr, this);
                        break;
                    }
                }
            }
        } else if (message.startsWith("LEGAL ")) {
            String from = message.substring(6).trim();
            synchronized (ChessServer.games) {
                for (GameSession game : ChessServer.games) {
                    if (game.containsPlayer(this)) {
                        game.sendLegalMoves(from, this);
                        break;
                    }
                }
            }
        } else if (message.startsWith("CHAT ")) {
            String chatMsg = message.substring(5);
            synchronized (ChessServer.games) {
                for (GameSession game : ChessServer.games) {
                    if (game.containsPlayer(this)) {
                        game.broadcastChat(this, chatMsg);
                        break;
                    }
                }
            }
        } else if (message.equals("START") || message.equals("NEW_GAME")) {
            synchronized (ChessServer.games) {
                for (GameSession game : ChessServer.games) {
                    if (game.containsPlayer(this)) {
                        game.restartGame(this);
                        break;
                    }
                }
            }
        } else if (message.equals("SURRENDER")) {
            synchronized (ChessServer.games) {
                for (GameSession game : ChessServer.games) {
                    if (game.containsPlayer(this)) {
                        game.handleSurrender(this);
                        break;
                    }
                }
            }
        } else if (message.equals("UNDO")) {
            synchronized (ChessServer.games) {
                for (GameSession game : ChessServer.games) {
                    if (game.containsPlayer(this)) {
                        game.handleUndo(this);
                        break;
                    }
                }
            }
        } else {
            sendMessage("INVALID_COMMAND");
        }
    }

    private void closeConnection() {
        try {
            synchronized (ChessServer.clients) {
                ChessServer.clients.remove(this);
            }
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            System.out.println(playerId + " disconnected");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
