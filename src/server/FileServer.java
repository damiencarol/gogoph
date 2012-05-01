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

import gogoph.GopherPipelineFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

public class FileServer {

	private static final Logger logger = Logger.getLogger(
			FileServer.class.getName());
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) {
		
		// Print license
		printLicense();
		
		// Set up a simple configuration that logs on the console.
        BasicConfigurator.configure();

		
		// Load properties
		Properties props = new Properties();
		try {
			props.load(new FileReader(args[0]));
		} catch (FileNotFoundException e1) {
			logger.fatal(e1);
			return;
		} catch (IOException e1) {
			logger.fatal(e1);
			return;
		}
		
		// Set up extended conf
		if (props.containsKey("logconf"))
		{
			logger.info("Loading extended conf " + props.getProperty("logconf"));
			DOMConfigurator.configure(props.getProperty("logconf"));
		}
		
		// Create local address
		InetSocketAddress address = new InetSocketAddress(props.getProperty("ip"), 
				Integer.parseInt(props.getProperty("port")));
		
		// Get reference to the dir
		File dir = new File(props.getProperty("live"));
		
		// Allocate file handler
		RootFileServerRequestHandler fileHandler = new RootFileServerRequestHandler();

		
		// Read the gopher map file name
		if (props.containsKey("gophermap"))
			fileHandler.setGophermapName(props.getProperty("gophermap").trim());
		else
			fileHandler.setGophermapName("gophermap");
		
		// Read the gopher tag file name
		if (props.containsKey("gophertag"))
			fileHandler.setGophertagName(props.getProperty("gophertag").trim());
		else
			fileHandler.setGophertagName("gophertag");
		
		// Read the extentions list
		for (int i = 0; i < 50; i ++)
		{
			if (props.containsKey("ext" + i + ".type"))
			{
				FileTransaction tr = new FileTransaction();
				tr.setType(props.getProperty("ext" + i + ".type").trim());
				tr.setTransaction(props.getProperty("ext" + i + ".transaction").trim());
				
				for (int j = 0; j < 50; j ++)
				{
					if (props.containsKey("ext" + i + ".regex" + j))
					{
						tr.getRegexs().add(Pattern.compile(props.getProperty("ext" + i + ".regex" + j)));
					}
				}
				
				fileHandler.getFileTransactions().add(tr);
			}
		}
		
		
		// Populate with live directory
		try {
			fileHandler.populate(dir, props.getProperty("host"), address.getPort());
		} catch (IOException e1) {
			logger.fatal(e1);
			return;
		}
		
		
		// Configure the server.
		ChannelFactory factory = new NioServerSocketChannelFactory(Executors
				.newCachedThreadPool(), Executors.newCachedThreadPool());
		ServerBootstrap bootstrap = new ServerBootstrap(factory);
		
		// Set up the event pipeline factory.
		bootstrap.setPipelineFactory(new GopherPipelineFactory(
				fileHandler));

		// Bind and start to accept incoming connections.
		logger.info("Start server");
		bootstrap.bind(address);
		
		
		
		
		try {
			boolean exit = false;
			Scanner in = new Scanner(System.in);
			while (!exit)
			{
				if (in.hasNext())
				{
					String cmd = in.nextLine();
					logger.info("CMD = " + cmd);
					if (cmd == null)
						logger.info("CMD = " + cmd);
					else if (cmd.equals("stop"))
						exit = true;
					else if (cmd.equals("status"))
						System.out.println("running");
				}
				Thread.sleep(1000);
			}
			
		} catch (InterruptedException e) {
			logger.error(e);
		} finally {
			logger.info("Shutting down server");
			bootstrap.releaseExternalResources();
		}
	}
	
	private static String license = "Gogoph Copyright (C) 2012  Damien CAROL\r\n" +
		"\r\n" +
		"This program comes with ABSOLUTELY NO WARRANTY.\r\n" +
		"This is free software, and you are welcome to redistribute it\r\n" +
		"under certain conditions.\r\n";
	
	/**
	 * Print license.
	 */
	private static void printLicense() {
		System.out.println(license);
	}
}
