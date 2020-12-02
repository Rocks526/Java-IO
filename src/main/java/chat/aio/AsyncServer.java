package chat.aio;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import util.Constant;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@Slf4j
public class AsyncServer {

    public static void main(String[] args) {
        AsyncServer asyncServer = new AsyncServer();
        asyncServer.start();
    }

    // Server
    private AsynchronousServerSocketChannel serverSocketChannel;

    private List<ClientHandler> clients;

    public AsyncServer(){
        clients = new ArrayList<>();
    }

    private void start(){
        try {
            // 使用默认的ChannelGroup
            serverSocketChannel = AsynchronousServerSocketChannel.open();
            // 绑定IP 端口
            serverSocketChannel.bind(new InetSocketAddress(Constant.ServerIP, Constant.ServerPort));

            log.info("服务端启动成功...端口:[{}]", Constant.ServerPort);

            while (true){
                // 开始客户端监听  两个参数分别是回调函数和传递给回调函数的数据attachment
                serverSocketChannel.accept(null , new AcceptHandler());
                // 等待系统输入  ==>  阻塞线程
                int read = System.in.read();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    // 客户端连接回调函数  泛型两个参数分别为 accept函数返回值类型和attachment类型
    private class AcceptHandler implements CompletionHandler<AsynchronousSocketChannel,Object> {

        @SneakyThrows
        @Override
        public void completed(AsynchronousSocketChannel clientChannel, Object attachment) {

            log.info("客户端[{}]连接成功!", clientChannel.getRemoteAddress());

            // 继续等待客户端连接
            if (serverSocketChannel.isOpen()){
                serverSocketChannel.accept(null, this);
            }
            // 读取客户端数据
            if (clientChannel != null && clientChannel.isOpen()){
                ByteBuffer byteBuffer = ByteBuffer.allocate(10240);
                HashMap<String, Object> info = new HashMap<>();
                info.put("type", "read");
                info.put("byteBuffer", byteBuffer);
                ClientHandler clientHandler = new ClientHandler(clientChannel);
                // 添加客户端到clients
                clients.add(clientHandler);
                clientChannel.read(byteBuffer, info, clientHandler);
            }
        }

        @Override
        public void failed(Throwable exc, Object attachment) {
            log.error("接收客户端连接失败,Reason:{}", exc.getMessage());
        }

    }

    // 客户端读写操作回调函数
    private class ClientHandler implements CompletionHandler<Integer, HashMap<String, Object>> {

        private AsynchronousSocketChannel clientChannel;

        public ClientHandler(AsynchronousSocketChannel clientChannel){
            this.clientChannel = clientChannel;
        }

        public void sendMsg(String msg, HashMap<String, Object> attachment) throws ExecutionException, InterruptedException {
//            clientChannel.write(Charset.forName("UTF-8").encode(msg), attachment, this);
            // 异步回调式会导致java.nio.channels.ReadPendingException
            Future<Integer> future = clientChannel.write(Charset.forName("UTF-8").encode(msg));
            future.get();
        }

        @SneakyThrows
        @Override
        public void completed(Integer result, HashMap<String, Object> attachment) {
            ByteBuffer buffer = (ByteBuffer) attachment.get("byteBuffer");
            String type = (String) attachment.get("type");
            // 读取或写入的长度
            if (result != null){
                if (result < 0){
                    // 客户端异常
                    log.error("客户端异常:客户端" + type + "长度小于0!");
                }else {
                    if ("read".equals(type)){
                        String msg = new String(buffer.array(), 0, result);
                        log.info("客户端[{}]发送数据:{}",clientChannel.getRemoteAddress(), msg);
                        buffer.flip();
                        attachment.put("type","write");
                        attachment.put("is_forward",true);
                        // 数据转发给其他客户端
                        for (ClientHandler clientHandler : clients){
                            if (clientHandler == this){
                                continue;
                            }
                            clientHandler.sendMsg("客户端[" + clientChannel.getRemoteAddress() + "]发送消息:[" + msg + "]!", attachment);
                        }
                        buffer.clear();
                    }else if ("write".equals(type)){
                        ByteBuffer byteBuffer = ByteBuffer.allocate(10240);
                        attachment.put("type","read");
                        attachment.put("byteBuffer",byteBuffer);
                        clientChannel.read(byteBuffer, attachment, this);
                    }else {
                        log.info("未知客户端操作....");
                    }
                }
            }
        }

        @Override
        public void failed(Throwable exc, HashMap<String, Object> attachment) {
            String type = (String) attachment.get("type");
            clients.remove(this);
            if ("read".equals(type)){
                log.error("读取客户端发送数据失败,Reason:{}", exc.getMessage());
            }else if ("write".equals(type)){
                log.error("往客户端写入数据失败,Reason:{}", exc.getMessage());
            }else {
                log.error("未知客户端操作失败失败,Reason:{}", exc.getMessage());
            }
        }
    }
}
