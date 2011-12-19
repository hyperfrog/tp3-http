/**
 * 
 */
package http.common;

import static util.StringUtil.bytesToString;
import static util.StringUtil.isAscii;
import static util.StringUtil.isValidUtf8;
import static util.StringUtil.split;
import static util.StringUtil.join;
import static util.StringUtil.unescape;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * La classe HttpResponseHeader modélise une entête HTTP de requête. 
 * 
 * @author Christian Lesage
 * @author Alexandre Tremblay
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
	public void make() throws BadHeaderException
	{
		String path = (this.fullPath == null || this.fullPath.isEmpty()) ? this.path : this.fullPath;

		// Cas d'erreur
		if (path == null || path.isEmpty())
		{
			throw new BadHeaderException("Mauvais chemin.");
		}
		else if (this.method == null || this.method.isEmpty())
		{
			throw new BadHeaderException("Mauvaise méthode.");
		}
		else if  (this.protocol == null || this.protocol.isEmpty())
		{
			throw new BadHeaderException("Mauvais protocole.");
		}

		this.text = String.format("%s %s %s\r\n", this.method , path, this.protocol);

		for (String field : this.fields.keySet())
		{
			this.text += String.format("%s: %s\r\n", field, this.fields.get(field));
		}
		this.text += "\r\n";
	}
	
	/* (non-Javadoc)
	 * @see http.common.HttpHeader#parse()
	 */
	public void parse() throws BadHeaderException
	{
		if (this.text != null && !this.text.isEmpty())
		{
			System.out.println(String.format("%s, %d", this.text, this.text.length()));
			
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

				if (headerLines.size() > 1)
				{
					this.parseFields(headerLines.subList(1, headerLines.size()));
				}

				if (this.protocol.equals("HTTP/1.1") && !this.fields.containsKey("Host"))
				{
					throw new BadHeaderException("Entête incomplète. Le champ Host est obligatoire.");
				}
			}
			else
			{
				throw new BadHeaderException();
			}
		}
		else
		{
			throw new BadHeaderException();
		}
		
	}
	
	// Parse les champs du header
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

	// Parse les paramètres GET de la requête
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
	 * Indique si le client accepte le type MIME spécifié.  
	 * 
	 * @param mimeType type dont l'acceptation par le client est à vérifier
	 * @return vrai si le client accepte le type MIME spécifié, faux sinon.
	 */
	public boolean accepts(String mimeType)
	{
		return acceptList.contains(mimeType);
	}

	/**
	 * Retourne la valeur du paramètre GET (query) spécifié
	 * 
	 * @param param paramètre dont la valeur est demandée  
	 * @return valeur du paramètre spécifié
	 */
	public String getParam(String param)
	{
		return this.parameters.get(param);
	}
	
	/**
	 * Retourne l'ensemble des noms des paramètres GET (query) de la requête
	 * 
	 * @return set of parameter keys
	 */
	public Set<String> getParamKeySet()
	{
		return this.parameters.keySet();
	}
	
	/**
	 * Retourne la méthode utilisée pour la requête (GET, PUT, POST, HEAD, etc.)
	 * 
	 * @return méthode de la requête (GET, PUT, POST, HEAD, etc.) utilisée
	 */
	public String getMethod()
	{
		return this.method;
	}

	/**
	 * Programme la méthode à utiliser pour la requête (GET, PUT, POST, HEAD, etc.).
	 * 
	 * @param method méthode de la requête (GET, PUT, POST, HEAD, etc.) à utiliser
	 */
	public void setMethod(String method)
	{
		this.method = method;
	}

	/**
	 * Retourne le chemin complet de la ressource (p. ex. /dossier/fichier.html?p1=oui&p2=bof)
	 * 
	 * @return chemin complet de la ressource 
	 */
	public String getFullPath()
	{
		return fullPath;
	}

	/**
	 * Programme le chemin complet de la ressource (p. ex. /dossier/fichier.html?p1=oui&p2=bof)
	 * 
	 * @param fullPath chemin complet de la ressource
	 */
	public void setFullPath(String fullPath)
	{
		this.fullPath = fullPath;
	}

	/**
	 * Retourne le chemin de la ressource sans les paramètres (p. ex. /dossier/fichier.html)
	 * 
	 * @return chemin de la ressource sans les paramètres (p. ex. /dossier/fichier.html)
	 */
	public String getPath()
	{
		return path;
	}
}
