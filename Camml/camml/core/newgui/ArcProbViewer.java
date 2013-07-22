package camml.core.newgui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

public class ArcProbViewer extends JFrame {

	private static final long serialVersionUID = -2880609243503233424L;
	protected static final int defaultRowHeaderWidth = 100;	//How wide should the row header column be by default?
	protected static final int defaultMinColumnWidth = 20;	//Minimum width for the columns in the main table
	protected static final int defaultColumnWidth = 60;		//Default width ('preferred width') for the main table columns
	
	private final double[][] arcProb;						//Arc probabilities for intraslice arcs
	private final double[][] arcProbTemporal;				//Arc probabilities for interslice arcs
	protected String[] names;
	protected JTabbedPane tabPane;
	protected JPanel intrasliceArcPanel;
	protected JPanel intersliceArcPanel;
	protected JTable intrasliceArcProbTable;				//Table that displays the intraslice arc probability values
	protected JTable intersliceArcProbTable;				//Table that displays the interslice (temporal) arc probability values
	protected JTable intrasliceArcProbTableRowHeader;					//Table that is used as a row header
	protected JTable intersliceArcProbTableRowHeader;					//Table that is used as a row header
	
	/**Constructor for arc probability viewer for DBNs. Frame is tabbed, with intraslice and interslice arcs on separate tabs. 
	 * Note that input (arcProb and names arrays) are not checked for length, null reference etc!<br>
	 * Usage:<br>
	 * JFrame frame = new ArcProbViewer( intrasliceProbs, intersliceProbs, names ); <br>
	 * frame.setVisible(true);
	 * @param arcProb NxN array of intraslice arc probabilities (i.e. from MetropolisSearch.getArcPortions())
	 * @param arcProbTemporal NxN array of interslice arc probabilites (i.e. from MetropolisSearchDBN.getArcPortionsDBN())
	 * @param names Array of names for the variables.
	 */
	public ArcProbViewer( double[][] arcProb, double[][] arcProbTemporal, String[] names ){
		this.arcProb = arcProb;
		this.arcProbTemporal = arcProbTemporal;
		this.names = names;
		
		//Set the look and feel:
		try{
			UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
		} catch( Exception e ){
		}
		
		
		
		//Create a tabbed pane, and put two JPanels in it:
		tabPane = new JTabbedPane();
		intrasliceArcPanel = new JPanel();
		intersliceArcPanel = new JPanel();
		this.add( tabPane );
		
		tabPane.addTab("Intraslice", intrasliceArcPanel);
		tabPane.addTab("Interslice", intersliceArcPanel);
		intrasliceArcPanel.setLayout( new GridBagLayout() );
		intersliceArcPanel.setLayout( new GridBagLayout() );
		
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 0.5;
		gbc.weighty = 0.5;
		
		//Intraslice arc tab:
		intrasliceArcProbTable = new JTable( new ArcProbTableModel() );
		intrasliceArcProbTable.getTableHeader().setReorderingAllowed(false);
		intrasliceArcProbTable.setCellSelectionEnabled( true );
		intrasliceArcProbTable.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );
		
		for( int i=0; i<arcProb[0].length; i++ ){
			TableColumn col = intrasliceArcProbTable.getColumnModel().getColumn(i);
			col.setMinWidth( defaultMinColumnWidth );
			col.setPreferredWidth( defaultColumnWidth );
		}
		
		intrasliceArcProbTableRowHeader = new RowHeaderTable( new ArcProbRowHeaderModel() );
		
		JScrollPane sp = new JScrollPane( intrasliceArcProbTable );
		sp.setHorizontalScrollBarPolicy( JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED );
		sp.setRowHeaderView( intrasliceArcProbTableRowHeader );
		sp.setCorner( JScrollPane.UPPER_LEFT_CORNER, intrasliceArcProbTableRowHeader.getTableHeader() );
		intrasliceArcPanel.add(sp, gbc);
		
		
		//Interslice (temporal) arc tab:
		intersliceArcProbTable = new JTable( new ArcProbTableModelTemporal() );
		intersliceArcProbTable.getTableHeader().setReorderingAllowed(false);
		intersliceArcProbTable.setCellSelectionEnabled( true );
		intersliceArcProbTable.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );
		
		for( int i=0; i<arcProbTemporal[0].length; i++ ){
			TableColumn col = intersliceArcProbTable.getColumnModel().getColumn(i);
			col.setMinWidth( defaultMinColumnWidth );
			col.setPreferredWidth( defaultColumnWidth );
		}
		
		intersliceArcProbTableRowHeader = new RowHeaderTable( new ArcProbRowHeaderModel() );
		
		JScrollPane sp2 = new JScrollPane( intersliceArcProbTable );
		sp2.setHorizontalScrollBarPolicy( JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED );
		sp2.setRowHeaderView( intersliceArcProbTableRowHeader );
		sp2.setCorner( JScrollPane.UPPER_LEFT_CORNER, intersliceArcProbTableRowHeader.getTableHeader() );
		intersliceArcPanel.add(sp2, gbc);
		
	}


	/**Constructor for arc probability viewer for BNs. Frame is simple (no tabs) 
	 * Note that input (arcProb and names arrays) are not checked for length, null reference etc!<br>
	 * Usage:<br>
	 * JFrame frame = new ArcProbViewer( intrasliceProbs, intersliceProbs, names ); <br>
	 * frame.setVisible(true);
	 * @param arcProb NxN array of arc probabilities (i.e. from MetropolisSearch.getArcPortions())
	 * @param names Array of names for the variables.
	 */
	public ArcProbViewer( double[][] arcProb, String[] names ){
		//Set the look and feel:
		try{
			UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
		} catch( Exception e ){
		}
		
		this.arcProb = arcProb;		this.arcProbTemporal = null;
		this.names = names;
		
		intrasliceArcProbTable = new JTable( new ArcProbTableModel() );
		intrasliceArcProbTable.getTableHeader().setReorderingAllowed(false);
		intrasliceArcProbTable.setCellSelectionEnabled( true );
		intrasliceArcProbTable.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );
		
		for( int i=0; i<arcProb[0].length; i++ ){
			TableColumn col = intrasliceArcProbTable.getColumnModel().getColumn(i);
			col.setMinWidth( defaultMinColumnWidth );
			col.setPreferredWidth( defaultColumnWidth );
		}
		
		intrasliceArcProbTableRowHeader = new RowHeaderTable( new ArcProbRowHeaderModel() );
		
		JScrollPane sp = new JScrollPane( intrasliceArcProbTable );
		sp.setHorizontalScrollBarPolicy( JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED );
		sp.setRowHeaderView( intrasliceArcProbTableRowHeader );
		sp.setCorner( JScrollPane.UPPER_LEFT_CORNER, intrasliceArcProbTableRowHeader.getTableHeader() );
		add( sp, BorderLayout.CENTER );
		
	}
	
	
	/**Table model for the table displaying the arc probabilities.
	 * Usage: myTable = new JTable( new ArcProbTableModel() );
	 */
	public class ArcProbTableModel extends AbstractTableModel{
		private static final long serialVersionUID = -6850890843769819154L;

		public int getColumnCount() {
			return arcProb[0].length;
		}

		public int getRowCount() {
			return arcProb[0].length;
		}

		//Note: arcProb[i][j] = P( j -> i ); If we want P( a -> b ), then this is arcProb[b][a]
		public Object getValueAt(int row, int col) {
			double val = arcProb[col][row];
			if( val == 0.0 ) return "0";
			if( val == 1.0 ) return "1";
			if( val >= 0.1 ) return GUIParameters.formatArcProbDecimal.format( val );	//between 0.1 and 1.0 in decimal format 
			return GUIParameters.formatArcProbSci.format( val );	//Values < 0.1 in scientific format
		}
		
		public String getColumnName( int col ){
			return names[ col ];
		}
	}
	
	/**Table model for the table displaying INTERSLICE (temporal) arc probabilities. */
	public class ArcProbTableModelTemporal extends ArcProbTableModel{
		private static final long serialVersionUID = -1090842372867243500L;

		public Object getValueAt( int row, int col ){
			double val = arcProbTemporal[col][row];
			if( val == 0.0 ) return "0";
			if( val == 1.0 ) return "1";
			if( val >= 0.1 ) return GUIParameters.formatArcProbDecimal.format( val );	//between 0.1 and 1.0 in decimal format 
			return GUIParameters.formatArcProbSci.format( val );	//Values < 0.1 in scientific format
		}
	}
	
	
	/**TableModel to be used with the RowHeaderTable class.
	 */
	public class ArcProbRowHeaderModel extends AbstractTableModel {
		private static final long serialVersionUID = 2451437277578402486L;

		public int getColumnCount() {
			return 1;
		}

		public int getRowCount() {
			return names.length;
		}

		public Object getValueAt(int row, int col) {
			return names[row];
		}
		
		public String getColumnName( int col ){
			return "";
		}
	}
	
	/**Class for displaying the table header. Is a modified JTable.
	 * Usage: myRowHeader = new RowHeaderTable( new ArcProbRowHeaderModel() )
	 * 
	 */
	public class RowHeaderTable extends JTable{
		private static final long serialVersionUID = 3539844182494634830L;

		public RowHeaderTable( TableModel tm ){
			super( tm );
			
			setFocusable( false );
			
			TableColumn col = this.getColumnModel().getColumn(0);
			col.setPreferredWidth( defaultRowHeaderWidth );
			col.setCellRenderer( new RowHeaderRenderer() );
			setPreferredScrollableViewportSize( getPreferredSize() );
			
			this.getTableHeader().setReorderingAllowed(false);
			
		}
		
	}
	
	/**Basic class used for rendering the row header cells - so that the header actually looks like a table
	 * header, and not like normal cells... Note: Minimal/no input checking.
	 */
	protected static class RowHeaderRenderer extends DefaultTableCellRenderer{
		private static final long serialVersionUID = -26862994347781869L;

		public Component getTableCellRendererComponent( JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column ){
			JTableHeader h = table.getTableHeader();
			
			setForeground( h.getForeground() );
			setBackground( h.getBackground() );
			setFont( h.getFont() );
			
			if( value == null ){
				setText( "" );
			} else {
				setText( value.toString() );
			}
			
			return this;
		}
	}
	
	
	public static void main( String[] args ){
		
		double[][] data = new double[][]{ {0.0, 0.1, 0.2, 0.3, 0.4}, {1.0, 1.1, 1.2, 1.3, 1.4}, {2.0, 2.1, 2.2, 2.3, 2.4}, {3.0, 3.1, 3.2, 3.3, 3.4}, {4.0, 4.1, 4.2, 4.3, 4.4} };
		
		String[] names = new String[]{ "Zero", "One", "Two", "Three", "Four" };
		
		JFrame frame = new ArcProbViewer( data, data, names );
		
		frame.setSize(640,480);
		frame.setTitle( "Title - arc viewer" );
		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
		frame.setVisible(true);
	}
}
