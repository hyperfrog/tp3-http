package http.client;

import java.util.List;
import javax.swing.table.AbstractTableModel;

/**
 * La classe DownloadTableModel repr�sente le mod�le utilis� par l'objet JTable �
 * l'int�rieur de AppFrame.
 * 
 * @author Christian Lesage
 * @author Alexandre Tremblay
 *
 */
public class DownloadTableModel extends AbstractTableModel
{
	// 
	private String[] columnNames = {"Adresse Url", "Taille", "�tat"};
	
	// 
	private List<DownloadThread> downloadsList;
	
	/**
	 * 
	 * @param data
	 */
	public DownloadTableModel(List<DownloadThread> data)
	{
		this.downloadsList = data;
	}
	
	/**
	 * 
	 */
	public int getColumnCount()
	{
		return this.columnNames.length;
	}
	
	/**
	 * 
	 */
	public int getRowCount()
	{
		return this.downloadsList.size();
	}
	
	/**
	 * 
	 */
	public String getColumnName(int col)
	{
		return this.columnNames[col];
	}
	
	/**
	 * 
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
