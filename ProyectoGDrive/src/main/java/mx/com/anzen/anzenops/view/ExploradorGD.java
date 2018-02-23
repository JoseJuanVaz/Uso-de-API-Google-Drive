package mx.com.anzen.anzenops.view;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeSelectionModel;

import com.google.api.services.drive.model.File;

import mx.com.anzen.anzenops.control.APIGoogleDrive;

public class ExploradorGD extends JPanel implements TreeSelectionListener {

	private static final long serialVersionUID = -8951676851450604091L;
	private JPanel panelDetalles;
	private JTree tree;
	private static boolean playWithLineStyle = false;
	private static String lineStyle = "Horizontal";

	/**
	 * Inicializamos componentes
	 */
	public ExploradorGD() {
		super(new GridLayout(1, 0));

		// Inicializando el Arbol de Carpetas
		DefaultMutableTreeNode top = new DefaultMutableTreeNode("Archivos en el Drive Anzen");

		// Create a tree that allows one selection at a time.
		tree = new JTree(top);
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

		// Listen for when the selection changes.
		tree.addTreeSelectionListener(this);

		if (playWithLineStyle) {
			System.out.println("line style = " + lineStyle);
			tree.putClientProperty("JTree.lineStyle", lineStyle);
		}

		// Create the scroll pane and add the tree to it.
		JScrollPane treeView = new JScrollPane(tree);

		// Creando el panel para listar archivos.
		panelDetalles = new JPanel();
		panelDetalles.setLayout(new GridLayout(0, 1));
		JScrollPane panelVista = new JScrollPane(panelDetalles);

		// Add the scroll panes to a split pane.
		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		splitPane.setTopComponent(treeView);
		splitPane.setBottomComponent(panelVista);

		Dimension minimumSize = new Dimension(100, 50);
		panelVista.setMinimumSize(minimumSize);
		treeView.setMinimumSize(minimumSize);
		splitPane.setDividerLocation(100);
		splitPane.setPreferredSize(new Dimension(500, 300));

		// Add the split pane to this panel.
		add(splitPane);

		inicializaNodoRaiz(top);
	}

	/**
	 * Inicializa la vista
	 * 
	 * @param top
	 */
	private void inicializaNodoRaiz(DefaultMutableTreeNode top) {
		List<DefaultMutableTreeNode> lstNodosNivelAnterior = new ArrayList<>();
		List<DefaultMutableTreeNode> lstNodosNivelNuevo = null;
		lstNodosNivelAnterior.add(top);
		int contadorNivel = 0;

		// Generando Arbol
		try {

			do {
				lstNodosNivelNuevo = new ArrayList<>();

				for (DefaultMutableTreeNode nodoAnterior : lstNodosNivelAnterior) {
					top = nodoAnterior;

					List<File> lstCarpetas = null;
					if (contadorNivel == 0) {
						lstCarpetas = APIGoogleDrive.consultaCarpetas(null);
					} else {
						File carpeta = (File) nodoAnterior.getUserObject();
						lstCarpetas = APIGoogleDrive.consultaCarpetas(carpeta.getId());
					}

					if (lstCarpetas != null && lstCarpetas.size() != 0) {
						// Agregando nuevos nodos al nivel
						for (File carpeta : lstCarpetas) {
							DefaultMutableTreeNode nodo = new DefaultMutableTreeNode(carpeta);
							lstNodosNivelNuevo.add(nodo);
							top.add(nodo);
						}
					}
				}

				lstNodosNivelAnterior = lstNodosNivelNuevo;
				contadorNivel++;
			} while (lstNodosNivelNuevo != null && lstNodosNivelNuevo.size() != 0);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Implementacion de listener
	 */
	@Override
	public void valueChanged(TreeSelectionEvent e) {
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();

		if (node == null) {
			return;
		}

		List<File> archivos = null;
		if (node.getUserObject() instanceof File) {
			File folder = (File) node.getUserObject();
			try {
				archivos = APIGoogleDrive.consultaArchivos(folder.getId());
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		} else {
			try {
				archivos = APIGoogleDrive.consultaArchivos(null);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}

		panelDetalles.removeAll();
		// Agregando los archivos encontrados
		if (archivos != null && archivos.size() > 0) {

			for (File file : archivos) {
				JTextField nombre = new JTextField(file.toString());
				panelDetalles.add(nombre);
			}
		}

		// renderiza el panel
		panelDetalles.validate();
	}

	/**
	 * Create the GUI and show it. For thread safety, this method should be
	 * invoked from the event dispatch thread.
	 */
	private static void createAndShowGUI() {

		// Create and set up the window.
		final JFrame frame = new JFrame("Consulta Archivos Google Drive Anzen");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		// Add content to the window.
		frame.add(new ExploradorGD());

		// Agregando el Menu
		JMenuBar menuBar;
		JMenu menu;

		// Create the menu bar.
		menuBar = new JMenuBar();

		// Build the first menu.
		menu = new JMenu("Casos de Uso");
		menuBar.add(menu);
		frame.setJMenuBar(menuBar);

		JMenuItem menuItem = new JMenuItem("Cargar un archivo a una carpeta en especifico");
		menu.add(menuItem);
		menuItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// Necesitamos el Id de la carpeta
				
				String respuesta = JOptionPane.showInputDialog(frame,
						"Escribe el id de la carpeta donde se va a subir el archivo");

				if (respuesta != null) {
					try {
						APIGoogleDrive.subirArchivo(respuesta);
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
			}
		});

		JMenuItem menuItem1 = new JMenuItem("Crear carpeta con el nombre de cada PO o con el RFC del empleado.");
		menu.add(menuItem1);
		menuItem1.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// Necesitamos el id de la carpeta donde va a crearse
				String respuesta = JOptionPane.showInputDialog(frame,
						"Escribe el id de la carpeta donde se va a crear");

				if (respuesta != null) {
					try {
						APIGoogleDrive.crearCarpeta(respuesta, "CarpetaCreadaJ");
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
			}
		});

		JMenuItem menuItem2 = new JMenuItem(
				"Realizar la descarga de un archivo que esta dentro de una carpeta en especifico");
		menu.add(menuItem2);
		menuItem2.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// Agregar Identificador del archivo
				String respuesta = JOptionPane.showInputDialog(frame,
						"Escribe el id del archivo a descargar");

				if (respuesta != null) {
					try {
						System.out.println("Realizando la descarga del ID: "+respuesta);
						APIGoogleDrive.descargarArchivo(respuesta);
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
			}
		});

		JMenuItem menuItem3 = new JMenuItem("Realizar la descarga .zip de una carpeta completa");
		menu.add(menuItem3);
		menuItem3.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// Agregar el id de la carpeta que se requiere desacargar
				String respuesta = JOptionPane.showInputDialog(frame,
						"Escribe el id de la carpeta a descargar");

				if (respuesta != null) {

				}
			}
		});

		frame.pack();
		frame.setVisible(true);
	}

	/**
	 * Inicializando la app
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		// Schedule a job for the event dispatch thread:
		// creating and showing this application's GUI.
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				createAndShowGUI();
			}
		});
	}
}
