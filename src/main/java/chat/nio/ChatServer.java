package chat.nio;

import lombok.extern.slf4j.Slf4j;
import util.Constant;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NIO聊天室服务端  单线程
 *      1.监听客户端连接，将客户端连接保存，注册可读事件
 *      2.监听客户端发送消息，将消息转发给其他客户端
 */
@Slf4j
public class ChatServer {

    public static void main(String[] args) throws IOException {

        ChatServer chatServer = new ChatServer();
        chatServer.start();

    }

    private ServerSocketChannel serverSocketChannel = null;

    private Selector selector = null;

    private ByteBuffer readBuffer = ByteBuffer.allocate(1024);

    private void start() throws IOException {
        //获取一个Selector
        selector = Selector.open();
        //创建服务端Channel
        serverSocketChannel = ServerSocketChannel.open();
        //绑定IP端口
        serverSocketChannel.bind(new InetSocketAddress(Constant.ServerIP,Constant.ServerPort));
        //设置非阻塞
        serverSocketChannel.configureBlocking(false);
        //注册连接事件
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        //进入循环
        while(true){
            try {
                //等待事件响应
                Integer selectedNumber = selector.select();

                //正常来说 只有事件触发才会返回 Jdk存在bug 可能空轮训
                if (selectedNumber.equals(0)){
                    log.info("Server Selector returned by : {}",selectedNumber);
                }

                //获取发生就绪事件的Channel集合
                Set<SelectionKey> selectionKeys = selector.selectedKeys();

                for (SelectionKey selectionKey : selectionKeys) {

                    //移除本次的响应事件 => 也可以不移除 留给下次处理
                    selectionKeys.remove(selectionKey);

                    //客户端连接事件
                    if (selectionKey.isAcceptable()) {
                        ClientConnectedHandler(selectionKey, selector);
                    }

                    //客户端发送消息事件
                    if (selectionKey.isReadable()) {
                        ClientSendMsgHandler(selectionKey, selector);
                    }

                }
            }catch (Exception e) {
                log.error("Server Running error, Reason:{}",e.getMessage());
                e.printStackTrace();
            }finally {
//                close();
            }
        }
    }


    private void close()  {
        try{
            if (selector != null){
                selector.close();
            }
            if (serverSocketChannel != null){
                serverSocketChannel.close();
            }
            if (readBuffer != null){
                readBuffer.clear();
            }
        }catch (Exception e){
            log.error("Server Close error, Reason:{}",e.getMessage());
        }
    }

    private void ClientSendMsgHandler(SelectionKey selectionKey, Selector selector) throws IOException {
        //将消息转发给其他客户端
        SocketChannel socketChannel = (SocketChannel)selectionKey.channel();
        readBuffer.clear();
        StringBuilder msg = new StringBuilder();
        try{
            while (socketChannel.read(readBuffer) > 0);
        }catch (Exception e){
            //客户端强行关闭会导致selector不断返回读就绪事件
            socketChannel.close();
        }
        readBuffer.flip();
        msg.append(Charset.forName("UTF-8").decode(readBuffer));
        Boolean isClientStop = msg.toString().equals(Constant.ClientStopCommand) || msg.toString().equals("");
        String endPrefix = " [From Client " + socketChannel.socket().getPort() + "]";
        msg.append(endPrefix);
        for (SelectionKey selectionKeyItem : selector.keys()){
            if (selectionKeyItem.channel() instanceof ServerSocketChannel){
                continue;
            }
            SocketChannel otherSocketChannel = (SocketChannel)selectionKeyItem.channel();
            if (selectionKeyItem.isValid() && !otherSocketChannel.equals(socketChannel)){
                otherSocketChannel.write(Charset.forName("UTF-8").encode(msg.toString()));
            }
            //如果客户端结束 消除此客户端selector
            if (isClientStop){
                selectionKey.cancel();
                selector.wakeup();
            }
        }

        //重新注册可读事件
        socketChannel.register(selector, SelectionKey.OP_READ);
    }


    private void ClientConnectedHandler(SelectionKey selectionKey, Selector selector) throws IOException {
        //将客户端连接加入map
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel)selectionKey.channel();
        SocketChannel socketChannel = serverSocketChannel.accept();
        //返回客户端连接成功响应
        socketChannel.write(Charset.forName("UTF-8").encode("Connected Server[" + Constant.ServerPort + "] Success.."));
        //注册可读事件
        socketChannel.configureBlocking(false);
        socketChannel.register(selector,SelectionKey.OP_READ);
    }


}
