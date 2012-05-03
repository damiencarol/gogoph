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

import gogoph.GopherDirectoryEntity;

import java.io.File;
import java.util.ArrayList;

//import org.apache.log4j.Logger;

public class Server {
	
	//private static final Logger logger = Logger.getLogger(
	//		Server.class.getName());

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		/*
		// Set up a simple configuration that logs on the console.
        BasicConfigurator.configure();

		// Configure the server.
		ServerBootstrap bootstrap = new ServerBootstrap(
				new NioServerSocketChannelFactory(Executors
						.newCachedThreadPool(), Executors.newCachedThreadPool()));

		// Load index
		IConfiguration lControllerFactory = new IConfiguration(new File(args[0]));
		
		GopherContext ctx = new GopherContext(lControllerFactory, "127.0.0.1");
		
		// Set up the event pipeline factory.
		bootstrap.setPipelineFactory(new GopherPipelineFactory(ctx));

		// Bind and start to accept incoming connections.
		bootstrap.bind(new InetSocketAddress(70));
		
		try {
			System.in.read();
		} catch (IOException e) {
		} finally {
			logger.info("Shutting down server");
			bootstrap.releaseExternalResources();
		}*/
		String gopher_query = args[1];
		
		// Load index
		IConfiguration lControllerFactory = new IConfiguration(new File(args[0]));
		for (GopherDirectoryEntity item : writeSearch(lControllerFactory, gopher_query))
		{
			System.out.print(item.getType() + item.getUsername() + "\t" +
					item.getSelector() + "\t" +
					item.getHost()  + "\t" +
					item.getPort() + "\r\n");
		}
	}
	
	private static ArrayList<GopherDirectoryEntity> writeSearch(IConfiguration fConfiguration, String searchTerms) {
    	
    	
    	SearchResult[] tib = fConfiguration.search(searchTerms);
    	
    	if (tib == null) {
    		//return errorMenu("Invalid Selector");
    		ArrayList<GopherDirectoryEntity> list = new ArrayList<GopherDirectoryEntity>();
    		GopherDirectoryEntity entError;
    		entError = newComment("Invalid Selector");
    		entError.setType("3");
    		list.add(entError);
    		return list;
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
    	
		return tab;
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
