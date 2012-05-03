package gogoph.crawler;

import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

public class CrawlerSite {

	public CrawlerSite(String host, Integer port) {
		super();
		this.host = host;
		this.port = port;
	}

	private String host;
	private int port;
	
	private ArrayList<String> errors = new ArrayList<String>();
	
	private Queue<CrawlerSiteNode> selectors = new LinkedBlockingQueue<CrawlerSiteNode>();
	private ArrayList<CrawlerSiteNode> visitedSelectors = new ArrayList<CrawlerSiteNode>();
	private Semaphore mutexSelectors = new Semaphore(1);
	
	/*
	 * putNode |
	 *         V
	 *     selectors --(visit)--> visitedSelectors
	 *                    |
	 *                    V
	 *              (null if no node)
	 *                   or
	 *            a Node of this site
	 */
	public void putNode(CrawlerSiteNode node) {
		boolean interr = false;
		try {
			mutexSelectors.acquire();
			
			if (!visitedSelectors.contains(node))
				selectors.add(node);
			
		} catch (InterruptedException e) {
			interr = true;
			e.printStackTrace();
		}
		finally
		{
			if (!interr)
				mutexSelectors.release();
		}
	}
	
	public CrawlerSiteNode visit() {
		boolean interr = false;
		try {
			mutexSelectors.acquire();
			
			CrawlerSiteNode node = selectors.poll();
			while (node != null && visitedSelectors.contains(node))
				node = selectors.poll();
			
			if (node !=null)
				visitedSelectors.add(node);

			return node;
			
		} catch (InterruptedException e) {
			interr = true;
			e.printStackTrace();
		}
		finally
		{
			if (!interr)
				mutexSelectors.release();
		}
		
		return null;
	}

	public void touch(CrawlerSiteNode node) {
		boolean interr = false;
		try {
			mutexSelectors.acquire();
			
			// put
			if (!visitedSelectors.contains(node))
				selectors.add(node);
			
			else
				visitedSelectors.add(node);
			
		} catch (InterruptedException e) {
			interr = true;
			e.printStackTrace();
		}
		finally
		{
			if (!interr)
				mutexSelectors.release();
		}
	}
	
	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public long getNbVisited() {
		return visitedSelectors.size();
	}

	public long getNbSelector() {
		return selectors.size() + visitedSelectors.size();
	}

	public ArrayList<CrawlerSiteNode> getSelectors() {
		return visitedSelectors;
	}

	/*public void setErrors(ArrayList<String> errors) {
		this.errors = errors;
	}*/

	public ArrayList<String> getErrors() {
		return errors;
	}
	
}
