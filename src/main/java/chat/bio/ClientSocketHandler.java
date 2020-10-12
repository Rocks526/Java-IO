package chat.bio;

import lombok.extern.slf4j.Slf4j;
import util.Constant;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

/**
 * 客户端连接处理器
 */
@Slf4j
public class ClientSocketHandler implements Runnable {

    private Socket clientSocket;

    private ChatServer chatServer;

    private Boolean clientRunningFlag = true;

    public ClientSocketHandler(Socket client, ChatServer chatServer) {
        this.clientSocket = client;
        this.chatServer = chatServer;
    }

    public void run() {
        try {
            //存储新上线的用户
            chatServer.addClientSocket(clientSocket);
            while (clientRunningFlag){
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                String msg = bufferedReader.readLine();
                String msgToSend = "Client[" + clientSocket.getPort() + "] sendMsg:" + msg;
                log.info(msgToSend);
                chatServer.forwardMsg(clientSocket,msgToSend);
                //检查用户是否退出
                if (Constant.ClientStopCommand.equals(msg)){
                    clientRunningFlag = false;
                }
            }
        } catch (IOException e) {
            log.error("Client[{}] running error, Reason:{}",clientSocket.getLocalPort(),e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                chatServer.removeClientSocket(clientSocket);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


}
