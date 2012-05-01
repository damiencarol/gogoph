package gogoph.crawler;

import gogoph.GopherDirectoryEntity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Semaphore;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.store.SimpleFSDirectory;

public class Crawler {
	
	private static final Logger logger = Logger.getLogger(
			Crawler.class.getName());

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
        
        
        // Load sites from file "sites"
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

        
    
		

		

		ArrayList<GopherDirectoryEntity> list = new ArrayList<GopherDirectoryEntity>();
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
			
			logger.info("Add gopherspace : " + "gopher.floodgap.com/1/new");
			pool.add(root);
		}

		// Add with list
		for (int i = 0; i < tab_host.size(); i++)
		{
			try {
			GopherDirectoryEntity root = new GopherDirectoryEntity();
			root.setType("1");
			root.setUsername(InetAddress.getByName(tab_host.get(i)).getHostAddress());
			root.setSelector("");
			root.setHost(tab_host.get(i));
			root.setPort(tab_port.get(i));
			
			logger.info("Add gopherspace : " + tab_host.get(i));
			pool.add(root);
			} catch ( java.net.UnknownHostException ex)
			{
				logger.error(ex);
			}
		}
		
		
		
		
	    int deep = Integer.parseInt(args[1]);
	    int nbThread = Integer.parseInt(args[2]);
		
		logger.info("Deep: " + deep);
		logger.info("Threads: " + nbThread);
		


		
		
		// 1. create the index
		Directory dir = new SimpleFSDirectory(new File(args[0]));
		//boolean created = !(new File(args[0]).exists());
		//dir.setLockFactory(new MyLockFactory());

		Semaphore semPool = new Semaphore(1);
		Semaphore semKnow = new Semaphore(1);
		Semaphore semIndex = new Semaphore(1);

		// Create a new crawler thread
		ArrayList<Thread> listThread = new ArrayList<Thread>();
		for (int j = 0; j < nbThread; j++) {
			Thread thread = new Thread(new CrawlerQueueThread(pool,
					semPool, 1 + (deep / nbThread), list, semKnow,
					blackList, dir, semIndex));
				thread.start();
				
			listThread.add(thread);
			System.out.println("Thread start " + j);
			
			Thread.sleep(200);
		}

		

		for (int j = 0; j < nbThread; j++) {
			listThread.get(j).join();
		}


		// Print visited sites
		ArrayList<String> listVisit = new ArrayList<String>();
		for (int i = 0; i < list.size(); i++) 
		{
			String forma = list.get(i).getHost() + ":" + list.get(i).getPort();
			if (list.get(i).getSelector().trim().equals("") ||
					list.get(i).getSelector().trim().equals("/")) {
				if (!listVisit.contains(forma))
					listVisit.add(forma);
			}
		}
		for (int i = 0; i < listVisit.size(); i++) 
		{
			System.out.println(listVisit.get(i));
		}
		return;
	}

}
