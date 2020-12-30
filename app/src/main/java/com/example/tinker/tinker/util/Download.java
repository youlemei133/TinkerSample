package com.example.tinker.tinker.util;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created By hudawei
 * on 2020/12/23 0023
 */
public class Download {
    private String url;
    private File dir;
    private String fileName;
    private Callback callback;
    private final Handler mHandler;
    private final int DOWN_LOAD_START = 0;
    private final int DOWN_LOAD_PROGRESS = 1;
    private final int DOWN_LOAD_COMPLETED = 2;
    private final int DOWN_LOAD_FAILED = 3;

    private final String downloadSize = "downloadSize";
    private final String totalSize = "totalSize";
    private final String exception = "exception";

    public Download() {
        mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                if (callback == null)
                    return;
                Bundle data = msg.getData();
                switch (msg.what) {
                    case DOWN_LOAD_START:
                        callback.onDownloadStarted(url);
                        break;
                    case DOWN_LOAD_PROGRESS:
                        callback.onDownloadProgress(url, data.getInt(downloadSize), data.getInt(totalSize));
                        break;
                    case DOWN_LOAD_COMPLETED:
                        callback.onDownloadCompleted(url, new File(dir, fileName));
                        break;
                    case DOWN_LOAD_FAILED:
                        callback.onDownloadFailed(url, (Exception) data.getSerializable(exception));
                        break;
                }
            }
        };
    }

    /**
     * 设置下载地址
     *
     * @param url 下载地址
     * @return Download
     */
    public Download remote(@NonNull String url) {
        this.url = url;
        return this;
    }

    /**
     * 设置下载文件保存路径
     *
     * @param dir      保存目录
     * @param fileName 保存文件名
     * @return Download
     */
    public Download local(@NonNull File dir, @NonNull String fileName) {
        this.dir = dir;
        this.fileName = fileName;
        return this;
    }

    /**
     * 开启线程开始下载
     *
     * @param callback 下载回调
     */
    public void start(Callback callback) {
        this.callback = callback;
        new Thread() {
            @Override
            public void run() {
                super.run();
                request();
            }
        }.start();
    }

    /**
     * 下载，会将下载相应过程调用对应回调方法
     */
    private void request() {
        if (TextUtils.isEmpty(url) || !url.startsWith("http")) {
            sendFailedMessage(new IllegalArgumentException("url格式不对"));
            return;
        }
        HttpURLConnection connection = null;
        InputStream inputStream = null;
        FileOutputStream outputStream = null;
        try {
            URL url = new URL(this.url);

            File localFile = getLocalFile();
            if (localFile == null) {
                sendFailedMessage(new IllegalArgumentException("文件无法创建"));
                return;
            }
            sendStartedMessage();
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10 * 1000);
            connection.connect();


            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                inputStream = connection.getInputStream();
                outputStream = new FileOutputStream(localFile);
                byte[] bytes = new byte[1024];
                int len;
                int totalSize = connection.getContentLength();
                int downloadSize = 0;
                while ((len = inputStream.read(bytes)) != -1) {
                    downloadSize += len;
                    sendProgressMessage(downloadSize, totalSize);
                    outputStream.write(bytes, 0, len);
                }
                outputStream.flush();
                sendCompletedMessage();
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendFailedMessage(e);
        } finally {
            if (connection != null)
                connection.disconnect();
            closeInputStream(inputStream);
            closeOutputStream(outputStream);
        }
    }

    private void sendStartedMessage() {
        sendMessage(DOWN_LOAD_START, null);
    }

    private void sendCompletedMessage() {
        sendMessage(DOWN_LOAD_COMPLETED, new Bundle());
    }

    private void sendFailedMessage(Exception e) {
        Bundle bundle = new Bundle();
        bundle.putSerializable(exception, e);
        sendMessage(DOWN_LOAD_FAILED, bundle);
    }

    private void sendProgressMessage(int downloadSize, int totalSize) {
        Bundle bundle = new Bundle();
        bundle.putSerializable(this.downloadSize, downloadSize);
        bundle.putSerializable(this.totalSize, totalSize);
        sendMessage(DOWN_LOAD_PROGRESS, bundle);
    }

    private void sendMessage(int code, Bundle bundle) {
        Message message = mHandler.obtainMessage();
        message.what = code;
        message.setData(bundle);
        mHandler.sendMessage(message);
    }

    private File getLocalFile() {
        try {
            if (!dir.exists() || !dir.isDirectory()) {
                dir.mkdirs();
            }
            File file = new File(dir, fileName);
            if (!file.exists() || !file.isFile())
                file.createNewFile();
            return file;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 关闭输入流
     *
     * @param inputStream 需关闭的输入流
     */
    private void closeInputStream(InputStream inputStream) {
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 关闭输出流
     *
     * @param outputStream 需关闭的输出流
     */
    private void closeOutputStream(OutputStream outputStream) {
        if (outputStream != null) {
            try {
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public interface Callback {
        void onDownloadStarted(String url);

        void onDownloadCompleted(String url, File localFile);

        void onDownloadFailed(String url, Exception e);

        void onDownloadProgress(String url, int downloadSize, int totalSize);
    }

    public static class SimpleCallback implements Callback {

        @Override
        public void onDownloadStarted(String url) {
            print("onDownloadStarted");
        }

        @Override
        public void onDownloadCompleted(String url, File localFile) {
            print("onDownloadCompleted " + localFile);
        }

        @Override
        public void onDownloadFailed(String url, Exception e) {
            print("onDownloadFailed");
        }

        @Override
        public void onDownloadProgress(String url, int downloadSize, int totalSize) {
            print("onDownloadProgress " + downloadSize + "/" + totalSize);
        }

        private void print(String message) {
            Log.e(getClass().getSimpleName(), message);
        }
    }
}
