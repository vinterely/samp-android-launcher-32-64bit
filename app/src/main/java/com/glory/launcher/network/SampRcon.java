package com.glory.launcher.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.StringTokenizer;

public class SampRcon {
    private DatagramSocket socket = null;
    private InetAddress server = null;
    private String serverString = "";
    private int port = 0;
    private String password;
    private PrintWriter out = null;
    private BufferedReader in = null;
    
    public SampRcon(String server, int port, String password) {
        try {
            this.serverString = server;
            this.password = password;
            this.server = InetAddress.getByName(this.serverString);
        } catch (UnknownHostException e) {
            System.out.println(e);
        }
        try {
            socket = new DatagramSocket(); // DatagramSocket for UDP connections
            socket.setSoTimeout(2000); // Set timeout to 2 seconds
        } catch (SocketException e) {
            System.out.println(e);
        }
        this.port = port;
    }

    public String[] getCommandList() {
        DatagramPacket packet = this.assemblePacket("cmdlist");
        String[] rawCommands = this.cleanArray(this.rconSend(packet, 1000));
        
        String[] commands = new String[rawCommands.length - 1];
        for (int i = 0; commands.length > i; i++) {
            commands[i] = rawCommands[i + 1];
        }
        return commands;
    }
    
    public String[][] getServerVariables() {
        DatagramPacket packet = this.assemblePacket("varlist");
        String[] rawVars = this.cleanArray(this.rconSend(packet, 1000));
        String[][] vars = new String[rawVars.length - 1][2];
        for (int i = 0; vars.length > i; i++) {
            String[] temp = rawVars[i + 1].split("=");
            vars[i][0] = temp[0].trim();
            vars[i][1] = temp[1].trim();
        }
        return vars;
    }
    
    /**
    * Sets the server's current weather. 
    * @param weatherID weather ID
    */
    public void setWeather(int weatherID) {
        DatagramPacket packet = this.assemblePacket("weather "+weatherID);
        this.rconSend(packet, 0);
    }
    
    /**
    * Sets the server's current weather to weather ID 1. 
    */
    public void setWeather() {
        this.setWeather(1);
    }
    
    /**
    * Sets the server's current gravity. 
    * @param gravity gravity double / float
    */
    public void setGravity(double gravity) {
        DatagramPacket packet = this.assemblePacket("gravity "+gravity);
        this.rconSend(packet, 0);
    }
    
    /**
    * Sets the server's current gravity to the default gravity level (0.008). 
    */
    public void setGravity() {
        this.setGravity(0.008);
    }
    
    /**
    * Ban a player from the server.
    * @param playerID player's ID
    * @return String[]<br />
    */
    public String[] ban(int playerID) {
        DatagramPacket packet = this.assemblePacket("ban "+playerID);
        return this.cleanArray(this.rconSend(packet, 1000));
    }
    
    /**
    * Kick a player from the server.
    * @param playerID player's ID
    * @return String[]
    */
    public String[] kick(int playerID) {
        DatagramPacket packet = this.assemblePacket("kick "+playerID);
        return this.cleanArray(this.rconSend(packet, 1000));
    }
    
    /**
    * Ban an IP address from the server.
    * @param address IP address
    * @return String[]
    */
    public String[] banAddress(String address) {
        DatagramPacket packet = this.assemblePacket("banip "+address);
        return this.cleanArray(this.rconSend(packet, 1000));
    }
    
    /**
    * Unban an IP address from the server.
    * @param address IP address
    * @return String[]
    */
    public String[] unbanAddress(String address) {
        DatagramPacket packet = this.assemblePacket("unbanip "+address);
        return this.cleanArray(this.rconSend(packet, 1000));
    }
    
    /**
    * Reload the server's log file.
    */
    public String[] reloadLog() {
        DatagramPacket packet = this.assemblePacket("reloadlog");
        return this.cleanArray(this.rconSend(packet, 1000));
    }
    
    /**
    * Reload the server's bans file.
    */
    public String[] reloadBans() {
        DatagramPacket packet = this.assemblePacket("reloadbans");
        return this.cleanArray(this.rconSend(packet, 1000));
    }
    
    /**
    * Send an admin message to players on the server.
    * @param message message to send to players
    */
    public void say(String message) {
        DatagramPacket packet = this.assemblePacket("say "+message);
        this.rconSend(packet, 0);
    }
    
    /**
    * Change the server's current gamemode.
    * @param gameMode game mode to change to
    */
    public void changeGameMode(String gameMode) {
        DatagramPacket packet = this.assemblePacket("changemode "+gameMode);
        this.rconSend(packet, 0);
    }
    
    /**
    * Set the server's current displayed game mode text.
    * @param gameModeText game mode text to change to
    */
    public void setGameModeText(String gameModeText) {
        DatagramPacket packet = this.assemblePacket("gamemodetext "+gameModeText);
        this.rconSend(packet, 0);
    }
    
    /**
    * Run the server's next gamemode (gmx).
    */
    public void nextGameMode() {
        DatagramPacket packet = this.assemblePacket("gmx");
        this.rconSend(packet, 0);
    }
    
    /**
    * Run the server's next gamemode (gmx).
    */
    public void gmx() {
        this.nextGameMode();
    }
    
    /**
    * Execute config file.
    * @param config config file to execute
    * @return String[]
    */
    public String[] execConfig(String config) {
        DatagramPacket packet = this.assemblePacket("exec "+config);
        return this.cleanArray(this.rconSend(packet, 1000));
    }
    
    /**
    * Load a filterscript.
    * @param fs filterscript to load
    * @return String[]
    */
    public String[] loadFilterscript(String fs) {
        DatagramPacket packet = this.assemblePacket("loadfs "+fs);
        return this.cleanArray(this.rconSend(packet, 1000));
    }
    
    /**
    * Load a filterscript.
    * @param fs filterscript to load
    * @return String[]
    */
    public String[] loadFS(String fs) {
        return this.loadFilterscript(fs);
    }
    
    /**
    * Unload a filterscript.
    * @param fs filterscript to unload
    * @return String[]
    */
    public String[] unloadFilterscript(String fs) {
        DatagramPacket packet = this.assemblePacket("unloadfs "+fs);
        return this.cleanArray(this.rconSend(packet, 1000));
    }
    
    /**
    * Unload a filterscript.
    * @param fs filterscript to unload
    * @return String[]
    */
    public String[] unloadFS(String fs) {
        return this.unloadFilterscript(fs);
    }
    
    /**
    * Reload a filterscript.
    * @param fs filterscript to reload
    * @return String[]
    */
    public String[] reloadFilterscript(String fs) {
        DatagramPacket packet = this.assemblePacket("reloadfs "+fs);
        return this.cleanArray(this.rconSend(packet, 1000));
    }
    
    /**
    * Reload a filterscript.
    * @param fs filterscript to reload
    * @return String[]
    */
    public String[] reloadFS(String fs) {
        return this.reloadFilterscript(fs);
    }
    
    /**
    * Shutdown the server.
    */
    public void exit() {
        DatagramPacket packet = this.assemblePacket("exit");
        this.rconSend(packet, 0);
    }
    
    /**
    * Sets the server's host name.
    * @param hostName the host name to set the server to
    */
    public void setHostName(String hostName) {
        DatagramPacket packet = this.assemblePacket("hostname "+hostName);
        this.rconSend(packet, 0);
    }
    
    /**
    * Sets the server's map name.
    * @param mapName the map name to set the server to
    */
    public void setMapName(String mapName) {
        DatagramPacket packet = this.assemblePacket("mapname "+mapName);
        this.rconSend(packet, 0);
    }
    
    /**
    * Sets the server's time.<br />
    * Note: This will not change the weather relative to the time.
    * @param time the time to set the server to
    */
    public void setTime(String time) {
        DatagramPacket packet = this.assemblePacket("worldtime "+time);
        this.rconSend(packet, 0);
    }
    
    /**
    * Sets the server's web url.
    * @param url the url to set the server to
    */
    public void setURL(String url) {
        DatagramPacket packet = this.assemblePacket("weburl "+url);
        this.rconSend(packet, 0);
    }
    
    /**
    * Sets the server's password.<br />
    * @param password the password to set the server to
    */
    public void setPassword(String password) {
        DatagramPacket packet = this.assemblePacket("password "+password);
        this.rconSend(packet, 0);
    }
    
    /**
    * Remove the server's password.
    */
    public void removePassword() {
        DatagramPacket packet = this.assemblePacket("password 0");
        this.rconSend(packet, 0);
    }
    
    /**
    * Sets the server's rcon password.
    * @param password the password the rcon password should be set to
    */
    public void setRconPassword(String password) {
        DatagramPacket packet = this.assemblePacket("rcon_password "+password);
        this.rconSend(packet, 0);
    }
    
    /**
    * Disables remote rcon to the server.
    */
    public void disableRcon() {
        DatagramPacket packet = this.assemblePacket("rcon 0");
        this.rconSend(packet, 0);
    }
    
    /**
    * Enables remote queries to the server.
    */
    public void enableQuery() {
        DatagramPacket packet = this.assemblePacket("query 1");
        this.rconSend(packet, 0);
    }
    
    /**
    * Disables remote queries to the server.
    */
    public void disableQuery() {
        DatagramPacket packet = this.assemblePacket("query 0");
        this.rconSend(packet, 0);
    }
    
    /**
    * Enables the server's announce.
    */
    public void enableAnnounce() {
        DatagramPacket packet = this.assemblePacket("announce 1");
        this.rconSend(packet, 0);
    }
    
    /**
    * Disables the server's announce.
    */
    public void disableAnnounce() {
        DatagramPacket packet = this.assemblePacket("announce 0");
        this.rconSend(packet, 0);
    }
    
    /**
    * Sets the server's max number of NPCs.
    * @param maxNPCs the maximum numbers of NPCs
    */
    public void setMaxNPCs(int maxNPCs) {
        DatagramPacket packet = this.assemblePacket("maxnpc "+maxNPCs);
        this.rconSend(packet, 0);
    } 
    
    /**
    * Execute an rcon command.
    * @param command command to execute
    * @param delay delay time, if you don't expect any data back set this to 0
    * @return String[]:<br />
    *   result[0]<br />
    *   result[1]<br />
    *   ...
    */
    public String[] call(String command, int delay) {
        DatagramPacket packet = this.assemblePacket(command);
        String[] result = this.rconSend(packet, delay);
        return result;
    }
    
    /**
    * Returns whether a successful connection was made. 
    * @return boolean
    */
    public boolean connect() {
        DatagramPacket sendPacket = null;
        try {
            StringTokenizer tok = new StringTokenizer(this.serverString, ".");

            String packetData = "SAMP";

            while (tok.hasMoreTokens()) {
                packetData += (char)(Integer.parseInt(tok.nextToken()));
            }

            packetData += (char)(this.port & 0xFF);
            packetData += (char)(this.port >> 8 & 0xFF);
            packetData += "p0101";

            byte[] data = packetData.getBytes("US-ASCII");

            sendPacket = new DatagramPacket(data, data.length, this.server, this.port);
            socket.send(sendPacket);
            } catch (Exception e) {
                System.out.println(e);
            }
        String reply = this.receive();
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
    
    /**
    * Closes the connection.
    */
    public void close() {
        socket.close();
    }
    
    private DatagramPacket assemblePacket(String command) {
        DatagramPacket sendPacket = null;
        try {
           StringTokenizer tok = new StringTokenizer(this.serverString, ".");
           
           String packetData = "SAMP";
           
           while (tok.hasMoreTokens()) {
               packetData += (char)(Integer.parseInt(tok.nextToken()));
           }

           packetData += (char)(this.port & 0xFF);
           packetData += (char)(this.port >> 8 & 0xFF);
           packetData += "x";
           packetData += (char)(this.password.length() & 0xFF);
           packetData += (char)(this.password.length() >> 8 & 0xFF);
           packetData += this.password;
           packetData += (char)(command.length() & 0xFF);
           packetData += (char)(command.length() >> 8 & 0xFF);
           packetData += command;
           
           byte[] data = packetData.getBytes("US-ASCII");
           
           sendPacket = new DatagramPacket(data, data.length, this.server, this.port);

        } catch (Exception e) {
            System.out.println(e);
        }
        return sendPacket;
    }
    
    private String[] rconSend(DatagramPacket packet, int delay) {
        String[] data = new String[1024];
        try {
            socket.send(packet);
            
            if (delay == 0) {
                return data;
            }
            
            int i = 0;
            long myTime = System.currentTimeMillis() + delay;
            while (System.currentTimeMillis() < myTime) {
                String temp = this.receive(128);
                if (temp.length() > 13) {
                    temp = temp.trim();
                    data[i] = temp.substring(13);
                    i++;
                } else {
                    break;
                }                
            }
        } catch (Exception e) {}
        return data;
    }
    
    private String receive(int buffer) {
        String modifiedSentence = "";
        byte[] receivedData = new byte[buffer];
        try {
            DatagramPacket receivedPacket = new DatagramPacket(receivedData, receivedData.length);
            socket.receive(receivedPacket);
            modifiedSentence = new String(receivedPacket.getData());
        } catch (IOException e) {
            System.out.println(e);
        }
        return modifiedSentence;
    }
    
    private String receive() {
        return receive(1024);
    }
    
    private String[] cleanArray(String[] data) {
        // Count elements that are not null
        int elementCount = 0;
        for (int i = 0; data.length > i; i++) {
            if (data[i] != null) {
                elementCount++;
            }
        }
        
        String[] cleanData = new String[elementCount];
        for (int i = 0; cleanData.length > i; i++) {
            cleanData[i] = data[i].trim();
        }
        return cleanData;
    }
}