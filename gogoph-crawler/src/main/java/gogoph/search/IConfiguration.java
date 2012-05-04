package gogoph.search;

import gogoph.crawler.GopherDirectoryEntity;

import java.io.File;
import java.io.IOException;

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
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.Version;

public class IConfiguration {
	
	private static final Logger logger = Logger.getLogger(
    		IConfiguration.class.getName());

	private StandardAnalyzer analyzer;
	
	private Directory index;

	public IConfiguration(File pIndexFile) {
		// 0. Specify the analyzer for tokenizing text.
	    //    The same analyzer should be used for indexing and searching
	    analyzer = new StandardAnalyzer(Version.LUCENE_34);

	    // 1. create the index
	    try {
			index = new SimpleFSDirectory(pIndexFile);
		} catch (IOException e1) {
			e1.printStackTrace();
		}//new RAMDirectory();

	    //new IndexWriterConfig(Version.LUCENE_34, analyzer);
	}
	

	
	public SearchResult[] search(String gopher_query)
	{
		try {
		
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
	    Query query = parser.parse(gopher_query);
	    
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
	    
	    return tab;
	    
		} catch (IOException e) {
			logger.error(e);
		} catch (ParseException e) {
			logger.error(e);
		}
		return null;
	}

	public int getIndexSize() {
		int num = -1;
		try {
			IndexReader reader = IndexReader.open(index);
		    num = reader.numDocs();
		    reader.close();
		} catch (IOException e) {
			logger.error(e);
		}
		return num;
	}
	
/*
	private XMLConfig config;
	private SearchEngine se;

	public IConfiguration() {
		super();
		try {
			// First, create configuration object. Do you remember where is your
			// jse.xml?
			config = new XMLConfig("jse.xml");

			// Create search engine object, master of all your searches.

			se = new SearchEngine(config);
			// Delete all stuff from storage, if there were something old and
			// useless.
			se.clean();
			// This function downloads URL from the website without crawling it
			se.addURL(new String[] { "http://www.me.lv/jse/" });
			se.addURL(new String[] { "gopher://gopher.floodgap.com/" });
			
			
			se.crawlURL(new String[] { "gopher://gopher.floodgap.com/" });
			
			
			// This function creates index
			se.index();
			
		} catch (fs e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public ResultPage getSearchResult(String searchTerms) {
		ResultPage rp1 = null;
		try {

			// Everything is done, now we can try to search something and print
			// it out. You will see some XML file with results.
			rp1 = se.search(searchTerms, "en", 0);
			System.out.println(rp1);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return rp1;
	}
*/
}
