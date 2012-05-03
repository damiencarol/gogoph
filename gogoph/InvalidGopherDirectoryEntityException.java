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

public class InvalidGopherDirectoryEntityException extends Exception {

	private static final long serialVersionUID = -8874184365635527684L;

	public InvalidGopherDirectoryEntityException() {
		super();
	}

	public InvalidGopherDirectoryEntityException(String message, Throwable cause) {
		super(message, cause);
	}

	public InvalidGopherDirectoryEntityException(String message) {
		super(message);
	}

	public InvalidGopherDirectoryEntityException(Throwable cause) {
		super(cause);
	}
}
