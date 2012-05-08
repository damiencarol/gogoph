package gogoph.crawler.gen3;

import gogoph.crawler.CrawlerSiteNode;
import gogoph.crawler.GopherClient;
import gogoph.crawler.GopherDirectoryEntity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
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
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.Version;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.ContentHandler;

public class SelectorIndexer {

	private static final Logger logger = Logger.getLogger(SelectorIndexer.class
			.getName());
	private static final long LOCK_TIMEOUT = 5000;

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {

		// Set up a simple configuration that logs on the console.
		// BasicConfigurator.configure();
		DOMConfigurator.configure("log4j.conf.xml");

		File indexFile = new File(args[0]);
		String host = args[1];
		int port = Integer.parseInt(args[2]);
		String selector = args[3];
		String type = args[4];
		String title = selector;
		if (args.length > 5)
			title = args[5];

		logger.info("INDEX:" + indexFile.getPath());
		logger.info("HOST:" + host);
		logger.info("PORT:" + port);
		logger.info("SELECTOR:" + selector);
		logger.info("TYPE:" + type);
		logger.info("TITLE:" + title);
		
		// Don't manage 

		// Connect to the index
		Directory dir = new SimpleFSDirectory(indexFile);

		// Get the node
		CrawlerSiteNode node = new CrawlerSiteNode(type, title, selector);
		Reader content = null;
		if (type.equals("1"))
			content = crawlMenu(host, port, node);
		else if (type.equals("0"))
			content = crawlGeneric(host, port, node);
		else
			content = new StringReader(title); 

		// If we can get content
		if (content != null)
		{
			deleteDocInternal(dir, host, port, node.getSelector());
			addDocInternal(dir, node.getType(), node.getUsername(), host, port,
					node.getSelector(), content);
		}

		// Closing directory
		dir.close();
		
		rawFile.deleteOnExit();
	}
	private static File rawFile;
	
	private static Reader crawlMenu(String host, int port, CrawlerSiteNode node) {
		
		// Get raw file
		rawFile = GopherClient.requestBinaryfile(host, port, node
				.getSelector());
		if (rawFile == null) {
			logger.error(">>> ERROR IN CRAWL : [" + host + "][" + port + "]["
					+ node.getType() + "][" + node.getSelector() + "]");
			return null;
		}
		
		// Get content of menu
		ArrayList<GopherDirectoryEntity> tab;
		tab = GopherClient.readFromFile(rawFile);

		if (tab == null) {
			logger.error(">>> ERROR IN CRAWLED : [" + host + "][" + port + "]["
					+ node.getType() + "][" + node.getSelector() + "]");
			return null;
		}

		logger.info(">>> CRAWLED : [" + host + "][" + port + "]["
				+ node.getType() + "][" + node.getSelector() + "]");

		// Get content with all nodes of the menu
		// and detect errors
		StringBuilder str = new StringBuilder();
		boolean err = false;
		for (int i = 0; i < tab.size(); i++) {
			if (tab.get(i).getType() != null) {
				if (tab.get(i).getType().equals("3")) {
					err = true;
					break;
				} else {
					if (tab.get(i).getUsername() != null)
						str.append(tab.get(i).getUsername() + " ");
				}
			}
		}
		// If error then return null
		if (err) {
			logger.error(">>> ERROR IN CRAWL : [" + host + "][" + port + "]["
					+ node.getType() + "][" + node.getSelector()
					+ "] node Error(3) detected");
			return null;
		}

		// Try to get the "ABOUT" node
		int index_about = -1;
		for (int i = 0; i < tab.size(); i++) {
			GopherDirectoryEntity ent = tab.get(i);
			if (ent.getSelector() != null)
				if (ent.getSelector().toLowerCase().trim().equals("/about")
						|| ent.getSelector().toLowerCase().trim().equals(
								"/about.txt")
						|| ent.getSelector().toLowerCase().trim().equals(
								"about")
						|| ent.getSelector().toLowerCase().trim().equals(
								"about.txt")) {
					index_about = i;
					break;
				}
		}
		if (index_about != -1) {
			for (int i = 0; i < tab.size(); i++) {
				if (tab.get(i).getSelector().toLowerCase().contains("about")
						&& tab.get(i).getType().equals("0")) {
					index_about = i;
					break;
				}
			}
		}
		if (index_about > -1) {
			File ABOUT_node = GopherClient.requestBinaryfile(tab.get(
					index_about).getHost(), tab.get(index_about).getPort(), tab
					.get(index_about).getSelector());
			if (ABOUT_node != null) {
				logger.info(">>> CRAWLED 'ABOUT' IN NODE : [" + host + "]["
						+ port + "][" + node.getType() + node.getSelector()
						+ "]");
				try {
					return new FileReader(ABOUT_node);
				} catch (FileNotFoundException e) {
					logger.error(e);
				}
			} else {
				logger.error(">>> ERROR IN CRAWLING 'ABOUT' IN NODE : [" + host
						+ "][" + port + "][" + node.getType()
						+ node.getSelector() + "]");
			}
			return null;
		}

		try {
			return new FileReader(rawFile);
		} catch (FileNotFoundException e) {
			logger.error(e);
		}
		return null;
	}

	private static Reader crawlGeneric(String host, int port,
			CrawlerSiteNode node) {

		// Get raw file
		rawFile = GopherClient.requestBinaryfile(host, port, node
				.getSelector());
		if (rawFile == null) {
			logger.error(">>> ERROR IN CRAWL : [" + host + "][" + port + "]["
					+ node.getType() + "][" + node.getSelector() + "]");
			return null;
		}

		logger.info(">>> CRAWLED : [" + host + "][" + port + "]["
				+ node.getType() + "][" + node.getSelector() + "]");

		Reader content = null;
		String contentStr;
		contentStr = node.getUsername();
		try {
			FileInputStream is = null;
			try {
				is = new FileInputStream(rawFile);

				ContentHandler contenthandler = new BodyContentHandler();
				Metadata metadata = new Metadata();
				metadata.set(Metadata.RESOURCE_NAME_KEY, rawFile.getName());
				Parser parser = new AutoDetectParser();
				parser.parse(is, contenthandler, metadata, new ParseContext());

				for (String str : metadata.names()) {
					if (metadata.isMultiValued(str)) {
						logger.debug("[" + str + "] is multi value");
						for (String sub : metadata.getValues(str)) {
							logger.debug("[" + str + "]='" + sub + "'");
							contentStr += " " + sub;
						}
					} else {
						logger.debug("[" + str + "]='" + metadata.get(str)
								+ "'");
						contentStr += " " + metadata.get(str);
					}
				}
				contentStr += " " + contenthandler.toString();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (is != null)
					is.close();
			}

			if (contentStr == null) {
				logger.error(">>> ERROR IN CRAWL : [" + host + "][" + port
						+ "][" + node.getType() + "][" + node.getSelector()
						+ "]");
				return null;
			}
			content = new StringReader(contentStr);

			logger.info(">>> CRAWLED : [" + host + "][" + port + "]["
					+ node.getType() + "][" + node.getSelector() + "]");

			return content;

		} catch (FileNotFoundException e) {
			logger.error(e);
			logger.error(">>> ERROR IN CRAWL : [" + host + "][" + port + "]["
					+ node.getType() + "][" + node.getSelector() + "]");
		} catch (IOException e) {
			logger.error(e);
			logger.error(">>> ERROR IN CRAWL : [" + host + "][" + port + "]["
					+ node.getType() + "][" + node.getSelector() + "]");
		}

		return content;
	}

	private static void deleteDocInternal(Directory direct2, String host,
			int port, String selector) {

		// 0. Specify the analyzer for tokenizing text.
		// The same analyzer should be used for indexing and searching
		Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_35);

		IndexWriter w = null;
		IndexWriterConfig conf = new IndexWriterConfig(Version.LUCENE_35,
				analyzer);
		conf.setWriteLockTimeout(LOCK_TIMEOUT);

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
		} finally {
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

	}

	/*private static void addDocInternal(Directory direct2, String type,
			String title, String host, int port, String selector, String content) {*/
	private static void addDocInternal(Directory direct2, String type,
			String title, String host, int port, String selector, Reader content) {

		// 0. Specify the analyzer for tokenizing text.
		// The same analyzer should be used for indexing and searching
		Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_35);
		//analyzer = new org.apache.lucene.analysis.standard.

		IndexWriter w = null;
		IndexWriterConfig conf = new IndexWriterConfig(Version.LUCENE_35,
				analyzer);
		conf.setWriteLockTimeout(LOCK_TIMEOUT);

		try {
			w = new IndexWriter(direct2, conf);

			Document doc = new Document();
			doc.add(new Field("type", type, Field.Store.YES,
					Field.Index.NOT_ANALYZED));
			doc.add(new Field("title", title, Field.Store.YES,
					Field.Index.ANALYZED));
			doc.add(new Field("host", host, Field.Store.YES,
					Field.Index.NOT_ANALYZED));
			doc.add(new Field("port", "" + port, Field.Store.YES,
					Field.Index.NOT_ANALYZED));
			doc.add(new Field("selector", selector, Field.Store.YES,
					Field.Index.NOT_ANALYZED));
			doc.add(new Field("content", content));
			/*doc.add(new Field("content", content, Field.Store.YES,
			Field.Index.ANALYZED));*/

			w.commit();
			
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
		} finally {
			// Close
			if (w != null)
				try {
					w.close();
				} catch (CorruptIndexException e) {
					logger.error(e);
				} catch (IOException e) {
					logger.error(e);
				}
		}

	}
}
