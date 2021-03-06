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
package org.columba.mail.gui.message.command;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.columba.api.command.ICommandReference;
import org.columba.api.command.IWorkerStatusController;
import org.columba.api.plugin.IExtension;
import org.columba.api.plugin.IExtensionHandler;
import org.columba.api.plugin.PluginException;
import org.columba.api.plugin.PluginHandlerNotFoundException;
import org.columba.core.base.cFileChooser;
import org.columba.core.base.cFileFilter;
import org.columba.core.command.Command;
import org.columba.core.command.CommandProcessor;
import org.columba.core.command.ProgressObservedInputStream;
import org.columba.core.command.Worker;
import org.columba.core.desktop.ColumbaDesktop;
import org.columba.core.gui.frame.DefaultContainer;
import org.columba.core.gui.frame.FrameManager;
import org.columba.core.io.StreamUtils;
import org.columba.core.logging.Logging;
import org.columba.core.plugin.PluginManager;
import org.columba.core.util.TempFileStore;
import org.columba.mail.command.IMailFolderCommandReference;
import org.columba.mail.command.MailFolderCommandReference;
import org.columba.mail.folder.IMailbox;
import org.columba.mail.folder.temp.TempFolder;
import org.columba.mail.gui.attachment.IAttachmentHandler;
import org.columba.mail.gui.message.util.AttachmentContext;
import org.columba.mail.gui.messageframe.MessageFrameController;
import org.columba.mail.gui.tree.FolderTreeModel;
import org.columba.mail.plugin.IExtensionHandlerKeys;
import org.columba.ristretto.coder.Base64DecoderInputStream;
import org.columba.ristretto.coder.QuotedPrintableDecoderInputStream;
import org.columba.ristretto.message.MimeHeader;

/**
 * @author freddy
 */
public class OpenAttachmentCommand extends SaveAttachmentCommand {
	private static final Logger LOG = Logger
			.getLogger("org.columba.mail.gui.message.attachment.command");

	private File tempFile;

	private TempFolder tempFolder;

	private Object tempMessageUid;

	private MimeHeader header;

	/**
	 * Constructor for OpenAttachmentCommand.
	 *
	 * @param references
	 *            command parameters
	 */
	public OpenAttachmentCommand(ICommandReference reference) {
		super(reference);

		priority = Command.REALTIME_PRIORITY;
		commandType = Command.NORMAL_OPERATION;
	}

	/**
	 * @see org.columba.api.command.Command#updateGUI()
	 */
	public void updateGUI() throws Exception {

		if (header.getMimeType().getType().toLowerCase().indexOf("message") != -1) {
			MessageFrameController c = new MessageFrameController();
			new DefaultContainer(c);

			Object[] uidList = new Object[1];
			uidList[0] = tempMessageUid;

			IMailFolderCommandReference r = new MailFolderCommandReference(
					tempFolder, uidList);

			c.setTreeSelection(r);
			c.setTableSelection(r);

			CommandProcessor.getInstance().addOp(new ViewMessageCommand(c, r));

		} else {

			boolean attachmentHandlerExecuted = false;
			try {
				IExtensionHandler handler = PluginManager
						.getInstance()
						.getExtensionHandler(
								IExtensionHandlerKeys.ORG_COLUMBA_ATTACHMENT_HANDLER);

				Enumeration<IExtension> e = handler.getExtensionEnumeration();
				while (e.hasMoreElements()) {
					IExtension extension = e.nextElement();
					try {
						IAttachmentHandler attachmentHandler = (IAttachmentHandler) extension
								.instanciateExtension(null);

						attachmentHandler.execute(new AttachmentContext(
								tempFile, header));
						attachmentHandlerExecuted &= true;
					} catch (PluginException e1) {
						LOG.severe("Error while loading plugin: "
								+ e1.getMessage());
						if (Logging.DEBUG)
							e1.printStackTrace();
					}
				}

			} catch (PluginHandlerNotFoundException e2) {
				LOG.severe("Error while loading plugin: " + e2.getMessage());
				if (Logging.DEBUG)
					e2.printStackTrace();
			}

			// in case no attachment handler was executed correctly
			// -> fall back to default handler
			if (!attachmentHandlerExecuted) {
				boolean success = ColumbaDesktop.getInstance().open(tempFile);

				// if attachment can't be opened, save it only
				if (!success) {
					File saveToFile = getDestinationFile(header);

					if (saveToFile.exists())
						saveToFile.delete();
					tempFile.renameTo(saveToFile);
				}
			}
		}
	}

	/**
	 * @see org.columba.api.command.Command#execute(Worker)
	 */
	public void execute(IWorkerStatusController worker) throws Exception {
		IMailFolderCommandReference r = (IMailFolderCommandReference) getReference();
		IMailbox folder = (IMailbox) r.getSourceFolder();
		Object[] uids = r.getUids();

		Integer[] address = r.getAddress();

		header = folder.getMimePartTree(uids[0]).getFromAddress(address)
				.getHeader();

		worker.setDisplayText("Opening " + header.getFileName());

		InputStream bodyStream = folder.getMimePartBodyStream(uids[0], address);
		// wrap with observable stream for progress bar updates
		bodyStream = new ProgressObservedInputStream(bodyStream, worker);

		if (header.getMimeType().getType().equals("message")) {

			tempFolder = FolderTreeModel.getInstance().getTempFolder();
			try {
				tempMessageUid = tempFolder.addMessage(bodyStream);
			} catch (Exception e) {
				LOG
						.warning("Could not create temporary email from the attachment.");
			}

		} else {

			String filename = header.getFileName();
			if (filename != null) {
				tempFile = TempFileStore.createTempFile(filename);
			} else {
				tempFile = TempFileStore.createTempFile();
			}

			int encoding = header.getContentTransferEncoding();

			switch (encoding) {
			case MimeHeader.QUOTED_PRINTABLE:
				bodyStream = new QuotedPrintableDecoderInputStream(bodyStream);
				break;

			case MimeHeader.BASE64:
				bodyStream = new Base64DecoderInputStream(bodyStream);
				break;
			default:
			}

			if (LOG.isLoggable(Level.FINE)) {
				LOG.fine("Storing the attachment to :" + tempFile);
			}

			FileOutputStream fileStream = new FileOutputStream(tempFile);
			StreamUtils.streamCopy(bodyStream, fileStream);
			fileStream.close();
			bodyStream.close();
		}
	}

	protected File getDestinationFile(MimeHeader header) {
		cFileChooser fileChooser;

		if (lastDir == null) {
			fileChooser = new cFileChooser();
		} else {
			fileChooser = new cFileChooser(lastDir);
		}

		cFileFilter fileFilter = new cFileFilter();
		fileFilter.acceptFilesWithProperty(cFileFilter.FILEPROPERTY_FILE);

		fileChooser.setDialogTitle("Save Attachment as ...");

		String fileName = getFilename(header);
		if (fileName != null) {
			fileChooser.forceSelectedFile(new File(fileName));
		}

		fileChooser.setSelectFilter(fileFilter);
		File tempFile = null;

		while (true) {
			if (fileChooser.showSaveDialog(null) != JFileChooser.APPROVE_OPTION) {
				return null;
			}

			tempFile = fileChooser.getSelectedFile();
			lastDir = tempFile.getParentFile();

			if (tempFile.exists()) {
				if (JOptionPane.showConfirmDialog(FrameManager.getInstance()
						.getActiveFrame(), "Overwrite File?",
						"Warning", JOptionPane.YES_NO_OPTION,
						JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
					break;
				}
			} else {
				break;
			}
		}
		return tempFile;
	}
}