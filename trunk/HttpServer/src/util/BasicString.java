package util;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

public class BasicString
{
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