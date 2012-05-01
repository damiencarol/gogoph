package gogoph.crawler;

import gogoph.GopherDirectoryEntity;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Queue;
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

public class CrawlerQueueThread implements Runnable {
	
	 private static final Logger logger = Logger.getLogger(
			 CrawlerQueueThread.class.getName());

	private ArrayList<GopherDirectoryEntity> knowList;
	private Semaphore mutexKnowList;
	 
	private ArrayList<String> blackList;
	private Directory direct;

	private Queue<GopherDirectoryEntity> pool;

	private Semaphore mutexPool;

	private int nb;

	private Semaphore mutexIndex;

	public CrawlerQueueThread(Queue<GopherDirectoryEntity> _pool, Semaphore _mutexPool,
			int _nb, 
			ArrayList<GopherDirectoryEntity> list, Semaphore _mutexKnowList,
			ArrayList<String> _blackList,
			Directory dir, Semaphore semIndex) 
	{
		pool = _pool;
		mutexPool = _mutexPool;
		nb = _nb;
		knowList = list;
		mutexKnowList = _mutexKnowList;
		blackList = _blackList;
		direct = dir;
		mutexIndex = semIndex;
	}

	public void run() {
		
		while (nb > 0)
		{
			// Get root
			GopherDirectoryEntity root = null;
			try {
				mutexPool.acquire();
				} catch (InterruptedException e2) {
				e2.printStackTrace();
			} 
			
				if (pool.isEmpty())
				{
					mutexPool.release();
					logger.info("Queue is empty.");
					try { Thread.sleep(500);} catch (InterruptedException e2) {				e2.printStackTrace();					}
				}
				else
				{
					root = pool.poll();
					mutexPool.release();
				}
			
			
			
			
			
			
			if (root != null)
			{			
				logger.info(">>> CRAWL STEP [" + nb + "]");
			    crawl(direct, root);
			}			
			
			logger.info(">>> CRAWL POOL SIZE [" + pool.size() + "]");
		}
	}

	private void crawl(Directory direct, GopherDirectoryEntity root) 
	{
		nb--;
		
		// Get unique key
		//String key = root.getHost() + ":" + root.getPort() + root.getSelector();

    	// Try to test if it's allready here
    	if (knowed(root))
    		return;
    	
    	
    	
		// If it's a text file
		if (root.getType().equals("0"))
		{
			String content = GopherClient.requestTextEntity(root.getHost(), root.getPort(), root.getSelector());
			if (content == null)
			{
				logger.error(">>> ERROR IN CRAWL : [gopher://" + root.getHost() + ":" + root.getPort() + "/" + root.getType() + root.getSelector() + "]");
			  	return;
				
			}
			
			logger.info(">>> CRAWLED : [" + root.getHost() + "][" + root.getPort() + "][" + root.getType() + "][" + root.getSelector() + "]");
			touch(root);


			deleteDocInternal(direct, root);
			addDocInternal(direct, root, content);    		
		}
		else
		if (root.getType().equals("1"))
		{
			crawlMenu(root, direct, this);
			return;
		}
		// If it's a image file
		else if (root.getType().equals("I"))
		{
			File content = GopherClient.requestBinaryfile(root.getHost(), root.getPort(), root.getSelector());
			if (content == null)
			{
				logger.error(">>> ERROR IN CRAWL : [gopher://" + root.getHost() + ":" + root.getPort() + "/" + root.getType() + root.getSelector() + "]");
			  	return;
				
			}
			
			logger.info(">>> CRAWLED : [" + root.getHost() + "][" + root.getPort() + "][" + root.getType() 
					+ "][" + root.getSelector() + "] -> " + content.getAbsolutePath());
			touch(root);

			
			deleteDocInternal(direct, root);
			addDocInternal(direct, root, root.getUsername());
			
		}
		else // If it's another file
		{
			
			/*File content = GopherClient.requestBinaryfile(root.getHost(), root.getPort(), root.getSelector());
			if (content == null)
			{
				logger.error(">>> ERROR IN CRAWL : [gopher://" + root.getHost() + ":" + root.getPort() + "/" + root.getType() + root.getSelector() + "]");
			  	return;
				
			}*/

			/*logger.info(">>> NOT CRAWLED : [" + root.getHost() + "][" + root.getPort() + "][" + root.getType() 
					+ "][" + root.getSelector() + "]");
			touch(root);

			try {
			deleteDocInternal(direct, root);
			addDocInternal(direct, root, root.getUserName());
			Thread.sleep(100);
    		} catch (CorruptIndexException e) {
    			logger.error(e);
			} catch (LockObtainFailedException e) {
				logger.error(e);		
			} catch (IOException e) {
				logger.error(e);
			} catch (InterruptedException e) {
				logger.error(e);
			}*/
		}
	}
	
	public static void crawlMenu(GopherDirectoryEntity root, Directory direct, CrawlerQueueThread crawler) {
		
		// Get content of menu
		ArrayList<GopherDirectoryEntity> tab;
	    tab = GopherClient.request(root.getHost(), root.getPort(), root.getSelector());
	    
	    if (tab == null)
	    {
	    	logger.error(">>> ERROR IN CRAWLED : [" + root.getHost() + "][" + root.getPort() + "][" + root.getType() + "][" + root.getSelector() + "]");
		    return;
	    }
	    
	    logger.info(">>> CRAWLED : [" + root.getHost() + "][" + root.getPort() + "][" + root.getType() + "][" + root.getSelector() + "]");
	    crawler.touch(root);
	    
	    // Get content with all nodes of the menu
	    // Detect errors
	    StringBuilder str = new StringBuilder();
	    boolean err = false;
		for (int i = 0; i < tab.size(); i++) {
			str.append(tab.get(i).getUsername() + " ");
			if (tab.get(i).getType().equals("3"))
				err = true;
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
	    	if (tab.get(i).getSelector().toLowerCase().trim().equals("/about") || 
	    			tab.get(i).getSelector().toLowerCase().trim().equals("/about.txt") ||
	    		tab.get(i).getSelector().toLowerCase().trim().equals("about") || 
    			tab.get(i).getSelector().toLowerCase().trim().equals("about.txt"))
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
	    	crawler.touch(tab.get(index_about));

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
		    
		    crawler.deleteDocInternal(direct, tab.get(index_about));
		}
	    

	    crawler.deleteDocInternal(direct, root);
	    crawler.addDocInternal(direct, root, content);
		
		
		
		
	    // Index recursively (add it to the pool)
	    for (GopherDirectoryEntity item : tab)
	    {
	    	//if (item.getType().equals(GopherType.Menu.toString()) || 
	    	//		item.getType().equals(GopherType.TextFile.toString()))
	    	//		item.getType() == GopherType.ImageFile || true) 
	    	if (!item.getType().equals("i") ||
	    			!item.getType().equals("3")) // no error or info node ("i")
	    	{
	    		try {
	    			if (!item.getHost().equals("(NULL)") &&
	    					!item.getHost().equals("error.host"))
	    			{
						if (InetAddress.getByName(item.getHost()) != null)
							crawler.addToPool(item);
	    			}
				} catch (UnknownHostException e) {
					logger.error(">>> CRAWLED : [" + root.getHost() + "][" + root.getPort() + "][" + root.getType() + "][" + root.getSelector() + "]");
					logger.error(">>> CRAWLED : [" + item.getHost() + "][" + item.getPort() + "][" + item.getType() + "][" + item.getSelector() + "]");
				    logger.error(e);
				}
		    }
	    }		
	}

	private void addToPool(GopherDirectoryEntity item) {
	
		if (pool.size() > 10000)
	    	return;

		try 
		{	mutexPool.acquire();
    		pool.offer(item);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		finally
		{
			mutexPool.release();					
		}
	}

	private boolean knowed(GopherDirectoryEntity root) {
		String key = root.getHost() + ":" + root.getPort() + root.getSelector();
		
		try {
			mutexKnowList.acquire();

			if (knowList.contains(key))
	 	    {
				return true;
	    	}
	    	
    	} catch (InterruptedException e1) {
			logger.error(e1);
		} finally {
			mutexKnowList.release();
		}
		
		return false;
	}
	
	private boolean touch(GopherDirectoryEntity root) {
		String key = root.getHost() + ":" + root.getPort() + root.getSelector();
		
		try {
			mutexKnowList.acquire();
			
			// If the selector is in blacklist
			if (blackList.contains(key))
				return false;
			
			if (!knowList.contains(key))
	 	    {
				knowList.add(root);
	    		return true;
	    	}
	    	
    	} catch (InterruptedException e1) {
			logger.error(e1);
		} finally {
			mutexKnowList.release();
		}
		
		return false;
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
		} 
		finally
		{
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
		}
		finally
		{
			mutexIndex.release(); 
		}
		
		
	}
		
}
