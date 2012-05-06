package gogoph.crawler.gen3;

import java.io.File;

import org.apache.log4j.Logger;

import gogoph.crawler.CrawlerSite;
import gogoph.crawler.GopherClient;

public class SeedThread implements Runnable {

	private static final Logger logger = Logger.getLogger(
			SeedThread.class.getName());
	
	private CrawlerSite site;

	private boolean ok;
	
	public SeedThread(CrawlerSite site) {
		super();
		this.site = site;
	}

	@Override
	public void run() {
		try {
		logger.info("Crawling root of [" + site.getHost() + "][" +  site.getPort() + "]...");
    	File rawFile = GopherClient.requestBinaryfile(site.getHost(), site.getPort(), "");
    	if (rawFile == null)
		{
			logger.error(">>> ERROR IN CRAWL : [" + site.getHost() + "][" +  site.getPort() + "][" + "0" + "][" + "" + "]");
			ok = false;
		}
		else
		{
			logger.info(">>> CRAWLED : [" + site.getHost() + "][" +  site.getPort() + "][" + "0" + "][" + "" + "]");
			ok = true;
		}
		} catch (Exception e)
		{
			logger.error(e);
		}
	}

	public boolean isOk() {
		return ok;
	}

	public CrawlerSite getSite() {
		return site;
	}

	public void setSite(CrawlerSite site) {
		this.site = site;
	}

}
