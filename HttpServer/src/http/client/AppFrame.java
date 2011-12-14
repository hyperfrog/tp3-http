package http.client;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;

/**
 * La classe AppFrame permet de cr�er une fen�tre servant de contenant
 * � la barre d'outils ainsi qu'au panneau principal de l'application.
 * 
 * @author Christian Lesage
 * @author Alexandre Tremblay
 * 
 */
public class AppFrame extends JFrame implements ActionListener, ComponentListener
{
	// Dimension initiale de la fen�tre
	private static final Dimension INIT_SIZE = new Dimension(600, 300);
	
	// Largeur minimale de la fen�tre
	private static final int MIN_WIDTH = 600;
	
	// Hauteur minimale de la fen�tre
	private static final int MIN_HEIGHT = 300;
	
	// Titre de la fen�tre
	private static final String INIT_TITLE = "Outils de t�l�chargement";
	
	// Objet pour g�rer les t�l�chargements
	private AppDownload appDownload;
	
	// Barre d'outils de l'application
	private AppToolBar appToolBar;
	
	// Tableau contenant la liste des t�l�chargements
	private JTable downloadsTable;
	
	/**
	 * Cr�er une nouvelle fen�tre
	 */
	public AppFrame()
	{
		super();
		this.setNativeLookAndFeel();
		
		this.appDownload = new AppDownload(this);
		this.appToolBar = new AppToolBar(this);
		this.downloadsTable = new JTable();
		
		this.setTitle(AppFrame.INIT_TITLE);
		this.setSize(AppFrame.INIT_SIZE);
		this.setLocationRelativeTo(null);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		DownloadTableModel tableModel = new DownloadTableModel(this.appDownload.getDownloadsList());
		this.downloadsTable.setModel(tableModel);
		this.downloadsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		JScrollPane scrollpane = new JScrollPane(this.downloadsTable);
		
		this.getContentPane().setLayout(new BorderLayout());
		
		this.getContentPane().add(this.appToolBar, BorderLayout.PAGE_START);
		this.getContentPane().add(scrollpane, BorderLayout.CENTER);
		
		// Sp�cifie l'�couteur pour la fen�tre
		this.addComponentListener(this);
	}
	
	// Change le look and feel pour celui de la plateforme
	private void setNativeLookAndFeel()
	{
		try
		{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (Exception e)
		{
			System.err.println("Incapable de changer l'apparence de l'application.");
		}
	}
	
	/*
	 * Affiche la bo�te de dialogue �Ajouter une adresse�
	 */
	private void showAddDialog()
	{
		AppAddDialog addDialog = new AppAddDialog(this);
		addDialog.setLocationRelativeTo(this);
		addDialog.setVisible(true);
	}
	
	/**
	 * Retourne l'objet AppDownload utilis� par AppFrame.
	 * 
	 * @return l'objet AppDownload utilis� par AppFrame
	 */
	public AppDownload getAppDownload()
	{
		return this.appDownload;
	}
	
	/**
	 * Met � jour le tableau des t�l�chargements
	 */
	public void updateTable()
	{
		((DownloadTableModel) this.downloadsTable.getModel()).fireTableDataChanged();
	}
	
	@Override
	/**
	 * Re�oit et traite les �v�nements relatifs aux boutons de la barre d'outils
	 * Cette m�thode doit �tre publique mais ne devrait pas �tre appel�e directement.
	 * 
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 * 
	 * @param evt �v�nement d�clencheur
	 */
	public void actionPerformed(ActionEvent evt)
	{
		boolean needUpdate = false;
		
		if (evt.getActionCommand().equals("ADD"))
		{
			this.showAddDialog();
			needUpdate = true;
		}
		else if (evt.getActionCommand().equals("DELETE"))
		{
			if (this.downloadsTable.getSelectedRow() != -1)
			{
				int response = JOptionPane.showConfirmDialog(
						this, 
						"�tes-vous s�r de vouloir supprimer ce t�l�chargement ?",
						"Confirmation", 
						JOptionPane.YES_NO_OPTION, 
						JOptionPane.QUESTION_MESSAGE);
				
				if (response == JOptionPane.YES_OPTION)
				{
					this.appDownload.removeDownload(this.downloadsTable.getSelectedRow());
					needUpdate = true;
				}
			}
		}
		else if (evt.getActionCommand().equals("MOVE_UP"))
		{
			if (this.downloadsTable.getSelectedRow() != -1)
			{
				this.appDownload.moveUp(this.downloadsTable.getSelectedRow());
				needUpdate = true;
			}
		}
		else if (evt.getActionCommand().equals("MOVE_DOWN"))
		{
			if (this.downloadsTable.getSelectedRow() != -1)
			{
				this.appDownload.moveDown(this.downloadsTable.getSelectedRow());
				needUpdate = true;
			}
		}
		else if (evt.getActionCommand().equals("START"))
		{
			if (this.downloadsTable.getSelectedRow() != -1)
			{
				this.appDownload.startDownload(this.downloadsTable.getSelectedRow());
			}
		}
		else if (evt.getActionCommand().equals("STOP"))
		{
			if (this.downloadsTable.getSelectedRow() != -1)
			{
				this.appDownload.stopDownload(this.downloadsTable.getSelectedRow());
			}
		}
		
		if (needUpdate)
		{
			this.updateTable();
		}
	}
	
	@Override
	public void componentResized(ComponentEvent evt)
	{
		int width = getWidth();
		int height = getHeight();
		// V�rifie si la largeur et la hauteur sont inf�rieures 
		// � la valeur minimale permise pour chacune
		boolean resize = false;
		if (width < AppFrame.MIN_WIDTH)
		{
			resize = true;
			width = AppFrame.MIN_WIDTH;
		}
		if (height < AppFrame.MIN_HEIGHT)
		{
			resize = true;
			height = AppFrame.MIN_HEIGHT;
		}
		if (resize)
		{
			this.setSize(width, height);
		}
	}
	
	@Override
	public void componentHidden(ComponentEvent evt)
	{	
	}

	@Override
	public void componentMoved(ComponentEvent evt)
	{
	}
	
	@Override
	public void componentShown(ComponentEvent evt)
	{
	}
}
