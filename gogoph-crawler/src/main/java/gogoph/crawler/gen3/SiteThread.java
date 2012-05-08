package gogoph.crawler.gen3;

import gogoph.crawler.CrawlerSiteNode;
import gogoph.crawler.GopherClient;
import gogoph.crawler.GopherDirectoryEntity;

import java.io.File;
import java.util.ArrayList;

import org.apache.log4j.Logger;

public class SiteThread implements Runnable {
	
	private static final Logger logger = Logger.getLogger(
			SiteThread.class.getName());

	public SiteThread(String host, int port, CrawlerSiteNode site) {
		super();
		this.host = host;
		this.port = port;
		this.node = site;
	}

	private String host;
	private int port;
	private CrawlerSiteNode node;
	private boolean ok;
	private ArrayList<GopherDirectoryEntity> nodes;
	
	@Override
	public void run() {
		try {
			
			if (node.getType().equals("1")) {
				crawlMenu();
				return;
			}

			ok = true;
			return;	
		
		}
		catch (Exception e)
		{
			ok = false;
			logger.error(e);
		}
	}

	private void crawlMenu() {
		
		File rawfile = GopherClient.requestBinaryfile(host, port, node.getSelector());
		if (rawfile == null) {
			logger.error(">>> ERROR IN CRAWL : [" + host + "][" + port + "]["
					+ node.getType() + "][" + node.getSelector() + "]");
			ok = false;
	    	return;
		}
		
		CrawlerSiteNode root = node;
		// Get content of menu
		ArrayList<GopherDirectoryEntity> tab;
	    tab = GopherClient.readFromFile(rawfile);
	    
	    if (tab == null)
	    {
	    	logger.error(">>> ERROR IN CRAWLED : [" + host + "][" + port + "][" + root.getType() + "][" + root.getSelector() + "]");
		    ok = false;
	    	return;
	    }
	    logger.info(">>> CRAWLED : [" + host + "][" + port + "][" + root.getType() + "][" + root.getSelector() + "]");
	    
	    rawfile.deleteOnExit();
	    this.nodes = tab;
	    ok = true;
	}

	public boolean isOk() {
		return ok;
	}

	public ArrayList<GopherDirectoryEntity> getNodes() {
		return nodes;
	}

}
