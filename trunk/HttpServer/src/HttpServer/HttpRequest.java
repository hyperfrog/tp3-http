package HttpServer;

import static util.BasicString.*;
import static HttpServer.MainApp.*;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern; 
import java.util.regex.Matcher; 

public class HttpRequest
{
	public String header;
	public String method;
	public String protocol;
	public String url;
	public String pathName;
	public Map<String, String> parameters;
	public Map<String, String> fields;
	
	public HttpRequest(String header)
	{
		this.header = header;
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
						ArrayList<String> keys = split(p, "=");
//						for (int i = 0; i <= 1; i++)
//						{
//							keys.set(i, unescape(keys.get(i)));
//							if (!UTF8_is_ASCII(keys.get(i)) && UTF8_is_MultiByte(keys.get(i))) 
//							{
//								keys.set(i, UTF8_to_Latin1(keys.get(i)));
//							}
//						}
						this.parameters.put(keys.get(0), keys.get(1));
					}
				}
			}
		}
		else
		{
			this.pathName = this.url;
		}
	}
}
