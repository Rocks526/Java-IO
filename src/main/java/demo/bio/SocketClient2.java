package demo.bio;

import lombok.extern.slf4j.Slf4j;
import util.Constant;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Client
 */
@Slf4j
public class SocketClient2 {

    public static void main(String[] args) {

        Socket socket = null;
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(Constant.ServerIP,Constant.ServerPort));
            log.info("Client [{}:{}] init success...",socket.getLocalAddress(),socket.getLocalPort());
            while (true){
                BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
                BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String msg = consoleReader.readLine();
                bufferedWriter.write(msg + "\n");
                bufferedWriter.flush();
                System.out.println(bufferedReader.readLine());
                if (msg.equals(Constant.ClientStopCommand) || msg.equals(Constant.ServerStopCommand)){
                    break;
                }
            }
        } catch (IOException e) {
            log.error("Client [{}] running error,Reason:{}",socket.getInetAddress()+":"+socket.getPort(),e.getMessage());
            e.printStackTrace();
        }finally {
            try {
                if (socket != null){
                    socket.close();
                }
                log.info("Client [{}:{}] closed success...",socket.getInetAddress(),socket.getPort());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

}
