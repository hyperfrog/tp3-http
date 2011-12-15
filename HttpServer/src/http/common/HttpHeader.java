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
 * La classe HttpHeader mod�lise ce qu'il y a de commun aux ent�tes HTTP de requ�te et de r�ponse. 
 * 
 * @author Christian Lesage
 * @author Alexandre Tremblay
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
	
	// Protocole utilis� (HTTP/1.x)
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
	 * Analyse l'ent�te de la requ�te ou de la r�ponse re�ue et la d�compose en �l�ments.
	 */
	public abstract void parse() throws BadHeaderException;
	
	/**
	 * Fabrique une ent�te HTTP pour la requ�te ou la r�ponse � envoyer.
	 */
	public abstract void make() throws BadHeaderException;
	
	/**
	 * Re�oit un header sur la stream d'entr�e sp�cifi�e.
	 * 
	 * @param is InputStream pour la lecture du header
	 * @throws IOException
	 */
	public void receive(InputStream is) throws IOException
	{
		BufferedReader in = new BufferedReader(new InputStreamReader(is));

		String headerText = new String();
		String line;
		
		// Lit l'en-t�te (header)
		do
		{
			line = in.readLine();
			headerText += line + "\r\n";

		} while (line != null && !line.isEmpty());

		this.text = headerText;
	}
	
	/**
	 * Envoie le header sur la stream de sortie sp�cifi�e. 
	 * 
	 * @param os OutputStream pour l'�criture du header
	 * @throws IOException
	 */
	public void send(OutputStream os) throws IOException, BadHeaderException
	{
		if (this.text == null || this.text.isEmpty())
		{
			 this.make();
		}

		OutputStreamWriter osw = new OutputStreamWriter(os);

		osw.write(this.text);
		osw.flush();
	}
	
	// Parse les champs du header
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
	 * Programme la valeur d'un champ.
	 * 
	 * @param field champ � programmer
	 * @param value valeur du champ
	 */
	public void setField(String field, String value)
	{
		this.fields.put(field, value);
	}
	
	/**
	 * Retourne la valeur d'un champ.
	 * 
	 * @param field champ dont la valeur doit �tre retourn�e
	 * @return valeur du champ sp�cifi�
	 */
	public String getField(String field)
	{
		return this.fields.get(field);
	}
	
	/**
	 * Retourne l'ensemble des noms de champ du header.
	 * 
	 * @return ensemble des noms de champ du header
	 */
	public Set<String> getFieldKeySet()
	{
		return this.fields.keySet();
	}

	/**
	 * Retourne la repr�sentation textuelle du header.
	 * 
	 * @return repr�sentation textuelle du header
	 */
	public String getText()
	{
		return text;
	}

//	/**
//	 * 
//	 * 
//	 * @param text the text to set
//	 */
//	public void setText(String text)
//	{
//		this.text = text;
//	}

	/**
	 * Retourne le protocole sp�cifi� dans la requ�te ou la r�ponse.
	 *  
	 * @return protocole sp�cifi� dans la requ�te ou la r�ponse
	 */
	public String getProtocol()
	{
		return protocol;
	}

	/**
	 * Programme le protocole � utiliser pour la requ�te ou la r�ponse.
	 * 
	 * @param protocole � utiliser pour la requ�te ou la r�ponse
	 */
	public void setProtocol(String protocol)
	{
		this.protocol = protocol;
	}
}
