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
package org.frapuccino.action;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Proxy;

import javax.swing.Action;
import javax.swing.JRadioButtonMenuItem;

import org.frapuccino.common.MnemonicSetter;

/**
 * Quick fix to fix the Mnemonic correctly.
 *
 * @author redsolo
 */

public class FJRadioButtonMenuItem extends JRadioButtonMenuItem {

	/**
	 * default constructor
	 */
	public FJRadioButtonMenuItem() {
		super();
	}

	/**
	 * @param name
	 *            the name of the radio button.
	 */
	public FJRadioButtonMenuItem(String name) {
		super();
		// Set text, possibly with a mnemonic if defined using &
		MnemonicSetter.setTextWithMnemonic(this, name);
	}

	/**
	 * Creates a checkbox menu item with a given action attached. <br>
	 * If the name of the action contains &, the next character is used as
	 * mnemonic. If not, the fall-back solution is to use default behaviour,
	 * i.e. the mnemonic defined using setMnemonic on the action.
	 *
	 * @param action
	 *            The action to attach to the menu item
	 */
	public FJRadioButtonMenuItem(AbstractSelectableAction action) {
		super(action);

		// Set text, possibly with a mnemonic if defined using &
		MnemonicSetter.setTextWithMnemonic(this, (String) action
				.getValue(Action.NAME));

		getModel().addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				AbstractSelectableAction a = (AbstractSelectableAction) getAction();

				if (a != null) {
					a.setSelected(e.getStateChange() == ItemEvent.SELECTED);
				}
			}
		});
	}

	/**
	 * Overridden to react to state changes of the underlying action.
	 */
	protected PropertyChangeListener createActionPropertyChangeListener(Action a) {
		return (PropertyChangeListener) Proxy.newProxyInstance(getClass()
				.getClassLoader(),
				new Class[] { PropertyChangeListener.class },
				new ButtonStateAdapter(this, super
						.createActionPropertyChangeListener(a)));
	}

	/**
	 * Overridden to initialize selection state according to action
	 */
	protected void configurePropertiesFromAction(Action a) {
		super.configurePropertiesFromAction(a);
		setSelected(((AbstractSelectableAction) a).getSelected());
	}
}
