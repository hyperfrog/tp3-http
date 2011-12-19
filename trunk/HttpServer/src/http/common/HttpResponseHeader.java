package http.common;

import static util.StringUtil.split;

import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import util.DateUtil;

/**
 * La classe HttpResponseHeader modélise une entête HTTP de réponse. 
 * 
 * @author Christian Lesage
 * @author Alexandre Tremblay
 *
 */
public class HttpResponseHeader extends HttpHeader
{
	// Code de la réponse (p. ex. 404)
	private int statusCode;
	
	// Description du code de la réponse
	private String statusCodeDesc;
	
	// Indique si la réponse peut être mise en cache par le client ou les proxys
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
	
	/* (non-Javadoc)
	 * @see http.common.HttpHeader#make()
	 */
	public void make() throws BadHeaderException
	{
		// Cas d'erreur
		if (!statusCodeDescMap.containsKey(this.statusCode))
		{
			throw new BadHeaderException("Code de statut inexistant.");
		}
		else if (this.protocol == null || this.protocol.isEmpty())
		{
			throw new BadHeaderException("Mauvais protocole.");
		}

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
	}

	/* (non-Javadoc)
	 * @see http.common.HttpHeader#parse()
	 */
	public void parse() throws BadHeaderException
	{
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
					throw new BadHeaderException("Code de statut inexistant.");
				}

				this.statusCodeDesc = mFullRequest.group(3);

				if (headerLines.size() > 1)
				{
					this.parseFields(headerLines.subList(1, headerLines.size()));
				}
			}
			else
			{
				throw new BadHeaderException("Entête mal formée.");
			}
		}
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
	 * Indique si la réponse peut être mise en cache par le client ou les proxys
	 * 
	 * @return vrai si la réponse peut être mise en cache, faux sinon
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
