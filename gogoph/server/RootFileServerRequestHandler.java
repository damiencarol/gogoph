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
package gogoph.server;

import gogoph.GopherBinaryTransactionResult;
import gogoph.GopherDirectoryEntity;
import gogoph.GopherMenuTransactionResult;
import gogoph.GopherTextTransactionResult;
import gogoph.GopherTransactionResult;
import gogoph.IGopherRequestHandler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

public class RootFileServerRequestHandler implements IGopherRequestHandler {

	private static final Logger logger = Logger.getLogger(
			RootFileServerRequestHandler.class.getName());
	
	private HashMap<String, GopherTransactionResult> dirNodes = new HashMap<String, GopherTransactionResult>();
	
	private String gophermapName;
	
	private String gophertagName;
	
	private ArrayList<FileTransaction> fileTransactions = new ArrayList<FileTransaction>();
	
	private Directory directory;

	private String host;

	private int port;
	
	public void populate(File dir, String host, int port) throws IOException 
	{
		// Check if the live directory exist
		if (!dir.exists())
			throw new IOException("The file is not a directory.");
		if (!dir.isDirectory())
			throw new IOException("The file is not a directory.");
		if (!dir.canRead())
			throw new IOException("Can't read the directory.");
		
		this.host = host;
		this.port = port;
		
		// Build index
		directory = new RAMDirectory();
		
		populateDir("", dir, host, port);
		
		for (String item : dirNodes.keySet())
		{
			logger.debug("Registered : '" + item + "'");
		}
		
		// Register the search node
		logger.debug("Registered Search node : '/search'");
		dirNodes.put("/search", new GopherSearchTransactionResult(directory));
	}

	public GopherTransactionResult process(String selector, String queryString) {

		// formalize selector
		while (selector.endsWith("/"))
			selector = selector.substring(0, selector.length()-1);
		
		if (selector.length() > 0)
			if (!selector.startsWith("/"))
				selector = "/" + selector;
		
		// Check if the node exists
		if (!dirNodes.containsKey(selector))
			return errorMenu("Invalid Selector");
			
		GopherTransactionResult res = dirNodes.get(selector);
		return res;
	}
	
	private void populateFile(File dest, String selectorRef) {
		
		GopherTransactionResult result;
		// By default the result is binary
		result = new GopherBinaryTransactionResult(dest);
		
		// Check if it's gophermap
		if (dest.getName().equals(gophermapName))
			result = errorMenu("Invalid selector");
		
		// Check if it's gophertag
		else if (dest.getName().equals(gophertagName))
			result = errorMenu("Invalid selector");
		
		// Check file transactions nodes
		else for (FileTransaction ft : fileTransactions)
		{
			for (Pattern reg : ft.getRegexs())
			{
				if (reg.matcher(dest.getName()).matches())
				{
					if (ft.getTransaction().equals("text"))
					{
						result = new GopherTextTransactionResult(dest);
					}
					else if (ft.getTransaction().equals("program"))
					{
						result = new GopherProgramTransactionResult(dest);
					}
					break;
				}
			}
		}
		
		// Add to nodes
		this.dirNodes.put(selectorRef + "/" + dest.getName(), result);
		
		// Index it
		GopherDirectoryEntity ent = new GopherDirectoryEntity();
		ent.setExtra(null);
		ent.setHost(host);
		ent.setPort(port);
		ent.setSelector(selectorRef + "/" + dest.getName());
		ent.setUsername(selectorRef + "/" + dest.getName());
		
		String content = (selectorRef + "/" + dest.getName()).replace("/", " ");
		if (result instanceof GopherTextTransactionResult)
		{
			ent.setType("0");
			
			FileReader fr;
			try {
				fr = new FileReader(dest);
			
			BufferedReader dis = new BufferedReader(fr);
			String line;
			while ((line = dis.readLine()) != null) {
				content += " " + line;
			}
		      
			
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else if (result instanceof GopherMenuTransactionResult)
		{
			ent.setType("1");
		}
		else
		{
			ent.setType("9");
		}

		addDocInternal(directory, ent, content);
	}

	private void populateDir(String selector, File dest, String host, int port) {
		
		ArrayList<GopherDirectoryEntity> tab = new ArrayList<GopherDirectoryEntity>();
		
		
		File gophermap = new File(dest.getPath() + File.separator + gophermapName);
		if (gophermap.exists())
		{
			tab = processGophermap(gophermap, selector, host, port);
		}
		else
		{
		// Add comments
		/*File folderInfo = new File(dest + File.separator + "folder.info");
		if (folderInfo.exists())
		{
			Reader bis = null;
		
		    BufferedReader dis = null;

		    try {
		      bis = new FileReader(folderInfo);
		      dis = new BufferedReader(bis);
			String line = dis.readLine();
		      while (line != null) {

		    	  tab.add(GopherDirectoryEntity.newComment(line));
		    	  
				line = dis.readLine();
		      } // dispose all the resources after using them.
		      dis.close();
		      bis.close();
		     
		    } catch (FileNotFoundException e) {
		    	logger.error(e);
			} catch (IOException e) {
		    	logger.error(e);
			} 
		}*/

		// Add the .. element
		/*if (!dest.getCanonicalPath().equals(root.getCanonicalPath()))
			{
				GopherDirectoryEntity upLink = new GopherDirectoryEntity();
				upLink.setType(GopherType.Menu.toString());
				upLink.setUserName(".. (go to parent dir)");
				upLink.setSelector(selector.substring(0, selector.lastIndexOf("/")));
				upLink.setHost(this.host);
				upLink.setPort(this.port);
				tab.add(upLink);
			}*/
		

		String[] list = dest.list();
		for (int i = 0; i < list.length; i++)
		{
			if (!list[i].equals(gophermapName) && !list[i].equals(gophertagName))
			{
				GopherDirectoryEntity node = null;
				node = newComment(list[i]);
				node.setHost(host);
				node.setPort(port);
		
				File file = new File(dest.getPath() + File.separator + list[i]);
				if (file.isDirectory())
				{
					try {
						File gophertag = new File(file.getPath() + "\\" + gophertagName);
						if (gophertag.exists())
						{
							String tag = new BufferedReader(new FileReader(gophertag)).readLine();
							node.setUsername(tag);
						}
					} catch (FileNotFoundException e) {
						logger.error(e);
					} catch (IOException e) {
						logger.error(e);
					}
					node.setType("1");
					node.setSelector(selector + "/" + list[i]);
				}
				else
				{
					// By default
					node.setType("9");
					// Check file transactions nodes
					for (FileTransaction ft : fileTransactions)
					{
						for (Pattern reg : ft.getRegexs())
						{
							if (reg.matcher(file.getName()).matches())
							{
								node.setType(ft.getType());
								
							}
						}
					}
					node.setSelector(selector + "/" + list[i]);
				}
				tab.add(node);
			}
		}
		}
		
		// Add internal search node
		if (selector.equals(""))
		{
			GopherDirectoryEntity e = new GopherDirectoryEntity();
			e.setExtra(null);
			e.setHost(host);
			e.setPort(port);
			e.setSelector(selector + "/search");
			e.setType("7");
			e.setUsername("Internal Search");
			
			tab.add(e);
		}
		
		// Add nodes
		this.dirNodes.put(selector,  new GopherMenuTransactionResult(tab));
		
		for (File item : dest.listFiles())
		{
			if (item.isDirectory()) {
				populateDir(selector + "/" + item.getName(), item, host, port);
			}
			else
			{				
				populateFile(item, selector);
			}
			
		}
	}

	/**
	 * <p>Process <code>gophermap</code> file to create Menu data.</p>
	 * @param mapfile the <tt>File</tt> to read from
	 * @param ref
	 * @param host
	 * @param port	
	 * @return
	 */
	private ArrayList<GopherDirectoryEntity> processGophermap(File mapfile, String ref, String host, int port) {
		
		ArrayList<GopherDirectoryEntity> taby = new ArrayList<GopherDirectoryEntity>();
		Reader bis = null;
		
	    BufferedReader dis = null;

	    try
	    {
	      bis = new FileReader(mapfile);
	      dis = new BufferedReader(bis);
	      String line = dis.readLine();
	      while (line != null) {

	    	  // If there are no TAB
	    	  if (line.indexOf("\t") == -1)
	    		  taby.add(newComment(line));
	    	  else {
	    		  String[] table = line.split("\t");
	    		  if (table.length == 4)
	    			  taby.add(new GopherDirectoryEntity(line));
	    		  else if (table.length == 2)
	    		  {
	    			  String node = table[1].trim();
	    			  if (!node.startsWith("/"))
	    				  node = "/" + node;
	    			  taby.add(new GopherDirectoryEntity(table[0] + "\t" + ref + node + "\t" + host + "\t" + port));
	    		  }
	    		  else if (table.length == 1) // manage gophertag
	    		  {  
	    			  GopherDirectoryEntity entity = new GopherDirectoryEntity(line + "\t" + host + "\t" + port);
	    			  if (entity.getSelector().trim().equals(""))
	    			  {
	    				  String can = entity.getUsername().trim();
	    				  if (!can.startsWith(File.separator))
	    					  can = "/" + can;
	    				  File gophertag = new File(mapfile.getParentFile().getPath() + can + File.separator + gophertagName);
	    				  if (gophertag.exists())
	    				  {
	    					  entity.setSelector(ref + can);
	    					  FileReader fr = new FileReader(gophertag);
	    					  String tag = new BufferedReader(fr).readLine();
	    					  entity.setUsername(tag);
	    					  fr.close();
	    				  }
	    			  }
	    			  taby.add(entity);
	    		  }
	    		  else
	    			  throw new Exception("ERROR IN GOPHERMAP");	    		  
	    	  }
	    	  
			line = dis.readLine();
	      } // dispose all the resources after using them.
	      dis.close();
	      bis.close();
	     
	      return taby;
	      
	    } catch (FileNotFoundException e) {
	    	logger.error(e);
		} catch (IOException e) {
	    	logger.error(e);
		} catch (Exception e) {
			logger.error(e);
		}
		
		return null;
	}

	

	public static GopherTransactionResult errorMenu(String message)
	{
		
		ArrayList<GopherDirectoryEntity> list = new ArrayList<GopherDirectoryEntity>();
		GopherDirectoryEntity entError;
		entError = newComment(message);
		entError.setType("3");
		list.add(entError);
		return new GopherMenuTransactionResult(list);
	}
	
	public static GopherDirectoryEntity newComment(String message)
	{
		GopherDirectoryEntity newComment = new GopherDirectoryEntity();
		newComment.setType("i");
		newComment.setUsername(message);
		newComment.setSelector("/");
		newComment.setHost("error.host");
		newComment.setPort(1);
		return newComment;
	}

	public String getGophermapName() {
		return gophermapName;
	}

	public void setGophermapName(String gophermapName) {
		this.gophermapName = gophermapName;
	}

	public String getGophertagName() {
		return gophertagName;
	}

	public void setGophertagName(String gophertagName) {
		this.gophertagName = gophertagName;
	}

	public ArrayList<FileTransaction> getFileTransactions() {
		return fileTransactions;
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
	
	/*private void deleteDocInternal(Directory direct2, GopherDirectoryEntity root) {
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
	}*/

	private static void addDocInternal(Directory direct2, GopherDirectoryEntity root, String content)  
	{		
		
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
	}
}
