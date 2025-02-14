import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.File;

public class ScrapeTest {
    String filePath = "./java/testFiles/game9036.html";
    Document doc = Jsoup.connect(url).get();

    public void testGetCategories() {
        Document doc = Jsoup.parse()
    }

}
