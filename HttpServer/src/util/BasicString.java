package util;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern; 
import java.util.regex.Matcher; 

public class BasicString
{

	public static byte[] unescape(String text)
	{
		Pattern pHexByte = Pattern.compile("([^%]*)%([0123456789ABCDEF]{2})", Pattern.CASE_INSENSITIVE);
		Matcher mHexByte = pHexByte.matcher(text);

		ArrayList<Byte> byteList = new ArrayList<Byte>();

		try
		{
			int lastMatchEndPos = 0;
			
			while (mHexByte.find())
			{
				lastMatchEndPos = mHexByte.end();

				for (byte b : mHexByte.group(1).getBytes("ASCII"))
				{
					byteList.add(b);
				}

				byteList.add((byte)Integer.parseInt(mHexByte.group(2), 16));
			}

			for (byte b : text.substring(lastMatchEndPos, text.length()).getBytes("ASCII"))
			{
				byteList.add(b);
			}
		}
		catch (UnsupportedEncodingException e)
		{
			e.printStackTrace();
		}
		
		byte[] bytes = new byte[byteList.size()];

		for (int i = 0; i < byteList.size(); i++)
		{
			bytes[i] = byteList.get(i);
		}

		return bytes;
	}
	
	public static Map<String, String> stringToMap(String in, String entrySep, String keyValSep, boolean trim)
	{
		HashMap<String, String> map = new HashMap<String, String>(); 

		ArrayList<String> entries = split(in, entrySep);

		for(String entry : entries)
		{
			ArrayList<String> sides = split(entry, keyValSep);
			if (sides.size() >= 2)
			{
				String key = trim ? sides.get(0).trim() : sides.get(0);
				String val = trim ? sides.get(1).trim() : sides.get(1);
				map.put(key, val);
			}
		}
		return map;
	}

	//  Bits  Pattern
	//  ----  -------
	//    7   0xxxxxxx
	//   11   110xxxxx 10xxxxxx
	//   16   1110xxxx 10xxxxxx 10xxxxxx
	//   21   11110xxx 10xxxxxx 10xxxxxx 10xxxxxx
	//   26   111110xx 10xxxxxx 10xxxxxx 10xxxxxx 10xxxxxx
	//   32   111111xx 10xxxxxx 10xxxxxx 10xxxxxx 10xxxxxx 10xxxxxx
	
	public static boolean isValidUtf8(byte[] bytes)
	{
		 int sLen;
		 int n = 0;

		 int[] UTF8len = 
			 {
				 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
				 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
				 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
				 2, 2, 2, 2, 2, 2, 2, 2, 3, 3, 3, 3, 4, 4, 5, 6
			 };

		while(n < bytes.length)
		{
			sLen = UTF8len[band(bshr(bytes[n], 2), 0x3F)];

			if (sLen == 0)
			{
				// erroneous: 10xxxxxx is normally found in the middle of a UTF-8 sequence
				return false;
			}

			while (sLen - 1 > 0)
			{
				sLen--;
				if (n == bytes.length - 1)
				{
					return false;
				}
				n++;
				if (band(bytes[n], 0xC0) != 0x80) 
				{
					// erroneous: it should be 10xxxxxx
					return false;
				}
			}
			n++;
		}
		return true;
	}
	
	public static boolean isAscii(byte[] bytes)
	{
		for(int i = 0; i < bytes.length; i++)
		{
			if ((bytes[i] & 0x80) > 0)
			{
				return false;
			}
		}

		return true;
	}
	
	public static String bytesToString(byte[] bytes, String encoding)
	{
		String str = null;
		try
		{
			str = new String(bytes, encoding);
		}
		catch (UnsupportedEncodingException e) 
		{
			System.err.println("Unsupported encoding : " + encoding);
			e.printStackTrace();
		}
		
		return str;
	}

	public static String toHex(byte[] bytes)
	{
		BigInteger bi = new BigInteger(1, bytes);
		return String.format("%0" + (bytes.length << 1) + "X", bi);
	}

	
//	public static String toUtf8(byte[] bytes)
//	{
//		String utf8 = null;
//		try
//		{
//			utf8 = new String(bytes, "UTF-8");
//		}
//		catch (UnsupportedEncodingException e) {}
//		
//		return utf8;
//	}
//	
//	public static String toLatin1(byte[] bytes)
//	{
//		String latin1 = null;
//		try
//		{
//			latin1 = new String(bytes, "ISO-8859-1");
//		}
//		catch (UnsupportedEncodingException e) {}
//		
//		return latin1;
//	}

	public static String Date$()
	{
		String date = DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date());
		return date;
	}
	
	public static String date$()
	{
		return Date$();
	}
	
	public static String Time$()
	{
		String time = DateFormat.getTimeInstance(DateFormat.MEDIUM).format(new Date());
		return time;
	}
	
	public static String time$()
	{
		return Time$();
	}
	
    public static String Str$(int n)
    {
          return "" + n;
    }
    
    public static String str$(int n)
    {
          return Str$(n);
    }
    
    public static String Str$(long n)
    {
          return "" + n;
    }
    
    public static String str$(long n)
    {
          return Str$(n);
    }
    
    public static String Left$(final String text, int length)
    {
          return text.substring(0, length);
    }
    
    public static String left$(final String text, int length)
    {
    	return Left$(text, length);
    }

    public static String Right$(final String text, int length)
    {
          return text.substring(text.length() - length, text.length());
    }  

    public static String right$(final String text, int length)
    {
    	return Right$(text, length);
    }
    
    public static String Mid$(final String text, int start, int length)
    {
    	return text.substring(start - 1, start - 1 + length);
    }  

    public static String mid$(final String text, int start, int length)
    {
    	return Mid$(text, start, length);
    }

    public static String Mid$(final String text, int start)
    {
          return text.substring(start - 1, text.length());
    }
    
    public static String mid$(final String text, int start)
    {
    	return Mid$(text, start);
    }

    public static ArrayList<String> Split(final String s, String d)
	{
    	return Split(s, d, false);
	}

    public static ArrayList<String> split(final String s, String d)
    {
    	return Split(s, d);
    }
    
    public static ArrayList<String> Split(final String s, String d, boolean combine)
	{
		ArrayList<String> list = new ArrayList<String>();

		int from = 0;
		int upTo = 0;
		
		while (upTo > -1)
		{
			upTo = s.indexOf(d, from);

			if (upTo > -1)
			{
				if (!combine || upTo - from > 0)
				{
					list.add(s.substring(from, upTo));
				}
				from = upTo + d.length();
			}
			else if (!combine || s.length() - from > 0)
			{
				list.add(s.substring(from, s.length()));
			}
		}
		
		return list;
	}
    
    public static ArrayList<String> split(final String s, String d, boolean combine)
	{
    	return Split(s, d, combine);
	}
    
    public static String Join(ArrayList<String> splitted, String sep)
    {
    	String joined = "";
    	
    	for(String element : splitted)
    	{
   			joined += element + sep;
    	}
    	
    	return joined.substring(0, Math.max(0, joined.length() - sep.length()));
    }
    
    public static String join(ArrayList<String> splitted, String sep)
    {
    	return Join(splitted, sep);
    }
    
	public static char Asc(final String s)
	{
		return s.charAt(0);
	}
    
	public static char asc(final String s)
	{
		return Asc(s);
	}
    
	public static String Chr$(char c)
	{
		return "" + c;
	}
	
	public static String chr$(char c)
	{
		return Chr$(c);
	}
	
	public static String Chr$(int c)
	{
		return "" + (char)c;
	}
	
	public static String chr$(int c)
	{
		return Chr$(c);
	}
	
	public static int Len(final String s)
	{
		return s.length();
	}
	
	public static int len(final String s)
	{
		return len(s);
	}
	
	public static int Pos(final String s1, final String s2)
	{
		return InStr(s1, s2);
	}
	
	public static int pos(final String s1, final String s2)
	{
		return Pos(s1, s2);
	}
	
	public static int InStr(int start, final String s1, final String s2)
	{
		return s1.indexOf(s2, start - 1) + 1;
	}
	
	public static int instr(int start, final String s1, final String s2)
	{
		return InStr(start, s1, s2);
	}
	
	public static int InStr(final String s1, final String s2)
	{
		return BasicString.InStr(1, s1, s2);
	}
	
	public static int instr(final String s1, final String s2)
	{
		return InStr(s1, s2);
	}
	
	public static String Trim$(final String s)
	{
		return s.trim();
	}
	
	public static String trim$(final String s)
	{
		return Trim$(s);
	}
	
	public static int Band(int i1, int i2)
	{
		return i1 & i2;
	}

	public static int band(int i1, int i2)
	{
		return Band(i1, i2);
	}

//	public static char Band(char i1, char i2)
//	{
//		return (char) (i1 & i2);
//	}
//
//	public static char band(char i1, char i2)
//	{
//		return Band(i1, i2);
//	}
	
	public static int Bshr(int i1, int i2)
	{
		return i1 >> i2;
	}
	
	public static int bshr(int i1, int i2)
	{
		return Bshr(i1, i2);
	}
	
}