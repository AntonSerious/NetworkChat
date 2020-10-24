package ru.geekbrains.java_two.chat.server.core;

import ru.geekbrains.java_two.chat.common.Library;
import ru.geekbrains.java_two.network.ServerSocketThread;
import ru.geekbrains.java_two.network.ServerSocketThreadListener;
import ru.geekbrains.java_two.network.SocketThread;
import ru.geekbrains.java_two.network.SocketThreadListener;

import java.net.ServerSocket;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Vector;
//намудрил с репозиторием.
public class ChatServer implements ServerSocketThreadListener, SocketThreadListener {
    private final DateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss: ");
    private final ChatServerListener listener;
    private final Vector<SocketThread> clients;
    private ServerSocketThread thread;

    public ChatServer(ChatServerListener listener) {
        this.listener = listener;
        this.clients = new Vector<>();
    }

    public void start(int port) {
        if (thread != null && thread.isAlive()) {
            putLog("Server already started");
        } else {
            thread = new ServerSocketThread(this, "Thread of server", port, 2000);
        }
    }

    public void stop() {
        if (thread == null || !thread.isAlive()) {
            putLog("Server is not running");
        } else {
            thread.interrupt();
        }
    }

    private void putLog(String msg) {
        msg = DATE_FORMAT.format(System.currentTimeMillis()) +
                Thread.currentThread().getName() + ": " + msg;
        listener.onChatServerMessage(msg);
    }

    /**
     * Server methods
     *
     * */

    @Override
    public void onServerStart(ServerSocketThread thread) {
        putLog("Server thread started");
        SqlClient.connect();
    }

    @Override
    public void onServerStop(ServerSocketThread thread) {
        putLog("Server thread stopped");
        SqlClient.disconnect();
        for (int i = 0; i < clients.size(); i++) {
            clients.get(i).close();
            
        }
    }

    @Override
    public void onServerSocketCreated(ServerSocketThread thread, ServerSocket server) {
        putLog("Server socket created");

    }

    @Override
    public void onServerTimeout(ServerSocketThread thread, ServerSocket server) {
//        putLog("Server timeout");

    }

    @Override
    public void onSocketAccepted(ServerSocketThread thread, ServerSocket server, Socket socket) {
        putLog("Client connected");
        String name = "SocketThread " + socket.getInetAddress() + ":" + socket.getPort();
        new ClientThread(this, name, socket);

    }

    @Override
    public void onServerException(ServerSocketThread thread, Throwable exception) {
        exception.printStackTrace();
    }

    /**
     * Socket methods
     *
     * */

    @Override
    public synchronized void onSocketStart(SocketThread thread, Socket socket) {
        putLog("Socket created");

    }

    @Override
    public synchronized void onSocketStop(SocketThread thread) {
        putLog("Socket stopped");
        clients.remove(thread);
        ClientThread client = (ClientThread) thread;
        if(client.isAuthorized() && !client.isReconnecting() ){
            sendToAllAuthorizedClients(Library.getTypeBroadcast("Server", client.getNickname() + " disconnected"));
        }
        sendToAllAuthorizedClients(Library.getUserList(getUsers()));

    }

    @Override
    public synchronized void onSocketReady(SocketThread thread, Socket socket) {
        putLog("Socket ready");
        clients.add(thread);
        sendToAllAuthorizedClients(Library.getUserList(getUsers()));
    }

    @Override
    public synchronized void onReceiveString(SocketThread thread, Socket socket, String msg) {
        ClientThread client = (ClientThread) thread;
        if (client.isAuthorized())
            handleAuthMessage(client, msg);
        else
            handleNonAuthMessage(client, msg);
    }

    private void handleNonAuthMessage(ClientThread client, String msg) {
        String[] arr = msg.split(Library.DELIMITER);
        if (arr.length != 3 || !arr[0].equals(Library.AUTH_REQUEST)) {
            client.msgFormatError(msg);
            return;
        }
        String login = arr[1];
        String password = arr[2];
        String nickname = SqlClient.getNickname(login, password);
        if (nickname == null) {
            putLog("Invalid login attempt: " + login);
            client.authFail();
            return;
        } else{
            ClientThread oldClient = findClientByNickname(nickname);
            client.authAccept(nickname);
            if(oldClient == null){
                sendToAllAuthorizedClients(Library.getTypeBroadcast("Server", nickname));
            }else{
                oldClient.reconnect();
                clients.remove(oldClient);
            }
        }
        client.authAccept(nickname);
        sendToAllAuthorizedClients(Library.getUserList(getUsers()));
        sendToAllAuthorizedClients(Library.getTypeBroadcast("Server", nickname + " connected"));
    }

    private void handleAuthMessage(ClientThread client, String msg) {
        String[] arr = msg.split(Library.DELIMITER);
        String msgType = arr[0];
        System.out.println(msg);
        switch (msgType){
            case Library.TYPE_BCAST_CLIENT:
                sendToAllAuthorizedClients(Library.getTypeBroadcast(client.getNickname(), arr[1]));
                break;
            case Library.TYPE_PRIVATE_CLIENT:
                String[] arr1 = arr[1].split(" ");
                String toNickname = arr1[1];
                String message = arr1[2];
                System.out.println("Пытаемся отправить приват сообщение");
                sendPrivateMsgToClient(toNickname, client.getNickname(), message);
                break;
            default:
                client.msgFormatError(msg);
        }
    }

    private void sendToAllAuthorizedClients(String msg) {
        for (int i = 0; i < clients.size(); i++) {
            ClientThread recipient = (ClientThread) clients.get(i);
            if (!recipient.isAuthorized()) continue;
            recipient.sendMessage(msg);
        }
    }
    private void sendPrivateMsgToClient(String toNickname, String fromNickname, String msg) {
        ClientThread privateClient = findClientByNickname(toNickname);
        if(privateClient != null) {
            privateClient.sendMessage(Library.getTypePrivate(fromNickname, toNickname, msg));  //отправялем сообщение в приват
        }
        ClientThread sender = findClientByNickname(fromNickname);
        if(sender != null) {
            sender.sendMessage(Library.getTypeEchoPrivate(fromNickname, toNickname, msg));              //отображаем это сообщение у отправителя в GUI клиенте
        }
    }
    private void sendEchoPrivateMsgToClient(String nickname, String fromNickname, String msg) {
        ClientThread privateClient = findClientByNickname(nickname);
        if(privateClient != null) {
            privateClient.sendMessage(msg);
        }
        ClientThread sender = findClientByNickname(fromNickname);
        if(sender != null) {
            sender.sendMessage(msg);
        }
    }




    private String getUsers(){
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < clients.size(); i++) {
            ClientThread client = (ClientThread) clients.get(i);
            if(!client.isAuthorized()) continue;
            sb.append(client.getNickname()).append(Library.DELIMITER);
        }
        return sb.toString();
    }
    private synchronized ClientThread findClientByNickname(String nickname) {
        for (int i = 0; i < clients.size(); i++) {
            ClientThread client = (ClientThread) clients.get(i);
            if (!client.isAuthorized()) continue;
            if (client.getNickname().equals(nickname))
                return client;
        }
        return null;
    }
    @Override
    public synchronized void onSocketException(SocketThread thread, Exception exception) {
        exception.printStackTrace();
    }

}
