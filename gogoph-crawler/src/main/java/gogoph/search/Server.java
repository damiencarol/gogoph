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
package gogoph.search;

import gogoph.crawler.GopherDirectoryEntity;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.Version;

//import org.apache.log4j.Logger;

public class Server {
	
	//private static final Logger logger = Logger.getLogger(
	//		Server.class.getName());

	/**
	 * @param args
	 * @throws IOException 
	 * @throws ParseException 
	 */
	public static void main(String[] args) throws IOException, ParseException {
		
		Directory index;
    	index = new SimpleFSDirectory(new File(args[0]));
		
		String searchTerms = args[1];
		
		
		StandardAnalyzer analyzer;
		// 0. Specify the analyzer for tokenizing text.
	    //    The same analyzer should be used for indexing and searching
	    analyzer = new StandardAnalyzer(Version.LUCENE_35);

	    QueryParser parser = new QueryParser(Version.LUCENE_35, "content", analyzer);
	    Query query = parser.parse(searchTerms);
	    
	    // 3. search
	    int hitsPerPage = 40;
	    IndexReader reader = IndexReader.open(index);
	    IndexSearcher searcher = new IndexSearcher(reader);
	    TopScoreDocCollector collector = TopScoreDocCollector.create(hitsPerPage, true);
	    searcher.search(query, collector);
	    ScoreDoc[] hits = collector.topDocs().scoreDocs;
	   
	    // 4. display results
	    SearchResult[] tab = new SearchResult[hits.length];
	    //System.out.println("Found " + hits.length + " hits.");
	    for(int i=0;i<hits.length;++i) {
	      int docId = hits[i].doc;
	      Document d = searcher.doc(docId);
	      //System.out.println((i + 1) + ". " + d.get("title"));
	      
	      GopherDirectoryEntity gop = new GopherDirectoryEntity();
	      gop.setType(d.get("type"));
	      gop.setUsername(d.get("title"));
	      gop.setHost(d.get("host"));
	      gop.setPort(Integer.parseInt(d.get("port")));
	      gop.setSelector(d.get("selector"));
	      
	      
	      tab[i] = new SearchResult(gop.getUsername(), gop, hits[i].score);
	    }

	    // searcher can only be closed when there
	    // is no need to access the documents any more.
	    searcher.close();
	    reader.close();
    	
    	ArrayList<GopherDirectoryEntity> tib;
		tib = new ArrayList<GopherDirectoryEntity>();
		for (int i=0; i< tab.length; i++)
    	{
			SearchResult item = tab[i];
			GopherDirectoryEntity node = item.getEntity();
			node.setUsername("(Score: " + item.getScore() + ") " + item.getTitle());
			
			GopherDirectoryEntity nodeComment = newComment("gopher://" + node.getHost() + ":" + 
						node.getPort() + "/" + node.getType() + node.getSelector());
			
			//GopherDirectoryEntity nodeComment2 = 
			//	GopherDirectoryEntity.newComment(node.getUserName());

			tib.add(node);
			tib.add(nodeComment);
			//tab.add(nodeComment2);
	    }
		index.close();
	
		// Load index
		for (GopherDirectoryEntity item : tib)
		{
			System.out.print(item.getType() + item.getUsername() + "\t" +
					item.getSelector() + "\t" +
					item.getHost()  + "\t" +
					item.getPort() + "\r\n");
		}
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
	public static GopherDirectoryEntity newSearch(String username, String host,
			String selector) {
		GopherDirectoryEntity newOne = new GopherDirectoryEntity();
		newOne.setType("1");
		newOne.setUsername(username);
		newOne.setHost(host);
		newOne.setPort(70);
		newOne.setSelector(selector);
		return newOne;
	}
}
