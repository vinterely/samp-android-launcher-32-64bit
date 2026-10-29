package com.glory.launcher.network;

import com.google.gson.annotations.SerializedName;

public final class Server {
    @SerializedName("name")
    private String name = "";
    @SerializedName("ip")
    private String ip = "";

    @SerializedName("port")
    private Integer port = 7777;

    @SerializedName("id")
    private Integer id;

    public Server(String ipAddr, Integer iPort, String serverName, Integer ID) {
        ip = ipAddr;
        port = iPort;
        name = serverName;
        id = ID;
    }

    public final String getIP() { return ip; }
    public final Integer getPort() { return port; }
    public String getName() { return name; }
    public Integer getID() { return id; }
}
