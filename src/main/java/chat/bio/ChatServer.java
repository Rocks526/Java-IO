package chat.bio;

import lombok.extern.slf4j.Slf4j;
import util.Constant;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 多人聊天室服务端
 *      1. 主线程接收客户端请求 保存客户端连接
 *      2. 子线程处理客户端逻辑 将消息给其他客户端转发
 */
@Slf4j
public class ChatServer {


    //客户端连接
    private Map<Integer, Writer> clients = new ConcurrentHashMap<Integer, Writer>();

    //线程池
    private ExecutorService executorService = Executors.newCachedThreadPool();

    //服务端Socket
    private ServerSocket serverSocket = null;

    //启动程序
    private void start() {

        //创建服务端Socket
        try {
            serverSocket = new ServerSocket(Constant.ServerPort);
            log.info("Server [{}:{}] init success...",serverSocket.getInetAddress(),serverSocket.getLocalPort());
            while (true){
                //监听客户端连接
                Socket client = serverSocket.accept();
//                log.info("Client [{}:{}] connected Server...",client.getInetAddress(),client.getPort());
                executorService.execute(new ClientSocketHandler(client,this));
            }
        } catch (IOException e) {
            log.error("Server[{}] running error , Reason:{}",Constant.ServerPort,e.getMessage());
            e.printStackTrace();
        }finally {
            close();
        }
    }

    //关闭连接
    private void close() {
        if (serverSocket != null){
            try {
                serverSocket.close();
                log.info("Server[{}] closed ... ", Constant.ServerPort);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //添加客户端连接
    public void addClientSocket(Socket client) throws IOException {
        if (client != null){
            Integer port = client.getPort();
            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
            clients.put(port, bufferedWriter);
            log.info("Client[{}] connected Server...",port);
        }
    }

    //移除客户端连接
    public void removeClientSocket(Socket client) throws IOException {
        if (client != null && clients.containsKey(client.getPort())){
            clients.get(client.getPort()).close();
            clients.remove(client.getPort());
            log.info("Client[{}] disConnected Server...",client.getPort());
        }
    }

    //转发客户端请求
    public void forwardMsg(Socket client,String msg) throws IOException {
        if (client != null){
            for (Integer port : clients.keySet()){
                if (!port.equals(client.getPort())){
                    Writer writer = clients.get(port);
                    writer.write(msg + "\n");
                    writer.flush();
                }
            }
        }
    }

    public static void main(String[] args) {
        ChatServer chatServer = new ChatServer();
        chatServer.start();
    }

}
