package gogoph.crawler.gen3;

import gogoph.crawler.CrawlerSiteNode;
import gogoph.crawler.GopherClient;
import gogoph.crawler.GopherDirectoryEntity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.ContentHandler;

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
	private String content;
	private boolean ok;
	private ArrayList<GopherDirectoryEntity> nodes;
	
	@Override
	public void run() {
		try {
			
			if (node.getType().equals("1")) {
				crawlMenu();
				return;
			}
			
			if (node.getType().equals("0")) {
				crawlGeneric();
				return;
			}
			else
				crawlGeneric();
		
		}
		catch (Exception e)
		{
			ok = false;
			logger.error(e);
		}
	}

	private void crawlMenu() {
		CrawlerSiteNode root = node;
		// Get content of menu
		ArrayList<GopherDirectoryEntity> tab;
	    tab = GopherClient.request(host, port, root.getSelector());
	    
	    if (tab == null)
	    {
	    	logger.error(">>> ERROR IN CRAWLED : [" + host + "][" + port + "][" + root.getType() + "][" + root.getSelector() + "]");
		    return;
	    }
	    
	    logger.info(">>> CRAWLED : [" + host + "][" + port + "][" + root.getType() + "][" + root.getSelector() + "]");
	    
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
	    content = str.toString();
	    
	    if (err)
	    {
	    	logger.error(">>> ERROR IN CRAWL : [" + host + "][" + port + "][" + root.getType() + "][" + root.getSelector() + "] node Error(3) detected");
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
	    	String ABOUT_node = GopherClient.requestTextEntity(tab.get(index_about).getHost(), 
	    			tab.get(index_about).getPort(), tab.get(index_about).getSelector());
		    if (ABOUT_node != null)
		    {
		    	logger.info(">>> CRAWLED 'ABOUT' IN NODE : [" + host + "][" + port + "][" + root.getType() + root.getSelector() + "]");
			  	content = ABOUT_node;
		    }
		    else
		    {
		    	logger.error(">>> ERROR IN CRAWLING 'ABOUT' IN NODE : [" + host + "][" + port + "][" + root.getType() + root.getSelector() + "]");
		    }
		    
		}
	    
	    this.nodes = tab;
	    ok = true;
	}

	private void crawlGeneric() {
		
			// Get raw file
			File rawFile = GopherClient.requestBinaryfile(host, port, node.getSelector());
			if (rawFile == null)
			{
				logger.error(">>> ERROR IN CRAWL : [" + host + "][" + port + "][" + node.getType() + "][" + node.getSelector() + "]");
				return;
			}
			
			logger.info(">>> CRAWLED : [" + host + "][" + port + "][" + node.getType() + "][" + node.getSelector() + "]");
			
			content = node.getUsername();
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
			    			  content += " " + sub;
			    		  }
			    	  }
			    	  else
			    	  {
			    		  logger.debug("[" + str + "]='" + metadata.get(str) + "'");
			    		  content += " " + metadata.get(str);
			    	  }	    	  
			      }
			      content += " " + contenthandler.toString();
			    }
			    catch (Exception e) {
			      e.printStackTrace();
			    }
			    finally {
			        if (is != null) is.close();
			    }
			
			if (content == null)
			{
				logger.error(">>> ERROR IN CRAWL : [" + host + "][" + port + "][" + node.getType() + "][" + node.getSelector() + "]");
				return;
				
			}
			
			logger.info(">>> CRAWLED : [" + host + "][" + port + "][" + node.getType() + "][" + node.getSelector() + "]");
				
			ok = true;
			return;
			
			} catch (FileNotFoundException e) {
				logger.error(e);
				logger.error(">>> ERROR IN CRAWL : [" + host + "][" + port + "][" + node.getType() + "][" + node.getSelector() + "]");
		    } catch (IOException e) {
				 logger.error(e);
				 logger.error(">>> ERROR IN CRAWL : [" + host + "][" + port + "][" + node.getType() + "][" + node.getSelector() + "]");
			}
	}

	public boolean isOk() {
		return ok;
	}

	public String getContent() {
		return content;
	}

	public ArrayList<GopherDirectoryEntity> getNodes() {
		return nodes;
	}

}
