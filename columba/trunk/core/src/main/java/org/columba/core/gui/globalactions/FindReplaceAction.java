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
package org.columba.core.gui.globalactions;

import java.awt.event.ActionEvent;

import org.columba.api.gui.frame.IFrameMediator;
import org.columba.core.gui.action.AbstractColumbaAction;
import org.columba.core.gui.util.FindReplaceDialog;
import org.columba.core.resourceloader.GlobalResourceLoader;
import org.columba.core.util.ComposerText;
import org.columba.mail.gui.composer.ComposerController;

@SuppressWarnings("serial")
public class FindReplaceAction extends AbstractColumbaAction {
	public FindReplaceAction(IFrameMediator controller) {
		super(controller,
		// GlobalResourceLoader.getString(null, null, "menu_edit_findagain"));
				GlobalResourceLoader.getString(null, null, "menu_edit_replace"));

		// tooltip text
		putValue(SHORT_DESCRIPTION, GlobalResourceLoader.getString(null, null,
		// "menu_edit_findagain_tooltip").replaceAll("&", ""));
				"menu_edit_replace_tooltip").replaceAll("&", ""));

		if (getFrameMediator() instanceof ComposerController) {
			setEnabled(true);
		} else
			setEnabled(false);
	}

	public void actionPerformed(ActionEvent e) {
		// @author Erich Schaer, Dmytro Podalyuk
		if (getFrameMediator() instanceof ComposerController) {
			ComposerText text = new ComposerText(
					(ComposerController) getFrameMediator());
			new FindReplaceDialog(text);
		}
	}
}
