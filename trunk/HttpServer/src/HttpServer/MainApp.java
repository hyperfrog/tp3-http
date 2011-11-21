package HttpServer;

import static util.BasicString.*;
import util.DateUtil;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern; 
import java.util.regex.Matcher; 

public class MainApp
{
	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		final String serverPath = "C:/Users/Public/Documents/HTTP_Server/";
		final String site = "Javadoc";
		final int portNum = 80;
		boolean serverIsActive = true;

		// Read MIME types
		Map<String, String> mimeTypes = new HashMap<String, String>();
		
		File mtFile = new File(serverPath + "mime_types.txt");
		if (mtFile.exists())
		{
			try
			{
				// Open the file 
				FileInputStream fis = new FileInputStream(mtFile);
				// Get the object of DataInputStream
				DataInputStream dis = new DataInputStream(fis);
				BufferedReader br = new BufferedReader(new InputStreamReader(dis));
				String line;
				//Read File Line By Line
				while ((line = br.readLine()) != null)
				{
					ArrayList<String> lineArr = split(line, "=");
					String key = trim$(lineArr.get(0));
					String val = trim$(lineArr.get(1));
					mimeTypes.put(key, val);
				}
				//Close the input stream
				br.close();
				dis.close();
				fis.close();
			}
			catch (IOException e) //Catch exception if any
			{
				System.err.println("Error: " + e.getMessage());
			}
		}

		ServerSocket server = null;
		Socket clientSocket = null;
		BufferedReader in = null;
		PrintWriter out = null;
		   
		try
		{
			server = new ServerSocket(portNum, 10, InetAddress.getByName("127.0.0.1"));
		}
		catch (IOException e)
		{
			System.out.println(String.format("Could not listen on port %d.", portNum));
			System.exit(-1);
		}

		while (serverIsActive)
		{
			try
			{
				clientSocket = server.accept();
				
				in = new BufferedReader(new InputStreamReader(
						clientSocket.getInputStream()));
				
				out = new PrintWriter(clientSocket.getOutputStream(), true);
			}
			catch (IOException e)
			{
				System.out.println(String.format("Accept failed: %d.", portNum));
				System.exit(-1);
			}

			try
			{
				String requestHeader = new String();
				String line;
				
				do
				{
					line = in.readLine();
					requestHeader += line + "\n";

				} while (!line.isEmpty());

				System.out.print(requestHeader);

				HttpRequest request = new HttpRequest(requestHeader);

				if (request.method.equals("GET")) 
				{
					HttpResponse response = new HttpResponse();

					response.cacheable = true;
					response.fileName = serverPath + site + request.pathName;

					File f = new File(response.fileName);

					if (f.exists()) 
					{
						// if dateTime1 is NOT earlier than dateTime2 -> 304 Not Modified
						if (request.fields.get("If-Modified-Since") != null 
							&& !DateUtil.parseDate(request.fields.get("If-Modified-Since")).before(
								DateUtil.parseDate(DateUtil.formatDate(new Date(f.lastModified())))))
						{
							response.statusCode = 304;
							response.makeHeader();
							out.write(response.header);
							out.flush();
							System.out.print(response.header);
						}
						else
						{
							response.statusCode = 200;

							int fileSize = (int) f.length();
							
							try
							{
								// Open the file 
								FileInputStream fis = new FileInputStream(f);
								// Get the object of DataInputStream	
								DataInputStream dis = new DataInputStream(fis);
								byte[] data = new byte[fileSize];
								dis.readFully(data);

								response.content = new String(data);
								response.fields.put("Content-Size", str$(fileSize));

								Pattern pFileExtension = Pattern.compile("\\.([^\\.]+)\\Z");
								Matcher mFileExtension = pFileExtension.matcher(response.fileName);
								
								if (mFileExtension.find()) 
								{
									String fileMimeType = mimeTypes.get(mFileExtension.group(1));
									response.fields.put("Content-Type", fileMimeType);
								}

								response.fields.put("Cache-Control", "public");
								response.fields.put("Last-Modified", DateUtil.formatDate(new Date(f.lastModified())));

								dis.close();
								fis.close();
							}
							catch (IOException e)
							{
								// TODO : Incapable de lire le fichier demandé
								System.out.println(e.getMessage());
							}

							response.makeHeader();

							out.write(response.header);
							out.flush();
							System.out.print(response.header);
							out.write(response.content);
						}
					}
					else
					{
						response.statusCode = 404;
						response.fields.put("Content-Size", str$(0));

						if (request.fields.get("Accept") != null 
							&& split(request.fields.get("Accept"), ", ").contains("text/html"))
						{
							File f404 = new File(serverPath + "error_404.htm");

							if (f404.exists())
							{
								int fileSize = (int) f404.length();

								try
								{
									// Open the file 
									FileInputStream fis = new FileInputStream(f404);
									// Get the object of DataInputStream	
									DataInputStream dis = new DataInputStream(fis);
									byte[] data = new byte[fileSize];

									dis.readFully(data);
									
									response.content = new String(data);
									response.fields.put("Content-Size", str$(fileSize));
									response.fields.put("Content-Type", "text/html");
									
									dis.close();
									fis.close();
								}
								catch (IOException e)
								{
									// TODO : Incapable de lire le fichier erreur 404
									System.out.println(e.getMessage());
								}
							}
						}

						response.makeHeader();

						out.write(response.header);
						out.flush();
						System.out.print(response.header);
						out.write(response.content);
					}
				}
				out.flush();
		        out.close();
		        in.close();
		        clientSocket.close();
			}
			catch (IOException e)
			{
				System.out.println(e.getMessage());
			}
		}
	}

//	public static String unescape(String text)
//	{
//		int hexCode;
//		int startPos; 
//		String convertedText = "";
//		re = regex("%([0123456789ABCDEF]{2})", "i");
//
//		while (re.match(text))
//		{
//			[hexCode, startPos] = re.info(1);
//			convertedText = convertedText + text[1 -> startPos - 2] + chr$(eval("0x" + hexCode));
//			text = text[startPos + 2 -> -1];
//		}
//		convertedText = convertedText + text;
//		return convertedText;
//	}

//	public static boolean UTF8_is_ASCII(String text)
//	{
//		for (int n = 0; n < len(text); n++)
//		{
//			if (text.charAt(n) > 127) 
//			{
//				return false;
//			}
//		}
//		return true;
//	}

//	public static String UTF8_to_Latin1(String text)
//	{
//		String convertedText = "";
//			 byte, uc, sLen, n = 0, convertedText = ""
//
//		//  Bits  Pattern
//		//  ----  -------
////		    7   0xxxxxxx
//		//   11   110xxxxx 10xxxxxx
//		//   16   1110xxxx 10xxxxxx 10xxxxxx
//		//   21   11110xxx 10xxxxxx 10xxxxxx 10xxxxxx
//		//   26   111110xx 10xxxxxx 10xxxxxx 10xxxxxx 10xxxxxx
//		//   32   111111xx 10xxxxxx 10xxxxxx 10xxxxxx 10xxxxxx 10xxxxxx
//
//
////			A map from the most-significant 6 bits of the first byte
////			to the total number of bytes : a UTF-8 character.
//
//			 UTF8len = [
//				1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
//				1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
//				0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
//				2, 2, 2, 2, 2, 2, 2, 2, 3, 3, 3, 3, 4, 4, 5, 6]
//			
//			while(n < len(text))
//				n = n + 1
//				byte = asc(text[n])
//
//				sLen = UTF8len [band(bshr(byte,2),0x3F)+1]
//
//				if sLen = 1 {
//					uc = band(byte,0x7F)
//				else if sLen = 2 {
//					uc = band(byte,0x1F)
//				else if sLen = 3 {
//					uc = band(byte,0x0F)
//				else if sLen = 4 {
//					uc = band(byte,0x07)
//				else if sLen = 5 {
//					uc = band(byte,0x03)
//				else if sLen = 6 {
//					uc = band(byte,0x01)
//				else if sLen = 0 {
//					// erroneous: 10xxxxxx is normally found : the middle of a UTF-8 sequence
//					uc = band(byte,0x3F)
//					sLen = 5
//				}
//
//				while ((sLen-1) && (n < len(text)))
//					sLen = sLen - 1
//					n = n + 1
//					byte = asc(text[n])
//
//					if (band(byte, 0xC0) = 0x80) {
//						uc = bor(bshl(uc, 6), band(byte, 0x3F))
//					else // unexpected start of a new character
//						n = n - 1
//					}
//				}
//
//				if (uc <= 0xFF) {
//					convertedText = convertedText + chr$(uc)
//				else // this character can't be represented : Latin-1
//					// a reasonable alternative is 0x1A (SUB)
//					convertedText = convertedText + chr$(0x1A)
//				}
//			}
//			return convertedText;
//		}

//		public static boolean UTF8_is_MultiByte(String text)
//		{
//			 sLen, n = 0
//
//			 UTF8len = [
//				1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
//				1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
//				0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
//				2, 2, 2, 2, 2, 2, 2, 2, 3, 3, 3, 3, 4, 4, 5, 6]
//
//			while(n < len(text))
//				n = n + 1
//
//				sLen = UTF8len[band(bshr(asc(text[n]),2),0x3F)+1]
//
//				if sLen = 0 {
//					// erroneous: 10xxxxxx is normally found : the middle of a UTF-8 sequence
//					return false
//				}
//
//				while (sLen-1)
//					sLen = sLen - 1
//					if n = len(text) {
//						return false
//					}
//					n = n + 1
//					if (band(asc(text[n]), 0xC0) <> 0x80) {
//						// erroneous: it should be 10xxxxxx
//						return false
//					}
//				}
//			}
//
//			return true;
//		}
	
}