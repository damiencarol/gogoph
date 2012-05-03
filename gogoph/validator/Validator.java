package gogoph.validator;

import gogoph.GopherDirectoryEntity;
import gogoph.crawler.GopherClient;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.sql.Date;
import java.util.ArrayList;

public class Validator {

	private static final long MAX_FILE_SIZE_MENU = 80 * 120;

	/**
	 * @param args
	 * @throws URISyntaxException 
	 */
	public static void main(String[] args) throws URISyntaxException {
		
		ArrayList<ValidationPoint> vals = new ArrayList<ValidationPoint>();
		
		String preArg = args[0];
		if (!preArg.startsWith("gopher://"))
		{
			preArg = "gopher://" + preArg;
		}
		if (!preArg.endsWith("/"))
		{
			preArg += "/";
		}
		
		//System.out.println("Validating :" + args[0]);
		URI uri = new URI(preArg);
		//URI uri = new URI("gopher://serenity.homeunix.org/1/");
		
		String category;
		
		
		category = "Network";		
		// Check host
		String host = null;
		InetAddress[] hosts;
		try {
			hosts = InetAddress.getAllByName(uri.getHost());
		} catch (UnknownHostException e) {
			addError(vals, category, "Host", e.getMessage(), "");
			printResult(uri.toString(), uri.getHost(), -1, "?", "???", vals);
			return;
		}
		if (hosts.length == 0)
		{
			addError(vals, category, "Host", "No address for host '" + uri.getHost() + "'", "");
			printResult(uri.toString(), uri.getHost(), -1, "?", "???", vals);
			return;
		}
		else
		{
			host = uri.getHost();
			String list = "";
			for (InetAddress add : hosts) {
				if (list.length() > 0)
					list += ", ";
				list += add.getHostAddress();
				
			}
			vals.add(new ValidationPoint(category, "Host", "Address found for host '" + uri.getHost() + "' : ", 1, list));
		}
		
		
		// Check port
		int port = 70;
		if (uri.getPort() != -1)
		{
			port = uri.getPort();
		}
		if (port != 70)
		{
			addWarning(vals, category, "Port", "Port is not the standard RFC 1453 port 70.", 
					"Try to change the port to value 70.");
		}
		else
		{
			addOk(vals, category, "Port", "Port is standard (RFC 1453).");
		}
		
		Socket socks = null;
		try {
			socks = new Socket(host, port);
			addOk(vals, category, "Socket", "Open socket.");
			socks.close();
			addOk(vals, category, "Socket", "Close socket.");
			
		} catch (UnknownHostException e) {
			addError(vals, category, "Socket", e.getMessage(), "");
			printResult(uri.toString(), host, port, "?", "???", vals);
			return;
		} catch (IOException e) {
			addError(vals, category, "Socket", e.getMessage(), "");
			printResult(uri.toString(), host, port, "?", "???", vals);
			return;
		}

		
		category = "Gopher";
		
		// Decode selector and type
		String selector = "";
		String type = "";
		if (uri.getPath().length() == 0) {
			selector = "";
			type = "1";
		} else if (uri.getPath().length() > 2) {
			selector = uri.getPath().substring(2);
			type = uri.getPath().substring(1, 2);
		} else {
			addError(vals, category, "Selector", "Impossible to decode selector/type", 
			"");
			printResult(uri.toString(), host, port, "?", "???", vals);
			return;
		}
			
		
		File binFile = GopherClient.requestBinaryfile(host, port, selector);
		binFile.deleteOnExit();
		if (binFile != null)
			addOk(vals, category, "Binary", "Try to get document in Binary transaction mode.");
		else
		{
			addError(vals, category, "Binary", "Unable to get the document.", 
			"");
			printResult(uri.toString(), host, port, type, selector, vals);
			return;
		}
		
		// Check size of the file
		long file_size = binFile.length();
		if (file_size < MAX_FILE_SIZE_MENU)
		{
			addOk(vals, category, "Binary", "Check file size : " + file_size + ".");
		}
		else
		{
			addError(vals, category, "Binary", "The size is too big for Menu : " + file_size + ".",
				"Check url is correct or try to split Menu in sub Menu.");
			printResult(uri.toString(), host, port, type, selector, vals);
			return;
		}

		// Make some tests with document type
		if (type.equals("1"))
		{
			checkMenu(vals, host, port, selector, uri.toString());
		}

		printResult(uri.toString(), host, port, type, selector, vals);
	}

	private static void checkMenu(ArrayList<ValidationPoint> vals, String host, int port, String selector, String uri) {
		String category = "Document";
		
		
		ArrayList<GopherDirectoryEntity> entries = GopherClient.request(host, port, selector);
		if (entries == null) 
		{
				addError(vals, category, "Menu", "Unable to get the document.", 
				"");
				printResult(uri, host, port, "1", selector, vals);
				return;
		}
		else
			addOk(vals, category, "Menu", "Try to get document in Menu transaction mode.");
		
		// Check number of entries
		int nb_i = 0;
		int nb_not_i = 0;
		for (GopherDirectoryEntity item : entries)
		{
			if (item.getType().equals("i"))
				nb_i++;
			else
				nb_not_i++;
		}
		if (nb_i < 50)
		{
			addOk(vals, category, "Menu", "Number of 'i' nodes is ok '" + nb_i + "'");
		} else if (nb_i < 75)
		{
			addWarning(vals, category, "Menu", "There too much 'i' nodes '" + nb_i + "'.", 
					"Try to limite number of 'i' nodes. Use Text File '/ABOUT' or '/ABOUT.txt' to display more infos.");
		} else
		{
			addError(vals, category, "Menu", "There too much 'i' nodes '" + nb_i + "'.", 
					"Try to limite number of 'i' nodes. Use Text File '/ABOUT' or '/ABOUT.txt' to display more infos.");
		} 
		
		if (nb_not_i < 50)
		{
			addOk(vals, category, "Menu", "Number of nodes is ok '" + nb_not_i + "'");
		} else if (nb_not_i < 75)
		{
			addWarning(vals, category, "Menu", "There too much nodes '" + nb_not_i + "'.", 
					"Try to limite number of nodes. Use sub-directory to display more nodes.");
		} else
		{
			addError(vals, category, "Menu", "There too much 'i' nodes '" + nb_not_i + "'.", 
			"Try to limite number of nodes. Use sub-directory to display more nodes.");
		} 
		
		// Check 'ABOUT' node
		checkAboutNode(entries, vals, category);
		
		for (GopherDirectoryEntity item : entries)
		{
			// Validate each node
			int nbWarn = 0;
			int nbError = 0;
			if (item.getUsername().length() > 70) {
				addWarning(vals, category, "Username", "Username of this Directory entry is too long " + 
					item.getUsername().length() + " (70 in the standard RFC 1453.", 
					"Try to change the size to value 70.");
				nbWarn += 1;
			}
			
			// If the node is broken
			if (!item.getType().equals("i") && !item.getType().equals("8"))
			if (GopherClient.requestBinaryfile(item.getHost(), item.getPort(), item.getSelector())==null)
			{
				addWarning(vals, category, "Menu", "Broken link to [" + item.getHost() + "][" + item.getPort() + "][" + item.getSelector() + "]",
						"Try to change the node.");
				nbWarn += 1;
			}
			
			if (item.getExtra() != null) 
			if (item.getExtra().trim().equals("+")){
				addOk(vals, category, "Gopher+", "Node Gopher+ '" + item.getSelector() + "' detected and well-formed.");
			}
			else {
				addError(vals, category, "Extra", "This Directory entry is not correct. " + 
					"There some extra data (not in the standard RFC 1453) : '" + item.getExtra() + "'.", 
					"Try to change the entry to avoid extra data.");
				nbError += 1;
			}
			if (nbWarn > 0)
				addWarning(vals, category, "Menu", "Warning(s) in node : [" + item.getHost() + "] [" + item.getPort() + "] [" + item.getType() + "] [" + item.getSelector() + "] >>> [" + item.getExtra() + "]", 
				"");
			else if (nbError > 0)
				addError(vals, category, "Menu", "Error(s) in node : [" + item.getHost() + "] [" + item.getPort() + "] [" + item.getType() + "] [" + item.getSelector() + "] >>> [" + item.getExtra() + "]", 
				"");
			else if (!item.getType().equals("i"))
				addOk(vals, category, "Menu", "Node 'gopher://" + item.getHost() + ":" + item.getPort() + "/" + item.getType() + item.getSelector() + "' is OK.");
		}
	}

	private static void checkAboutNode(ArrayList<GopherDirectoryEntity> entries, ArrayList<ValidationPoint> vals, String category) {
		// 'ABOUT' Nodes detection in validator
		
		GopherDirectoryEntity aboutSelector = null;
		for (GopherDirectoryEntity item : entries)
		{
			String comp = item.getSelector().trim().toLowerCase();
			if (comp.equals("about")) {
				aboutSelector = item; break;
			} else if (comp.equals("/about")) {
				aboutSelector = item; break;
			} else if (comp.equals("about.txt")) {
				aboutSelector = item; break;
			} else if (comp.equals("/about.txt")) {
				aboutSelector = item; break;
			} 
		}
		
		if (aboutSelector != null)
		{
			addOk(vals, category, "ABOUT Node", "Node 'ABOUT' found for '" + aboutSelector.getSelector() + "'");
		}
		else
		{
			addWarning(vals, category, "Menu", "No 'ABOUT' node (link to 'ABOUT' , 'ABOUT.txt' , '/ABOUT' or '/ABOUT.txt' is KO).",
				"Add 'ABOUT' file with '/ABOUT' selector with description of you Menu.");
			return;
		}
		
		File aboutFile = GopherClient.requestBinaryfile(aboutSelector.getHost(), aboutSelector.getPort(), 
				aboutSelector.getSelector());
		if (aboutFile == null) {
			addWarning(vals, category, "Menu", "Link to 'ABOUT' node '" + aboutSelector.getSelector() + "' is KO).",
				"Add 'ABOUT' file with '/ABOUT' selector with description of you Menu.");
		}
	}

	private static void addOk(ArrayList<ValidationPoint> vals,
			String category, String subject, String comment) {
		vals.add(new ValidationPoint(category, subject, comment, 1, ""));
	}
	
	private static void addWarning(ArrayList<ValidationPoint> vals,
			String category, String subject, String comment, String resolution) {
		vals.add(new ValidationPoint(category, subject, comment, 0, resolution));
	}

	private static void addError(ArrayList<ValidationPoint> vals,
			String category, String subject, String comment, String resolution) {
		addError(vals, category, subject, comment, -1, resolution);
	}
	
	private static void addError(ArrayList<ValidationPoint> vals,
			String category, String subject, String comment, int level, String resolution) {
		vals.add(new ValidationPoint(category, subject, comment, level, resolution));
	}
	
	@SuppressWarnings("deprecation")
	private static void printResult(String title, String host, int port, String type, String selector,
			ArrayList<ValidationPoint> vals) {
		
		float global_score = 0;
		for (ValidationPoint val : vals)
		{
			global_score += val.getLevel();
		}
		global_score = (global_score / vals.size()) * 100;
		
		
		System.out.println("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">");
		System.out.println("<html xmlns=\"http://www.w3.org/1999/xhtml\" >");
		System.out.println("  <head>");
		System.out.println("    <title>Validate" + title + "</title>");
		System.out.println("    <style type=\"text/css\">.ok-pre { background-color:#55FF55; } .ok { background-color:Green; } .warn-pre { background-color:Yellow; } .warn { background-color:Yellow; } .error-pre { background-color:Red; } .error { background-color:Red; color:Yellow; }</style>");
		System.out.println("  </head>");
		System.out.println("  <body>");
		System.out.println("    <p><strong>" + title + "</strong></p>");
		String url = "gopher://" + host + ":" + port + "/" + type + selector;
		System.out.println("    <p>ANALYSED : <a href=\"" + url + "\">" + url + "</a></p>");
		System.out.println("    <p>GENERATED FROM : <a href=\"gopher://dams.zapto.org/1/\">gopher://dams.zapto.org/1/</a></p>");
		System.out.println("    <table>");
		System.out.println("    <tr><td>Host</td><td>[" + host + "]</td></tr>");
		System.out.println("    <tr><td>Port</td><td>[" + port + "]</td></tr>");
		System.out.println("    <tr><td>Type</td><td>[" + type + "]</td></tr>");
		System.out.println("    <tr><td>Selector</td><td>[" + selector + "]</td></tr>");
		System.out.println("    </table>");
		System.out.println("    <p>Generated : " + new Date(System.currentTimeMillis()).toGMTString() + "</p>");
		
		String cssClassScore;
		if (global_score > 10) {
			cssClassScore = "ok";
		} else if (global_score < -10) {
			cssClassScore = "error";
		} else {
			cssClassScore = "warn";
		}
		System.out.println("    <p class=\"" + cssClassScore + "-pre\">GLOBAL SCORE = " + global_score + "%</p>");
		
		
		System.out.println("    <table cellpadding=\"5\" > ");
		String lastCat = "";
		for (ValidationPoint item : vals) {
			// Headers
			if (item.getCategory() != lastCat)
			{
				lastCat = item.getCategory();
				System.out.println("      <tr>");
				System.out.println("        <th>Category</th>");
				System.out.println("        <th>Subject</th>");
				System.out.println("        <th>Comment</th>");
				System.out.println("        <th>Level</th>");
				System.out.println("      </tr>");
			}			
			
			System.out.println("      <tr>");
			String level= "";
			if (item.getLevel() > 0)
			{
				level = "ok";
			} else if (item.getLevel() == 0)
			{
				level = "warn";
			} else {
				level = "error";
			}
			
			System.out.println("        <td class=\"" + level + "-pre\">" + item.getCategory() + "</td>");
			System.out.println("        <td class=\"" + level + "-pre\">" + item.getSuject() + "</td>");
			if (item.getResolution() == "")
				System.out.println("        <td class=\"" + level + "-pre\"><p>" + item.getComment() + "</p></td>");
			else
				System.out.println("        <td class=\"" + level + "-pre\"><p>" + item.getComment() + "</p><p>" + item.getResolution() + "</p></td>");
			System.out.println("        <td class=\"" + level + "\">" + level + "</td>");
			System.out.println("      </tr>");
		}
		System.out.println("    </table>");
		System.out.println("    <p></p>");
		System.out.println("    <p>GOGOPH - Gopher Validator - Copyright (C) 2012  Damien CAROL</p>");
		System.out.println("  </body>");
		System.out.println("</html>");
	}

}
