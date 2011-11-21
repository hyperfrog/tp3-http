package http;

import static http.MainApp.*;
import static util.BasicString.*;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern; 
import java.util.regex.Matcher; 

public class HttpRequest
{
	private String header;
	private String method;
	private String protocol;
	private String url;
	private String pathName;
	private Map<String, String> parameters;
	private Map<String, String> fields;
	
	public HttpRequest(String header)
	{
		this.header = header == null ? "" : header;
		this.method = "";
		this.protocol = "";
		this.url = "";
		this.pathName = "";
		this.parameters = new HashMap<String, String>();
		this.fields = new HashMap<String, String>();
		this.parseHeader();
	}

	private void parseHeader()
	{
		Pattern pFullRequest = Pattern.compile("\\A((GET)|(HEAD)|(POST)|(PUT)) (/[^ ]*) (HTTP/.+)\\Z");
		Pattern pRequestFields = Pattern.compile("\\A([^:]+): (.+)\\Z");

		ArrayList<String> headerLines = split(this.header, "\n", true);

		Matcher mFullRequest = pFullRequest.matcher(headerLines.get(0));

		if (mFullRequest.find()) 
		{
			this.method = mFullRequest.group(1);
			this.protocol = mFullRequest.group(7);

			try
			{
				this.url = URLDecoder.decode(mFullRequest.group(6), "UTF-8");
			}
			catch (UnsupportedEncodingException e)
			{
				this.url = mFullRequest.group(6);
			}

			if (this.method.equals("GET"))
			{
				this.parseGetParams();
			}

//			this.pathName = unescape(this.pathName);
//			if (!UTF8_is_ASCII(this.pathName) && UTF8_is_MultiByte(this.pathName)) 
//			{
//				this.pathName = UTF8_to_Latin1(this.pathName);
//			}

			// Remplace les / par des \ 
//			this.pathName = join(split(this.pathName, "/"), "\\");

			if (headerLines.size() > 1)
			{
				for (String line : headerLines.subList(1, headerLines.size()))
				{
					Matcher mRequestFields = pRequestFields.matcher(line);

					if (mRequestFields.find())
					{
						this.fields.put(mRequestFields.group(1), mRequestFields.group(2));
					}
				}
			}
		}
	}

	private void parseGetParams()
	{
		if (this.url.indexOf('?') > -1) 
		{
			ArrayList<String> tmpArr = split(this.url, "?");		//get pathname and parameters
			this.pathName = tmpArr.get(0);
			if (tmpArr.get(1).length() > 0) 
			{
				ArrayList<String> tmpParam = split(tmpArr.get(1), "&");	//dissects parameters for detail
				if (tmpParam.size() > 0) 
				{
					for(String p : tmpParam)
					{
						ArrayList<String> sides = split(p, "=");
//						for (int i = 0; i < sides.size(); i++)
//						{
//							sides.set(i, unescape(sides.get(i)));
//							if (!UTF8_is_ASCII(sides.get(i)) && UTF8_is_MultiByte(sides.get(i))) 
//							{
//								sides.set(i, UTF8_to_Latin1(sides.get(i)));
//							}
//						}
						if (sides.size() >= 2)
						{
							this.parameters.put(sides.get(0), sides.get(1));
						}
					}
				}
			}
		}
		else
		{
			this.pathName = this.url;
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
	 * @param param
	 * @param value
	 */
	public void setParam(String param, String value)
	{
		this.fields.put(param, value);
	}
	
	/**
	 * @param param
	 * @return
	 */
	public String getParam(String param)
	{
		return this.fields.get(param);
	}
	
	/**
	 * @return the header
	 */
	public String getHeader()
	{
		return header;
	}

	/**
	 * @param header the header to set
	 */
	public void setHeader(String header)
	{
		this.header = header;
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
	 * @return the url
	 */
	public String getUrl()
	{
		return url;
	}

	/**
	 * @param url the url to set
	 */
	public void setUrl(String url)
	{
		this.url = url;
	}

	/**
	 * @return the pathName
	 */
	public String getPathName()
	{
		return pathName;
	}

	/**
	 * @param pathName the pathName to set
	 */
	public void setPathName(String pathName)
	{
		this.pathName = pathName;
	}
}
