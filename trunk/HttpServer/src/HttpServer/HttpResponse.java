package HttpServer;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import util.DateUtil;

public class HttpResponse
{
	private static final Map<Integer, String> codeDescription = new HashMap<Integer, String>() {
        {
        	put(100, "Continue");
        	put(101, "Switching Protocols");
        	put(200, "OK");
        	put(201, "Created");
        	put(202, "Accepted");
        	put(203, "Non-Authoritative Information");
        	put(204, "No Content");
        	put(205, "Reset Content");
        	put(206, "Partial Content");
        	put(300, "Multiple Choices");
        	put(301, "Moved Permanently");
        	put(302, "Found");
        	put(303, "See Other");
        	put(304, "Not Modified");
        	put(305, "Use Proxy");
        	put(306, "(Unused)");
        	put(307, "Temporary Redirect");
        	put(400, "Bad Request");
        	put(401, "Unauthorized");
        	put(402, "Payment Required");
        	put(403, "Forbidden");
        	put(404, "Not Found");
        	put(405, "Method Not Allowed");
        	put(406, "Not Acceptable");
        	put(407, "Proxy Authentication Required");
        	put(408, "Request Timed Out");
        	put(409, "Conflict");
        	put(410, "Gone");
        	put(411, "Length Required");
        	put(412, "Precondition Failed");
        	put(413, "Request Entity Too Large");
        	put(414, "Request URL Too Long");
        	put(415, "Unsupported Media Type");
        	put(416, "Requested Range Not Satisfiable");
        	put(417, "Expectation Failed");
        	put(500, "Internal Server Error");
        	put(501, "Not implemented");
        	put(502, "Bad Gateway");
        	put(503, "Service Unavailable");
        	put(504, "Gateway Timeout");
        	put(505, "HTTP Version Not Supported");
        }
    };

	public String header;
	public String content;
	public String fileName;
	public String protocol;
	public int statusCode;
	public Map<String, String> fields;
	public boolean cacheable;
	
	public HttpResponse()
	{
		this.header = "";
		this.content = "";
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
		if (codeDescription.get(this.statusCode) != null) 
		{
			this.fields.put("Date", DateUtil.formatDate(new Date()));
			if (!this.cacheable) 
			{
				this.fields.put("Expires", this.fields.get("Date"));
				this.fields.put("Cache-Control", "max-age=0, must-revalidate");
			}

			this.header = String.format("%s %d %s\r\n", this.protocol, this.statusCode, codeDescription.get(this.statusCode));

			for (String field : this.fields.keySet())
			{
				this.header += String.format("%s: %s\r\n", field, this.fields.get(field));
			}
			this.header += "\r\n";
		}
	}
}
