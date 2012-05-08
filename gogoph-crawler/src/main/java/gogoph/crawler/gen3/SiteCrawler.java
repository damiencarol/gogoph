package gogoph.crawler.gen3;

import gogoph.crawler.CrawlerSiteNode;
import gogoph.crawler.GopherClient;
import gogoph.crawler.GopherDirectoryEntity;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

public class SiteCrawler {

	private static final Logger logger = Logger.getLogger(
				SiteCrawler.class.getName());
		
	private static final String portSep = ":";

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {

		
		// Set up a simple configuration that logs on the console.
        //BasicConfigurator.configure();
        DOMConfigurator.configure("log4j.conf.xml");
		
		String host;
		int port;
		if (args[0].contains(":"))
		{
			port = Integer.parseInt(args[0].split(":")[1]);
			host = args[0].split(":")[0];
		}
		else
		{
			port = 70;
			host = args[0];
		}
		int sleepingtime = Integer.parseInt(args[1]);

    	logger.info("HOST:" + host);
    	logger.info("PORT:" + port);
    	logger.info("TIME:" + sleepingtime);
 		
    	 
		// Create crawler root
		File crawlDir = new File("crawler");
		if (!crawlDir.exists())
		{
			crawlDir.mkdir();
		}
		
		// Get site dir
    	File folder = new File(crawlDir.getPath() + File.separator + host + portSep +  port);
    	if (!folder.exists())
		{
    		folder.mkdir();
		}
    	
    	// Try to read selector file
    	File textFile = new File(folder.getPath() + File.separator + "selectors");
    	if (!textFile.exists())
    		textFile.createNewFile();
        HashMap<String, CrawlerSiteNode> tab_selectors = new HashMap<String, CrawlerSiteNode>();
        BufferedReader dis = null;
	    try {
	      dis = new BufferedReader(new FileReader(textFile));
	      String line;
	      while ((line = dis.readLine()) != null) {
	    	  CrawlerSiteNode node = new CrawlerSiteNode(line.split("\t")[1], 
	    			  line.split("\t")[2], line.split("\t")[0]);

	    	  if (!tab_selectors.containsKey(line.split("\t")[0].trim()))
	    	  {
	  	    		tab_selectors.put(node.getSelector(), node);
	  	    		logger.info("SELECTOR:" + node.getSelector());
	    	  }
		  }
	    } catch(Exception e)
	    { 
	    	logger.error(e);
	    	return;
	    }
	    
	    // Hack if there no main selector
	    if (!tab_selectors.containsKey("") && 
	    		GopherClient.requestBinaryfile(host, port, "") != null)
  	  	{
	    	CrawlerSiteNode node = new CrawlerSiteNode("1", "ROOT", "");
	    	tab_selectors.put(node.getSelector(), node);
  		  	logger.info("SELECTOR:" + node.getSelector());
  	  	}
	    // Hack if there no caps selector
	    if (!tab_selectors.containsKey("/caps.txt")&& 
	    		GopherClient.requestBinaryfile(host, port, "/caps.txt") != null)
  	  	{
	    	CrawlerSiteNode node = new CrawlerSiteNode("0", "CAPS", "/caps.txt");
	    	tab_selectors.put(node.getSelector(), node);
  		  	logger.info("SELECTOR:" + node.getSelector());
  	  	}
	    // Hack if there no ABOUT selector
	    if (!tab_selectors.containsKey("/about.txt") && !tab_selectors.containsKey("about.txt")
	    		&& !tab_selectors.containsKey("/ABOUT.txt") && !tab_selectors.containsKey("ABOUT.txt"))
  	  	{
	    	CrawlerSiteNode node = new CrawlerSiteNode("0", "ABOUT", "/about.txt");
	    	tab_selectors.put(node.getSelector(), node);
  		  	logger.info("SELECTOR:" + node.getSelector());
  	  	}
	    
	    HashMap<String, CrawlerSiteNode> crawledSelectors = new HashMap<String, CrawlerSiteNode>();
	    for (String key : tab_selectors.keySet())
	    {
	    	CrawlerSiteNode node = tab_selectors.get(key);
	    	SiteThread sith = new SiteThread(host, port, node);
	    	Thread th = new Thread(sith);
	    	th.start();
	    	try {
				th.join();
				if (sith.isOk())
				{
					// If it's menu seed it !
					if (node.getType().equals("1"))
					{
						for (GopherDirectoryEntity item : sith.getNodes())
						{
							if (!item.getType().equals("i"))
							{
								seed(crawlDir.getPath(), item);								
							}
						}
					}
					
					crawledSelectors.put(node.getSelector(), node);
				}
				Thread.sleep(sleepingtime);
			} catch (InterruptedException e) {
				logger.error(e);
			}			
	    }
	    
	    // Update site file
		// Save on file 'selectors.crawled'
		{
		File discovertextFile = new File(folder.getPath() + File.separator + "selectors.crawled");
        BufferedWriter disc = null;
        disc = new BufferedWriter(new FileWriter(discovertextFile));
        for (CrawlerSiteNode node : crawledSelectors.values())
		{	
        	disc.write(node.getSelector() + "\t" + node.getType() + "\t" + node.getUsername() + "\r\n");		  
		}
        disc.flush();
        disc.close();
		}
	}

	private static void seed(String pathfile, GopherDirectoryEntity node) throws IOException {
		
		logger.info("SEEDING [" + node.getHost() + "][" + node.getPort() + "][" + node.getType() + "][" + node.getSelector() + "]");
		File folder = new File(pathfile + File.separator + node.getHost() + portSep + node.getPort());
		if (!folder.exists())
			folder.mkdir();
		File file = new File(folder.getPath() + File.separator + "selectors");
		BufferedWriter disc = null;
        disc = new BufferedWriter(new FileWriter(file, true));
        disc.append(node.getSelector() + "\t" + node.getType() + "\t" + node.getUsername() + "\r\n");
        disc.flush();
        disc.close();
	}

	/*private static void saveContent(String pathfile, String content) throws IOException {
		logger.info("SAVING CONTENT '" + pathfile + "' size=" + content.length());
		File file = new File(pathfile);
		file.createNewFile();
        BufferedWriter disc = null;
        disc = new BufferedWriter(new FileWriter(file));
        disc.write(content);
        disc.flush();
        disc.close();
	}*/

}
