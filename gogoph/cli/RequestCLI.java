package gogoph.cli;

import gogoph.GopherDirectoryEntity;
import gogoph.crawler.GopherClient;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import org.apache.log4j.BasicConfigurator;

public class RequestCLI {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		BasicConfigurator.configure();
		
		
		try {
			//System.out.println("parsing :" + args[0]);
			URI uri = new URI(args[0]);
			
			
			ArrayList<GopherDirectoryEntity> entries = GopherClient.request(uri.getHost(), uri.getPort(), uri.getPath());
			
			for (GopherDirectoryEntity gop : entries) {
				System.out.println(gop.getUsername());
			}
			
			/*System.out.println("<html>");
			System.out.println("  <head>");
			System.out.println("  </head>");
			System.out.println("  <body>");
			for (GopherDirectoryEntity gop : entries) {
				if (gop.getType() == GopherType.Info)
				{
					System.out.println("    <p>" + gop.getUserName() + "</p>");
				} else if (gop.getType() == GopherType.Web)
				{
					System.out.println("    <p><a href=\"" + gop.getSelector().substring(4) + "\">" + gop.getUserName() + "</a></p>");
				} else {
					System.out.println("    <p><a href=\"gopher://" + gop.getHost() + "/" + gop.getType().toString() + gop.getSelector() + "\">" + 
						gop.getUserName() + "</a></p>");
				}
			}
			System.out.println("  </body>");
			System.out.println("</html>");*/
			
			
			
			//final String[] browserArgs = {"http://google.com/", "nodebug", "dispose"};
	        //se.bysoft.sureshot.gui.browser.MiniBrowser.main(browserArgs);
			
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

}
