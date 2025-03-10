package com.hexa.downloader.core;

import com.hexa.downloader.config.Config;
import com.hexa.downloader.core.task.DownloadTask;
import com.hexa.downloader.core.task.GetFileInfoTask;
import com.hexa.downloader.core.task.GetFileInfoTask.OnGetFileInfoListener;
import com.hexa.downloader.core.thread.DownloadThread;
import com.hexa.downloader.core.thread.DownloadThread.DownloadProgressListener;
import com.hexa.downloader.domain.DownloadInfo;
import com.hexa.downloader.domain.DownloadThreadInfo;
import com.hexa.downloader.exception.DownloadException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

public class DownloadTaskImpl implements DownloadTask, OnGetFileInfoListener,
        DownloadProgressListener {

    private final ExecutorService executorService;
    private final DownloadResponse downloadResponse;
    private final DownloadInfo downloadInfo;
    private final Config config;
    private final List<DownloadThread> downloadThreads;
    private final DownloadTaskListener downloadTaskListener;
    private long lastRefreshTime = System.currentTimeMillis();
    private long progress;
    private final AtomicBoolean isComputerDownload = new AtomicBoolean(false);

    public DownloadTaskImpl(ExecutorService executorService, DownloadResponse downloadResponse,
                            DownloadInfo downloadInfo, Config config, DownloadTaskListener downloadTaskListener) {
        this.executorService = executorService;
        this.downloadResponse = downloadResponse;
        this.downloadInfo = downloadInfo;
        this.config = config;
        this.downloadTaskListener = downloadTaskListener;
        this.downloadThreads = new ArrayList<>();

    }

    @Override
    public void start() {
        if (downloadInfo.getSize() <= 0) {
            //get downloadInfo info size
            getFileInfo();
        } else {
            List<DownloadThreadInfo> downloadThreadInfos = downloadInfo.getDownloadThreadInfos();
            for (DownloadThreadInfo downloadThreadInfo : downloadThreadInfos
            ) {
                DownloadThread downloadThread = new DownloadThread(downloadThreadInfo, downloadResponse,
                        config,
                        downloadInfo, this);
                executorService.submit(downloadThread);
                downloadThreads.add(downloadThread);
            }

            downloadInfo.setStatus(DownloadInfo.STATUS_DOWNLOADING);
            downloadResponse.onStatusChanged(downloadInfo);
        }
    }

    private void getFileInfo() {
        GetFileInfoTask getFileInfoTask = new GetFileInfoTask(downloadResponse, downloadInfo, this);
        executorService.submit(getFileInfoTask);
    }

    @Override
    public void onSuccess(long size, boolean isSupportRanges) {
        downloadInfo.setSupportRanges(isSupportRanges);
        downloadInfo.setSize(size);

        List<DownloadThreadInfo> downloadThreadInfos = new ArrayList<>();
        if (isSupportRanges) {
            long length = downloadInfo.getSize();
            final int threads = config.getEachDownloadThread();
            final long average = length / threads;
            for (int i = 0; i < threads; i++) {
                long start = average * i;
                long end;
                if (i == threads - 1) {
                    end = length - 1;
                } else {
                    end = start + average - 1;
                }
                DownloadThreadInfo downloadThreadInfo = new DownloadThreadInfo(i, downloadInfo.getId(),
                        downloadInfo
                                .getUri(), start,
                        end);
                downloadThreadInfos.add(downloadThreadInfo);

                DownloadThread downloadThread = new DownloadThread(downloadThreadInfo, downloadResponse,
                        config,
                        downloadInfo, this);
                executorService.submit(downloadThread);
                downloadThreads.add(downloadThread);
            }
        } else {
            DownloadThreadInfo downloadThreadInfo = new DownloadThreadInfo(0, downloadInfo.getId(),
                    downloadInfo
                            .getUri(), 0,
                    downloadInfo.getSize());
            downloadThreadInfos.add(downloadThreadInfo);

            DownloadThread downloadThread = new DownloadThread(downloadThreadInfo, downloadResponse,
                    config,
                    downloadInfo, this);
            executorService.submit(downloadThread);
            downloadThreads.add(downloadThread);
        }
        downloadInfo.setDownloadThreadInfos(downloadThreadInfos);
        downloadInfo.setStatus(DownloadInfo.STATUS_DOWNLOADING);
        downloadResponse.onStatusChanged(downloadInfo);

    }

    @Override
    public void onFailed(DownloadException exception) {

    }

    @Override
    public void onProgress() {
        if (!isComputerDownload.get()) {
            synchronized (this) {
                if (!isComputerDownload.get()) {
                    isComputerDownload.set(true);
                    long currentTimeMillis = System.currentTimeMillis();
                    if ((currentTimeMillis - lastRefreshTime) > 1000) {
                        computerDownloadProgress();
                        downloadResponse.onStatusChanged(downloadInfo);
                        lastRefreshTime = currentTimeMillis;
                    }
                    isComputerDownload.set(false);
                }
            }
        }

    }

    @Override
    public void onDownloadSuccess() {
        computerDownloadProgress();
        if (downloadInfo.getProgress() == downloadInfo.getSize()) {
            downloadInfo.setStatus(DownloadInfo.STATUS_COMPLETED);
            downloadResponse.onStatusChanged(downloadInfo);
            if (downloadTaskListener != null) {
                downloadTaskListener.onDownloadSuccess(downloadInfo);
            }
        }
    }

    private void computerDownloadProgress() {
        progress = 0;
        List<DownloadThreadInfo> downloadThreadInfos = downloadInfo.getDownloadThreadInfos();
        for (DownloadThreadInfo info : downloadThreadInfos) {
            progress += info.getProgress();
        }
        downloadInfo.setProgress(progress);

    }

    public interface DownloadTaskListener {

        void onDownloadSuccess(DownloadInfo downloadInfo);
    }
}
