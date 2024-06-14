package org.example.webserver;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.example.db.DataBase;
import org.example.model.User;
import org.example.util.HttpRequestUtils;
import org.example.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestHandler extends Thread {

    private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);

    private Socket connection;

    public RequestHandler(final Socket connectionSocket) {
        this.connection = connectionSocket;
    }

    public void run() {
        log.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(),
                connection.getPort());

        try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {
            // 요구사항 1. index.html 응답
            BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)); // UTF-8 처리
            String line = br.readLine();
            if (line == null) { // 마지막 줄 EOF를 null로 읽어옴
                return;
            }

            // 요구사항 3. POST 방식으로 회원가입하기
            String path = HttpRequestUtils.getUrl(line);
            Map<String, String> headers = new HashMap<>();

            boolean logined = false;
            while (!line.isEmpty()) {
                log.debug("header : {}", line);
                line = br.readLine();

                String[] headerTokens = line.split(": ");
                if (headerTokens.length == 2) {
                    headers.put(headerTokens[0], headerTokens[1]); // content-length 구해서 넣기
                }
                if (line.contains("Cookie")) {
                    logined = isLogined(line);
                }
            }

            if (path.startsWith("/user/create")) {
                log.debug("path = {}", path);

                String requestBody = IOUtils.readData(br, Integer.parseInt(headers.get("Content-Length")));
                User user = createUser(requestBody);
                log.debug("user = {}", user);

                path = "/index.html";

                // 요구사항 4. 302 status code 적용
                DataOutputStream dos = new DataOutputStream(out);
                byte[] body = Files.readAllBytes(new File("./webapp" + path).toPath());

                response302Header(dos);
            } else if (path.equals("/user/login")) {
                // 요구사항 5. 로그인하기
                String requestBody = IOUtils.readData(br, Integer.parseInt(headers.get("Content-Length")));
                log.debug("Request Body = {}", requestBody);

                Map<String, String> params = HttpRequestUtils.parseQueryString(requestBody);
                log.debug("userId = {}, password = {}", params.get("userId"), params.get("password"));

                User findUser = DataBase.findUserById(params.get("userId"));
                if (findUser == null) {
                    log.debug("User Not Found!");
                    DataOutputStream dos = new DataOutputStream(out);
                    response302HeaderWithCookieLoginFail(dos, "logined=false");
                } else if (findUser.getPassword().equals(params.get("password"))) {
                    log.debug("Login Success!");
                    DataOutputStream dos = new DataOutputStream(out);
                    response302HeaderWithCookie(dos, "logined=true");
                } else {
                    log.debug("Password Mismatch");
                    DataOutputStream dos = new DataOutputStream(out);
                    response302HeaderWithCookieLoginFail(dos, "logined=false");
                }

            } else if (path.equals("/user/list")) {
                // 요구사항 6. 사용자 목록 출력
                if (logined) {
                    Collection<User> users = DataBase.findAll();
                    StringBuilder sb = new StringBuilder();
                    for (User user : users) {
                        sb.append(user.getUserId() + " : " + user.getName() + " : " + user.getEmail() + "<br/>");
                    }
                    byte[] body = sb.toString().getBytes();
                    DataOutputStream dos = new DataOutputStream(out);
                    response200Header(dos, body.length);
                    responseBody(dos, body);
                } else {
                    DataOutputStream dos = new DataOutputStream(out);
                    byte[] body = Files.readAllBytes(new File("./webapp/login.html").toPath());
                    response200Header(dos, body.length);
                    responseBody(dos, body);
                }
            } else if (path.endsWith(".css")) {
                // 요구사항 7. CSS 지원하기
                DataOutputStream dos = new DataOutputStream(out);
                byte[] body = Files.readAllBytes(new File("./webapp" + path).toPath());

                response200HeaderWithCss(dos, body.length);
                responseBody(dos, body);

            } else {
                DataOutputStream dos = new DataOutputStream(out);
                byte[] body = Files.readAllBytes(new File("./webapp" + path).toPath());

                response200Header(dos, body.length);
                responseBody(dos, body);
            }

        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private boolean isLogined(final String line) {
        String[] headerTokens = line.split(":");
        Map<String, String> cookies = HttpRequestUtils.parseCookies(headerTokens[1].trim());
        String value = cookies.get("logined");
        if (value == null) {
            return false;
        }
        return Boolean.parseBoolean(value);
    }

    private User createUser(final String queryString) {
        Map<String, String> params = HttpRequestUtils.parseQueryString(queryString);
        log.debug("requests = {}", params);

        User user = new User(
                params.get("userId"),
                params.get("password"),
                params.get("name"),
                params.get("email")
        );
        DataBase.addUser(user);
        return user;
    }

    private void response302Header(final DataOutputStream dos) {
        try {
            dos.writeBytes("HTTP/1.1 302 Found \r\n");
            dos.writeBytes("Location: /index.html\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void response302HeaderWithCookie(final DataOutputStream dos, final String cookie) {
        try {
            dos.writeBytes("HTTP/1.1 302 Found \r\n");
            dos.writeBytes("Location: /index.html\r\n");
            dos.writeBytes("Set-Cookie: " + cookie + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void response302HeaderWithCookieLoginFail(final DataOutputStream dos, final String cookie) {
        try {
            dos.writeBytes("HTTP/1.1 302 Found \r\n");
            dos.writeBytes("Location: /user/login_failed.html\r\n");
            dos.writeBytes("Set-Cookie: " + cookie + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void response200Header(final DataOutputStream dos, final int lengthOfBodyContent) {
        try {
            dos.writeBytes("HTTP/1.1 200 OK \r\n");
            dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void response200HeaderWithCss(final DataOutputStream dos, final int lengthOfBodyContent) {
        try {
            dos.writeBytes("HTTP/1.1 200 OK \r\n");
            dos.writeBytes("Content-Type: text/css;charset=utf-8\r\n");
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void responseBody(final DataOutputStream dos, final byte[] body) {
        try {
            dos.write(body, 0, body.length);
            dos.flush();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

}
