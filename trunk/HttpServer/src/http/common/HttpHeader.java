/**
 * 
 */
package http.common;

import static util.BasicString.bytesToString;
import static util.BasicString.isAscii;
import static util.BasicString.isValidUtf8;
import static util.BasicString.split;
import static util.BasicString.unescape;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import util.DateUtil;

/**
 * @author Christian
 *
 */
public class HttpHeader
{
	private static final String DEFAULT_URL_ENCODING = "ISO-8859-1";
	
	private static final Map<Integer, String> statusCodeDesc = new HashMap<Integer, String>();
	static
	{
		statusCodeDesc.put(100, "Continue");
		statusCodeDesc.put(101, "Switching Protocols");
		statusCodeDesc.put(200, "OK");
		statusCodeDesc.put(201, "Created");
		statusCodeDesc.put(202, "Accepted");
		statusCodeDesc.put(203, "Non-Authoritative Information");
		statusCodeDesc.put(204, "No Content");
		statusCodeDesc.put(205, "Reset Content");
		statusCodeDesc.put(206, "Partial Content");
		statusCodeDesc.put(300, "Multiple Choices");
		statusCodeDesc.put(301, "Moved Permanently");
		statusCodeDesc.put(302, "Found");
		statusCodeDesc.put(303, "See Other");
		statusCodeDesc.put(304, "Not Modified");
		statusCodeDesc.put(305, "Use Proxy");
		statusCodeDesc.put(306, "(Unused)");
		statusCodeDesc.put(307, "Temporary Redirect");
		statusCodeDesc.put(400, "Bad Request");
		statusCodeDesc.put(401, "Unauthorized");
		statusCodeDesc.put(402, "Payment Required");
		statusCodeDesc.put(403, "Forbidden");
		statusCodeDesc.put(404, "Not Found");
		statusCodeDesc.put(405, "Method Not Allowed");
		statusCodeDesc.put(406, "Not Acceptable");
		statusCodeDesc.put(407, "Proxy Authentication Required");
		statusCodeDesc.put(408, "Request Timed Out");
		statusCodeDesc.put(409, "Conflict");
		statusCodeDesc.put(410, "Gone");
		statusCodeDesc.put(411, "Length Required");
		statusCodeDesc.put(412, "Precondition Failed");
		statusCodeDesc.put(413, "Request Entity Too Large");
		statusCodeDesc.put(414, "Request URL Too Long");
		statusCodeDesc.put(415, "Unsupported Media Type");
		statusCodeDesc.put(416, "Requested Range Not Satisfiable");
		statusCodeDesc.put(417, "Expectation Failed");
		statusCodeDesc.put(500, "Internal Server Error");
		statusCodeDesc.put(501, "Not implemented");
		statusCodeDesc.put(502, "Bad Gateway");
		statusCodeDesc.put(503, "Service Unavailable");
		statusCodeDesc.put(504, "Gateway Timeout");
		statusCodeDesc.put(505, "HTTP Version Not Supported");
	}
	
	// Texte complet du header
	private String text;
	
	// Méthode de la requête (GET, PUT, POST, HEAD, etc.)
	private String method;
	
	// Protocole utilisé (HTTP/1.x)
	private String protocol;

	// Chemin complet de la ressource (p. ex. /dossier/fichier.html?p1=oui&p2=bof)
	private String fullPath;
	
	// Chemin de la ressource sans les paramètres (p. ex. /dossier/fichier.html)
	private String path;
	
	// Code de la réponse (p. ex. 404)
	private int statusCode;
	
	// Dictionnaire des paramètres 
	private Map<String, String> parameters;
	
	// Dictionnaire des champs supplémentaires
	private Map<String, String> fields;
	
	// Liste des types MIME acceptés 
	private ArrayList<String> acceptList;
	
	/**
	 * Construit un header.
	 * 
	 */
	public HttpHeader()
	{
		this.text = null;
		this.method = "";
		this.protocol = "";
		this.fullPath = "";
		this.path = "";
		this.statusCode = 0;
		this.parameters = new HashMap<String, String>();
		this.fields = new HashMap<String, String>();
		this.acceptList = new ArrayList<String>();
	}
	
	public boolean makeResponseHeader(boolean isCacheable)
	{
		if (statusCodeDesc.containsKey(this.statusCode))
		{
			this.fields.put("Date", DateUtil.formatDate(new Date()));
			if (!isCacheable) 
			{
				this.fields.put("Expires", this.fields.get("Date"));
				this.fields.put("Cache-Control", "max-age=0, must-revalidate");
			}

			this.text = String.format("%s %d %s\r\n", this.protocol, this.statusCode, statusCodeDesc.get(this.statusCode));

			for (String field : this.fields.keySet())
			{
				this.text += String.format("%s: %s\r\n", field, this.fields.get(field));
			}
			this.text += "\r\n";
			
			return true;
		}
		return false;
	}
	
	public boolean makeRequestHeader()
	{
		
			String path = (this.fullPath == null || this.fullPath.isEmpty()) ? this.path : this.fullPath;
			
			/* TODO: Si this.parameters n'est pas vide et que path ne contient pas de paramètres,
			 * il faudrait ajouter les paramètres du dictionnaire au path
			 */
			
			if (path == null || path.isEmpty() || this.method == null || this.method.isEmpty() || this.protocol == null || this.protocol.isEmpty())
			{
				return false;
			}
		
			this.text = String.format("%s %s %s\r\n", this.method , path, this.protocol);

			for (String field : this.fields.keySet())
			{
				this.text += String.format("%s: %s\r\n", field, this.fields.get(field));
			}
			this.text += "\r\n";
			
			return true;
	}
	
	public void parseRequestHeader()
	{
		if (this.text != null) 
		{
			ArrayList<String> headerLines = split(this.text, "\r\n", true);

			Pattern pFullRequest = Pattern.compile("\\A((GET)|(HEAD)|(POST)|(PUT)) (/[^ ]*) (HTTP/.+)\\Z");
			Matcher mFullRequest = pFullRequest.matcher(headerLines.get(0));

			if (mFullRequest.find()) 
			{
				this.method = mFullRequest.group(1);
				this.protocol = mFullRequest.group(7);
				this.fullPath = mFullRequest.group(6);

				ArrayList<String> fullPathParts = split(this.fullPath, "?");

				byte[] pathBytes = unescape(fullPathParts.get(0));

				if (!isAscii(pathBytes) && isValidUtf8(pathBytes))
				{
					this.path = bytesToString(pathBytes, "UTF-8");
				}
				else
				{
					this.path = bytesToString(pathBytes, DEFAULT_URL_ENCODING);
				}

//				if (this.method.equals("GET"))
				if (fullPathParts.size() > 1)
				{
					this.parseGetParams(fullPathParts.get(1));
				}

				// Remplace les / par des \ 
//				this.pathName = join(split(this.pathName, "/"), "\\");

				if (headerLines.size() > 1)
				{
					this.parseFields(headerLines.subList(1, headerLines.size()));
				}
			}
		}
	}
	
	private void parseFields(List<String> fieldLines)
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

		if (this.fields.containsKey("Accept"))
		{
			this.acceptList = split(this.getField("Accept"), ",");

			for (int i = 0; i < acceptList.size(); i++)
			{
				this.acceptList.set(i, this.acceptList.get(i).trim());
			}
		}
	}

	private void parseGetParams(String params)
	{
		ArrayList<String> paramList = split(params, "&");
		if (paramList.size() > 0) 
		{
			for(String param : paramList)
			{
				ArrayList<String> paramParts = split(param, "=");
				for (int i = 0; i < 2 && i < paramParts.size(); i++)
				{
					byte[] paramPartBytes = unescape(paramParts.get(i));

					if (!isAscii(paramPartBytes) && isValidUtf8(paramPartBytes)) 
					{
						paramParts.set(i, bytesToString(paramPartBytes, "UTF-8")); 
					}
					else
					{
						paramParts.set(i, bytesToString(paramPartBytes, DEFAULT_URL_ENCODING)); 
					}
				}

				this.parameters.put(paramParts.get(0), paramParts.size() == 2 ? paramParts.get(1) : null);
			}
		}
	}
	
	/**
	 * @param mimeType
	 * @return
	 */
	public boolean accepts(String mimeType)
	{
		return acceptList.contains(mimeType);
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
	 * @param param
	 * @return
	 */
	public String getParam(String param)
	{
		return this.parameters.get(param);
	}
	
	/**
	 * @return set of parameter keys
	 */
	public Set<String> getParamKeySet()
	{
		return this.parameters.keySet();
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
	 * @return the method
	 */
	public String getMethod()
	{
		return method;
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

	/**
	 * @param statusCode the statusCode to set
	 */
	public boolean setStatusCode(int statusCode)
	{
		if (statusCodeDesc.containsKey(statusCode))
		{
			this.statusCode = statusCode;
			return true;
		}
		return false;
	}

	/**
	 * @return the statusCode
	 */
	public int getStatusCode()
	{
		return statusCode;
	}

	/**
	 * @return the fullPath
	 */
	public String getFullPath()
	{
		return fullPath;
	}

	/**
	 * @return the path
	 */
	public String getPath()
	{
		return path;
	}
}
