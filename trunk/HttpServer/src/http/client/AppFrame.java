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
 * La classe AppFrame permet de créer une fenêtre servant de contenant
 * à la barre d'outils ainsi qu'au panneau principal de l'application.
 * 
 * @author Christian Lesage
 * @author Alexandre Tremblay
 * 
 */
public class AppFrame extends JFrame implements ActionListener, ComponentListener
{
	// Dimension initiale de la fenêtre
	private static final Dimension INIT_SIZE = new Dimension(600, 300);
	
	// Largeur minimale de la fenêtre
	private static final int MIN_WIDTH = 600;
	
	// Hauteur minimale de la fenêtre
	private static final int MIN_HEIGHT = 300;
	
	// Titre de la fenêtre
	private static final String INIT_TITLE = "Outils de téléchargement";
	
	// Objet pour gèrer les téléchargements
	private AppDownload appDownload;
	
	// Barre d'outils de l'application
	private AppToolBar appToolBar;
	
	// Tableau contenant la liste des téléchargements
	private JTable downloadsTable;
	
	/**
	 * Créer une nouvelle fenêtre
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
		
		// Spécifie l'écouteur pour la fenêtre
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
	 * Affiche la boîte de dialogue «Ajouter une adresse»
	 */
	private void showAddDialog()
	{
		AppAddDialog addDialog = new AppAddDialog(this);
		addDialog.setLocationRelativeTo(this);
		addDialog.setVisible(true);
	}
	
	/**
	 * Retourne l'objet AppDownload utilisé par AppFrame.
	 * 
	 * @return l'objet AppDownload utilisé par AppFrame
	 */
	public AppDownload getAppDownload()
	{
		return this.appDownload;
	}
	
	/**
	 * Met à jour le tableau des téléchargements
	 */
	public void updateTable()
	{
		((DownloadTableModel) this.downloadsTable.getModel()).fireTableDataChanged();
	}
	
	@Override
	/**
	 * Reçoit et traite les événements relatifs aux boutons de la barre d'outils
	 * Cette méthode doit être publique mais ne devrait pas être appelée directement.
	 * 
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 * 
	 * @param evt événement déclencheur
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
						"Êtes-vous sûr de vouloir supprimer ce téléchargement ?",
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
		// Vérifie si la largeur et la hauteur sont inférieures 
		// à la valeur minimale permise pour chacune
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
