package com.hexa.downloader.core;

import com.hexa.downloader.domain.DownloadInfo;
import com.hexa.downloader.exception.DownloadException;

public interface DownloadResponse {

    void onStatusChanged(DownloadInfo downloadInfo);

    void handleException(DownloadInfo downloadInfo, DownloadException exception);
}
