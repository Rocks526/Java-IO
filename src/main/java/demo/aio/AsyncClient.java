package demo.aio;

import lombok.extern.slf4j.Slf4j;
import util.Constant;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@Slf4j
public class AsyncClient {

    public static void main(String[] args) throws InterruptedException, ExecutionException, IOException {
        new AsyncClient().start();

    }

    private AsynchronousSocketChannel clientChannel;

    private void start() throws IOException, ExecutionException, InterruptedException {
        clientChannel = AsynchronousSocketChannel.open();
        Future<Void> future = clientChannel.connect(new InetSocketAddress(Constant.ServerIP, Constant.ServerPort));
        // 阻塞等待返回结果
        future.get();
        // 接收用户输入
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            String readLine = bufferedReader.readLine();

            // 发送服务端
            Future<Integer> write = clientChannel.write(Charset.forName("UTF-8").encode(readLine));
            write.get();

            // 获取服务端响应
            ByteBuffer buffer = ByteBuffer.allocate(1024);
//            buffer.flip();
            Future<Integer> read = clientChannel.read(buffer);
            Integer size = read.get();
            byte[] array = buffer.array();
            System.out.println(new String(array, 0, size));
        }
    }

}
