package jamesbaker.textprocessing.sitecategorization;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.StringJoiner;

import org.apache.commons.codec.digest.DigestUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.google.common.io.Files;

import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.fetcher.PageFetcher;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtConfig;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtServer;
import jamesbaker.textprocessing.sitecategorization.classifier.ClassifierData;
import jamesbaker.textprocessing.sitecategorization.crawler.CrawlerFactory;
import opennlp.tools.doccat.DoccatModel;
import opennlp.tools.doccat.DocumentCategorizerME;
import opennlp.tools.doccat.DocumentSample;
import opennlp.tools.doccat.DocumentSampleStream;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.PlainTextByLineStream;

public class SiteCategorization {

	public static void main(String[] args) throws Exception {
		if(args.length != 3)
			return;
		
		File modelFile = new File(DigestUtils.md5Hex(args[0] + args[1]) +".bin");
		DoccatModel model = null;

		if(!modelFile.exists()) {
			ClassifierData cdIn = new ClassifierData("in");
			ClassifierData cdOut = new ClassifierData("out");
			File tempFolder = Files.createTempDir();
	
			CrawlConfig configIn = new CrawlConfig();
			CrawlConfig configOut = new CrawlConfig();
	
			configIn.setCrawlStorageFolder(new File(tempFolder, "in").getAbsolutePath());
			configOut.setCrawlStorageFolder(new File(tempFolder, "out").getAbsolutePath());
	
			configIn.setPolitenessDelay(1000);
			configOut.setPolitenessDelay(1000);
	
			configIn.setMaxPagesToFetch(200);
			configOut.setMaxPagesToFetch(200);
	
			PageFetcher pageFetcherIn = new PageFetcher(configIn);
			PageFetcher pageFetcherOut = new PageFetcher(configOut);
	
			RobotstxtConfig robotstxtConfig = new RobotstxtConfig();
			RobotstxtServer robotstxtServerIn = new RobotstxtServer(robotstxtConfig, pageFetcherIn);
			RobotstxtServer robotstxtServerOut = new RobotstxtServer(robotstxtConfig, pageFetcherOut);
	
			CrawlController controllerIn = new CrawlController(configIn, pageFetcherIn, robotstxtServerIn);
			CrawlController controllerOut = new CrawlController(configOut, pageFetcherOut, robotstxtServerOut);
	
			Files.readLines(new File(args[0]), StandardCharsets.UTF_8).forEach(controllerIn::addSeed);
			Files.readLines(new File(args[1]), StandardCharsets.UTF_8).forEach(controllerOut::addSeed);
	
			controllerIn.startNonBlocking(new CrawlerFactory(cdIn), 5);
			controllerOut.startNonBlocking(new CrawlerFactory(cdOut), 5);
	
			controllerIn.waitUntilFinish();
			controllerOut.waitUntilFinish();
	
			StringJoiner sj = new StringJoiner("\n");
	
			for(String s : cdIn.getData()) {
				sj.add("in\t" + s.replaceAll("[\r\n]", " "));
			}
			for(String s : cdOut.getData()) {
				sj.add("out\t" + s.replaceAll("[\r\n]", " "));
			}
	
			try (InputStream data = new ByteArrayInputStream(sj.toString().getBytes(StandardCharsets.UTF_8))) {
				ObjectStream<String> lineStream = new PlainTextByLineStream(data, StandardCharsets.UTF_8);
				ObjectStream<DocumentSample> sampleStream = new DocumentSampleStream(lineStream);
	
				model = DocumentCategorizerME.train("en", sampleStream);
			}
			
			
			OutputStream modelOut = null;
			try {
				modelOut = new BufferedOutputStream(new FileOutputStream(modelFile));
				model.serialize(modelOut);
			}catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (modelOut != null) {
					try {
						modelOut.close();
					}
					catch (IOException e) {
						// Failed to correctly save model.
						// Written model might be invalid.
						e.printStackTrace();
					}
				}
			}
		} else {
			model = new DoccatModel(modelFile);
		}
		
		DocumentCategorizerME categorizer = new DocumentCategorizerME(model);
		
		Files.readLines(new File(args[2]), StandardCharsets.UTF_8).forEach(s -> {
			String inputText = readUrl(s);
			
			double[] outcomes = categorizer.categorize(inputText);
			String category = categorizer.getBestCategory(outcomes);
			
			System.out.println(s + "  ::  " + category);
		});
	}

	private static String readUrl(String url) {
		try {
			Document doc = Jsoup.connect(url).get();
			return doc.body().text();
		} catch (IOException e) {
			return "";
		}
	}
}
