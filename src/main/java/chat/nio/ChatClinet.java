package chat.nio;

import chat.bio.UserInputHandler;
import lombok.extern.slf4j.Slf4j;
import util.Constant;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Set;

/**
 * 多人聊天室客户端
 *      1.接收用户输入，发送到服务端(由于用户输入是阻塞式的 因此采用子线程处理)
 *      2.接收服务端响应，打印信息
 */
@Slf4j
public class ChatClinet {

    private SocketChannel socketChannel = null;

    private Selector selector = null;

    private Boolean isClientRunning = Boolean.TRUE;

    private ByteBuffer byteBuffer = ByteBuffer.allocate(1024);

    public static void main(String[] args) {

        ChatClinet chatClinet = new ChatClinet();
        chatClinet.start();
    }

    private void start(){

        try {
            selector = Selector.open();
            socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);
            socketChannel.connect(new InetSocketAddress(Constant.ServerIP,Constant.ServerPort));
            socketChannel.register(selector, SelectionKey.OP_CONNECT);

            while (isClientRunning){

                Integer selectedNum = selector.select();

                //正常来说 只有事件触发才会返回 Jdk存在bug 可能空轮训
                if (selectedNum.equals(0)){
                    log.info("Server Selector returned by : {}",selectedNum);
                }

                Set<SelectionKey> selectionKeys = selector.selectedKeys();

                for (SelectionKey selectionKey : selectionKeys){

                    if (selectionKey.isConnectable()){
                        ConnectedServerHandler(selector,selectionKey);
                    }

                    if (selectionKey.isReadable()){
                        RecivedMsgFromServerHandler(selector,selectionKey);
                    }

                    selectionKeys.remove(selectionKey);

                }

            }
        }catch (Exception e){
            log.error("Client Running error,Reason:{}", e.getMessage());
        }finally {
            close();
        }
    }

    private void ConnectedServerHandler(Selector selector,SelectionKey selectionKey) throws IOException {
        SocketChannel socketChannel = (SocketChannel)selectionKey.channel();
        if (socketChannel.isConnectionPending()){
            socketChannel.finishConnect();
            new Thread(new NioUserInputHandler(this)).start();
        }
        socketChannel.register(selector, SelectionKey.OP_READ);
    }


    private void RecivedMsgFromServerHandler(Selector selector, SelectionKey selectionKey) throws IOException {
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        byteBuffer.clear();
        while (socketChannel.read(byteBuffer) > 0);
        byteBuffer.flip();
        System.out.println(Charset.forName("UTF-8").decode(byteBuffer));
        socketChannel.register(selector, SelectionKey.OP_READ);
    }

    private void close() {
        try {
            if (selector != null){
                selector.close();
            }
            if (socketChannel != null){
                socketChannel.close();
            }
        }catch (Exception e){
            log.error("Client Close error, Reason:{}",e.getMessage());
        }
    }

    public void ClientExit(){
        this.isClientRunning = false;
        selector.wakeup();
    }

    public void sendMsgToServer(String msg) throws IOException {
        socketChannel.write(Charset.forName("UTF-8").encode(msg));
    }

}
