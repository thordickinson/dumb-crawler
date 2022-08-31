package com.thordickinson.dumbcrawler.services;

import org.apache.hadoop.shaded.org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.stereotype.Service;

@Service
public class ConnectionService {

    private PoolingHttpClientConnectionManager connectionManager;

    public void getConnection() {
    }

}
