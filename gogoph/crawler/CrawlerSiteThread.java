package gogoph.crawler;

import gogoph.GopherDirectoryEntity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Semaphore;

import net.htmlparser.jericho.Source;
import net.htmlparser.jericho.TextExtractor;

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

	@Override
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
		
		// If it's a text file
		if (root.getType().equals("0"))
		{
			String content = GopherClient.requestTextEntity(root.getHost(), root.getPort(), root.getSelector());
			if (content == null)
			{
				site.getErrors().add("Can't get [" + root.getHost() + "][" + root.getPort() + "][" + root.getType() + "][" + root.getSelector() + "]");
				logger.error(">>> ERROR IN CRAWL : [" + root.getHost() + "][" + root.getPort() + "][" + root.getType() + "][" + root.getSelector() + "]");
				return;
			}
			
			logger.info(">>> CRAWLED : [" + root.getHost() + "][" + root.getPort() + "][" + root.getType() + "][" + root.getSelector() + "]");
			

			deleteDocInternal(direct, root);
			addDocInternal(direct, root, content);    		
		}
		else
		if (root.getType().equals("1"))
		{
			crawlMenu(direct, root);
			return;
		}
		else if (root.getType().equals("h"))
		{
			crawlHtml(direct, root);
			return;
		}			
	}

	private void crawlHtml(Directory direct, GopherDirectoryEntity root) {
		
		// Get raw file
		File htmlFile = GopherClient.requestBinaryfile(root.getHost(), root.getPort(), root.getSelector());
		if (htmlFile == null)
		{
			logger.error(">>> ERROR IN CRAWL : [gopher://" + root.getHost() + ":" + root.getPort() + "/" + root.getType() + root.getSelector() + "]");
		  	return;
		}
		
		logger.info(">>> CRAWLED : [" + root.getHost() + "][" + root.getPort() + "][" + root.getType() + "][" + root.getSelector() + "]");
		
		try {
			Source src;
			src = new Source(new FileReader(htmlFile));
		
		TextExtractor extr = new TextExtractor(src);
		
		String content = extr.toString();
		if (content == null)
		{
			logger.error(">>> ERROR IN CRAWL : [gopher://" + root.getHost() + ":" + root.getPort() + "/" + root.getType() + root.getSelector() + "]");
		  	return;
			
		}
		
		logger.info(">>> CRAWLED : [" + root.getHost() + "][" + root.getPort() + "][" + root.getType() + "][" + root.getSelector() + "]");
		

		deleteDocInternal(direct, root);
		addDocInternal(direct, root, content);   
		
		} catch (FileNotFoundException e) {
			logger.error(e);
			logger.error(">>> ERROR IN CRAWL : [gopher://" + root.getHost() + ":" + root.getPort() + "/" + root.getType() + root.getSelector() + "]");
		  } catch (IOException e) {
			 logger.error(e);
			logger.error(">>> ERROR IN CRAWL : [gopher://" + root.getHost() + ":" + root.getPort() + "/" + root.getType() + root.getSelector() + "]");
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
		    
		    deleteDocInternal(direct, tab.get(index_about));
		}
	    

	    deleteDocInternal(direct, root);
	    addDocInternal(direct, root, content);
		
		
		
		
	    // Index recursively (add it to the pool)
	    for (GopherDirectoryEntity item : tab)
	    {
	    	//if (item.getType().equals(GopherType.Menu.toString()) || 
	    	//		item.getType().equals(GopherType.TextFile.toString()))
	    	//		item.getType() == GopherType.ImageFile || true) 
	    	//if (!item.getType().equals("i") ||
	    	//		!item.getType().equals("3")) // no error or info node ("i")
	    	if (item.getType() == null)
	    	{
	    		logger.error("Node with Type == null in : [" + root.getHost() + "][" + root.getPort() + "][" + root.getType() + "][" + root.getSelector() + "]");
			}
	    	else
	    	if (item.getType().equals("0") ||
	    		item.getType().equals("1"))
	    	{
	    		CrawlerSite distSite;
	    		distSite = getSiteFormSitesMutex(item.getHost(), item.getPort());
	    		distSite.putNode(new CrawlerSiteNode(item.getType(), item.getUsername(), item.getSelector()));
	    		
	    		
	    		/*try {
	    			if (!item.getHost().equals("(NULL)") &&
	    					!item.getHost().equals("error.host"))
	    			{
						if (InetAddress.getByName(item.getHost()) != null)
							crawler.addToPool(item);
	    			}
				} catch (Exception e) {
					logger.error(">>> CRAWLED : [" + root.getHost() + "][" + root.getPort() + "][" + root.getType() + "][" + root.getSelector() + "]");
					logger.error(">>> CRAWLED : [" + item.getHost() + "][" + item.getPort() + "][" + item.getType() + "][" + item.getSelector() + "]");
				    logger.error(e);
				}*/
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

	/*private void addToKnowList(GopherDirectoryEntity root) {
		try {
			mutexKnowList.acquire();
			//knowList.add(root.getHost() + ":" + root.getPort() + root.getSelector());
			knowList.add(root);
			
		} catch (InterruptedException e1) {
			logger.error(e1);
		} finally {
			mutexKnowList.release();
		}
	}*/
	
	private void deleteDocInternal(Directory direct2, GopherDirectoryEntity root) {
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
			TermQuery q1 = new TermQuery(new Term("host", root.getHost()));
			query.add(q1, Occur.MUST);
			TermQuery q2 = new TermQuery(new Term("port", new Integer(root.getPort()).toString()));
			query.add(q2, Occur.MUST);
			TermQuery q3 = new TermQuery(new Term("selector", root.getSelector()));
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

	/*
	private void obsoleteImage(GopherDirectoryEntity entity, IndexWriter w) throws CorruptIndexException, IOException
	{
		if (entity.getType() == GopherType.ImageFile)
		{
			String content = "";
			File data = GopherClient.requestBinaryfile(entity);
			if (data != null)
			{
				Metadata dataMeta;
				try {
					dataMeta = ImageMetadataReader.readMetadata(data);
				
	
					StringBuilder str = new StringBuilder();
					for (Directory item : dataMeta.getDirectories())
					{							
						logger.debug("Directory : " + item.getName() + " " + item.getTagCount());
						//for (Tag tag : item.getTags())
						//{
						//	logger.debug("Tag : " + tag.getTagName() + " " + tag.toString());
						//	str.append(tag.toString() + " ");								
						//}
						
						if (item instanceof XmpDirectory)
						{
							XmpDirectory xmp = (XmpDirectory) item;
							XMPMeta met = xmp.getXMPMeta();
							try {
								int i = 1;
								XMPProperty toto = met.getProperty("http://purl.org/dc/elements/1.1/", "subject[" + i + "]");
								while (toto != null)
								{
									str.append(toto.getValue() + " ");
									i++;
									toto = met.getProperty("http://purl.org/dc/elements/1.1/", "subject[" + i + "]");
								}
				//			XMPIterator it;
				//				it = met.iterator();
				//			
				//			while (it.hasNext())
				//			{
				//				Object ob = it.next();
				//				logger.debug("It : " + ob);
				//				str.append(tag.toString() + " ");								
				//			}
							} catch (XMPException e) {
								logger.error(e);
							}
						}
					}
			    
					content = str.toString();
				} catch (ImageProcessingException e) {
					logger.error(e);
				}
				
				addDocInternal(w, entity, content);
				return;
			}
		}
	}
	*/
	private void addDocInternal(Directory direct2, GopherDirectoryEntity root, String content)  
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
				doc.add(new Field("type", root.getType(), Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
				doc.add(new Field("title", root.getUsername(), Field.Store.YES, Field.Index.ANALYZED));
				doc.add(new Field("host", root.getHost(), Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
				doc.add(new Field("port", new Integer(root.getPort()).toString(), Field.Store.YES, Field.Index.ANALYZED));
				doc.add(new Field("selector", root.getSelector(), Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
				doc.add(new Field("content", content, Field.Store.YES, Field.Index.ANALYZED));
		
				doc.add(new Field("hash", root.getHost() + "|" + root.getPort() + "|" + root.getSelector(), 
						Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
			
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
