package chat.bio;

import lombok.extern.slf4j.Slf4j;
import util.Constant;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;

/**
 * 多人聊天室客户端
 *      1. 主线程不断监听服务端转发来的消息
 *      2. 子线程接收用户输入
 */
@Slf4j
public class ChatClient {

    private Socket clientSocket = null;

    private BufferedReader reader = null;

    private BufferedWriter writer = null;

    private Boolean ClientRunningFlag = true;

    //向服务端发送消息
    public void sendMsgToServer(String msg) throws IOException {
        if (!clientSocket.isOutputShutdown()){
            writer.write(msg + "\n");
            writer.flush();
        }
    }

    //接收服务端的消息
    public String revicedMsgFromServer() throws IOException {
        String msg = null;
        if (!clientSocket.isInputShutdown()){
            msg = reader.readLine();
        }
        return msg;
    }

    //客户端连接关闭
    private void close() {

        if (clientSocket != null){
            try {
                clientSocket.close();
                log.info("Client closed ...");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //更新ClientRunningFlag
    public void updateFlag(){
        ClientRunningFlag = false;
    }

    //启动程序
    public void start(){
        try {
            //创建客户端连接
            clientSocket = new Socket();
            clientSocket.connect(new InetSocketAddress(Constant.ServerIP,Constant.ServerPort));
            log.info("Server [{}:{}] init success...",clientSocket.getLocalAddress(),clientSocket.getLocalPort());

            //创建IO
            writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
            reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            //处理用户输入
            new Thread(new UserInputHandler(this)).start();

            //读取服务端转发信息
            while (ClientRunningFlag){
                String msg = revicedMsgFromServer();
                log.info(msg);
            }

        } catch (IOException e) {
            log.error("Client Running error, Reason:{}",e.getMessage());
            e.printStackTrace();
        } finally {
          close();
        }
    }

    public static void main(String[] args) {
        ChatClient chatClient = new ChatClient();
        chatClient.start();
    }



}
