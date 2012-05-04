/*
    GOGOPH - Modern Gopher Server easy to manage.
    Copyright (C) 2012  Damien CAROL

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package gogoph.crawler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

import org.apache.log4j.Logger;

/**
 * Gopher site crawled.
 * @author D.Carol
 *
 */
public class CrawlerSite {
	
	private static final Logger logger = Logger.getLogger(
			CrawlerSite.class.getName());

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

	public void saveCrawlerSiteInfo(String dirName) {
		File siteDir = new File(dirName);
		if (!siteDir.exists())
		{
			siteDir.mkdir();
		}
		
		// Save on file 'selectors' all nodes got
		{
		File sitetextFile = new File(siteDir.getPath() + File.separator + "selectors");
        BufferedWriter disc = null;
	    try {
	      disc = new BufferedWriter(new FileWriter(sitetextFile));
	      for (CrawlerSiteNode node : this.getSelectors())
			{
	    	  disc.write("[" + node.getType() + "]" + node.getSelector() + " => '" + node.getUsername() + "'\r\n");
			}
	      disc.flush();
	      disc.close();
	    } catch(Exception e)
	    { logger.error(e);
	    }
		}
	    
	    // Save on file 'errors' all errors
	    {
		File siteErrFile = new File(siteDir.getPath() + File.separator + "errors");
        BufferedWriter disc = null;
	    try {
	      disc = new BufferedWriter(new FileWriter(siteErrFile));
	      for (String str : this.getErrors())
			{
	    	  disc.write(str + "\r\n");
			}
	      disc.flush();
	      disc.close();
	    } catch(Exception e)
	    { logger.error(e);
	    }
	    }
	}
	
}
