/**
 * 
 */
package http.common;

import static util.BasicString.bytesToString;
import static util.BasicString.isAscii;
import static util.BasicString.isValidUtf8;
import static util.BasicString.split;
import static util.BasicString.unescape;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
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
public class HttpRequestHeader extends HttpHeader
{
	// Méthode de la requête (GET, PUT, POST, HEAD, etc.)
	protected String method;
	
	// Chemin complet de la ressource (p. ex. /dossier/fichier.html?p1=oui&p2=bof)
	protected String fullPath;
	
	// Chemin de la ressource sans les paramètres (p. ex. /dossier/fichier.html)
	protected String path;
	
	// Dictionnaire des paramètres 
	protected Map<String, String> parameters;
	
	// Liste des types MIME acceptés 
	protected ArrayList<String> acceptList;
	
	/**
	 * Construit un header.
	 * 
	 */
	public HttpRequestHeader()
	{
		this.method = "";
		this.fullPath = "";
		this.path = "";
		this.parameters = new HashMap<String, String>();
		this.fields = new HashMap<String, String>();
		this.acceptList = new ArrayList<String>();
	}
	
	/* (non-Javadoc)
	 * @see http.common.HttpHeader#make()
	 */
	public boolean make()
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
	
	/* (non-Javadoc)
	 * @see http.common.HttpHeader#parse()
	 */
	public boolean parse()
	{
		boolean success = false;
		
		if (this.text != null) 
		{
			ArrayList<String> headerLines = split(this.text, "\r\n", true);

			Pattern pFullRequest = Pattern.compile("\\A(HEAD|GET|POST|PUT|DELETE|TRACE|OPTIONS|CONNECT|PATCH) (/[^ ]*) (HTTP/.+)\\Z");
			Matcher mFullRequest = pFullRequest.matcher(headerLines.get(0));

			if (mFullRequest.find()) 
			{
				this.method = mFullRequest.group(1);
				this.fullPath = mFullRequest.group(2);
				this.protocol = mFullRequest.group(3);

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

				if (fullPathParts.size() > 1)
				{
					this.parseGetParams(fullPathParts.get(1));
				}

				// Remplace les / par des \ 
//				this.pathName = join(split(this.pathName, "/"), "\\");

				if (this.protocol.equals("HTTP/1.0"))
				{
					success = true;
				}
				
				if (headerLines.size() > 1)
				{
					this.parseFields(headerLines.subList(1, headerLines.size()));
					
					if (this.protocol.equals("HTTP/1.1") && this.fields.containsKey("Host"))
					{
						success = true;
					}
				}
			}
		}
		
		return success;
	}
	
	@Override
	protected void parseFields(List<String> fieldLines)
	{
		super.parseFields(fieldLines);

		if (this.fields.containsKey("Accept"))
		{
			this.acceptList = split(this.getField("Accept"), ",");

			for (int i = 0; i < acceptList.size(); i++)
			{
				this.acceptList.set(i, this.acceptList.get(i).trim());
			}
		}
	}

	protected void parseGetParams(String params)
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
	 * @return the method
	 */
	public String getMethod()
	{
		return method;
	}

	/**
	 * @param method the method to set
	 */
	public void setMethod(String method)
	{
		this.method = method;
	}

	/**
	 * @return the fullPath
	 */
	public String getFullPath()
	{
		return fullPath;
	}

	/**
	 * @param fullPath the fullPath to set
	 */
	public void setFullPath(String fullPath)
	{
		this.fullPath = fullPath;
	}

	/**
	 * @return the path
	 */
	public String getPath()
	{
		return path;
	}

}
