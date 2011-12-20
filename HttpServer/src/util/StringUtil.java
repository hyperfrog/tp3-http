package util;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern; 
import java.util.regex.Matcher; 

/**
 * Cette classe comporte une panoplie de fonctions �utilitaires� en rapport avec le traitement des cha�nes de caract�res.
 * 
 * @author Christian Lesage
 * @author Alexandre Tremblay
 *
 */
public class StringUtil
{

	/**
	 * Convertit une cha�ne de caract�res �chap�s en tableau d'octets.
	 * 
	 * @param text la cha�ne de caract�res �chap�s � convertir
	 * @return le tableau de bytes r�sultant de la conversion
	 */
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
	
	/**
	 * Convertit une cha�ne de caract�res en dictionnaire.
	 * 
	 * @param in la cha�ne � convertir
	 * @param entrySep la s�quence s�parant les entr�es du dictionnaire
	 * @param keyValSep la s�quence s�parant les cl�s des valeurs
	 * @param trim si vrai, les espaces devant et derri�re les cl�s et les valeurs sont enlev�s 
	 * @return le dictionnaire r�sultant de la conversion
	 */
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
	
	/**
	 * Convertit un dictionnaire de <String, String> en cha�ne de caract�res.
	 * 
	 * @param in le dictionnaire � convertir 
	 * @param entrySep la s�quence s�parant les entr�es du dictionnaire
	 * @param keyValSep la s�quence s�parant les cl�s des valeurs
	 * @return la cha�ne r�sultant de la conversion
	 */
	public static String mapToString(Map<String, String> in, String entrySep, String keyValSep)
	{
		String out = new String();
		
		for (Map.Entry<String, String> e : in.entrySet())
		{
			out += e.getKey() + keyValSep + e.getValue() + entrySep;
		}

		return out.substring(0, Math.max(0, out.length() - entrySep.length()));
	}

	/**
	 * Divise une cha�ne et cr�e une liste des �l�ments.
	 * 
	 * @param s Cha�ne � diviser
	 * @param d S�parateur d'�l�ments
	 * @param combine Si vrai, enl�ve les �l�ments vides; si faux, retourne les �l�ments vides 
	 * @return Liste des �l�ments de la cha�ne d'entr�e
	 */
	public static ArrayList<String> Split(final String s, String d, boolean combine)
	{
		ArrayList<String> list = new ArrayList<String>();
	
		if (s != null)
		{
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
		}
		
		return list;
	}

	/**
	 * Divise une cha�ne et cr�e une liste des �l�ments.
	 * 
	 * @param s Cha�ne � diviser
	 * @param d S�parateur d'�l�ments
	 * @param combine Si vrai, enl�ve les �l�ments vides; si faux, retourne les �l�ments vides 
	 * @return Liste des �l�ments de la cha�ne d'entr�e
	 */
	public static ArrayList<String> split(final String s, String d, boolean combine)
	{
		return Split(s, d, combine);
	}

	/**
	 * Divise une cha�ne et cr�e une liste des �l�ments sans combiner les �l�ments vides.
	 * 
	 * @param s Cha�ne � diviser
	 * @param d S�parateur d'�l�ments
	 * @return liste des �l�ments s�par�s de la cha�ne d'entr�e
	 */
	public static ArrayList<String> Split(final String s, String d)
	{
		return Split(s, d, false);
	}

	/**
	 * Divise une cha�ne et cr�e une liste des �l�ments sans combiner les �l�ments vides.
	 * 
	 * @param s Cha�ne � diviser
	 * @param d S�parateur d'�l�ments
	 * @return liste des �l�ments s�par�s de la cha�ne d'entr�e
	 */
	public static ArrayList<String> split(final String s, String d)
	{
		return Split(s, d);
	}
	
	/**
	 * Combine les �l�ments d'une liste de cha�nes pour former une seule cha�ne.
	 *  
	 * @param splitted liste de cha�nes � combiner
	 * @param sep s�parateur d'�l�ments
	 * @return cha�ne form�e de la combinaison des �l�ments 
	 */
	public static String Join(ArrayList<String> splitted, String sep)
	{
		String joined = "";
		
		for(String element : splitted)
		{
			joined += element + sep;
		}
		
		return joined.substring(0, Math.max(0, joined.length() - sep.length()));
	}

	/**
	 * Combine les �l�ments d'une liste de cha�nes pour former une seule cha�ne.
	 *  
	 * @param splitted liste de cha�nes � combiner
	 * @param sep s�parateur d'�l�ments
	 * @return cha�ne form�e de la combinaison des �l�ments 
	 */
	public static String join(ArrayList<String> splitted, String sep)
	{
		return Join(splitted, sep);
	}

	/**
	 * Indique si un tableau de bytes peut repr�senter une cha�ne UTF-8 valide.
	 * 
	 * @param bytes le tableau de bytes � valider
	 * @return vrai si le tableau de bytes peut repr�senter une cha�ne UTF-8 valide 
	 */
	public static boolean isValidUtf8(byte[] bytes)
	{
		//  Bits  Pattern
		//  ----  -------
		//    7   0xxxxxxx
		//   11   110xxxxx 10xxxxxx
		//   16   1110xxxx 10xxxxxx 10xxxxxx
		//   21   11110xxx 10xxxxxx 10xxxxxx 10xxxxxx
		//   26   111110xx 10xxxxxx 10xxxxxx 10xxxxxx 10xxxxxx
		//   32   111111xx 10xxxxxx 10xxxxxx 10xxxxxx 10xxxxxx 10xxxxxx

		int sLen;
		int n = 0;

		// Indique la longueur d'une s�quence UTF-8 en fonction  
		// des six bits les plus significatifs du premier octet
		int[] UTF8len = 
			{
				1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
				1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
				0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
				2, 2, 2, 2, 2, 2, 2, 2, 3, 3, 3, 3, 4, 4, 5, 6
			};

		while(n < bytes.length)
		{
			sLen = UTF8len[(bytes[n] >> 2) & 0x3F];

			if (sLen == 0)
			{
				// erreur : 10xxxxxx se trouve normalement au milieur d'une s�quence UTF-8 
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
				if ((bytes[n] & 0xC0) != 0x80) 
				{
					// erreur : devrait �tre 10xxxxxx
					return false;
				}
			}
			n++;
		}
		return true;
	}
	
	/**
	 * Indique si un tableau de bytes peut repr�senter une cha�ne ASCII valide.
	 * 
	 * @param bytes le tableau de bytes � valider
	 * @return vrai si le tableau de bytes peut repr�senter une cha�ne ASCII valide 
	 */
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
	
	/**
	 * Convertit un tableau de bytes en cha�ne avec l'encodage sp�cifi�.
	 * 
	 * @param bytes tableau de bytes � convertir
	 * @param encoding encodage pour la conversion
	 * @return cha�ne r�sultant de la conversion
	 */
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
}