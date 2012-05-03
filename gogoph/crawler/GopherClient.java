package gogoph.crawler;

import gogoph.GopherDirectoryEntity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

import org.apache.log4j.Logger;

public class GopherClient {
	
    private static final Logger logger = Logger.getLogger(
    		GopherClient.class.getName());

    /*public static ArrayList<GopherDirectoryEntity> request(URI uri) throws Exception {
    	
    	// Check that url is gopher
    	if (!uri.getScheme().equals("gopher"))
    		throw new URISyntaxException("Invalid scheme : " + uri.getScheme(), "The scheme must be 'gopher'");
    	
    	String host = uri.getHost();
    	int port = uri.getPort();
    	if (port == -1)
    	{
    		port = 70;
    	}
    	
    	String path = uri.getPath();
    	String selector;
    	if (path.length() < 3)
    	{
    		selector = "/";
    	}
    	else
    		selector = path.substring(3);
    	
    	
    	return request(host, port, selector);
	}*/
    
	public static ArrayList<GopherDirectoryEntity> request(String host, int port, String selector) {

		try {

			Socket mySocket = null;
			PrintWriter out = null;
			BufferedReader in = null;
			
			mySocket = new Socket(host, port);
			out = new PrintWriter(mySocket.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(mySocket
					.getInputStream()));
			mySocket.setSoTimeout(5000);
			
			// Send blank one
			out.println(selector); 
			String line = in.readLine();
			ArrayList<GopherDirectoryEntity> list = new ArrayList<GopherDirectoryEntity>();
			long i = 1;
			while (line != null)
			{
				if (!line.equals("."))
				{
					try
					{
						// Hack for i nodes
						/*if (line.startsWith("i"))
						{
							if (line.length() )
						}
						else*/
						{
							GopherDirectoryEntity item = null;
							item = new GopherDirectoryEntity(line);
							list.add(item);
						}
					} 
					catch (Exception e) 
					{
						logger.error("Invalid Menu Entity in gopher://" + host + ":" + port + "/1" + selector);
						logger.error("Invalid Menu Entity in [" + host + "][" + port + "][1][" + selector + "]");
						logger.error("Line(" + i + ") = '" + line + "'");
						logger.error(e);
					}	
				}
				line = in.readLine();
				i++;
			}
			
			mySocket.shutdownOutput();
			//Thread.sleep(1000);
			mySocket.close();
			//Thread.sleep(1000);
			return list;
			
		} catch (UnknownHostException e) {
			logger.error(e);
		} catch (IOException e) {
			logger.error(e);
		} 

		logger.error("In node [" + host + "][" + port + "][1][" + selector + "]");
		return null;
	}



	public static String requestTextEntity(String host, int port, String selector) {
	
		try {

			Socket mySocket = null;
			PrintWriter out = null;
			BufferedReader in = null;
			
			
			mySocket = new Socket(host, port);
			out = new PrintWriter(mySocket.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(mySocket
					.getInputStream()));
			mySocket.setSoTimeout(5000);

			// Send the selector
			out.println(selector); 
			String line = in.readLine();
			StringBuilder res = new StringBuilder();
			long i = 1;
			while (line != null)
			{
				res.append(line + "\r\n");
				
				if (line.equals("."))
				{
					/*GopherDirectoryEntity item = new GopherDirectoryEntity(line);
					//if (item.type != GopherType.fromTypeToString(GopherType.Info))
						list.add(item);*/
					break;
				}
				line = in.readLine();
				i++;
				
				if (i > 5000)
				{
					break;
				}
			}
			
			mySocket.shutdownOutput();
			mySocket.close();

			return res.toString();
			
		} catch (UnknownHostException e) {
			logger.error(e);
		} catch (IOException e) {
			logger.error(e);
		}
		
		logger.error("In node [" + host + "][" + port + "][0][" + selector + "]");
		return null;
	}
	
	public static File requestBinaryfile(String host, int port, String selector) {

		try {
			InetAddress address = InetAddress.getByName(host);

			Socket mySocket = null;
			mySocket = new Socket(address, port);
			mySocket.setSoTimeout(5000);
			PrintWriter out = null;
			out = new PrintWriter(mySocket.getOutputStream(), true);

			// Send the selector
			out.println(selector); 
			
			
			
			File tmpFile = File.createTempFile(host + "-" + port + "-", "");
			RandomAccessFile nFile = new RandomAccessFile(tmpFile, "rw");
			FileOutputStream file = new FileOutputStream(tmpFile);
			byte[] b = new byte[1024];
			int datum = mySocket.getInputStream().read(b, 0, 1024);
			while (datum != -1) {
				nFile.write(b, 0, datum);
				datum = mySocket.getInputStream().read(b, 0, 1024);
			}
				
			file.flush();
			file.close();

			mySocket.close();

			return tmpFile;
			
		} catch (UnknownHostException e) {
			logger.error(e);
		} catch (IOException e) {
			logger.error(e);
		}
		logger.error("In node [" + host + "][" + port + "][?][" + selector + "]");
		
		return null;
	}
}
