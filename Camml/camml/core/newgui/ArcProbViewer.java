package camml.core.newgui;

import java.awt.BorderLayout;
import java.awt.Component;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

/**Simple class for viewing the arc probabilities calculated in CaMML
 * Note: At this point, only arc probabilities from MetropolisSearch can be displayed - 
 * i.e. displaying of DBN arc probabilities is not yet supported. 
 * @author Alex Black
 */
public class ArcProbViewer extends JFrame {
	private static final long serialVersionUID = 3415167685453273168L;
	
	protected static final int defaultRowHeaderWidth = 80;	//How wide should the row header column be by default?
	protected static final int defaultMinColumnWidth = 20;	//Minimum width for the columns in the main table
	protected static final int defaultColumnWidth = 60;		//Default width ('preferred width') for the main table columns
	
	private final double[][] arcProb;
	protected String[] names;
	protected JTable arcProbTable;				//Table that displays the arc probability values
	protected JTable arcProbTableRowHeader;		//Table that is used as a row header
	
	/**Constructor for arc probability viewer. 
	 * Note that input (arcProb and names arrays) are not checked for length, null reference etc!<br>
	 * Usage:<br>
	 * JFrame frame = new ArcProbViewer( data, names ); <br>
	 * frame.setVisible(true);
	 * @param arcProb NxN array of arc probabilities (i.e. from MetropolisSearch.getArcPortions())
	 * @param names Array of names for the variables.
	 */
	public ArcProbViewer( double[][] arcProb, String[] names ){
		this.arcProb = arcProb;
		this.names = names;
		
		arcProbTable = new JTable( new ArcProbTableModel() );
		arcProbTable.getTableHeader().setReorderingAllowed(false);
		arcProbTable.setCellSelectionEnabled( true );
		arcProbTable.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );
		
		for( int i=0; i<arcProb[0].length; i++ ){
			TableColumn col = arcProbTable.getColumnModel().getColumn(i);
			col.setMinWidth( defaultMinColumnWidth );
			col.setPreferredWidth( defaultColumnWidth );
		}
		
		arcProbTableRowHeader = new RowHeaderTable( new ArcProbRowHeaderModel() );
		
		JScrollPane sp = new JScrollPane( arcProbTable );
		sp.setHorizontalScrollBarPolicy( JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED );
		sp.setRowHeaderView( arcProbTableRowHeader );
		sp.setCorner( JScrollPane.UPPER_LEFT_CORNER, arcProbTableRowHeader.getTableHeader() );
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
		
		JFrame frame = new ArcProbViewer( data, names );
		
		frame.setSize(640,480);
		frame.setTitle( "Title - arc viewer" );
		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
		frame.setVisible(true);
	}

}
