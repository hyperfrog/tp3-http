package http;

import static util.BasicString.stringToMap;

import http.event.RequestEvent;
import http.event.RequestEventProcessor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class MainApp
{
	private final static int DURATION = 0;
	private final static String SERVER_PATH = "/C:/Users/Public/Documents/HTTP_Server/";
	private final static String SITE_FOLDER = "Javadoc";
	private final static String IP_ADDRESS = "127.0.0.1"; 
	private final static int PORT_NUM = 80;

	/**
	 * @param args
	 */
	@SuppressWarnings("unused")
	public static void main(String[] args)
	{
		SocketListener sl = new SocketListener(SERVER_PATH, SITE_FOLDER, IP_ADDRESS, PORT_NUM);
		Thread slt = new Thread(sl);
		slt.start();
		
		try
		{
			if (DURATION > 0)
			{
				Thread.sleep(DURATION * 1000);
				sl.stop();
				System.out.println(String.format("%d secondes �coul�es.", DURATION));
			}
			else
			{
				slt.join();
			}
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
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

