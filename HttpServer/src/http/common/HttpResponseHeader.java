/**
 * 
 */
package http.common;

import static util.BasicString.split;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import util.DateUtil;

/**
 * @author Christian
 *
 */
public class HttpResponseHeader extends HttpHeader
{
	// Code de la réponse (p. ex. 404)
	private int statusCode;
	
	// Description du code de la réponse
	private String statusCodeDesc;
	
	private boolean isCacheable;
	
	/**
	 * Construit un header.
	 * 
	 */
	public HttpResponseHeader()
	{
		this.statusCode = 0;
		this.statusCodeDesc = "";
		this.isCacheable = true;
	}
	
	/**
	 * @param os
	 * @return
	 * @throws IOException
	 */
	public boolean send(OutputStream os) throws IOException
	{
		if (this.text == null || this.text.isEmpty())
		{
			if (!this.make())
			{
				return false;
			}
		}

		return super.send(os);
	}
	
	/**
	 * Fabrique un header HTTP pour la réponse.
	 * 
	 * @return vrai si le header a pu être fabriqué, faux sinon
	 */
	public boolean make()
	{
		if (statusCodeDescMap.containsKey(this.statusCode))
		{
			this.fields.put("Date", DateUtil.formatDate(new Date()));
			if (!this.isCacheable) 
			{
				this.fields.put("Expires", this.fields.get("Date"));
				this.fields.put("Cache-Control", "max-age=0, must-revalidate");
			}

			this.text = String.format("%s %d %s\r\n", this.protocol, this.statusCode, statusCodeDescMap.get(this.statusCode));

			for (String field : this.fields.keySet())
			{
				this.text += String.format("%s: %s\r\n", field, this.fields.get(field));
			}
			this.text += "\r\n";
			
			return true;
		}
		return false;
	}

	public boolean parse()
	{
		boolean success = true;
		
		if (this.text != null) 
		{
			ArrayList<String> headerLines = split(this.text, "\r\n", true);

			Pattern pFullRequest = Pattern.compile("\\A(HTTP/.+) (\\d{3}) (.+)\\Z");
			Matcher mFullRequest = pFullRequest.matcher(headerLines.get(0));

			if (mFullRequest.find()) 
			{
				this.protocol = mFullRequest.group(1);
				
				try
				{
					this.statusCode = Integer.parseInt(mFullRequest.group(2));
				}
				catch (NumberFormatException e)
				{
					success = false;
				}

				this.statusCodeDesc = mFullRequest.group(3);

				if (headerLines.size() > 1)
				{
					this.parseFields(headerLines.subList(1, headerLines.size()));
				}
			}
		}
		
		return success;
	}

	/**
	 * @param statusCode the statusCode to set
	 */
	public boolean setStatusCode(int statusCode)
	{
		if (statusCodeDescMap.containsKey(statusCode))
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
	 * @return the statusCodeDescription
	 */
	public String getStatusCodeDescription()
	{
		return statusCodeDesc;
	}

	/**
	 * @return the isCacheable
	 */
	public boolean isCacheable()
	{
		return isCacheable;
	}

	/**
	 * @param isCacheable the isCacheable to set
	 */
	public void setCacheable(boolean isCacheable)
	{
		this.isCacheable = isCacheable;
	}
	
}
