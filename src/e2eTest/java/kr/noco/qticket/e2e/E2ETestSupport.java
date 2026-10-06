package kr.noco.qticket.e2e;

import org.springframework.boot.test.web.server.LocalServerPort;

abstract class E2ETestSupport {

    @LocalServerPort
    protected int port;

    protected String url(String path) {
        return "http://127.0.0.1:" + port + path;
    }
}
