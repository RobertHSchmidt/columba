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
package org.columba.mail.gui.table.plugins;

import java.awt.Component;

import javax.swing.JTable;

import org.columba.mail.gui.table.model.MessageNode;



public class BasicRenderer extends DefaultLabelRenderer {
	
	private static final java.util.logging.Logger LOG = 
        java.util.logging.Logger.getLogger("org.columba.mail.gui.table.plugins"); //$NON-NLS-1$
	
    private String key;

    public BasicRenderer(String key) {
        super();

        this.key = key;
    }

    public Component getTableCellRendererComponent(JTable table, Object value,
        boolean isSelected, boolean hasFocus, int row, int column) {
    	
    	super.getTableCellRendererComponent(table, value, isSelected,
                hasFocus, row, column);
    	
        if (value == null) {
            setText("");

            return this;
        }

        String str = null;

        try {
            str = (String) ((MessageNode) value).getHeader().get(key);
        } catch (ClassCastException ex) {
            LOG.info("headertablecommonrenderer: " + ex.getMessage()); //$NON-NLS-1$
            str = "";
        }

        setText(str);

        return this;
    }
}
