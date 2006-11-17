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
package org.columba.core.filter;

import org.columba.core.config.DefaultItem;
import org.columba.core.xml.XmlElement;


/**
 * @author freddy
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class FilterActionList extends DefaultItem implements IFilterActionList {
    public FilterActionList(XmlElement root) {
        super(root);
    }

    /* (non-Javadoc)
     * @see org.columba.core.filter.IFilterActionList#get(int)
     */
    public FilterAction get(int index) {
        return new FilterAction(getRoot().getElement(index));
    }

    /* (non-Javadoc)
     * @see org.columba.core.filter.IFilterActionList#remove(int)
     */
    public void remove(int index) {
        getRoot().removeElement(index);
    }

    /* (non-Javadoc)
     * @see org.columba.core.filter.IFilterActionList#addEmptyAction()
     */
    public void addEmptyAction() {
        XmlElement action = new XmlElement("action");

        //action.addAttribute("class", "org.columba.mail.filter.action.MarkMessageAsReadFilterAction");
        action.addAttribute("type", "Mark Message");

        getRoot().addElement(action);
    }
}
