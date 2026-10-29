package com.glory.launcher.network.model;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

public class LauncherManifest {

    @SerializedName("schema")
    private int schema = 2;

    @SerializedName("launcher")
    private LauncherInfo launcher;

    @SerializedName("data")
    private DataInfo data;

    public int getSchema() {
        return schema;
    }

    public void setSchema(int schema) {
        this.schema = schema;
    }

    public LauncherInfo getLauncher() {
        return launcher;
    }

    public void setLauncher(LauncherInfo launcher) {
        this.launcher = launcher;
    }

    public DataInfo getData() {
        return data;
    }

    public void setData(DataInfo data) {
        this.data = data;
    }

    public static class LauncherInfo {
        @SerializedName("versionCode")
        private int versionCode;

        @SerializedName("versionName")
        private String versionName;

        @SerializedName("apkUrl")
        private String apkUrl;

        public int getVersionCode() {
            return versionCode;
        }

        public void setVersionCode(int versionCode) {
            this.versionCode = versionCode;
        }

        public String getVersionName() {
            return versionName;
        }

        public void setVersionName(String versionName) {
            this.versionName = versionName;
        }

        public String getApkUrl() {
            return apkUrl;
        }

        public void setApkUrl(String apkUrl) {
            this.apkUrl = apkUrl;
        }
    }

    public static class DataInfo {
        @SerializedName("baseUrl")
        private String baseUrl;

        @SerializedName("extractedSize")
        private long extractedSize;

        @SerializedName("files")
        private List<DataFile> files = new ArrayList<>();

        @SerializedName("requiredFiles")
        private List<RequiredFile> requiredFiles = new ArrayList<>();

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public long getExtractedSize() {
            return extractedSize;
        }

        public void setExtractedSize(long extractedSize) {
            this.extractedSize = extractedSize;
        }

        public List<DataFile> getFiles() {
            return files;
        }

        public void setFiles(List<DataFile> files) {
            this.files = files;
        }

        public List<RequiredFile> getRequiredFiles() {
            return requiredFiles;
        }

        public void setRequiredFiles(List<RequiredFile> requiredFiles) {
            this.requiredFiles = requiredFiles;
        }
    }

    public static class DataFile {
        @SerializedName("name")
        private String name;

        @SerializedName("size")
        private long size;

        @SerializedName("sha256")
        private String sha256;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }

        public String getSha256() {
            return sha256;
        }

        public void setSha256(String sha256) {
            this.sha256 = sha256;
        }
    }

    public static class RequiredFile {
        @SerializedName("path")
        private String path;

        @SerializedName("size")
        private long size;

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }
    }
}
