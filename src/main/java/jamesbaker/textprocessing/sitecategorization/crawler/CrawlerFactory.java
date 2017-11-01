package jamesbaker.textprocessing.sitecategorization.crawler;

import edu.uci.ics.crawler4j.crawler.CrawlController;
import jamesbaker.textprocessing.sitecategorization.classifier.ClassifierData;


public class CrawlerFactory implements CrawlController.WebCrawlerFactory<Crawler> {

    private ClassifierData classifierData;
    
    public CrawlerFactory(ClassifierData classifierData) {
    		this.classifierData = classifierData;
    }

    @Override
    public Crawler newInstance() throws Exception {
        return new Crawler(classifierData);
    }
}