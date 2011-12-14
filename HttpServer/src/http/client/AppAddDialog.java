package http.client;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * La classe AppAddDialog sert de bo�te de dialogue pour ajouter une nouvelle adresse.
 *
 * @author Christian Lesage
 * @author Alexandre Tremblay
 * 
 */
public class AppAddDialog extends JDialog implements ActionListener, WindowListener
{
	// JFrame parent
	private AppFrame parent;
	
	// Panneau contenant les champs
	private JPanel fieldsPanel;
	
	// Panneau contenant le Label et le TextField pour l'adresse 
	private JPanel urlPanel;
	
	// Label contenant �Adresse URL � ajouter : �
	private JLabel urlLabel;
	
	// TextField pour entrer l'adresse URL � t�l�charger
	private JTextField urlField;
	
	// Panneau contenant le Label et le TextField pour la destination
	private JPanel destinationPanel;
	
	// Label contenant �Destination : �
	private JLabel destinationLabel;
	
	// TextField pour entrer la destination du fichier
	private JTextField destinationField;
	
	// Panneau contenant les boutons
	private JPanel buttonsPanel;

	// Bouton pour ajouter l'adresse
	private JButton addButton;
	
	// Bouton pour annuler l'op�ration et fermer la fen�tre
	private JButton cancelButton;
	
	/**
	 * Construit la bo�te de dialogue Ajouter une adresse
	 * 
	 * @param parent objet parent de la bo�te de dialogue
	 */
	public AppAddDialog(AppFrame parent)
	{
		super(parent);
		
		this.parent = parent;
		
		this.setTitle("Ajouter une adresse");
		this.setResizable(false);
		this.setModal(true);
		
		// Initialise les composants
		this.initComponents();
		this.pack();
		
		this.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		
		// Cette bo�te de dialogue impl�mente son propre �couteur Window
		this.addWindowListener(this);
	}
	
	// Initialise les composants de la bo�te de dialogue
	private void initComponents()
	{
		this.fieldsPanel = new JPanel();
		this.urlPanel = new JPanel();
		this.urlLabel = new JLabel();
		this.urlField = new JTextField();
		this.destinationPanel = new JPanel();
		this.destinationLabel = new JLabel();
		this.destinationField = new JTextField();
		this.buttonsPanel = new JPanel();
		this.addButton = new JButton();
		this.cancelButton = new JButton();
		
		this.fieldsPanel.setLayout(new BorderLayout());
		
		this.urlPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
		
		this.urlLabel.setText("Adresse URL � ajouter : ");
		
		this.urlField.setColumns(30);
		
		this.urlPanel.add(this.urlLabel);
		this.urlPanel.add(this.urlField);
		
		this.destinationPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
		
		this.destinationLabel.setText("Destination : ");
		
		this.destinationField.setColumns(30);
		
		this.destinationPanel.add(this.destinationLabel);
		this.destinationPanel.add(this.destinationField);
		
		this.fieldsPanel.add(this.urlPanel, BorderLayout.NORTH);
		this.fieldsPanel.add(this.destinationPanel, BorderLayout.SOUTH);
		
		this.buttonsPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));
		
		this.addButton.setText("Ajouter");
		this.addButton.setActionCommand("ADD");
		
		this.cancelButton.setText("Annuler");
		this.cancelButton.setActionCommand("CANCEL");
		
		this.buttonsPanel.add(this.addButton);
		this.buttonsPanel.add(this.cancelButton);
		
		this.getContentPane().add(this.fieldsPanel, BorderLayout.CENTER);
		this.getContentPane().add(this.buttonsPanel, BorderLayout.PAGE_END);
		
        // Sp�cifie les �couteurs d'action pour les boutons
		this.addButton.addActionListener(this);
		this.cancelButton.addActionListener(this);
	}
	
	// Ferme la bo�te de dialogue
	private void close()
	{
		this.setVisible(false);
		this.dispose();
	}
	
	// V�rifie la validit� du dossier de destination et cr�er les dossier manquants
	private void addUrl()
	{
		if (this.urlField.getText().equals("") || this.destinationField.getText().equals(""))
		{
			JOptionPane.showMessageDialog(this, "Les champs ne doivent pas �tre vides.", "Erreur", JOptionPane.ERROR_MESSAGE);
		}
		else
		{
			URL newUrl = null;
			try
			{
				newUrl = new URL(this.urlField.getText());
			}
			catch (MalformedURLException e)
			{
				JOptionPane.showMessageDialog(this, "L'adresse URL entr�e n'est pas valide.", "Erreur", JOptionPane.ERROR_MESSAGE);
				e.printStackTrace();
			}
			
			if (newUrl != null)
			{
				File dest = new File(this.destinationField.getText());
				
				String savePath = null;
				try
				{
					// V�rifie que le chemin ne contient pas de caract�res ill�gaux
					savePath = dest.getCanonicalPath();
				}
				catch (IOException e)
				{
					JOptionPane.showMessageDialog(this, "La destination n'est pas valide.", "Erreur", JOptionPane.ERROR_MESSAGE);
					e.printStackTrace();
				}
				
				// R�cup�re le chemin sans fichier � la fin
				File finalDest = new File(new File(savePath).getParent());
				
				// Si des r�pertoires sont manquant alors on les cr�er.
				if (!finalDest.exists())
				{
					finalDest.mkdirs();
				}
				
				// Ajoute l'adresse
				this.parent.getAppDownload().addDownload(this.urlField.getText(), finalDest.getParent() + "\\");
				
				// Ferme la bo�te de dialogue
				this.close();
			}
		}
	}
	
	/**
	 * Re�oit et traite les �v�nements relatifs aux boutons
	 * Cette m�thode doit �tre publique mais ne devrait pas �tre appel�e directement.
	 * 
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 * 
	 * @param evt �v�nement d�clencheur
	 */
	@Override
	public void actionPerformed(ActionEvent evt)
	{
		if (evt.getActionCommand().equals("ADD"))
		{
			this.addUrl();
		}
		else if (evt.getActionCommand().equals("CANCEL"))
		{
			this.close();
		}
	}
	
	/** 
	 * M�thode appel�e quand la fen�tre va �tre ferm�e.
	 * Cette m�thode doit �tre publique mais ne devrait pas �tre appel�e directement.
	 * 
	 * @param evt �v�nement d�clencheur
	 */
	@Override
	public void windowClosing(WindowEvent evt)
	{
		this.close();
	}
	
	@Override
	public void windowActivated(WindowEvent evt)
	{
	}

	@Override
	public void windowClosed(WindowEvent evt)
	{
	}

	@Override
	public void windowDeactivated(WindowEvent evt)
	{
	}

	@Override
	public void windowDeiconified(WindowEvent evt)
	{
	}

	@Override
	public void windowIconified(WindowEvent evt)
	{
	}

	@Override
	public void windowOpened(WindowEvent evt)
	{
	}
}
