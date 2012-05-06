package gogoph.crawler.gen3;

import gogoph.crawler.CrawlerSiteNode;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
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

public class SiteIndexer {

	private static final Logger logger = Logger.getLogger(SiteIndexer.class
			.getName());

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {

		// Set up a simple configuration that logs on the console.
		// BasicConfigurator.configure();
		DOMConfigurator.configure("log4j.conf.xml");

		String host;
		int port;
		if (args[0].contains(":")) {
			port = Integer.parseInt(args[0].split(":")[1]);
			host = args[0].split(":")[0];
		} else {
			port = 70;
			host = args[0];
		}

		logger.info("HOST:" + host);
		logger.info("PORT:" + port);

		// Create crawler root
		File crawlDir = new File("crawler");
		if (!crawlDir.exists()) {
			crawlDir.mkdir();
		}

		// Get site dir
		File folder = new File(crawlDir.getPath() + File.separator + host + ":"
				+ port);
		if (!folder.exists()) {
			folder.mkdir();
		}

		// Try to read selector file
		File textFile = new File(folder.getPath() + File.separator
				+ "selectors.crawled");
		textFile.createNewFile();
		ArrayList<CrawlerSiteNode> tab_selectors = new ArrayList<CrawlerSiteNode>();
		BufferedReader dis = null;
		try {
			dis = new BufferedReader(new FileReader(textFile));
			String line;
			while ((line = dis.readLine()) != null) {
				CrawlerSiteNode node = new CrawlerSiteNode(line.split("\t")[1],
						line.split("\t")[2], line.split("\t")[0]);

				tab_selectors.add(node);
				logger.info("SELECTOR:" + line.split("\t")[0]);
			}
		} catch (Exception e) {
			logger.error(e);
			return;
		}

		// Connect to the index
		Directory dir = new SimpleFSDirectory(new File(args[1]));

		// For each selector index
		for (int i = 0; i < tab_selectors.size(); i++) {
			CrawlerSiteNode node = tab_selectors.get(i);
			String path = new String(Base64.encode(node.getType()
					+ node.getSelector()));
			deleteDocInternal(dir, host, port, node.getSelector());
			String content = readfile(folder.getPath() + File.separator + path
					+ ".link");
			addDocInternal(dir, node.getType(), node.getUsername(), 
					host, port, node.getSelector(), content);
		}
		
		// Closing directory
		dir.close();
	}

	private static String readfile(String pathfile)
			throws IOException {
		logger.info("SAVING CONTENT '" + pathfile + "'");
		File file = new File(pathfile);
		String content = "";
		BufferedReader dis = null;
		try {
			dis = new BufferedReader(new FileReader(file));
			String line;
			while ((line = dis.readLine()) != null) {
				content += " " + line;
			}
		} catch (Exception e) {
			logger.error(e);
		}
		return content;
	}

	private static void deleteDocInternal(Directory direct2, String host, int port,
			String selector) {

		// 0. Specify the analyzer for tokenizing text.
		// The same analyzer should be used for indexing and searching
		Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_35);

		IndexWriter w = null;
		IndexWriterConfig conf = new IndexWriterConfig(Version.LUCENE_35,
				analyzer);
		conf.setWriteLockTimeout(2000);

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

	private static void addDocInternal(Directory direct2, String type, String title,
			String host, int port, String selector, String content) {

		// 0. Specify the analyzer for tokenizing text.
		// The same analyzer should be used for indexing and searching
		Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_35);

		IndexWriter w = null;
		IndexWriterConfig conf = new IndexWriterConfig(Version.LUCENE_35,
				analyzer);
		conf.setWriteLockTimeout(2000);

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
			doc.add(new Field("content", title, Field.Store.YES,
					Field.Index.ANALYZED));

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
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}

	}
}
