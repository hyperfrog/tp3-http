package http;

import static util.BasicString.str$;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import util.DateUtil;

public class HttpResponse
{
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

	private String header;
	private byte[] content;
	private String fileName;
	private String protocol;
	private int statusCode;
	private boolean cacheable;
	private Map<String, String> fields;
	
	public HttpResponse()
	{
		this.header = "";
		this.content = null;
		this.fileName = "";
		this.protocol = "HTTP/1.1";
		this.statusCode = 0;
		this.fields = new HashMap<String, String>();
		this.fields.put("Server", "CLES 0.1");
		this.fields.put("Connection", "close");
		this.cacheable = true;
	}

	public void makeHeader()
	{
		if (statusCodeDesc.get(this.statusCode) != null) 
		{
			this.fields.put("Date", DateUtil.formatDate(new Date()));
			if (!this.cacheable) 
			{
				this.fields.put("Expires", this.fields.get("Date"));
				this.fields.put("Cache-Control", "max-age=0, must-revalidate");
			}

			this.header = String.format("%s %d %s\r\n", this.protocol, this.statusCode, statusCodeDesc.get(this.statusCode));

			for (String field : this.fields.keySet())
			{
				this.header += String.format("%s: %s\r\n", field, this.fields.get(field));
			}
			this.header += "\r\n";
		}
	}
	
	public void send(OutputStream os) throws IOException
	{
		if (this.header.isEmpty())
		{
			this.makeHeader();
		}
		
		OutputStreamWriter osw = new OutputStreamWriter(os);
		
		osw.write(this.header);
		osw.flush();
		
		if (this.content != null && this.content.length > 0)
		{
			os.write(this.content);
			os.flush();
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
	 * @return the header
	 */
	public String getHeader()
	{
		return header;
	}

//	/**
//	 * @param header the header to set
//	 */
//	public void setHeader(String header)
//	{
//		this.header = header;
//	}

	/**
	 * @return the content
	 */
	public byte[] getContent()
	{
		return this.content;
	}

	/**
	 * @param content the content to set
	 */
	public void setContent(byte[] content)
	{
		this.content = content;
		this.fields.put("Content-Size", str$(this.content.length));
	}

	/**
	 * @param content the content to set
	 */
	public void setContent(String content, Charset cs)
	{
		this.setContent(content.getBytes(cs));
	}

	/**
	 * @return the fileName
	 */
	public String getFileName()
	{
		return fileName;
	}

	/**
	 * @param fileName the fileName to set
	 */
	public void setFileName(String fileName)
	{
		this.fileName = fileName;
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
	 * @return the statusCode
	 */
	public int getStatusCode()
	{
		return statusCode;
	}

	/**
	 * @param statusCode the statusCode to set
	 */
	public void setStatusCode(int statusCode)
	{
		this.statusCode = statusCode;
	}

	/**
	 * @return the cacheable
	 */
	public boolean isCacheable()
	{
		return cacheable;
	}

	/**
	 * @param cacheable the cacheable to set
	 */
	public void setCacheable(boolean cacheable)
	{
		this.cacheable = cacheable;
	}
}
