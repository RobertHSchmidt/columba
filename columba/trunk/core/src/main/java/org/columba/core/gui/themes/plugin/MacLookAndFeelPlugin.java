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
package org.columba.core.gui.themes.plugin;

import javax.swing.UIManager;

/**
 * @author frd
 */
public class MacLookAndFeelPlugin extends AbstractThemePlugin {
	/**
	 * 
	 */
	public MacLookAndFeelPlugin() {
		super();
	}

	/**
	 * @see org.columba.core.gui.themes.plugin.AbstractThemePlugin#setLookAndFeel()
	 */
	public void setLookAndFeel() throws Exception {

		try {
			// try the system aqua look and feel
			UIManager.setLookAndFeel("apple.laf.AquaLookAndFeel");

		} catch (Exception e) {
			System.out.println("Something went wrong setting Aqua LookAndFeel");
			e.printStackTrace();
		}
	}
}
