package http.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class HttpRequest
{
	private HttpHeader header;
	private byte[] content; // Pas utilisé, mais servirait si la méthode POST était implémentée

	/**
	 * 
	 */
	public HttpRequest()
	{
		this.header = new HttpHeader();
	}

	/**
	 * @param is
	 * @throws IOException
	 */
	public void receiveHeader(InputStream is) throws IOException
	{
		BufferedReader in = new BufferedReader(new InputStreamReader(is));

		String requestHeader = new String();
		String line;
		
		// Lit l'en-tête (header)
		do
		{
			line = in.readLine();
			requestHeader += line + "\r\n";

		} while (line != null && !line.isEmpty());

		this.header.setText(requestHeader);
//		this.header.parseRequestHeader();
	}
	
	/**
	 * @param os
	 * @return
	 * @throws IOException
	 */
	public boolean send(OutputStream os) throws IOException
	{
		if (this.header.getText() == null || this.header.getText().isEmpty())
		{
			if (!this.header.makeRequestHeader())
			{
				return false;
			}
		}

		OutputStreamWriter osw = new OutputStreamWriter(os);

		osw.write(this.header.getText());
		osw.flush();
		return true;
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
