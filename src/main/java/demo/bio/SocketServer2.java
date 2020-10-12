package demo.bio;

import lombok.extern.slf4j.Slf4j;
import util.Constant;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Server 多线程 主线程接收客户端请求 子线程处理客户端逻辑
 *          有多少客户端连接即创建多少个线程 服务端资源消耗很大
 */
@Slf4j
public class SocketServer2 {

    private static Boolean serverRunningFlag = true;

    public static void main(String[] args) {
        SocketServer2 socketServer2 = new SocketServer2();
        socketServer2.start();
    }


    public void start(){
        ServerSocket serverSocket = null;
        Socket clientSocket = null;
        try {
//            serverSocket = new ServerSocket(8888);
            serverSocket = new ServerSocket();
            serverSocket.bind(new InetSocketAddress(Constant.ServerIP,Constant.ServerPort));
            log.info("Server [{}:{}] init success...",Constant.ServerIP,Constant.ServerPort);
            //进入循环 一直接收客户端请求 直到客户端发送ServerStopCommand
            while (serverRunningFlag){
                clientSocket = serverSocket.accept();
                //连接到一个客户端 处理与该客户端的交互 直到该客户端发送ClientStopCommand
                log.info("Client [{}:{}] connected Server...",clientSocket.getInetAddress(),clientSocket.getPort());
                new Thread(new ClientHandler(clientSocket)).start();
            }
            log.info("Server [{}:{}] is closed...",Constant.ServerIP,Constant.ServerPort);
        } catch (IOException e) {
            log.error("Server [{}] running error,Reason:{}",Constant.ServerIP+":"+Constant.ServerPort,e.getMessage());
            e.printStackTrace();
        }finally {
            if (clientSocket != null){
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (serverSocket != null){
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }




    //客户端请求处理器
    class ClientHandler implements Runnable{

        private Socket clientSocket = null;

        private Boolean clientRunningFlag = true;

        public ClientHandler(Socket client){
            this.clientSocket = client;
        }

        public void run() {
            while (clientRunningFlag){
                try{
                    BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
                    String msg = reader.readLine();
                    log.info("Client [{}] sendMsg:{}",clientSocket.getInetAddress() + ":" +clientSocket.getPort(),msg);
                    writer.write(msg + "[Server]" + "\n");
                    writer.flush();
                    if (Constant.ServerStopCommand.equals(msg)){
                        serverRunningFlag = false;
                        break;
                    }
                    if (Constant.ClientStopCommand.equals(msg)){
                        clientRunningFlag = false;
                        break;
                    }
                }catch (Exception e){
                    clientRunningFlag = false;
                    break;
                }
            }
            log.info("Client [{}:{}] disconnected Server...",clientSocket.getInetAddress(),clientSocket.getPort());
        }
    }

}
