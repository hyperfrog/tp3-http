/**
 * 
 */
package http.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Christian
 *
 */
public abstract class HttpHeader
{
	protected static final String DEFAULT_URL_ENCODING = "ISO-8859-1";
	
	protected static final Map<Integer, String> statusCodeDescMap = new HashMap<Integer, String>();
	static
	{
		statusCodeDescMap.put(100, "Continue");
		statusCodeDescMap.put(101, "Switching Protocols");
		statusCodeDescMap.put(200, "OK");
		statusCodeDescMap.put(201, "Created");
		statusCodeDescMap.put(202, "Accepted");
		statusCodeDescMap.put(203, "Non-Authoritative Information");
		statusCodeDescMap.put(204, "No Content");
		statusCodeDescMap.put(205, "Reset Content");
		statusCodeDescMap.put(206, "Partial Content");
		statusCodeDescMap.put(300, "Multiple Choices");
		statusCodeDescMap.put(301, "Moved Permanently");
		statusCodeDescMap.put(302, "Found");
		statusCodeDescMap.put(303, "See Other");
		statusCodeDescMap.put(304, "Not Modified");
		statusCodeDescMap.put(305, "Use Proxy");
		statusCodeDescMap.put(306, "(Unused)");
		statusCodeDescMap.put(307, "Temporary Redirect");
		statusCodeDescMap.put(400, "Bad Request");
		statusCodeDescMap.put(401, "Unauthorized");
		statusCodeDescMap.put(402, "Payment Required");
		statusCodeDescMap.put(403, "Forbidden");
		statusCodeDescMap.put(404, "Not Found");
		statusCodeDescMap.put(405, "Method Not Allowed");
		statusCodeDescMap.put(406, "Not Acceptable");
		statusCodeDescMap.put(407, "Proxy Authentication Required");
		statusCodeDescMap.put(408, "Request Timed Out");
		statusCodeDescMap.put(409, "Conflict");
		statusCodeDescMap.put(410, "Gone");
		statusCodeDescMap.put(411, "Length Required");
		statusCodeDescMap.put(412, "Precondition Failed");
		statusCodeDescMap.put(413, "Request Entity Too Large");
		statusCodeDescMap.put(414, "Request URL Too Long");
		statusCodeDescMap.put(415, "Unsupported Media Type");
		statusCodeDescMap.put(416, "Requested Range Not Satisfiable");
		statusCodeDescMap.put(417, "Expectation Failed");
		statusCodeDescMap.put(500, "Internal Server Error");
		statusCodeDescMap.put(501, "Not implemented");
		statusCodeDescMap.put(502, "Bad Gateway");
		statusCodeDescMap.put(503, "Service Unavailable");
		statusCodeDescMap.put(504, "Gateway Timeout");
		statusCodeDescMap.put(505, "HTTP Version Not Supported");
	}
	
	// Texte complet du header
	protected String text;
	
	// Protocole utilisé (HTTP/1.x)
	protected String protocol;

	// Dictionnaire des champs obligatoires et optionnels
	protected Map<String, String> fields;
	
	/**
	 * Construit un header.
	 * 
	 */
	public HttpHeader()
	{
		this.text = null;
		this.protocol = "";
		this.fields = new HashMap<String, String>();
	}
	
	/**
	 * @return
	 */
	public abstract boolean parse();
	
	/**
	 * Fabrique un header HTTP pour la requête ou la réponse.
	 * 
	 * @return vrai si le header a pu être fabriqué, faux sinon
	 */
	public abstract boolean make();
	
	/**
	 * @param is
	 * @throws IOException
	 */
	public void receive(InputStream is) throws IOException
	{
//		System.out.println(String.format("Thread %d (Requête)", Thread.currentThread().getId()));

		BufferedReader in = new BufferedReader(new InputStreamReader(is));

		String headerText = new String();
		String line;
		
		// Lit l'en-tête (header)
		do
		{
			line = in.readLine();
			headerText += line + "\r\n";

		} while (line != null && !line.isEmpty());

		this.text = headerText;
		
//		System.out.print(headerText);
	}
	
	/**
	 * @param os
	 * @return
	 * @throws IOException
	 */
	public boolean send(OutputStream os) throws IOException
	{
		if ((this.text == null || this.text.isEmpty()) && !this.make())
		{
			return false;
		}

		OutputStreamWriter osw = new OutputStreamWriter(os);

		osw.write(this.text);
		osw.flush();
		
		return true;
	}
	
	protected void parseFields(List<String> fieldLines)
	{
		Pattern pRequestFields = Pattern.compile("\\A([^:]+): (.+)\\Z");

		for (String line : fieldLines)
		{
			Matcher mRequestFields = pRequestFields.matcher(line);

			if (mRequestFields.find())
			{
				this.fields.put(mRequestFields.group(1), mRequestFields.group(2));
			}
		}
	}

	/**
	 * @param field
	 * @param value
	 */
	public void setField(String field, String value)
	{
		this.fields.put(field, value);
	}
	
	/**
	 * @param field
	 * @return
	 */
	public String getField(String field)
	{
		return this.fields.get(field);
	}
	
	/**
	 * @return
	 */
	public Set<String> getFieldKeySet()
	{
		return this.fields.keySet();
	}

	/**
	 * @return the header
	 */
	public String getText()
	{
		return text;
	}

	/**
	 * @param text the text to set
	 */
	public void setText(String text)
	{
		this.text = text;
	}

	/**
	 * @return the protocol
	 */
	public String getProtocol()
	{
		return protocol;
	}

	/**
	 * @param protocol the protocol to set
	 */
	public void setProtocol(String protocol)
	{
		this.protocol = protocol;
	}
}
