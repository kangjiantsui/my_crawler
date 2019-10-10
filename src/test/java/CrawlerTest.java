import cn.kang.dao.ImageDao;
import cn.kang.model.Image;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

public class CrawlerTest {

    private ImageDao mapper;

    @Before
    public void init() throws IOException {
        Properties prop = new Properties();
        try (InputStream fis = Resources.getResourceAsStream("test.properties");
             InputStream inputStream = Resources.getResourceAsStream("mybatis.xml")) {
            prop.load(fis);
            SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream, "development", prop);
            SqlSession sqlSession = sqlSessionFactory.openSession();
            mapper = sqlSession.getMapper(ImageDao.class);
        }
    }

    @Test
    public void demo1() {
        getComicsByUrl("http://www.zerobyw4.com/plugin.php?id=jameson_manhua&a=bofang&kuid=1657");
    }

    public void getComicsByUrl(String allPageUrl) {
        var list = Collections.synchronizedList(new ArrayList<Image>());
        var loginUrl = "http://www.zerobyw4.com/member.php?mod=logging&action=login&loginsubmit=yes&frommessage&loginhash=LPH5e&inajax=1";
        var formBody = "username=kangjiantsui&password=Iamwinner!";
        HttpClient httpClient = HttpClient.newBuilder().build();
        HttpRequest loginRequest = HttpRequest.newBuilder()
                .uri(URI.create(loginUrl))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:50.0) Gecko/20100101 Firefox/50.0")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formBody))
                .build();
        httpClient.sendAsync(loginRequest, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::headers)
                .thenAccept(headers -> {
                    var temp = headers.map().get("set-cookie").toString().replaceAll(",", ";");
                    var cookieMap = new HashMap<String, String>();
                    Arrays.stream(temp.split(";"))
                            .map(String::trim)
                            .filter(e -> e.startsWith("kd5S_2132_saltkey")
                                    || e.startsWith("kd5S_2132_lip")
                                    || e.startsWith("kd5S_2132_ulastactivity")
                                    || e.startsWith("kd5S_2132_sid")
                                    || e.startsWith("kd5S_2132_lastact")
                                    || e.startsWith("kd5S_2132_lastcheckfeed")
                                    || e.startsWith("kd5S_2132_auth")
                                    || e.startsWith("kd5S_2132_tshuz_accountlogin")
                                    || e.startsWith("kd5S_2132_lastvisit")
                            )
                            .map(e -> e.split("="))
                            .forEach(e -> cookieMap.put(e[0], e[1]));
                    var cookie = new StringBuilder();
                    cookieMap.forEach((k, v) -> cookie.append(k).append("=").append(v).append(";"));
                    var allPageRequest = HttpRequest.newBuilder()
                            .uri(URI.create(allPageUrl))
                            .header("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:50.0) Gecko/20100101 Firefox/50.0")
                            .GET()
                            .build();
                    httpClient.sendAsync(allPageRequest, HttpResponse.BodyHandlers.ofString())
                            .thenApply(HttpResponse::body)
                            .thenAccept(body -> {
                                Document document = Jsoup.parse(body);
                                document.getElementsByClass("muludiv").parallelStream().forEach(e -> {
                                    var chapter = e.getElementsByClass("uk-button uk-button-default").text();
                                    var pageUrl = e.getElementsByClass("uk-button uk-button-default").attr("href").replaceAll("./", document.baseUri());
                                    var pageRequest = HttpRequest.newBuilder()
                                            .uri(URI.create(pageUrl))
                                            .header("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:50.0) Gecko/20100101 Firefox/50.0")
                                            .header("Cookie", cookie.toString())
                                            .GET()
                                            .build();
                                    httpClient.sendAsync(pageRequest, HttpResponse.BodyHandlers.ofString())
                                            .thenApply(HttpResponse::body)
                                            .thenAccept(subBody -> {
                                                Document subDocument = Jsoup.parse(subBody);
                                                var zjimg = subDocument.getElementsByClass("zjimg text-center mt0 mb0").size() != 0
                                                        ? subDocument.getElementsByClass("zjimg text-center mt0 mb0")
                                                        : subDocument.getElementsByClass("uk-text-center mb0");
                                                zjimg.forEach(f -> {
                                                    String imgSrc = f.getElementsByTag("img").attr("src");
                                                    list.add(new Image("Onepunch Man", Integer.valueOf(chapter), imgSrc));
//                                                    System.out.println(imgSrc);
                                                });
                                            }).join();
                                });
                            }).join();
                }).join();
        System.out.println(list.size());
        mapper.addImageList(list);
    }

    @Test
    public void demo2() {
        mapper.addImage(new Image("haha", 1, "heihei"));
    }
}
