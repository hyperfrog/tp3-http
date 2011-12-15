package http.client;

import java.util.List;
import javax.swing.table.AbstractTableModel;

/**
 * La classe DownloadTableModel représente le modèle utilisé par l'objet JTable à
 * l'intérieur de AppFrame.
 * 
 * @author Christian Lesage
 * @author Alexandre Tremblay
 *
 */
public class DownloadTableModel extends AbstractTableModel
{
	// Titre des colonnes
	private String[] columnNames = {"Adresse Url", "Taille", "État"};
	
	// Liste des téléchargements
	private List<DownloadThread> downloadsList;
	
	/**
	 * Créer un nouveau modèle pour le tableau.
	 * 
	 * @param data la liste à utiliser comme données
	 */
	public DownloadTableModel(List<DownloadThread> data)
	{
		this.downloadsList = data;
	}
	
	/**
	 * Retourne le nombre de colonnes.
	 * 
	 * @return le nombre de colonnes
	 */
	public int getColumnCount()
	{
		return this.columnNames.length;
	}
	
	/**
	 * Retourne le nombre de rangées.
	 * 
	 * @return le nombre de rangées
	 */
	public int getRowCount()
	{
		return this.downloadsList.size();
	}
	
	/**
	 * Retourne la nom de la colonne pour l'indice de la colonne
	 * passé en paramètre.
	 * 
	 * @param col l'indice de la colonne
	 * @return le nom de la colonne
	 */
	public String getColumnName(int col)
	{
		return this.columnNames[col];
	}
	
	/**
	 * Retourne la valeur de la case du tableau situé à la rangée et la colonne
	 * passés en paramètre.
	 * 
	 * @param row l'indice de la rangée
	 * @param col l'indice de la colonne
	 * @return value la valeur de la case du tableau
	 */
	public Object getValueAt(int row, int col)
	{
		Object value = null;
		DownloadThread dl = this.downloadsList.get(row); 
		
		if (dl != null)
		{
			switch (col)
			{
			case 0:
				value = dl.getUrl();
				break;
			case 1:
				value = dl.getSize();
				break;
			case 2:
				value = dl.getCurrentState();
				break;
			}
		}
		
		return value;
	}
}
