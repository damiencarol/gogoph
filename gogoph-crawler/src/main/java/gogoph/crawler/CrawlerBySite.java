package gogoph.crawler;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Semaphore;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.store.SimpleFSDirectory;

public class CrawlerBySite {
	
	private static final Logger logger = Logger.getLogger(
			CrawlerBySite.class.getName());

	/**
	 * @param args
	 * @throws IOException
	 * @throws LockObtainFailedException
	 * @throws CorruptIndexException
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws CorruptIndexException,
			LockObtainFailedException, IOException, InterruptedException {

		// Set up a simple configuration that logs on the console.
        //BasicConfigurator.configure();
        DOMConfigurator.configure("log4j.conf.xml");
        
        // Basicaly how long in seconds the crawlers works
        int deep = Integer.parseInt(args[1]);
	    logger.info("Deep: " + deep);
		
	    // How many thread per site
	    int nbThread = Integer.parseInt(args[2]);
		logger.info("Threads: " + nbThread);
		
		// How many time a thread wait until 2 requests per site
	    int sleepingTimeThread = Integer.parseInt(args[3]);
		logger.info("Sleeping Time for Threads: " + sleepingTimeThread);
		

		
		
		// Connect to the index
		Directory dir = new SimpleFSDirectory(new File(args[0]));
		Semaphore semIndex = new Semaphore(1);
		
		
		
        // Load seeds from "sites" file
        File textFile = new File("sites");
        ArrayList<String> tab_host = new ArrayList<String>();
        ArrayList<Integer> tab_port = new ArrayList<Integer>();
        BufferedReader dis = null;
	    try {
	      dis = new BufferedReader(new FileReader(textFile));
	      String line;
	      while ((line = dis.readLine()) != null) {

	    	  tab_host.add(line.split(":")[0]);
	    	  tab_port.add(new Integer(line.split(":")[1]));
		  }
	    } catch(Exception e)
	    { logger.error(e);
	    return;
	    }

       

		// Create list off CrawlerSite
		HashMap<String, CrawlerSite> sites = new HashMap<String, CrawlerSite>();
		Semaphore mutexSites = new Semaphore(1);
		
		// For each sites add the root
		for (int index = 0; index < tab_host.size(); index++)
		{
			CrawlerSite site = new CrawlerSite(tab_host.get(index), tab_port.get(index));
			
			CrawlerSiteNode node = new CrawlerSiteNode("1", "Root", "");
			site.putNode(node);
			
			sites.put(tab_host.get(index) + ":" + tab_port.get(index), site);
		}
		
		// For each sites add N thread which start crawling
		ArrayList<Thread> crawlers = new ArrayList<Thread>();
		ArrayList<CrawlerSiteThread> cruns = new ArrayList<CrawlerSiteThread>();
		for (CrawlerSite site : sites.values())
		{
			for (int nb = 0; nb < nbThread; nb++)
			{
				CrawlerSiteThread crun = new CrawlerSiteThread(dir, semIndex, site, sites, mutexSites, sleepingTimeThread);
				Thread thread = new Thread(crun);
				thread.start();
				
				crawlers.add(thread);
				cruns.add(crun);
			}
		}
		
		
		
		
		
		
		/*ArrayList<GopherDirectoryEntity> list = new ArrayList<GopherDirectoryEntity>();
		ArrayList<String> blackList = new ArrayList<String>();
		blackList.add("gopher.wensley.org.uk:70/env"); // this node crash !
		blackList.add("www.redhill.net.nz:70/phlog/germ.cgi"); // this node crash !
		
		blackList.add("127.0.0.1:70/"); // useless
		blackList.add("127.0.0.1:70"); // useless

		// Creation du seed
		Queue<GopherDirectoryEntity> pool = new LinkedList<GopherDirectoryEntity>();
		
		// Add 	Floodgap root !!!
		{
			GopherDirectoryEntity root = new GopherDirectoryEntity();		
			root.setType("1");
			root.setUsername("Floodgap mother homeland Komrad!");
			root.setSelector("/new");
			root.setHost("gopher.floodgap.com");
			root.setPort(70);
			
			pool.add(root);
		}*/

		
		
		
		
		
	

		// Wait deep seconds
		//for (int i = 0; i < crawlers.size(); i++)
		//	crawlers.get(i).start();
		
		Thread.sleep(deep * 1000);

		for (CrawlerSiteThread crun : cruns)
		{
			crun.setExit(true);
		}
		logger.info("Waiting thread stop...");
		while (!crawlers.isEmpty()) {
			Thread cr = crawlers.remove(0);
			cr.join(5000);
			cr.interrupt();
		}
		logger.info("Thread stop OK.");
		
		
		// Close directory
		dir.close();
		

		// Print visited sites
		for (CrawlerSite site : sites.values())
		{
			logger.debug(site.getHost() + ":" + site.getPort() +
					" | " + site.getNbSelector() + " selector(s)" +
			        " | " + site.getNbVisited() + " visited");
		}
		
		
		// Save on file 'discover'
		{
		File discovertextFile = new File("sites.discover");
        BufferedWriter disc = null;
	    try {
	      disc = new BufferedWriter(new FileWriter(discovertextFile));
	      for (CrawlerSite site : sites.values())
			{
	    	  disc.write(site.getHost().trim().toLowerCase() + ":" + site.getPort() + "\r\n");
			}
	      disc.flush();
	      disc.close();
	    } catch(Exception e)
	    { logger.error(e);
	    return;
	    }
		}
	    
	    
	    // Create crawler root
		File crawlDir = new File("crawler");
		if (!crawlDir.exists())
		{
			crawlDir.mkdir();
		}
		// Create site dir
		for (CrawlerSite site : sites.values())
		{
			if (site.getNbVisited() > 0 || site.getErrors().size() > 0)
			{
				site.saveCrawlerSiteInfo(crawlDir.getPath() + File.separator + 
						(site.getHost() + "_" + site.getPort()).replace(File.separator, "_"));
			}
		}
		
		// Update site file
		// Save on file 'discover'
		{
		File discovertextFile = new File("sites");
        BufferedWriter disc = null;
        disc = new BufferedWriter(new FileWriter(discovertextFile));
        for (CrawlerSite site : sites.values())
		{
			  if (site.getNbSelector() > 0)
				  try {
						  if (InetAddress.getByName(site.getHost()) != null)
						  disc.write(site.getHost().trim().toLowerCase() + ":" + site.getPort() + "\r\n");
					  } catch(Exception e)
					 { logger.error(e);}
				}
			  disc.flush();
			  disc.close();
		}
	}
}
