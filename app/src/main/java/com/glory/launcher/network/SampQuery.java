package com.glory.launcher.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.StringTokenizer;

public class SampQuery {
    private DatagramSocket socket = null;
    private InetAddress server = null;
    private String serverString = "";
    private int port = 0;
    private PrintWriter out = null;
    private BufferedReader in = null;

    public SampQuery(String server, int port) {
        try {
            this.serverString = server;
            this.server = InetAddress.getByName(this.serverString);
        } catch (UnknownHostException e) { System.out.println(e); }
        try {
            socket = new DatagramSocket(); // DatagramSocket for UDP connections
            socket.setSoTimeout(2000); // Set timeout to 2 seconds
        } catch (SocketException e) {
            e.printStackTrace();
        }
        this.port = port;
    }

    public String[] getInfo() {
        DatagramPacket packet = this.assemblePacket("i");
        this.send(packet);
        byte[] reply = this.receiveBytes();
        ByteBuffer buff = ByteBuffer.wrap(reply);
        buff.order(ByteOrder.LITTLE_ENDIAN);
        buff.position(11);
        String[] serverInfo = new String[6];

        int password = buff.get();
        short players = buff.getShort();
        short maxPlayers = buff.getShort();
        int len = buff.getInt();
        byte[] hostnameBA = new byte[len];

        for (int i = 0; len > i; i++) { hostnameBA[i] = buff.get(); }

        /* todo Ñ„Ð¸ÐºÑ ÐºÐ¾Ð´Ð¸Ñ€Ð¾Ð²ÐºÐ¸*/
        String hostname = new String(hostnameBA, Charset.forName("cp1251"));
        /**/
        int lenG = buff.getInt();
        byte[] gamemodeBA = new byte[lenG];
        for (int i = 0; lenG > i; i++) { gamemodeBA[i] = buff.get(); }

        String gamemode = new String(gamemodeBA, Charset.forName("cp1251"));

        int lenM = buff.getInt();
        byte[] mapBA = new byte[lenM];
        for (int i = 0; lenM > i; i++) { mapBA[i] = buff.get(); }

        String map = new String(mapBA, Charset.forName("cp1251"));

        serverInfo[0] = ""+password;
        serverInfo[1] = ""+players;
        serverInfo[2] = ""+maxPlayers;
        serverInfo[3] = hostname;
        serverInfo[4] = gamemode;
        serverInfo[5] = map;
        return serverInfo;
    }

    public String[][] getBasicPlayers() {
        DatagramPacket packet = this.assemblePacket("c");
        this.send(packet);
        byte[] reply = this.receiveBytes();
        ByteBuffer buff = ByteBuffer.wrap(reply);
        buff.order(ByteOrder.LITTLE_ENDIAN);
        buff.position(11);

        short playerCount = buff.getShort();
        String[][] players = new String[playerCount][2];

        for (int i = 0; players.length > i; i++) {
            byte len = buff.get();
            byte[] nameBA = new byte[len];

            for (int j = 0; len > j; j++) { nameBA[j] = buff.get(); }
            String name = new String(nameBA);
            int score = buff.getInt();
            players[i][0] = name;
            players[i][1] = ""+score;
        }
        return players;
    }

    public String[][] getDetailedPlayers() {
        DatagramPacket packet = this.assemblePacket("d");
        this.send(packet);
        byte[] reply = this.receiveBytes();
        ByteBuffer buff = ByteBuffer.wrap(reply);
        buff.order(ByteOrder.LITTLE_ENDIAN);
        buff.position(11);

        int playerCount = buff.getShort();
        String[][] players = new String[playerCount][4];

        for (int i = 0; players.length > i; i++) {
            int id = buff.get();
            int len = buff.get();
            byte[] nameBA = new byte[len];

            for (int j = 0; len > j; j++) { nameBA[j] = buff.get(); }
            String name = new String(nameBA);
            int score = buff.getInt();
            int ping = buff.getInt();

            players[i][0] = ""+id;
            players[i][1] = name;
            players[i][2] = ""+score;
            players[i][3] = ""+ping;
        }
        return players;
    }

    public String[][] getRules() { // Finished
        DatagramPacket packet = this.assemblePacket("r");
        this.send(packet);
        byte[] reply = this.receiveBytes();
        ByteBuffer buff = ByteBuffer.wrap(reply);
        buff.order(ByteOrder.LITTLE_ENDIAN);
        buff.position(11);

        short ruleCount = buff.getShort();
        String[][] rules = new String[ruleCount][2];

        for (int i = 0; rules.length > i; i++) {
            int len = buff.get();
            byte[] ruleBA = new byte[len];

            for (int j = 0; len > j; j++) {
                ruleBA[j] = buff.get();
            }
            String rule = new String(ruleBA);

            int lenV = buff.get();
            byte[] valBA = new byte[lenV];

            for (int j = 0; lenV > j; j++) {
                valBA[j] = buff.get();
            }
            String val = new String(valBA);

            rules[i][0] = rule;
            rules[i][1] = val;
        }
        return rules;
    }

    public long getPing() { // Finished
        long ping = 0;
        DatagramPacket packet = this.assemblePacket("p0101");
        long beforeSend = System.currentTimeMillis();
        this.send(packet);
        this.receiveBytes();
        long afterReceive = System.currentTimeMillis();
        ping = afterReceive - beforeSend;

        return ping;
    }

    public boolean connect() {
        DatagramPacket packet = assemblePacket("p0101");
        send(packet);
        String reply = receive();

        try {
            // Clean up reply
            reply = reply.substring(10);
            reply = reply.trim();

            if (reply.equals("p0101")) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) { return false; }
    }

    public void close() { socket.close(); }

    private DatagramPacket assemblePacket(String type) {
        DatagramPacket sendPacket = null;
        try {
           StringTokenizer tok = new StringTokenizer(this.serverString, ".");
           String packetData = "SAMP";
           while (tok.hasMoreTokens()) {
               packetData += (char)(Integer.parseInt(tok.nextToken()));
           }
           packetData += (char)(this.port & 0xFF);
           packetData += (char)(this.port >> 8 & 0xFF);
           packetData += type;
           byte[] data = packetData.getBytes("US-ASCII");
           sendPacket = new DatagramPacket(data, data.length, this.server, this.port);

        } catch (Exception e) { System.out.println(e); }
        return sendPacket;
    }

    private void send(DatagramPacket packet) {
        try { socket.send(packet); }
        catch (IOException e) { System.out.println(e); }
    }

    private String receive() {
        String modifiedSentence = null;
        byte[] receivedData = new byte[1024];
        try {
            DatagramPacket receivedPacket = new DatagramPacket(receivedData, receivedData.length);
            socket.receive(receivedPacket);
            modifiedSentence = new String(receivedPacket.getData());
        } catch (IOException e) { System.out.println(e); }
        return modifiedSentence;
    }

    private byte[] receiveBytes() {
        byte[] receivedData = new byte[3072];
        DatagramPacket receivedPacket = null;
        try {
            receivedPacket = new DatagramPacket(receivedData, receivedData.length);
            socket.receive(receivedPacket);
        }
        catch (IOException e) { System.out.println(e); }
        return receivedPacket.getData();
    }
}