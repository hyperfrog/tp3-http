package http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class HttpRequest
{
	private HttpHeader header;
	private byte[] content; // Pas utilis�; servirait si la m�thode POST �tait impl�ment�e

	public HttpRequest()
	{
		this.header = new HttpHeader();
	}

	public void receiveHeader(InputStream is) throws IOException
	{
		BufferedReader in = new BufferedReader(new InputStreamReader(is));

		String requestHeader = new String();
		String line;
		
		// Lit l'en-t�te (header)
		do
		{
			line = in.readLine();
			requestHeader += line + "\r\n";

		} while (line != null && !line.isEmpty());

		this.header.setText(requestHeader);
		this.header.parseRequestHeader();
	}
	
	/**
	 * @return
	 */
	public HttpHeader getHeader()
	{
		return this.header;
	}
	
	/**
	 * @return the content
	 */
	public byte[] getContent()
	{
		return this.content;
	}
}
