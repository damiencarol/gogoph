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

import java.util.ArrayList;

/**
 * The <code>GopherMenuEntity</code> class represents character strings.
 * All
 * string literals in Java programs, such as <code>"abc"</code>, are
 * implemented as instances of this class.
 * <p>
 * @author D.Carol
 */
public class GopherDirectoryEntity {
	
	/*private String type;
	private String username;
	private String selector;
	private String host;
	private int port;
	private String extra;*/
	
	private String[] raw = new String[5];
	
	/**
	 * <p>
	 * Parses the string argument as a Gopher directory entity.
	 * </p>
	 * @param line
	 * 		String to parse.
	 * @throws InvalidGopherDirectoryEntityException
	 */
	public GopherDirectoryEntity(String line) throws InvalidGopherDirectoryEntityException {
	
		String[] tab = line.split("\t");		
		for (int i = 0; i < tab.length; i++)
		{
			raw[i] = tab[i];
		}

		/*int ind = line.indexOf("\t");
		if (ind == -1)
			throw new InvalidGopherDirectoryEntityException("Invalid gopher menu entity [" + line + "]");
		
		type = line.substring(0, 1);		
		username = line.substring(1, ind);
		
		line = line.substring(ind + 1);
		ind = line.indexOf("\t");
		if (ind == -1)
			throw new InvalidGopherDirectoryEntityException("Invalid gopher menu entity [" + line + "]");
		
		selector = line.substring(0, ind);
		
		line = line.substring(ind + 1);
		ind = line.indexOf("\t");
		if (ind == -1)
			throw new InvalidGopherDirectoryEntityException("Invalid gopher menu entity [" + line + "]");
		
		host = line.substring(0, ind);
		
		line = line.substring(ind + 1);
		ind = line.indexOf("\t");
		
		if (ind == -1)
			port = Integer.parseInt(line);
		else {
			port = Integer.parseInt(line.substring(0, ind));
			line = line.substring(ind + 1);
			extra = line;
		}		*/
	}

	public GopherDirectoryEntity() {
	}

	public String getUsername() {
		return raw[0].substring(1);
	}

	public void setUsername(String username) {
		this.raw[0] = getType() + username;
	}

	public String getHost() {
		return raw[2];
	}

	public void setHost(String host) {
		this.raw[2] = host;
	}

	public int getPort() {
		if (raw[3] == null)
			return 70;
		if (raw[3].trim() == "")
			return 70;
		return Integer.parseInt(raw[3].trim());
	}

	public void setPort(int port) {		
		this.raw[3] = "" + port;
	}

	public String getType() {
		if (raw[0] == null)
			return null;
		if (raw[0].length() == 0)
			return null;
		
		return raw[0].substring(0, 1);
	}

	public void setType(String type) {
		String old = this.raw[0];
		if (old == null)
			this.raw[0] = type;
		else
			this.raw[0] = type + old.substring(1) ;
	}

	public String getSelector() {
		return raw[1];
	}

	public void setSelector(String selector) {
		this.raw[1] = selector;
	}

	public String getExtra() {
		if (raw.length > 4)
			return raw[4];
		else
			return null;
	}

	public void setExtra(String extra) {
		this.raw[4] = extra;
	}
}
