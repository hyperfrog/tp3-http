package util;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern; 
import java.util.regex.Matcher; 

/**
 * Cette classe comporte une panoplie de fonctions «utilitaires» en rapport avec le traitement des chaînes de caractères.
 * 
 * @author Christian Lesage
 * @author Alexandre Tremblay
 *
 */
public class StringUtil
{

	/**
	 * Convertit une chaîne de caractères échapés en tableau d'octets.
	 * 
	 * @param text la chaîne de caractères échapés à convertir
	 * @return le tableau de bytes résultant de la conversion
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
	 * Convertit une chaîne de caractères en dictionnaire.
	 * 
	 * @param in la chaîne à convertir
	 * @param entrySep la séquence séparant les entrées du dictionnaire
	 * @param keyValSep la séquence séparant les clés des valeurs
	 * @param trim si vrai, les espaces devant et derrière les clés et les valeurs sont enlevés 
	 * @return le dictionnaire résultant de la conversion
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
	 * Convertit un dictionnaire de <String, String> en chaîne de caractères.
	 * 
	 * @param in le dictionnaire à convertir 
	 * @param entrySep la séquence séparant les entrées du dictionnaire
	 * @param keyValSep la séquence séparant les clés des valeurs
	 * @return la chaîne résultant de la conversion
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
	 * Divise une chaîne et crée une liste des éléments.
	 * 
	 * @param s Chaîne à diviser
	 * @param d Séparateur d'éléments
	 * @param combine Si vrai, enlève les éléments vides; si faux, retourne les éléments vides 
	 * @return Liste des éléments de la chaîne d'entrée
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
	 * Divise une chaîne et crée une liste des éléments.
	 * 
	 * @param s Chaîne à diviser
	 * @param d Séparateur d'éléments
	 * @param combine Si vrai, enlève les éléments vides; si faux, retourne les éléments vides 
	 * @return Liste des éléments de la chaîne d'entrée
	 */
	public static ArrayList<String> split(final String s, String d, boolean combine)
	{
		return Split(s, d, combine);
	}

	/**
	 * Divise une chaîne et crée une liste des éléments sans combiner les éléments vides.
	 * 
	 * @param s Chaîne à diviser
	 * @param d Séparateur d'éléments
	 * @return liste des éléments séparés de la chaîne d'entrée
	 */
	public static ArrayList<String> Split(final String s, String d)
	{
		return Split(s, d, false);
	}

	/**
	 * Divise une chaîne et crée une liste des éléments sans combiner les éléments vides.
	 * 
	 * @param s Chaîne à diviser
	 * @param d Séparateur d'éléments
	 * @return liste des éléments séparés de la chaîne d'entrée
	 */
	public static ArrayList<String> split(final String s, String d)
	{
		return Split(s, d);
	}
	
	/**
	 * Combine les éléments d'une liste de chaînes pour former une seule chaîne.
	 *  
	 * @param splitted liste de chaînes à combiner
	 * @param sep séparateur d'éléments
	 * @return chaîne formée de la combinaison des éléments 
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
	 * Combine les éléments d'une liste de chaînes pour former une seule chaîne.
	 *  
	 * @param splitted liste de chaînes à combiner
	 * @param sep séparateur d'éléments
	 * @return chaîne formée de la combinaison des éléments 
	 */
	public static String join(ArrayList<String> splitted, String sep)
	{
		return Join(splitted, sep);
	}

	/**
	 * Indique si un tableau de bytes peut représenter une chaîne UTF-8 valide.
	 * 
	 * @param bytes le tableau de bytes à valider
	 * @return vrai si le tableau de bytes peut représenter une chaîne UTF-8 valide 
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

		// Indique la longueur d'une séquence UTF-8 en fonction  
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
				// erreur : 10xxxxxx se trouve normalement au milieur d'une séquence UTF-8 
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
					// erreur : devrait être 10xxxxxx
					return false;
				}
			}
			n++;
		}
		return true;
	}
	
	/**
	 * Indique si un tableau de bytes peut représenter une chaîne ASCII valide.
	 * 
	 * @param bytes le tableau de bytes à valider
	 * @return vrai si le tableau de bytes peut représenter une chaîne ASCII valide 
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
	 * Convertit un tableau de bytes en chaîne avec l'encodage spécifié.
	 * 
	 * @param bytes tableau de bytes à convertir
	 * @param encoding encodage pour la conversion
	 * @return chaîne résultant de la conversion
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