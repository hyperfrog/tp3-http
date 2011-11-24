package http;

import static util.BasicString.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern; 
import java.util.regex.Matcher; 

public class HttpRequest
{
	private static final String DEFAULT_URL_ENCODING = "ISO-8859-1";
	
	private String header;
	private String method;
	private String protocol;
	private String fullPath;
	private String path;
	private Map<String, String> parameters;
	private Map<String, String> fields;
	private ArrayList<String> acceptList;
	
	public HttpRequest(String header)
	{
		this.header = header == null ? "" : header;
		this.method = "";
		this.protocol = "";
		this.fullPath = "";
		this.path = "";
		this.parameters = new HashMap<String, String>();
		this.fields = new HashMap<String, String>();
		this.acceptList = new ArrayList<String>();
		this.parseHeader();
	}
	
	private void parseHeader()
	{
		ArrayList<String> headerLines = split(this.header, "\n", true);

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

//			if (this.method.equals("GET"))
			if (fullPathParts.size() > 1)
			{
				this.parseGetParams(fullPathParts.get(1));
			}

			// Remplace les / par des \ 
//			this.pathName = join(split(this.pathName, "/"), "\\");

			if (headerLines.size() > 1)
			{
				Pattern pRequestFields = Pattern.compile("\\A([^:]+): (.+)\\Z");

				for (String line : headerLines.subList(1, headerLines.size()))
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
		}
	}

	private void parseGetParams(String params)
	{
//		this.parameters = stringToMap(pathParts.get(1), "&", "=", true);

		ArrayList<String> paramList = split(params, "&");	//dissects parameters for detail
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
	public String getHeader()
	{
		return header;
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
