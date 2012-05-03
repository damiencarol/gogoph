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

import gogoph.GopherDirectoryEntity;
import gogoph.GopherMenuTransactionResult;
import gogoph.GopherTransactionResult;
import gogoph.search.SearchResult;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.log4j.Logger;
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
import org.apache.lucene.util.Version;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;

public class GopherSearchTransactionResult extends GopherTransactionResult {

	private static final Logger logger = Logger
			.getLogger(GopherSearchTransactionResult.class.getName());

	private Directory indexDirectory;

	public GopherSearchTransactionResult(Directory directory) {
		indexDirectory = directory;
	}

	/**
	 * 
	 */
	public ChannelFuture processChannel(Channel channel, String queryString) {
		
		try {
			// 0. Specify the analyzer for tokenizing text.
		    //    The same analyzer should be used for indexing and searching
		    StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_35);
		    
			// 2. query
		    /*String[] querystr = gopher_query.split(" ");

		    BooleanQuery query = new BooleanQuery();
		    
		    for (int i = 0; i < querystr.length; i++)
		    {
		    	Query query1 = new TermQuery(new Term("title", querystr[i].toLowerCase()));
		    	Query query2 = new TermQuery(new Term("content", querystr[i].toLowerCase()));
		    	Query query3 = new TermQuery(new Term("host", querystr[i].toLowerCase()));
		    	query.add(query1, Occur.SHOULD);
		    	query.add(query2, Occur.SHOULD);
		    	query.add(query3, Occur.SHOULD);
			}*/

		    QueryParser parser = new QueryParser(Version.LUCENE_35, "content", analyzer);
		    Query query = parser.parse(queryString);
		    
		    // 3. search
		    int hitsPerPage = 40;
		    IndexReader reader = IndexReader.open(indexDirectory);
		    IndexSearcher searcher = new IndexSearcher(reader);
		    TopScoreDocCollector collector = TopScoreDocCollector.create(hitsPerPage, true);
		    searcher.search(query, collector);
		    ScoreDoc[] hits = collector.topDocs().scoreDocs;
		    
		    // 4. display results
		    SearchResult[] tib = new SearchResult[hits.length];
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
		      
		      
		      tib[i] = new SearchResult(gop.getUsername(), gop, hits[i].score);
		    }

		    // searcher can only be closed when there
		    // is no need to access the documents any more.
		    searcher.close();
		    reader.close();
		    
		    if (tib == null) {
	    		//return errorMenu("Invalid Selector");
	    		ArrayList<GopherDirectoryEntity> list = new ArrayList<GopherDirectoryEntity>();
	    		GopherDirectoryEntity entError;
	    		entError = newComment("Invalid Selector");
	    		entError.setType("3");
	    		list.add(entError);
	    		return new GopherMenuTransactionResult(list).processChannel(channel, queryString);
	    	}
	    	
	    	ArrayList<GopherDirectoryEntity> tab;
			tab = new ArrayList<GopherDirectoryEntity>();
			for (int i=0; i< tib.length; i++)
	    	{
				SearchResult item = tib[i];
				GopherDirectoryEntity node = item.getEntity();
				node.setUsername("(Score: " + item.getScore() + ") " + item.getTitle());
				
				GopherDirectoryEntity nodeComment = newComment("gopher://" + node.getHost() + ":" + 
							node.getPort() + "/" + node.getType() + node.getSelector());
				
				//GopherDirectoryEntity nodeComment2 = 
				//	GopherDirectoryEntity.newComment(node.getUserName());

				tab.add(node);
				tab.add(nodeComment);
				//tab.add(nodeComment2);
		    }
			
		    return new GopherMenuTransactionResult(tab).processChannel(channel, queryString);
		    
			} catch (IOException e) {
				logger.error(e);
			} catch (ParseException e) {
				logger.error(e);
			}
			return null;
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
}
