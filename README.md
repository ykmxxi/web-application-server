# web-application-server

## 📌 요구사항
### 1. index.html 응답하기
- http://localhost:8080/index.html 접속했을 때 webapp 디렉토리의 index.html 파일을 읽어 클라이언트에 응답
- `BufferedReader`, `Files.readAllBytes()`
- 별도의 Util로 분리해 재사용

### 2. GET 방식으로 회원가입하기
- http://localhost:8080/user/form.html 이동하면서 회원가입
- 회원가입을 하면 다음과 같은 형태로 사용자가 입력한 값이 서버에 전달
  - `GET /user/create?userId=javajigi&password=password&name=JaeSung HTTP/1.1`    
- key=value 형태의 값을 User에 담는다
  - key=value 파싱은 `HttpRequestUtils` 클래스의 `parseQueryString()` 활용
- 요청 URL과 key=value 값을 분리해야 한다
  - `?` 기준 split
  - url: `/user/create`
  - query: `userId=javajigi&password=password&...`

### 3. POST 방식으로 회원가입하기
- http://localhost:8080/user/form.html 파일의 form 태그 method를 post로 변경
- HTTP Header와 Body는 아래와 같음
```text
POST /user/create HTTP/1.1
Host: localhost:8080
Connection: keep-alive
Content-Length: 59
Content-Type: application/x-www-form-urlencoded
Accept: */*

userId=javajigi&password=password&name=JaeSung
```
- POST 요청시 데이터는 Body에 존재, HTTP Header 이후 빈 공백을 가지는 한 줄 다음부터 시작
- Body는 `HttpRequestUtils.readData()` 활용, content-length는 본문의 길이
  - Http Header에서 Content-Length 구해서 메소드를 통해 Body 가져오기

### 4. 302 status code 적용
### 5. 로그인하기
### 6. 사용자 목록 출력
### 7. CSS 지원
