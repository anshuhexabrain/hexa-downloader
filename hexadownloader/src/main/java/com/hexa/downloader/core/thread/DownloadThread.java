package com.hexa.downloader.core.thread;


import android.os.Process;
import android.util.Log;

import com.hexa.downloader.config.Config;
import com.hexa.downloader.core.DownloadResponse;
import com.hexa.downloader.domain.DownloadInfo;
import com.hexa.downloader.domain.DownloadThreadInfo;
import com.hexa.downloader.exception.DownloadException;
import com.hexa.downloader.exception.DownloadPauseException;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;


public class DownloadThread implements Runnable {

    public static final String TAG = "DownloadThread";

    private final DownloadThreadInfo downloadThreadInfo;
    private final DownloadResponse downloadResponse;
    private final Config config;
    private final DownloadInfo downloadInfo;
    private final DownloadProgressListener downloadProgressListener;
    private final long lastProgress;
    private InputStream inputStream;

    public DownloadThread(DownloadThreadInfo downloadThreadInfo, DownloadResponse downloadResponse,
                          Config config,
                          DownloadInfo downloadInfo, DownloadProgressListener downloadProgressListener) {
        this.downloadThreadInfo = downloadThreadInfo;
        this.downloadResponse = downloadResponse;
        this.config = config;
        this.downloadInfo = downloadInfo;
        this.lastProgress = downloadThreadInfo.getProgress();
        this.downloadProgressListener = downloadProgressListener;
    }

    @Override
    public void run() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        checkPause();
        try {
            executeDownload();
        } catch (DownloadException e) {
            downloadResponse.handleException(downloadInfo, e);
        }
    }

    private void executeDownload() {
        HttpURLConnection httpConnection = null;
        try {
            final URL url = new URL(downloadThreadInfo.getUri());
            httpConnection = (HttpURLConnection) url.openConnection();
            httpConnection.setConnectTimeout(config.getConnectTimeout());
            httpConnection.setReadTimeout(config.getReadTimeout());
            httpConnection.setRequestMethod(config.getMethod());
            long lastStart = downloadThreadInfo.getStart() + lastProgress;
            if (downloadInfo.isSupportRanges()) {
                httpConnection.setRequestProperty("Range",
                        "bytes=" + lastStart + "-" + downloadThreadInfo.getEnd());
            }
            final int responseCode = httpConnection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_PARTIAL
                    || responseCode == HttpURLConnection.HTTP_OK) {
                inputStream = httpConnection.getInputStream();
                RandomAccessFile raf = new RandomAccessFile(downloadInfo.getPath(), "rwd");

                raf.seek(lastStart);
                final byte[] bf = new byte[1024 * 4];
                int len = -1;
                int offset = 0;
                while (true) {
                    checkPause();
                    len = inputStream.read(bf);
                    if (len == -1) {
                        break;
                    }
                    raf.write(bf, 0, len);
                    offset += len;

                    //          synchronized (downloadProgressListener) {
                    downloadThreadInfo.setProgress(lastProgress + offset);
                    downloadProgressListener.onProgress();
                    //          }

                    Log.d(TAG,
                            "downloadInfo:" + downloadInfo.getId() + " thread:" + downloadThreadInfo.getThreadId()
                                    + " progress:"
                                    + downloadThreadInfo.getProgress()
                                    + ",start:" + downloadThreadInfo.getStart() + ",end:" + downloadThreadInfo
                                    .getEnd());
                }

                //downloadInfo success
                downloadProgressListener.onDownloadSuccess();
            } else {
                throw new DownloadException(DownloadException.EXCEPTION_SERVER_SUPPORT_CODE,
                        "UnSupported response code:" + responseCode);
            }
            checkPause();
        } catch (ProtocolException e) {
            throw new DownloadException(DownloadException.EXCEPTION_PROTOCOL, "Protocol error", e);
        } catch (IOException e) {
            throw new DownloadException(DownloadException.EXCEPTION_IO_EXCEPTION, "IO error", e);
        } catch (DownloadPauseException e) {
            //TODO process pause logic
        } catch (Exception e) {
            throw new DownloadException(DownloadException.EXCEPTION_OTHER, "other error", e);
        } finally {
            if (httpConnection != null) {
                httpConnection.disconnect();
            }
        }
    }

    private void checkPause() {
        if (downloadInfo.isPause()) {
            throw new DownloadPauseException(DownloadException.EXCEPTION_PAUSE);
        }
    }

    /**
     * Download thread progress listener.
     */
    public interface DownloadProgressListener {

        void onProgress();

        void onDownloadSuccess();
    }


}
