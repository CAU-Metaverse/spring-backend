//package com.metaverse.cau.domain.socket;
//
//import java.io.*;
//import java.net.*;
//
//import lombok.Getter;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.boot.SpringApplication;
//import org.springframework.boot.autoconfigure.SpringBootApplication;
//import org.springframework.context.event.ContextRefreshedEvent;
//import org.springframework.context.event.EventListener;
//import org.springframework.stereotype.Service;
//
//@Service
//public class ServerApplication
//{
//    @Getter
//    @Value("${server.port}")
//    private int serverPort;
//
//    @EventListener(ContextRefreshedEvent.class)
//    public void startServer()
//    {
//        int port = 8200;
//        try (ServerSocket serverSocket = new ServerSocket(port))
//        {
//            System.out.println("Server is listening on port " + port);
//            while (true)
//            {
//                Socket socket = serverSocket.accept();
//                System.out.println("Client connected: " + socket.getInetAddress());
//
//                // 클라이언트로부터 메시지 수신
//                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
//                String receivedMessage = reader.readLine();
//                System.out.println("Received from client: " + receivedMessage);
//
//                // 클라이언트에게 메시지 전송
//                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
//                writer.println("Hello from Java Spring!");
//
//                socket.close();
//            }
//        }
//        catch (IOException e)
//        {
//            e.printStackTrace();
//        }
//    }
//}