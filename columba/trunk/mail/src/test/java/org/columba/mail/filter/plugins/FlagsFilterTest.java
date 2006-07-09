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
package org.columba.mail.filter.plugins;

import org.columba.mail.filter.MailFilterCriteria;
import org.columba.mail.filter.MailFilterFactory;
import org.columba.mail.folder.MailboxTstFactory;
import org.columba.ristretto.message.Flags;


/**
 * @author fdietz
 *
 */
public class FlagsFilterTest extends AbstractFilterTst {
	
	/**
     * @param arg0
     */
    public FlagsFilterTest(String arg0) {
        super(arg0);
        
    }
    
    /**
     * @param arg0
     */
    public FlagsFilterTest(MailboxTstFactory factory, String arg0) {
        super(factory, arg0);
        
    }

    public void testIsSeen() throws Exception {
        // add message to folder
        Object uid = addMessage();
       
        Flags flags = getSourceFolder().getFlags(uid);
        flags.setSeen(true);
        
        // create filter configuration
        MailFilterCriteria criteria = MailFilterFactory.createIsSeenMessages();
        
        // create filter
        FlagsFilter filter = new FlagsFilter();

        // init configuration
        filter.setUp(criteria);

        // execute filter
        boolean result = filter.process(getSourceFolder(), uid);
        assertEquals("filter result", true, result);
    }
    
    public void testIsNotSeen() throws Exception {
        // add message to folder
        Object uid = addMessage();
       
        Flags flags = getSourceFolder().getFlags(uid);
        flags.setSeen(true);
        
        // create filter configuration
        MailFilterCriteria criteria = MailFilterFactory.createIsNotSeenMessages();
        
        // create filter
        FlagsFilter filter = new FlagsFilter();

        // init configuration
        filter.setUp(criteria);

        // execute filter
        boolean result = filter.process(getSourceFolder(), uid);
        assertEquals("filter result", false, result);
    }
    
    public void testIsExpunged() throws Exception {
        // add message to folder
        Object uid = addMessage();
       
        Flags flags = getSourceFolder().getFlags(uid);
        flags.setDeleted(true);
        
        // create filter configuration
        MailFilterCriteria criteria = MailFilterFactory.createExpungedMessages();
        
        // create filter
        FlagsFilter filter = new FlagsFilter();

        // init configuration
        filter.setUp(criteria);

        // execute filter
        boolean result = filter.process(getSourceFolder(), uid);
        assertEquals("filter result", true, result);
    }
    
    public void testIsFlagged() throws Exception {
        // add message to folder
        Object uid = addMessage();
       
        Flags flags = getSourceFolder().getFlags(uid);
        flags.setFlagged(true);
        
        // create filter configuration
        MailFilterCriteria criteria = MailFilterFactory.createFlaggedMessages();
        
        // create filter
        FlagsFilter filter = new FlagsFilter();

        // init configuration
        filter.setUp(criteria);

        // execute filter
        boolean result = filter.process(getSourceFolder(), uid);
        assertEquals("filter result", true, result);
    }
    
    public void testIsRecent() throws Exception {
        // add message to folder
        Object uid = addMessage();
       
        Flags flags = getSourceFolder().getFlags(uid);
        flags.setRecent(true);
        
        // create filter configuration
        MailFilterCriteria criteria = MailFilterFactory.createIsRecentMessages();
        
        // create filter
        FlagsFilter filter = new FlagsFilter();

        // init configuration
        filter.setUp(criteria);

        // execute filter
        boolean result = filter.process(getSourceFolder(), uid);
        assertEquals("filter result", true, result);
    }
    
    public void testIsSpam() throws Exception {
        // add message to folder
        Object uid = addMessage();
       
       getSourceFolder().setAttribute(uid, "columba.spam", Boolean.TRUE);
        
        // create filter configuration
       MailFilterCriteria criteria = MailFilterFactory.createSpamMessages();
        
        // create filter
        FlagsFilter filter = new FlagsFilter();

        // init configuration
        filter.setUp(criteria);

        // execute filter
        boolean result = filter.process(getSourceFolder(), uid);
        assertEquals("filter result", true, result);
    }

}
