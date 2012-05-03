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
package gogoph;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;

public class GopherTextTransactionResult extends GopherTransactionResult {

	private File textFile;

	public GopherTextTransactionResult(File dest) {
		textFile = dest;
	}

	public ChannelFuture processChannel(Channel channel, String queryString) {
		ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
		BufferedReader dis = null;
	    try {
	      dis = new BufferedReader(new FileReader(textFile));

	      String line;
	      while ((line = dis.readLine()) != null) {

	    	  buffer.writeBytes(line.getBytes(ascii));
	    	  
	    	  buffer.writeByte( (byte)0x0D );  // <CR>
	    	  buffer.writeByte( (byte)0x0A );  // <LF>
	      }
	      	
	      buffer.writeByte( (byte)0x2E );  // .
	      buffer.writeByte( (byte)0x0D );  // <CR>
	      buffer.writeByte( (byte)0x0A );  // <LF>
	
	      // dispose all the resources after using them.
	      dis.close();

	    } catch (FileNotFoundException e) {
	      e.printStackTrace();
	    } catch (IOException e) {
	      e.printStackTrace();
	    }
	    return channel.write(buffer);
	}
}
