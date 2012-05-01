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

/**
 * The <code>GopherMenuEntity</code> class represents character strings.
 * All
 * string literals in Java programs, such as <code>"abc"</code>, are
 * implemented as instances of this class.
 * <p>
 * @author D.Carol
 */
public class GopherDirectoryEntity {
	
	private String type;
	private String username;
	private String selector;
	private String host;
	private int port;
	private String extra;
	
	/**
	 * <p>
	 * Parses the string argument as a Gopher directory entity.
	 * </p>
	 * @param line
	 * 		String to parse.
	 * @throws InvalidGopherDirectoryEntityException
	 */
	public GopherDirectoryEntity(String line) throws InvalidGopherDirectoryEntityException {
	
		int ind = line.indexOf("\t");
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
		}		
	}

	public GopherDirectoryEntity() {
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {		
		this.port = port;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getSelector() {
		return selector;
	}

	public void setSelector(String selector) {
		this.selector = selector;
	}

	public String getExtra() {
		return extra;
	}

	public void setExtra(String extra) {
		this.extra = extra;
	}
}
