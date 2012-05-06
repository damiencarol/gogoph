package gogoph.crawler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Semaphore;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.util.Version;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.ContentHandler;

public class CrawlerSiteThread implements Runnable {

	private static final Logger logger = Logger
			.getLogger(CrawlerSiteThread.class.getName());
	private boolean exit;
	private CrawlerSite site;
	private HashMap<String, CrawlerSite> sites;
	private Semaphore mutexSites;
	private Directory directory;
	private Semaphore mutexIndex;
	private int sleepingTime;

	public CrawlerSiteThread(Directory direct, Semaphore mutexIndex, CrawlerSite site, HashMap<String, CrawlerSite> sites, Semaphore mutexSites, int sleepingTime) {
		this.directory = direct;
		this.mutexIndex = mutexIndex;
		this.site = site;
		this.sites = sites;
		this.mutexSites = mutexSites;
		this.sleepingTime = sleepingTime;
	}

	public void run() {
		int nbNoSelector = 0;
		while (!exit) {
			try {			
				Thread.sleep(sleepingTime);

				// Get node
				CrawlerSiteNode node = site.visit();
				
				if (node == null)
				{
					nbNoSelector++;
					//logger.warn("No selector for site " + site.getHost() + ":" + site.getPort() + " ! (" + nbNoSelector + ")");
					Thread.sleep(1000);
					
					if (nbNoSelector > 10)
					{
						exit = true;
						logger.info("Too much empty pool. Exiting...");
					}
				}
				else
				{
					nbNoSelector = 0;
					crawl(directory, node);
				}
				
			} catch (InterruptedException e) {
				logger.error(e);
			}
		}
	}

	private void crawl(Directory direct, CrawlerSiteNode node) {
		
		GopherDirectoryEntity root = new GopherDirectoryEntity();
		root.setExtra(null);
		root.setHost(site.getHost());
		root.setPort(site.getPort());
		root.setSelector(node.getSelector());
		root.setType(node.getType());
		root.setUsername(node.getUsername());
		
		// If it's a Menu
		if (root.getType().equals("1"))
		{
			crawlMenu(direct, root);
			return;
		}
		else if (root.getType().equals("h"))
		{
			if (root.getSelector() == null)
				return;
			else if (root.getSelector().startsWith("URL:"))
				return;
			else if (root.getSelector().startsWith("/URL:"))
				return;

			crawlGeneric(direct, node);
			return;
		}
		else if (root.getType().equals("0"))
			crawlGeneric(direct, node);

		else if (root.getType().equals("d"))
			crawlGeneric(direct, node);
		
		else if (root.getType().equals("I"))
			crawlGeneric(direct, node);
		
		else
			{
			deleteDocInternal(direct, site.getHost(), site.getPort(), node.getSelector());
			addDocInternal(direct, node.getType(), node.getUsername(), site.getHost(), site.getPort(), node.getSelector(), node.getSelector());   
			
			}
	}

	private void crawlGeneric(Directory direct, CrawlerSiteNode node) {
		
		// Get raw file
		File rawFile = GopherClient.requestBinaryfile(site.getHost(), site.getPort(), node.getSelector());
		if (rawFile == null)
		{
			logger.error(">>> ERROR IN CRAWL : [" + site.getHost() + "][" + site.getPort() + "][" + node.getType() + "][" + node.getSelector() + "]");
			return;
		}
		
		logger.info(">>> CRAWLED : [" + site.getHost() + "][" + site.getPort() + "][" + node.getType() + "][" + node.getSelector() + "]");
		
		String content = node.getUsername();
		//Reader content = new StringReader(node.getUsername());
		try {
			FileInputStream is = null;
		    try {
		      is = new FileInputStream(rawFile);

		      ContentHandler contenthandler = new BodyContentHandler();
		      Metadata metadata = new Metadata();
		      metadata.set(Metadata.RESOURCE_NAME_KEY, rawFile.getName());
		      Parser parser = new AutoDetectParser();
		      parser.parse(is, contenthandler, metadata, new ParseContext());
		      
		      for (String str : metadata.names())
		      {
		    	  if (metadata.isMultiValued(str))
		    	  {
		    		  logger.debug("[" + str + "] is multi value");
			    	  for (String sub : metadata.getValues(str))
		    		  {
		    			  logger.debug("[" + str + "]='" + sub + "'");
		    		  }
		    	  }
		    	  else
		    	  {
		    		  logger.debug("[" + str + "]='" + metadata.get(str) + "'");
		    	  }	    	  
		      }
		      //content += " " + contenthandler.toString();
		      content = contenthandler.toString();
		    }
		    catch (Exception e) {
		      e.printStackTrace();
		    }
		    finally {
		        if (is != null) is.close();
		    }
		
		if (content == null)
		{
			logger.error(">>> ERROR IN CRAWL : [" + site.getHost() + "][" + site.getPort() + "][" + node.getType() + "][" + node.getSelector() + "]");
			return;
			
		}
		
		logger.info(">>> CRAWLED : [" + site.getHost() + "][" + site.getPort() + "][" + node.getType() + "][" + node.getSelector() + "]");
			

		deleteDocInternal(direct, site.getHost(), site.getPort(), node.getSelector());
		addDocInternal(direct, node.getType(), node.getUsername(), site.getHost(), site.getPort(), node.getSelector(), content);   
		
		} catch (FileNotFoundException e) {
			logger.error(e);
			logger.error(">>> ERROR IN CRAWL : [" + site.getHost() + "][" + site.getPort() + "][" + node.getType() + "][" + node.getSelector() + "]");
	    } catch (IOException e) {
			 logger.error(e);
			 logger.error(">>> ERROR IN CRAWL : [" + site.getHost() + "][" + site.getPort() + "][" + node.getType() + "][" + node.getSelector() + "]");
		}
	}




	private void crawlMenu(Directory direct, GopherDirectoryEntity root) {
		
		// Get content of menu
		ArrayList<GopherDirectoryEntity> tab;
	    tab = GopherClient.request(root.getHost(), root.getPort(), root.getSelector());
	    
	    if (tab == null)
	    {
	    	logger.error(">>> ERROR IN CRAWLED : [" + root.getHost() + "][" + root.getPort() + "][" + root.getType() + "][" + root.getSelector() + "]");
		    return;
	    }
	    
	    logger.info(">>> CRAWLED : [" + root.getHost() + "][" + root.getPort() + "][" + root.getType() + "][" + root.getSelector() + "]");
	    
	    // Get content with all nodes of the menu
	    // Detect errors
	    StringBuilder str = new StringBuilder();
	    boolean err = false;
		for (int i = 0; i < tab.size(); i++) {
			if (tab.get(i).getType() != null)
			{
				if (tab.get(i).getType().equals("3")) {
					err = true;
					break;
				}
				else
				{
					if (tab.get(i).getUsername() != null)
						str.append(tab.get(i).getUsername() + " ");
				}
			}
		}
	    String content = str.toString();
	    
	    if (err)
	    {
	    	logger.error(">>> ERROR IN CRAWL : [" + root.getHost() + "][" + root.getPort() + "][" + root.getType() + "][" + root.getSelector() + "] node Error(3) detected");
		  	return;
	    }
	    
	    // Try to get the "ABOUT" node
	    int index_about = -1;
	    for (int i = 0; i < tab.size(); i++)
	    {
	    	GopherDirectoryEntity ent = tab.get(i);
	    	if (ent.getSelector() != null)
	    	if (ent.getSelector().toLowerCase().trim().equals("/about") || 
	    		ent.getSelector().toLowerCase().trim().equals("/about.txt") ||
	    		ent.getSelector().toLowerCase().trim().equals("about") || 
	    		ent.getSelector().toLowerCase().trim().equals("about.txt"))
    		{
			  	index_about = i;
			  	break;
	    	}
	    }
	    if (index_about != -1)
	    {
		    for (int i = 0; i < tab.size(); i++)
		    {
		    	if (tab.get(i).getSelector().toLowerCase().contains("about") && 
		    			tab.get(i).getType().equals("0"))
		    	{
				  	index_about = i;
				  	break;
		    	}
		    }
	    }
	    if (index_about > -1)
	    {
	    	// Notify that about is touched 
	    	site.touch(new CrawlerSiteNode("0", tab.get(index_about).getUsername(), tab.get(index_about).getSelector()));

	    	String ABOUT_node = GopherClient.requestTextEntity(tab.get(index_about).getHost(), 
	    			tab.get(index_about).getPort(), tab.get(index_about).getSelector());
		    if (ABOUT_node != null)
		    {
		    	logger.info(">>> CRAWLED 'ABOUT' IN NODE : [gopher://" + root.getHost() + ":" + root.getPort() + "/" + root.getType() + root.getSelector() + "]");
			  	content = ABOUT_node;
		    }
		    else
		    {
		    	logger.error(">>> ERROR IN CRAWLING 'ABOUT' IN NODE : [gopher://" + root.getHost() + ":" + root.getPort() + "/" + root.getType() + root.getSelector() + "]");
		    }
		    
		    deleteDocInternal(direct, tab.get(index_about).getHost(), 
		    		tab.get(index_about).getPort(), tab.get(index_about).getSelector());
		}
	    

	    deleteDocInternal(direct, root.getHost(), root.getPort(), root.getSelector());
	    addDocInternal(direct, root.getType(), root.getUsername(), root.getHost(), root.getPort(), root.getSelector(), content);
		
		
		
		
	    // Index recursively (add it to the pool)
	    for (GopherDirectoryEntity item : tab)
	    {
	    	// Check that type is not null
	    	if (item.getType() == null)
	    	{
	    		logger.error("Node with Type == null in : [" + root.getHost() + "][" + root.getPort() + "][" + root.getType() + "][" + root.getSelector() + "]");
			}
	    	else if (item.getHost() == null)
	    	{
	    		logger.error("Node with Host == null in : [" + root.getHost() + "][" + root.getPort() + "][" + root.getType() + "][" + root.getSelector() + "]");
			}
	    	// else if (item.getType().equals("0") ||
	    	//	item.getType().equals("1"))
	    	else if (!item.getType().equals("i") &&
			  		!item.getType().equals("3") &&
			  		!item.getType().equals("7")) 
		    {
	    		try {
		    		// no error or info node ("i") or index node
		    		CrawlerSite distSite;
		    		distSite = getSiteFormSitesMutex(item.getHost(), item.getPort());
		    		distSite.putNode(new CrawlerSiteNode(item.getType(), item.getUsername(), item.getSelector()));

				} catch (Exception e) {
					logger.error(">>> ERROR IN CRAWLED : [" + root.getHost() + "][" + root.getPort() + "][" + root.getType() + "][" + root.getSelector() + "]");
					logger.error(">>> CRAWLED : [" + item.getHost() + "][" + item.getPort() + "][" + item.getType() + "][" + item.getSelector() + "]");
				    logger.error(e);
				}
		    }
	    	else if (item.getType().equals("h"))
	    	{
	    		
	    		/*CrawlerSite distSite;
	    		distSite = getSiteFormSitesMutex(item.getHost(), item.getPort());
	    		distSite.putNode(new CrawlerSiteNode(item.getType(), item.getUsername(), item.getSelector()));*/
	    		//logger.error(">>> CRAWLED HTML : " + item.getSelector()); 
	    		
	    		// Filter URL that crap !!!
	    		if (item.getSelector() != null)
	    		if (!item.getSelector().startsWith("URL:") && !item.getSelector().startsWith("/URL:") )
	    	    {
	    			CrawlerSite distSite;
		    		distSite = getSiteFormSitesMutex(item.getHost(), item.getPort());
		    		distSite.putNode(new CrawlerSiteNode(item.getType(), item.getUsername(), item.getSelector()));
	    		}
	    	}
	    }		
	}

	private CrawlerSite getSiteFormSitesMutex(String host, int port) {
		
		boolean inter = false;
		try
		{
			mutexSites.acquire();
			CrawlerSite distSite;
			String key = host.trim().toLowerCase() + ":" + port;
    		
			if (!sites.containsKey(key))
			{
				logger.info("DISCOVER NEW SITE : " + key);
				distSite = 	new CrawlerSite(host, port);
				sites.put(key, distSite);
			}
			else
				distSite = sites.get(key);
			
			return distSite;
				
		} catch (InterruptedException e) {
			logger.info(e);
			inter = true;
		}
		finally
		{
			if (!inter)
				mutexSites.release();
		}
		
		return null;
	}

	public boolean isExit() {
		return exit;
	}

	public void setExit(boolean exit) {
		this.exit = exit;
	}


	
	private void deleteDocInternal(Directory direct2, String host, int port, String selector) {
		boolean inter = false;
		try {
			mutexIndex.acquire();
		
		// 0. Specify the analyzer for tokenizing text.
		// The same analyzer should be used for indexing and searching
		Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_35);
	
		IndexWriter w = null;
		IndexWriterConfig conf = new IndexWriterConfig(Version.LUCENE_35, analyzer);
		conf.setWriteLockTimeout(2000);
		
		try {
			w = new IndexWriter(direct2, conf);
			
			
	
			BooleanQuery query = new BooleanQuery();
			TermQuery q1 = new TermQuery(new Term("host", host));
			query.add(q1, Occur.MUST);
			TermQuery q2 = new TermQuery(new Term("port", "" + port));
			query.add(q2, Occur.MUST);
			TermQuery q3 = new TermQuery(new Term("selector", selector));
			query.add(q3, Occur.MUST);
			try {
				w.deleteDocuments(query);
			} catch (CorruptIndexException e) {
				logger.error(e);
			} catch (IOException e) {
				logger.error(e);
			}
			
		} catch (CorruptIndexException e) {
			logger.error(e);
		} catch (LockObtainFailedException e) {
			logger.error(e);
		} catch (IOException e) {
			logger.error(e);
		} 
		finally
		{
			// Close
			try {
				if (w != null)
					w.close();
			} catch (CorruptIndexException e) {
				logger.error(e);
			} catch (IOException e) {
				logger.error(e);
			}
		}
		
		} catch (InterruptedException e) {
			logger.error(e);
			inter = true;
		} 
		finally
		{
			if (!inter)
				mutexIndex.release();
		}
	}

	private void addDocInternal(Directory direct2, String type, String title, String host, int port, String selector, String content)  
	{		
		boolean inter = false;
		try 
		{
			mutexIndex.acquire();
		
			// 0. Specify the analyzer for tokenizing text.
			// The same analyzer should be used for indexing and searching
			Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_35);
	
			IndexWriter w = null;
			IndexWriterConfig conf = new IndexWriterConfig(Version.LUCENE_35, analyzer);
			conf.setWriteLockTimeout(2000);
	
			try
			{
				w = new IndexWriter(direct2, conf);
	
				Document doc = new Document();
				doc.add(new Field("type", type, Field.Store.YES, Field.Index.NOT_ANALYZED));
				doc.add(new Field("title", title, Field.Store.YES, Field.Index.ANALYZED));
				doc.add(new Field("host", host, Field.Store.YES, Field.Index.NOT_ANALYZED));
				doc.add(new Field("port", "" + port, Field.Store.YES, Field.Index.NOT_ANALYZED));
				doc.add(new Field("selector", selector, Field.Store.YES, Field.Index.NOT_ANALYZED));
				doc.add(new Field("content", title, Field.Store.YES, Field.Index.ANALYZED));
			
				try {
					w.addDocument(doc);
				} catch (CorruptIndexException e) {
					logger.error(e);
				} catch (IOException e) {
					logger.error(e);
				}
			
			} catch (CorruptIndexException e) {
				logger.error(e);
			} catch (LockObtainFailedException e) {
				logger.error(e);
			} catch (IOException e) {
				logger.error(e);
			}
			finally
			{
						// Close
				if (w != null)
					try {
						w.close();
					} catch (CorruptIndexException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
			}
		
	
		} catch (InterruptedException e) {
			logger.error(e);
			inter = true;
		}
		finally
		{
			if (!inter)
				mutexIndex.release();
		}
		
		
	}
}
