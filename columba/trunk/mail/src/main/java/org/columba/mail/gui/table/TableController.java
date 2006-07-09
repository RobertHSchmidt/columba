// The contents of this file are subject to the Mozilla Public License Version
// 1.1
//(the "License"); you may not use this file except in compliance with the
//License. You may obtain a copy of the License at http://www.mozilla.org/MPL/
//
//Software distributed under the License is distributed on an "AS IS" basis,
//WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
//for the specific language governing rights and
//limitations under the License.
//
//The Original Code is "The Columba Project"
//
//The Initial Developers of the Original Code are Frederik Dietz and Timo
// Stich.
//Portions created by Frederik Dietz and Timo Stich are Copyright (C) 2003.
//
//All Rights Reserved.
package org.columba.mail.gui.table;

import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Observable;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.EventListenerList;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.tree.TreePath;

import org.columba.api.gui.frame.IFrameMediator;
import org.columba.core.folder.IFolder;
import org.columba.core.folder.IFolderCommandReference;
import org.columba.core.gui.base.AscendingIcon;
import org.columba.core.gui.base.DescendingIcon;
import org.columba.core.gui.menu.ExtendablePopupMenu;
import org.columba.core.gui.menu.MenuXMLDecoder;
import org.columba.core.io.DiskIO;
import org.columba.mail.folder.IMailFolder;
import org.columba.mail.folder.IMailbox;
import org.columba.mail.folder.event.FolderEventDelegator;
import org.columba.mail.folderoptions.FolderOptionsController;
import org.columba.mail.gui.frame.MailFrameMediator;
import org.columba.mail.gui.table.model.HeaderTableModel;
import org.columba.mail.gui.table.model.MessageNode;
import org.columba.mail.gui.table.model.TableModelChangedEvent;
import org.columba.mail.gui.table.model.TableModelChangedListener;
import org.columba.mail.gui.table.model.TableModelSorter;
import org.columba.mail.gui.table.model.TableModelThreadedView;
import org.columba.mail.message.IColumbaHeader;
import org.columba.mail.message.IHeaderList;
import org.columba.mail.util.MailResourceLoader;
import org.frapuccino.treetable.Tree;

/**
 * Shows the message list. By default, this is the read/unread state if a
 * message, Subject:, Date:, From: and Size headerfields.
 * <p>
 * Folder-specific configuration options are handled by
 * <code>FolderOptionsController</code> and can be configured by the user in the
 * AbstractMessageFolder Options Dialog.
 * 
 * @author fdietz
 */
public class TableController implements ListSelectionListener,
		TableModelChangedListener, ITableController {

	/** JDK 1.4+ logging framework logger, used for logging. */
	private static final Logger LOG = Logger
			.getLogger("org.columba.mail.gui.table");

	/**
	 * table model
	 */
	private HeaderTableModel headerTableModel;

	/**
	 * table view
	 */
	protected TableView view;

	/**
	 * reference to mail framemediator
	 */
	protected IFrameMediator frameController;

	/**
	 * table view context menu
	 */
	protected ExtendablePopupMenu menu;

	/**
	 * sorting model
	 */
	protected TableModelSorter tableModelSorter;

	/**
	 * threaded-view model
	 */
	protected TableModelThreadedView tableModelThreadedView;

	/**
	 * previously selected rows
	 */
	protected int[] previouslySelectedRows;

	private MessageNode[] previouslySelectedNodes;

	/**
	 * previously selected folder
	 */
	private IMailbox previouslySelectedFolder;

	/**
	 * tooltip mouse handler
	 */
	private ColumnHeaderTooltips tips;

	
	protected EventListenerList listenerList = new EventListenerList();
	
	/**
	 * Constructor
	 * 
	 * @param mailFrameController
	 *            mail framemediator
	 */
	public TableController(IFrameMediator frameController) {
		this.frameController = frameController;

		// init table model
		headerTableModel = new HeaderTableModel();

		// init threaded-view model
		tableModelThreadedView = new TableModelThreadedView();
		headerTableModel.registerVisitor(tableModelThreadedView);

		// init sorting model
		tableModelSorter = new TableModelSorter();
		headerTableModel.registerVisitor(tableModelSorter);

		// init view
		view = new TableView(headerTableModel, tableModelSorter);

		// pass tree to model, used by the threaded-view
		headerTableModel.setTree((Tree) view.getTree());

		getView().setTransferHandler(
				new TableViewTransferHandler(getFrameController()));
		getView().setDragEnabled(true);

		// MouseListener sorts table when clicking on a column header
		new TableHeaderMouseListener(getTableModelSorter());
		view.getColumnModel().addColumnModelListener(headerTableModel);

		// we need this for the focus manager
		getView().getSelectionModel().addListSelectionListener(this);

		JTableHeader header = view.getTableHeader();

		tips = new ColumnHeaderTooltips();
		header.addMouseMotionListener(tips);

		// register interest on folder events
		FolderEventDelegator.getInstance().addTableListener(this);

	}

	/**
	 * Assigns a tooltip for each column
	 * <p>
	 * Tooltips for columns can be found in
	 * org.columba.mail.i18n.header.header.properties.
	 * 
	 * @see org.columba.mail.folderoptions.ColumnOptionsPlugin
	 * 
	 */
	public void initTooltips() {
		tips.clear();

		// Assign a tooltip for each of the columns
		for (int c = 0; c < view.getColumnCount(); c++) {
			TableColumn col = view.getColumnModel().getColumn(c);

			// column IDs are all lower case
			String lookup = ((String) col.getIdentifier()).toLowerCase();

			// append "_tooltip"
			lookup = lookup + "_tooltip";

			// get translation
			String s = MailResourceLoader.getString("header", "header", lookup);

			tips.setToolTip(col, s);
		}
	}

	/**
	 * Get view of table controller
	 * 
	 * @return table view
	 */
	public TableView getView() {
		return view;
	}

	/**
	 * Get table model
	 * 
	 * @return table model
	 */
	public IHeaderTableModel getHeaderTableModel() {
		return headerTableModel;
	}

	/**
	 * Select messages with UIDs.
	 * <p>
	 * Message UIDs are converted to {@link MessageNode}objects.
	 * 
	 * @param uids
	 *            array of message UIDs
	 */
	public void setSelected(Object[] uids) {

		// select nodes
		MessageNode[] nodes = new MessageNode[uids.length];

		for (int i = 0; i < uids.length; i++) {
			nodes[i] = ((HeaderTableModel) getHeaderTableModel())
					.getMessageNode(uids[i]);
		}

		int[] rows = new int[nodes.length];
		for (int i = 0; i < nodes.length; i++) {
			TreePath path = new TreePath(nodes[i].getPath());
			rows[i] = view.getTree().getRowForPath(path);

		}
		view.selectRow(rows[0]);

	}

	/** ************************ actions ******************************* */
	/**
	 * create the PopupMenu
	 */
	public void createPopupMenu() {
		try {
			InputStream is = DiskIO
					.getResourceStream("org/columba/mail/action/table_contextmenu.xml");

			menu = new MenuXMLDecoder(getFrameController()).createPopupMenu(is);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Get popup menu
	 * 
	 * @return popup menu
	 */
	public JPopupMenu getPopupMenu() {
		return menu;
	}

	/**
	 * Method is called if folder data changed.
	 * <p>
	 * It is responsible for updating the correct underlying model.
	 * 
	 * @param event
	 *            update event
	 */
	public void tableChanged(TableModelChangedEvent event) {

		// selected rows before updating the model
		// -> used later to restore the selection
		previouslySelectedRows = view.getSelectedRows();

		// folder in which the update occurs
		IFolder folder = event.getSrcFolder();

		if (folder == null) {
			return;
		}

		LOG.info("source folder=" + folder.getName());

		// get current selection
		IFolderCommandReference r = (IFolderCommandReference) ((MailFrameMediator) frameController)
				.getTableSelection();
		IFolder srcFolder = r.getSourceFolder();

		// its always possible that no folder is currenlty selected
		if (srcFolder != null) {
			LOG.info("selected folder=" + srcFolder.getName());
		}

		// update infopanel (gray panel below the toolbar)
		// showing total/unread/recent messages count
		/*
		 * if (getFrameController() instanceof MailFrameMediator) { if
		 * (srcFolder != null) { ((ThreePaneMailFrameController)
		 * getFrameController()) .getFolderInfoPanel().setFolder((IMailFolder)
		 * srcFolder); } }
		 */

		// only update table if, this folder is the same
		// as the currently selected
		if (!folder.equals(srcFolder)) {
			return;
		}

		switch (event.getEventType()) {
		case -1: {
			getHeaderTableModel().set(event.getHeaderList());

			// FIXME threaded-view auto collapse
			/*
			 * if (getTableModelThreadedView().isEnabled()) { // expand all
			 * unread message nodes for (int i = 0; i < getView().getRowCount();
			 * i++) { System.out.println("i=" + i + " count=" +
			 * getView().getRowCount());
			 * 
			 * TreePath path = getView().getTree().getPathForRow(i); MessageNode
			 * node = (MessageNode) path .getLastPathComponent(); IColumbaHeader
			 * h = node.getHeader(); boolean unseen = !h.getFlags().getSeen();
			 * if (unseen) { getView().getTree().expandPath(path); } } }
			 */
			break;
		}

		case TableModelChangedEvent.UPDATE: {
			getHeaderTableModel().update();

			break;
		}

		case TableModelChangedEvent.REMOVE: {
			getHeaderTableModel().remove(event.getUids());

			break;
		}

		case TableModelChangedEvent.MARK: {
			getHeaderTableModel().modify(event.getUids());

			break;
		}
		}

		// when marking messages, don't touch selection
		if (event.getEventType() == TableModelChangedEvent.MARK)
			return;

		// re-select previous selection
		if (previouslySelectedRows != null) {
			// only re-select if only a single row was formerly selected
			if ((previouslySelectedRows.length == 1)
					&& (previouslySelectedNodes.length > 0)) {
				int row = ((HeaderTableModel) getHeaderTableModel())
						.getRow(previouslySelectedNodes[0]);

				// if message was removed from JTable
				if (row == -1)
					row = previouslySelectedRows[0];

				// select row
				view.selectRow(row);

				// scrolling to the selected row
				getView().makeRowVisible(row);

			}
		}

	}

	/**
	 * Returns the mailFrameController.
	 * 
	 * @return MailFrameController
	 */
	public IFrameMediator getFrameController() {
		return frameController;
	}

	/**
	 * Show the headerlist of currently selected folder.
	 * <p>
	 * Additionally, implements folderoptions plugin entrypoint.
	 * 
	 * @see org.columba.mail.folder.folderoptions
	 * @see org.columba.mail.gui.frame.ViewHeaderListInterface#showHeaderList(org.columba.mail.folder.Folder,
	 *      org.columba.mail.message.HeaderList)
	 */
	public void showHeaderList(IMailbox folder, IHeaderList headerList)
			throws Exception {

		// save previously selected folder options
		if (previouslySelectedFolder != null) {
			((MailFrameMediator) getFrameController())
					.getFolderOptionsController()
					.save(previouslySelectedFolder);
		}

		// load options of newly selected folder
		((MailFrameMediator) getFrameController()).getFolderOptionsController()
				.load(folder, FolderOptionsController.STATE_BEFORE);

		getHeaderTableModel().set(headerList);

		// load options of newly selected folder
		((MailFrameMediator) getFrameController()).getFolderOptionsController()
				.load(folder, FolderOptionsController.STATE_AFTER);

		// remember previously selected folder
		previouslySelectedFolder = folder;
	}

	/**
	 * Show empty messagelist with no elements.
	 * 
	 */
	public void clear() {
		// clear model
		getHeaderTableModel().set(null);

	}

	/** *********** implement getter/setter methods ******************** */
	/**
	 * return the table model sorter
	 */
	public TableModelSorter getTableModelSorter() {
		return tableModelSorter;
	}

	/**
	 * return the threaded view model
	 */
	public TableModelThreadedView getTableModelThreadedView() {
		return tableModelThreadedView;
	}

	/** ************************* ListSelectionListener interface ************* */

	/**
	 * @see javax.swing.event.ListSelectionListener#valueChanged(javax.swing.event.ListSelectionEvent)
	 */
	public void valueChanged(ListSelectionEvent arg0) {

		// enable/disable cut/copy/paste/selectall actions
		// FocusManager.getInstance().updateActions();

		// if user is currently changing selection, don't do anything
		// -> wait until the final selection is available
		if (arg0.getValueIsAdjusting())
			return;

		/*
		 * // @author fdietz // bug #983931, message jumps while downloading new
		 * messages if (getView().getSelectedNodes().length == 0) { // skip if
		 * no message selected
		 * 
		 * if (getView().getRowCount() > 0) // if folder contains messages // ->
		 * skip to fix above bug return; }
		 */

		// rememember selected nodes
		previouslySelectedNodes = getView().getSelectedNodes();

		List<String> v = new Vector<String>();
		for ( int i=0; i<previouslySelectedNodes.length; i++) {
			v.add(previouslySelectedNodes[i].getUid().toString());
		}
		
		fireMessageListSelectionChangedEvent(v);
		
		
	}

	/**
	 * @see org.columba.mail.gui.table.model.TableModelChangedListener#isInterestedIn(IFolder)
	 */
	public boolean isInterestedIn(IMailFolder folder) {

		return folder == previouslySelectedFolder;
	}

	/**
	 * @see org.columba.mail.gui.table.ITableController#getSelectedNodes()
	 */
	public IMessageNode[] getSelectedNodes() {
		return getView().getSelectedNodes();
	}

	/**
	 * @see org.columba.mail.gui.table.ITableController#selectFirstRow()
	 */
	public Object selectFirstRow() {
		Object result = getView().selectFirstRow();

		return result;
	}

	/**
	 * @see org.columba.mail.gui.table.ITableController#selectLastRow()
	 */
	public Object selectLastRow() {
		if (getView().getColumnCount() > 0) {
			Object result = getView().selectLastRow();
			return result;
		} else {
			return null;
		}

	}

	/**
	 * @see org.columba.mail.gui.table.ITableController#selectRow(int)
	 */
	public void selectRow(int row) {
		getView().selectRow(row);

	}

	/**
	 * @see org.columba.mail.gui.table.ITableController#getMessagNode(java.lang.Object)
	 */
	public IMessageNode getMessageNode(Object uid) {
		return getView().getMessagNode(uid);
	}

	/**
	 * @see org.columba.mail.gui.table.ITableController#enableThreadedView(boolean)
	 */
	public void enableThreadedView(boolean enableThreadedMode,
			boolean updateModel) {
		getTableModelThreadedView().setEnabled(enableThreadedMode);
		getHeaderTableModel().enableThreadedView(enableThreadedMode);
		getView().enableThreadedView(enableThreadedMode);

		if (updateModel)
			getHeaderTableModel().update();

		if (enableThreadedMode) {
			// expand all unread message nodes
			for (int i = 0; i < getView().getRowCount(); i++) {
				LOG.info("i=" + i + " count=" //$NON-NLS-1$ //$NON-NLS-2$
						+ getView().getRowCount());

				TreePath path = getView().getTree().getPathForRow(i);
				MessageNode node = (MessageNode) path.getLastPathComponent();
				IColumbaHeader h = node.getHeader();
				boolean unseen = !h.getFlags().getSeen();
				if (unseen) {
					getView().getTree().expandPath(path);
				}
			}
		}
	}

	/**
	 * @see org.columba.mail.gui.table.ITableController#clearSelection()
	 */
	public void clearSelection() {
		getView().getSelectionModel().clearSelection();
	}

	/**
	 * @see org.columba.mail.gui.table.ITableController#makeSelectedRow
	 */
	public void makeSelectedRowVisible() {
		getView().scrollRectToVisible(
				getView().getCellRect(getView().getSelectedRow(), 0, false));
	}

	/**
	 * @see org.columba.mail.gui.table.ITableController#getSortingStateObservable()
	 */
	public Observable getSortingStateObservable() {
		return getTableModelSorter().getSortingStateObservable();
	}

	/**
	 * @see org.columba.mail.gui.table.ITableController#setSortingOrder(boolean)
	 */
	public void setSortingOrder(boolean order) {
		getTableModelSorter().setSortingOrder(order);
	}

	/**
	 * @see org.columba.mail.gui.table.ITableController#setSortingColumn(java.lang.String)
	 */
	public void setSortingColumn(String column) {
		getTableModelSorter().setSortingColumn(column);
	}

	/**
	 * @see org.columba.mail.gui.table.ITableController#getSortingColumn()
	 */
	public String getSortingColumn() {
		return getTableModelSorter().getSortingColumn();
	}

	/**
	 * @see org.columba.mail.gui.table.ITableController#getSortingOrder()
	 */
	public boolean getSortingOrder() {
		return getTableModelSorter().getSortingOrder();
	}

	/**
	 * @see org.columba.mail.gui.table.ITableController#isThreadedViewEnabled()
	 */
	public boolean isThreadedViewEnabled() {
		return getTableModelThreadedView().isEnabled();
	}

	/**
	 * @see org.columba.mail.gui.table.ITableController#getRowCount()
	 */
	public int getRowCount() {
		return getView().getRowCount();
	}

	/**
	 * @see org.columba.mail.gui.table.ITableController#getColumnModel()
	 */
	public TableColumnModel getColumnModel() {
		return view.getColumnModel();
	}

	/**
	 * @see org.columba.mail.gui.table.ITableController#resetColumnModel()
	 */
	public void resetColumnModel() {
		TableView view = getView();

		// remove all columns from table model
		getHeaderTableModel().clearColumns();

		// reset row height
		view.resetRowHeight();
		view.setShowHorizontalLines(false);

		// remove all columns for column model
		view.getColumnModel().removeColumnModelListener(getHeaderTableModel());
		view.setColumnModel(new DefaultTableColumnModel());
		view.getColumnModel().addColumnModelListener(getHeaderTableModel());

		// for some weird reason the table loses its inter-cell spacing
		// property, when changing the underlying column model
		// -> setting this to (0,0) again
		view.setIntercellSpacing(new Dimension(0, 0));

		// if new columns were added, we have to initialize the tooltips
		initTooltips();
	}

	/**
	 * @see org.columba.mail.gui.table.ITableController#createColumn(java.lang.String, int)
	 */
	public TableColumn createTableColumn(String name, int size) {
		return view.createTableColumn(name, size);
	}

	/**
	 * @see org.columba.mail.gui.table.ITableController#addColumn(javax.swing.table.TableColumn)
	 */
	public void addColumn(TableColumn column) {
		view.addColumn(column);
	}

	
	/**
	 * Mouse listener for selecting columns with the left mouse to change the
	 * sorting order.
	 * <p>
	 * Also responsible for changing the icon in the renderer
	 * 
	 * @author fdietz
	 */
	class TableHeaderMouseListener extends MouseAdapter {
		
		private TableModelSorter sorter;

		private SortingStateObservable observable;

		private ImageIcon ascending = new AscendingIcon();

		private ImageIcon descending = new DescendingIcon();

		/**
		 *  
		 */
		public TableHeaderMouseListener(
				TableModelSorter sorter) {
	
			
			this.sorter = sorter;

			this.observable = sorter.getSortingStateObservable();

			JTableHeader th = view.getTableHeader();
			th.addMouseListener(this);
		}

		public void mouseClicked(MouseEvent e) {
			TableColumnModel columnModel = view.getColumnModel();
			int viewColumn = columnModel.getColumnIndexAtX(e.getX());
			int column = viewColumn;

			//int column = view.convertColumnIndexToModel(viewColumn);
			//int column2 = view.convertColumnIndexToView(viewColumn);
			if (column != -1) {
				ImageIcon icon = null;

				if (sorter.getSortingOrder() == true) {
					icon = ascending;
				} else {
					icon = descending;
				}

				// disable every icon
				// -> set appropriate icon for selected column
				for (int i = 0; i < columnModel.getColumnCount(); i++) {
					JLabel renderer = (JLabel) columnModel.getColumn(i)
							.getHeaderRenderer();

					if (i == column) {
						renderer.setIcon(icon);
					} else {
						renderer.setIcon(null);
					}
				}

				// remember selected node
				MessageNode[] nodes = view.getSelectedNodes();
				Object uid = null;

				if ((nodes != null) && (nodes.length > 0))
					uid = nodes[0].getUid();

				// repaint table header
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						view.getTableHeader().repaint();
					}
				});

				String columnName = getHeaderTableModel().getColumnName(
						column);

				// notify the model to sort the table
				//sorter.sort(column);
				boolean order = false;
				
				if (sorter.getSortingColumn().equals(columnName)) {
					order = !sorter.getSortingOrder();
				}
				// notify observers (sorting state submenu)
				observable.setSortingState(columnName, order);

				setSortingColumn(columnName);
				setSortingOrder(order);
				getHeaderTableModel().update();
				
				// make selected row visible again
				if (uid != null)
					setSelected(new Object[] { uid });

			}
		}
	}


	public ListSelectionModel getListSelectionModel() {
		return view.getSelectionModel();
	}

	/**
	 * @see org.columba.mail.gui.table.ITableController#getSelectedRows()
	 */
	public int[] getSelectedRows() {
		return view.getSelectedRows();
	}

	/**
	 * @see org.columba.mail.gui.table.ITableController#getPathForRow(int)
	 */
	public TreePath getPathForRow(int row) {
		return view.getTree().getPathForRow(row);
	}
	
	
	public void addMessageListSelectionListener(IMessageListSelectionListener l) {
		listenerList.add(IMessageListSelectionListener.class, l);
	}

	public void removeMessageListSelectionListener(IMessageListSelectionListener l) {
		listenerList.remove(IMessageListSelectionListener.class, l);
	}
	
	protected void fireMessageListSelectionChangedEvent(List<String> messageIds) {

		IMessageListSelectionEvent e = new MessageListSelectionEvent(this, messageIds);
		// Guaranteed to return a non-null array
		Object[] listeners = listenerList.getListenerList();

		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == IMessageListSelectionListener.class) {
				((IMessageListSelectionListener) listeners[i + 1]).selectionChanged(e);
			}
		}
	}

	
	
}