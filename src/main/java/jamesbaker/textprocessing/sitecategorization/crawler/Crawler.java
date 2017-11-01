package jamesbaker.textprocessing.sitecategorization.crawler;

import java.util.regex.Pattern;

import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.parser.HtmlParseData;
import edu.uci.ics.crawler4j.url.WebURL;
import jamesbaker.textprocessing.sitecategorization.classifier.ClassifierData;

public class Crawler extends WebCrawler {

    private static final Pattern IGNORE_EXTENSIONS = Pattern.compile(".*\\.(bmp|gif|jpg|png|jpeg|mp3|css|js)$");

    private ClassifierData classifierData;
    
    public Crawler(ClassifierData classifierData) {
    		this.classifierData = classifierData;
    }
    
    @Override
    public boolean shouldVisit(Page referringPage, WebURL url) {
        String href = url.getURL().toLowerCase();

        if (IGNORE_EXTENSIONS.matcher(href).matches()) {
            return false;
        }

        return referringPage == null || url.getDomain().equals(referringPage.getWebURL().getDomain());
    }

    @Override
    public void visit(Page page) {
        if (page.getParseData() instanceof HtmlParseData) {
            HtmlParseData htmlParseData = (HtmlParseData) page.getParseData();
            
            classifierData.addData(htmlParseData.getText());
        }
    }
}