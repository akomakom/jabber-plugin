/**
 * Copyright (c) 2007-2017 the original author or authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE
 */
package hudson.plugins.jabber.im.transport;

import java.io.Reader;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;

import hudson.plugins.jabber.im.LoggingFilterReader;
import hudson.plugins.jabber.im.LoggingFilterWriter;

import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.debugger.SmackDebugger;
import org.jivesoftware.smack.packet.Stanza;
import org.jxmpp.util.XmppStringUtils;

/**
 * Logs detailed info to the log in level FINE or FINEST.
 *
 * @author kutzi
 */
public class JabberConnectionDebugger implements SmackDebugger {

	private static final Logger LOGGER = Logger.getLogger(JabberConnectionDebugger.class.getName());
	private static final Level MIN_LOG_LEVEL = Level.FINE;

	private final XMPPConnection connection;
	private Writer writer;
	private Reader reader;

	private StanzaListener listener;

	private ConnectionListener connListener;

	public JabberConnectionDebugger(XMPPConnection connection, Writer writer, Reader reader) {
		this.connection = connection;
		this.writer = writer;
		this.reader = reader;
		init();
	}

	private void init() {

		LoggingFilterReader debugReader = new LoggingFilterReader(this.reader, LOGGER, MIN_LOG_LEVEL);
		this.reader = debugReader;

		LoggingFilterWriter debugWriter = new LoggingFilterWriter(this.writer, LOGGER, MIN_LOG_LEVEL);
		this.writer = debugWriter;

		this.listener = new StanzaListener() {
			public void processPacket(Stanza packet) {
				if (LOGGER.isLoggable(Level.FINEST)) {
					LOGGER.finest("RCV PKT: " + packet.toXML());
				}
			}
		};

		this.connListener = new ConnectionListener() {
			public void connected(XMPPConnection connection) {
				if (LOGGER.isLoggable(MIN_LOG_LEVEL)) {
					LOGGER.fine("Connection " + connection + " established");
				}
			}

			@Override
			public void authenticated(XMPPConnection connection, boolean resumed) {
				if (LOGGER.isLoggable(MIN_LOG_LEVEL)) {
					LOGGER.fine("Connection " + connection + " authenticated");
				}
			}

			public void connectionClosed() {
				if (LOGGER.isLoggable(MIN_LOG_LEVEL)) {
					LOGGER.fine("Connection closed");
				}
			}

			public void connectionClosedOnError(Exception e) {
				if (LOGGER.isLoggable(MIN_LOG_LEVEL)) {
					LOGGER.log(MIN_LOG_LEVEL, "Connection closed due to an exception", e);
				}
			}

			public void reconnectionFailed(Exception e) {
				if (LOGGER.isLoggable(MIN_LOG_LEVEL)) {
					LOGGER.log(MIN_LOG_LEVEL, "Reconnection failed due to an exception", e);
				}
			}

			public void reconnectionSuccessful() {
				if (LOGGER.isLoggable(MIN_LOG_LEVEL)) {
					LOGGER.log(MIN_LOG_LEVEL, "Reconnection successful");
				}
			}

			public void reconnectingIn(int seconds) {
				if (LOGGER.isLoggable(MIN_LOG_LEVEL)) {
					LOGGER.log(MIN_LOG_LEVEL, "Reconnecting in " + seconds + " seconds");
				}
			}
		};
	}

	@Override
	public Reader getReader() {
		return this.reader;
	}

	@Override
	public StanzaListener getReaderListener() {
		return this.listener;
	}

	@Override
	public Writer getWriter() {
		return this.writer;
	}

	@Override
	public StanzaListener getWriterListener() {
		return null;
	}

	@Override
	public Reader newConnectionReader(Reader newReader) {
		LoggingFilterReader debugReader = new LoggingFilterReader(newReader, LOGGER, MIN_LOG_LEVEL);
		this.reader = debugReader;
		return this.reader;
	}

	@Override
	public Writer newConnectionWriter(Writer newWriter) {
		LoggingFilterWriter debugWriter = new LoggingFilterWriter(newWriter, LOGGER, MIN_LOG_LEVEL);
		this.writer = debugWriter;
		return this.writer;
	}

	@Override
	public void userHasLogged(String user) {
		if (LOGGER.isLoggable(MIN_LOG_LEVEL)) {
			boolean isAnonymous = "".equals(XmppStringUtils.parseLocalpart(user));
			String title = "User logged in (" + this.connection.hashCode() + "): "
					+ ((isAnonymous) ? "" : XmppStringUtils.parseBareJid(user)) + "@" + this.connection.getServiceName()
					+ ":" + this.connection.getPort();

			title = title + "/" + XmppStringUtils.parseResource(user);
			LOGGER.fine(title);
		}

		this.connection.addConnectionListener(this.connListener);
	}
}
