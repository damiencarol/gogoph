package gogoph.crawler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

import org.apache.log4j.Logger;

/**
 * 
 * @author D.Carol
 *
 */
public class GopherClient {
	
    private static final Logger logger = Logger.getLogger(
    		GopherClient.class.getName());

    /**
     * 
     * @param host
     * @param port
     * @param selector
     * @return
     */
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
			
			
			
			File tmpFile = File.createTempFile(host + "-" + port + "-", null);
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
	
	public static ArrayList<GopherDirectoryEntity> readFromFile(File menufile) {

		try {
			BufferedReader in = null;
			in = new BufferedReader(new FileReader(menufile));

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
						logger.error("Invalid Menu Entity in [" + menufile.getName() + "]");
						logger.error("Line(" + i + ") = '" + line + "'");
						logger.error(e);
					}	
				}
				line = in.readLine();
				i++;
			}
			return list;
			
		} catch (UnknownHostException e) {
			logger.error(e);
		} catch (IOException e) {
			logger.error(e);
		} 

		logger.error("Error in Menu [" + menufile.getName() + "]");
		return null;
	}
}
