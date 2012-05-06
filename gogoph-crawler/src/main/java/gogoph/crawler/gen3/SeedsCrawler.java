package gogoph.crawler.gen3;

import gogoph.crawler.CrawlerSite;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

public class SeedsCrawler {

	private static final Logger logger = Logger.getLogger(
			SeedsCrawler.class.getName());

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		
		// Set up a simple configuration that logs on the console.
        //BasicConfigurator.configure();
        DOMConfigurator.configure("log4j.conf.xml");
        
		// Create crawler root
		File crawlDir = new File("crawler");
		if (!crawlDir.exists())
		{
			crawlDir.mkdir();
		}
		
		// Load seeds from "sites" file
        File textFile = new File(crawlDir.getPath() + File.separator + "sites");
        textFile.createNewFile();
        ArrayList<String> tab_host = new ArrayList<String>();
        ArrayList<Integer> tab_port = new ArrayList<Integer>();
        BufferedReader dis = null;
	    try {
	      dis = new BufferedReader(new FileReader(textFile));
	      String line;
	      while ((line = dis.readLine()) != null) {

	    	  tab_host.add(line.split(":")[0]);
	    	  if (line.split(":").length > 1)
	    		  tab_port.add(new Integer(line.split(":")[1]));
	    	  else
	    		  tab_port.add(70);
		  }
	    } catch(Exception e)
	    { 
	    	logger.error(e);
	    	return;
	    }
	    
	    HashMap<String, CrawlerSite> crawledSites = new HashMap<String, CrawlerSite>();
	    ArrayList<Thread> threads = new ArrayList<Thread>();
	    ArrayList<SeedThread> seedRun = new ArrayList<SeedThread>();
	    
	    // For each seed we add or delete the folder
	    for (int i = 0; i < tab_host.size(); i++)
	    {
	    	String host = tab_host.get(i);
	    	int port = tab_port.get(i);
	    	SeedThread sdth = new SeedThread(new CrawlerSite(host, port));
	    	Thread th = new Thread(sdth);
	    	th.start();
	    	
	    	threads.add(th);
	    	seedRun.add(sdth);
	    }
	    for (int i = 0; i < tab_host.size(); i++)
	    {
	    	String host = tab_host.get(i);
	    	int port = tab_port.get(i);
	    	Thread th = threads.get(i);
	    	SeedThread sdth = seedRun.get(i);
	    	try {
				th.join();
				File folder = new File(crawlDir.getPath() + File.separator + host + ":" +  port);
				if (!sdth.isOk())
				{
					if (folder.exists())
						folder.delete();
				}
				else
				{
					if (!folder.exists())
						folder.mkdir();
					crawledSites.put(host + ":" +  port, sdth.getSite());
				}
			} catch (InterruptedException e) {
				logger.error(e);
			}
	    	
	    }
	    
	    // Update site file
		// Save on file 'sites.crawled'
		{
		File discovertextFile = new File(crawlDir.getPath() + File.separator + "sites.crawled");
        BufferedWriter disc = null;
        disc = new BufferedWriter(new FileWriter(discovertextFile));
        for (CrawlerSite site : crawledSites.values())
		{					  
			disc.write(site.getHost().trim().toLowerCase());
			if (site.getPort() != 70)
				disc.write(":" + site.getPort());
			disc.write("\r\n");			  
		}
        disc.flush();
        disc.close();
		}
	}

}
