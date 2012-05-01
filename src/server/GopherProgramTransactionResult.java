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

import gogoph.GopherTransactionResult;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;

import org.apache.log4j.Logger;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.DefaultFileRegion;
import org.jboss.netty.channel.FileRegion;

public class GopherProgramTransactionResult extends GopherTransactionResult {

	private static final Logger logger = Logger
			.getLogger(GopherProgramTransactionResult.class.getName());

	private File programFile;

	public GopherProgramTransactionResult(File dest) {
		programFile = dest;
	}

	/**
	 * <p>
	 * If the message is Binary file Transaction (Type 9 or 5 item)
	 * </p>
	 * <p>
	 * <code>
	 * C: Opens Connection.<br />
	 * S: Accepts connection<br />
	 * C: Sends Selector String.<br />
	 * S: Sends a binary file and closes connection when done.<br />
	 * </code>
	 * </p>
	 */
	public ChannelFuture processChannel(Channel ch, String queryString) {
		
		try 
		{
			File tempFile = File.createTempFile("gogoph-prog-", "");
			tempFile.deleteOnExit();
			
			String[] cmdarray = new String[] {programFile.getAbsolutePath(), queryString};
			
			Process proc = Runtime.getRuntime().exec(cmdarray);
			
			InputStream stdout = proc.getInputStream();
	        InputStreamReader isr = new InputStreamReader(stdout);
	        BufferedReader br = new BufferedReader(isr);
	        
	        OutputStreamWriter tempout = new FileWriter(tempFile);
	        char cbuf[] = new char[1024];
	        int len = br.read(cbuf, 0, 1024);
	        while (len != -1)
	        {
	        	logger.info(" read : " + len + " byte(s)");
	        	logger.debug(" read : " + new String( cbuf) + " byte(s)");
	            tempout.write(cbuf, 0, len);
	            len = br.read(cbuf, 0, 1024);
	        }
	
	        int exitVal = proc.waitFor();
	        logger.info("Process exitValue: " + exitVal);
	        tempout.flush();
	        tempout.close();
        
        
        
		
        	File binFile = tempFile;
		
		
			RandomAccessFile raf;
			raf = new RandomAccessFile(binFile, "r");

			long fileLength = raf.length();

			// Write the content.
			ChannelFuture writeFuture;
			// use zero-copy.
			FileRegion region = new DefaultFileRegion(raf.getChannel(), 0,
					fileLength);
			writeFuture = ch.write(region);
			return writeFuture;

		} catch (FileNotFoundException e) {
			logger.error(e);
		} catch (IOException e) {
			logger.error(e);
		} catch (InterruptedException e) {
			logger.error(e);
		}
		return null;
	}
}
