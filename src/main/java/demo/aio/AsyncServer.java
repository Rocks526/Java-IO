package demo.aio;

import com.sun.media.jfxmediaimpl.HostUtils;
import lombok.extern.slf4j.Slf4j;
import util.Constant;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.HashMap;

@Slf4j
public class AsyncServer {

    public static void main(String[] args) {
        AsyncServer asyncServer = new AsyncServer();
        asyncServer.start();
    }

    // Server
    private AsynchronousServerSocketChannel serverSocketChannel;

    private void start(){
        try {
            // 使用默认的ChannelGroup
            serverSocketChannel = AsynchronousServerSocketChannel.open();
            // 绑定IP 端口
            serverSocketChannel.bind(new InetSocketAddress(Constant.ServerIP, Constant.ServerPort));

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

        @Override
        public void completed(AsynchronousSocketChannel clientChannel, Object attachment) {
            // 继续等待客户端连接
            if (serverSocketChannel.isOpen()){
                serverSocketChannel.accept(null, this);
            }
            // 读取客户端数据
            if (clientChannel != null && clientChannel.isOpen()){
                ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                HashMap<String, Object> info = new HashMap<>();
                info.put("type", "read");
                info.put("byteBuffer", byteBuffer);
                clientChannel.read(byteBuffer, info, new ClientHandler(clientChannel));
            }
        }

        @Override
        public void failed(Throwable exc, Object attachment) {
            log.error("接收客户端连接失败,Reason:{}", exc.getMessage());
        }


        // 客户端读写操作回调函数
        private class ClientHandler implements CompletionHandler<Integer, HashMap<String, Object>> {

            private AsynchronousSocketChannel clientChannel;

            public ClientHandler(AsynchronousSocketChannel clientChannel){
                this.clientChannel = clientChannel;
            }

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
                            System.out.println("客户端发送数据:" + new String(buffer.array(), 0, result));
                            buffer.flip();
                            attachment.put("type","write");
                            clientChannel.write(buffer, attachment, this);
                            buffer.clear();
                        }else if ("write".equals(type)){
                            ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                            attachment.put("type","read");
                            attachment.put("byteBuffer",byteBuffer);
                            clientChannel.read(byteBuffer, attachment, this);
                        }else {
                            System.out.println("未知客户端操作....");
                        }
                    }
                }
            }

            @Override
            public void failed(Throwable exc, HashMap<String, Object> attachment) {
                String type = (String) attachment.get("type");
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
}
