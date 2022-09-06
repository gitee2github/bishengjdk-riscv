/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @test
 * @bug 8292044
 * @summary Tests behaviour of HttpClient when server responds with 102 or 103 status codes
 * @modules java.base/sun.net.www.http
 * java.net.http/jdk.internal.net.http.common
 * java.net.http/jdk.internal.net.http.frame
 * java.net.http/jdk.internal.net.http.hpack
 * java.logging
 * jdk.httpserver
 * @library http2/server
 * @build Http2TestServer HttpServerAdapters
 * @run testng/othervm -Djdk.internal.httpclient.debug=true
 * *                   -Djdk.httpclient.HttpClient.log=headers,requests,responses,errors Response1xxTest
 */
public class Response1xxTest {
    private static final String EXPECTED_RSP_BODY = "Hello World";

    private ServerSocket serverSocket;
    private Http11Server server;
    private String http1RequestURIBase;


    private HttpServerAdapters.HttpTestServer http2Server;
    private String http2RequestURIBase;

    @BeforeClass
    public void setup() throws Exception {
        serverSocket = new ServerSocket(0, 0, InetAddress.getLoopbackAddress());
        server = new Http11Server(serverSocket);
        new Thread(server).start();
        http1RequestURIBase = "http://" + serverSocket.getInetAddress().getHostAddress()
                + ":" + serverSocket.getLocalPort();

        http2Server = HttpServerAdapters.HttpTestServer.of(new Http2TestServer("localhost", false, 0));
        http2Server.addHandler(new Http2Handler(), "/http2/102");
        http2Server.addHandler(new Http2Handler(), "/http2/103");
        http2Server.addHandler(new OnlyInformationalHandler(), "/http2/only-informational");
        http2RequestURIBase = "http://" + http2Server.serverAuthority() + "/http2";
        http2Server.start();
        System.out.println("Started HTTP2 server at " + http2Server.getAddress());
    }

    @AfterClass
    public void teardown() throws Exception {
        if (server != null) {
            server.stop = true;
            System.out.println("(HTTP 1.1) Server stop requested");
        }
        if (serverSocket != null) {
            serverSocket.close();
            System.out.println("Closed (HTTP 1.1) server socket");
        }
        if (http2Server != null) {
            http2Server.stop();
            System.out.println("Stopped HTTP2 server");
        }
    }

    private static final class Http11Server implements Runnable {
        private static final int CONTENT_LENGTH = EXPECTED_RSP_BODY.getBytes(StandardCharsets.UTF_8).length;

        private static final String HTTP_1_1_RSP_200 = "HTTP/1.1 200 OK\r\n" +
                "Content-Length: " + CONTENT_LENGTH + "\r\n\r\n" +
                EXPECTED_RSP_BODY;

        private static final String REQ_LINE_FOO = "GET /test/foo HTTP/1.1\r\n";
        private static final String REQ_LINE_BAR = "GET /test/bar HTTP/1.1\r\n";


        private final ServerSocket serverSocket;
        private volatile boolean stop;

        private Http11Server(final ServerSocket serverSocket) {
            this.serverSocket = serverSocket;
        }

        @Override
        public void run() {
            try {
                System.out.println("Server running at " + serverSocket);
                while (!stop) {
                    // accept a connection
                    final Socket socket = serverSocket.accept();
                    System.out.println("Accepted connection from client " + socket);
                    // read request
                    final String requestLine;
                    try {
                        requestLine = readRequestLine(socket);
                    } catch (Throwable t) {
                        // ignore connections from potential rogue client
                        System.err.println("Ignoring connection/request from client " + socket
                                + " due to exception:");
                        t.printStackTrace();
                        // close the socket
                        safeClose(socket);
                        continue;
                    }
                    System.out.println("Received following request line from client " + socket
                            + " :\n" + requestLine);
                    final int informationalResponseCode;
                    if (requestLine.startsWith(REQ_LINE_FOO)) {
                        // handle /test/foo request (we will send intermediate/informational 102
                        // response in this case)
                        informationalResponseCode = 102;
                    } else if (requestLine.startsWith(REQ_LINE_BAR)) {
                        // handle /test/bar request (we will send intermediate/informational 103
                        // response in this case)
                        informationalResponseCode = 103;
                    } else {
                        // unexpected client. ignore and close the client
                        System.err.println("Ignoring unexpected request from client " + socket);
                        safeClose(socket);
                        continue;
                    }
                    try (final OutputStream os = socket.getOutputStream()) {
                        // send informational response headers a few times (spec allows them to
                        // be sent multiple times)
                        for (int i = 0; i < 3; i++) {
                            // send 1xx response header
                            os.write(("HTTP/1.1 " + informationalResponseCode + "\r\n\r\n")
                                    .getBytes(StandardCharsets.UTF_8));
                            os.flush();
                            System.out.println("Sent response code " + informationalResponseCode
                                    + " to client " + socket);
                        }
                        // now send a final response
                        System.out.println("Now sending 200 response code to client " + socket);
                        os.write(HTTP_1_1_RSP_200.getBytes(StandardCharsets.UTF_8));
                        os.flush();
                        System.out.println("Sent 200 response code to client " + socket);
                    }
                }
            } catch (Throwable t) {
                System.err.println("Stopping server due to exception");
                t.printStackTrace();
            }
        }

        static String readRequestLine(final Socket sock) throws IOException {
            final InputStream is = sock.getInputStream();
            final StringBuilder sb = new StringBuilder("");
            byte[] buf = new byte[1024];
            while (!sb.toString().endsWith("\r\n\r\n")) {
                final int numRead = is.read(buf);
                if (numRead == -1) {
                    return sb.toString();
                }
                final String part = new String(buf, 0, numRead, StandardCharsets.ISO_8859_1);
                sb.append(part);
            }
            return sb.toString();
        }

        private static void safeClose(final Socket socket) {
            try {
                socket.close();
            } catch (Throwable t) {
                // ignore
            }
        }
    }

    private static class Http2Handler implements HttpServerAdapters.HttpTestHandler {

        @Override
        public void handle(final HttpServerAdapters.HttpTestExchange exchange) throws IOException {
            final URI requestURI = exchange.getRequestURI();
            final int informationResponseCode = requestURI.getPath().endsWith("/102") ? 102 : 103;
            // send informational response headers a few times (spec allows them to
            // be sent multiple times)
            for (int i = 0; i < 3; i++) {
                exchange.sendResponseHeaders(informationResponseCode, -1);
                System.out.println("Sent " + informationResponseCode + " response code from H2 server");
            }
            // now send 200 response
            try {
                final byte[] body = EXPECTED_RSP_BODY.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, body.length);
                System.out.println("Sent 200 response from H2 server");
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
                System.out.println("Sent response body from H2 server");
            } catch (Throwable e) {
                System.err.println("Failed to send response from HTTP2 handler:");
                e.printStackTrace();
                throw e;
            }
        }
    }

    private static class OnlyInformationalHandler implements HttpServerAdapters.HttpTestHandler {

        @Override
        public void handle(final HttpServerAdapters.HttpTestExchange exchange) throws IOException {
            // we only send informational response and then return
            for (int i = 0; i < 5; i++) {
                exchange.sendResponseHeaders(102, -1);
                System.out.println("Sent 102 response code from H2 server");
                // wait for a while before sending again
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    // just return
                    System.err.println("Handler thread interrupted");
                }
            }
        }
    }

    /**
     * Tests that when a HTTP/1.1 server sends intermediate 1xx response codes and then the final
     * response, the client (internally) will ignore those intermediate informational response codes
     * and only return the final response to the application
     */
    @Test
    public void test1xxForHTTP11() throws Exception {
        final HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .proxy(HttpClient.Builder.NO_PROXY).build();
        final URI[] requestURIs = new URI[]{
                new URI(http1RequestURIBase + "/test/foo"),
                new URI(http1RequestURIBase + "/test/bar")};
        for (final URI requestURI : requestURIs) {
            final HttpRequest request = HttpRequest.newBuilder(requestURI).build();
            System.out.println("Issuing request to " + requestURI);
            final HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            Assert.assertEquals(response.version(), HttpClient.Version.HTTP_1_1,
                    "Unexpected HTTP version in response");
            Assert.assertEquals(response.statusCode(), 200, "Unexpected response code");
            Assert.assertEquals(response.body(), EXPECTED_RSP_BODY, "Unexpected response body");
        }
    }

    /**
     * Tests that when a HTTP2 server sends intermediate 1xx response codes and then the final
     * response, the client (internally) will ignore those intermediate informational response codes
     * and only return the final response to the application
     */
    @Test
    public void test1xxForHTTP2() throws Exception {
        final HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .proxy(HttpClient.Builder.NO_PROXY).build();
        final URI[] requestURIs = new URI[]{
                new URI(http2RequestURIBase + "/102"),
                new URI(http2RequestURIBase + "/103")};
        for (final URI requestURI : requestURIs) {
            final HttpRequest request = HttpRequest.newBuilder(requestURI).build();
            System.out.println("Issuing request to " + requestURI);
            final HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            Assert.assertEquals(response.version(), HttpClient.Version.HTTP_2,
                    "Unexpected HTTP version in response");
            Assert.assertEquals(response.statusCode(), 200, "Unexpected response code");
            Assert.assertEquals(response.body(), EXPECTED_RSP_BODY, "Unexpected response body");
        }
    }


    /**
     * Tests that when a request is issued with a specific request timeout and the server
     * responds with intermediate 1xx response code but doesn't respond with a final response within
     * the timeout duration, then the application fails with a request timeout
     */
    @Test
    public void test1xxRequestTimeout() throws Exception {
        final HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .proxy(HttpClient.Builder.NO_PROXY).build();
        final URI requestURI = new URI(http2RequestURIBase + "/only-informational");
        final Duration requestTimeout = Duration.ofSeconds(2);
        final HttpRequest request = HttpRequest.newBuilder(requestURI).timeout(requestTimeout)
                .build();
        System.out.println("Issuing request to " + requestURI);
        // we expect the request to timeout
        Assert.assertThrows(HttpTimeoutException.class, () -> {
            client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        });
    }
}
