//The contents of this file are subject to the Mozilla Public License Version 1.1
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
//The Initial Developers of the Original Code are Frederik Dietz and Timo Stich.
//Portions created by Frederik Dietz and Timo Stich are Copyright (C) 2003. 
//
//All Rights Reserved.
package org.columba.mail.folderoptions;

import java.util.Enumeration;

import javax.swing.table.TableColumn;

import org.columba.core.config.DefaultItem;
import org.columba.core.config.IDefaultItem;
import org.columba.core.xml.XmlElement;
import org.columba.mail.folder.IMailbox;
import org.columba.mail.gui.frame.MailFrameMediator;
import org.columba.mail.gui.frame.TableViewOwner;
import org.columba.mail.gui.table.ITableController;

/**
 * Stores all visible columns of the message list.
 * 
 * @author fdietz
 */
public class ColumnOptionsPlugin extends AbstractFolderOptionsPlugin {
	public final static String[] COLUMNS = { "Status", "Attachment", "Flagged",
			"Priority", "Subject", "From", "Date", "Size", "Spam", "To", "Cc", "MultiLine" };

	/**
	 * Constructor
	 * 
	 * @param mediator
	 *            mail frame mediator
	 */
	public ColumnOptionsPlugin(MailFrameMediator mediator) {
		super("columns", "ColumnOptions", mediator);
	}

	/**
	 * Get list of available columns.
	 * 
	 * @return string array of columns
	 */
	public static String[] getColumns() {
		return COLUMNS;
	}

	/**
	 * @see org.columba.mail.folderoptions.AbstractFolderOptionsPlugin#saveOptionsToXml(IMailbox)
	 */
	public void saveOptionsToXml(IMailbox folder) {
		XmlElement columns = getConfigNode(folder);

		ITableController tableController = ((ITableController) ((TableViewOwner) getMediator())
				.getTableController());

		Enumeration enumeration = tableController.getColumnModel().getColumns();

		// check if there are columns which need to be saved
		if ( tableController.getColumnModel().getColumnCount() == 0) 
			return;
		
		// remove all child nodes
		columns.removeAllElements();
		
		while (enumeration.hasMoreElements()) {
			TableColumn tc = (TableColumn) enumeration.nextElement();
			String name = (String) tc.getHeaderValue();

			XmlElement column = new XmlElement("column");
			column.addAttribute("name", name);

			// save width
			int size = tc.getWidth();
			column.addAttribute("width", Integer.toString(size));

			columns.addElement(column);
		}
	}

	/**
	 * @see org.columba.mail.folderoptions.AbstractFolderOptionsPlugin#loadOptionsFromXml(IMailbox)
	 */
	public void loadOptionsFromXml(IMailbox folder) {
		XmlElement columns = getConfigNode(folder);

		/*
		 * if ( columns.count() == 0) { // no columns specified // -> create new
		 * default columns XmlElement parent = columns.getParent();
		 * columns.removeFromParent(); columns = createDefaultElement(true);
		 * parent.addElement(columns);
		 *  }
		 */

		ITableController tableController = ((ITableController) ((TableViewOwner) getMediator())
				.getTableController());
		tableController.resetColumnModel();

		
		// add columns
		for (int i = 0; i < columns.count(); i++) {
			XmlElement column = columns.getElement(i);
			IDefaultItem columnItem = new DefaultItem(column);

			String name = columnItem.get("name");
			int size = columnItem.getInteger("width");

			//int position= columnItem.getInteger("position");
			// add column to table model
			tableController.getHeaderTableModel().addColumn(name);

			// add column to JTable column model
			TableColumn tc = tableController.createTableColumn(name, size);

			//tc.setModelIndex(position);
			tc.setModelIndex(i);

			// resize column width
			tc.setPreferredWidth(size);

			tableController.addColumn(tc);
		}

		
	}

	/**
	 * @see org.columba.mail.folderoptions.AbstractFolderOptionsPlugin#createDefaultElement()
	 */
	public XmlElement createDefaultElement(boolean global) {
		XmlElement columns = super.createDefaultElement(global);

		// these are the items, enabled as default
		columns.addElement(createColumn("Status", "23"));
		columns.addElement(createColumn("Attachment", "23"));
		columns.addElement(createColumn("Flagged", "23"));
		columns.addElement(createColumn("Priority", "23"));
		columns.addElement(createColumn("Subject", "200"));
		columns.addElement(createColumn("From", "150"));
		columns.addElement(createColumn("Date", "60"));
		columns.addElement(createColumn("Size", "30"));
		columns.addElement(createColumn("Spam", "23"));

		return columns;
	}

	/**
	 * Create new XmlElement with custom attributes.
	 * 
	 * @param name
	 *            name of column
	 * @param width
	 *            column width
	 * @param position
	 *            column position
	 * @return parent xml element
	 */
	private static XmlElement createColumn(String name, String width) {
		XmlElement column = new XmlElement("column");
		column.addAttribute("name", name);
		column.addAttribute("width", width);

		return column;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.columba.mail.folderoptions.AbstractFolderOptionsPlugin#restoreUISettings()
	 */
	public void restoreUISettings() {

	}
}