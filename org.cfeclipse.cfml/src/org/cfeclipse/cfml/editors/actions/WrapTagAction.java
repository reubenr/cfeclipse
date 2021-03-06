/*
 * Created on July 20, 2004
 *
 * The MIT License
 * Copyright (c) 2004 Rob Rohan
 *
 * Permission is hereby granted, free of charge, to any person obtaining a 
 * copy of this software and associated documentation files (the "Software"), 
 * to deal in the Software without restriction, including without limitation 
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 * and/or sell copies of the Software, and to permit persons to whom the Software 
 * is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, 
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE 
 * SOFTWARE.
 */
package org.cfeclipse.cfml.editors.actions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.cfeclipse.cfml.editors.CFMLEditor;
import org.cfeclipse.cfml.editors.EditorSynchronizer;
import org.cfeclipse.cfml.editors.OccurrencesFinder;
import org.cfeclipse.cfml.wizards.cfmlwizard.NewCFMLWizard;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.link.ILinkedModeListener;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.link.LinkedModeUI;
import org.eclipse.jface.text.link.LinkedPosition;
import org.eclipse.jface.text.link.LinkedPositionGroup;
import org.eclipse.jface.text.link.LinkedModeUI.ExitFlags;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.link.EditorLinkedModeUI;

/**
 * @author Denny Valliant
 * 
 *         This class wraps the selection with an linked editor field at each end.
 */
public class WrapTagAction extends GenericEncloserAction implements IEditorActionDelegate, IWorkbenchWindowActionDelegate {

	protected CFMLEditor editor = null;

	public WrapTagAction() {
		super();

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IWorkbenchWindowActionDelegate#dispose()
	 */
	public void dispose() {

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.eclipse.ui.IWorkbenchWindowActionDelegate#init(org.eclipse.ui.
	 * IWorkbenchWindow)
	 */
	public void init(IWorkbenchWindow window) {
		IEditorPart activeEditor = window.getActivePage().getActiveEditor();
		if (activeEditor instanceof CFMLEditor) {
			editor = (CFMLEditor) activeEditor;
		}

	}

	public void setActiveEditor(IAction action, IEditorPart targetEditor) {
		editor = (CFMLEditor) targetEditor;
	}

	public void run(IAction action) {
		run();
	}

	public void run() {
		ISourceViewer viewer = editor.getViewer();
		IDocument document = viewer.getDocument();
		int offset = ((ITextSelection) editor.getSelectionProvider().getSelection()).getOffset();
		LinkedPositionGroup group = new LinkedPositionGroup();
		IDocument doc =  editor.getDocumentProvider().getDocument(editor.getEditorInput()); 
		ITextSelection sel = (ITextSelection)editor.getSelectionProvider().getSelection();
		Point curRange = viewer.getSelectedRange();
		this.enclose(doc, sel, "<>", "</>");
		List positions = new ArrayList();
		// the offset / content of groups much match ("<[>]" and "</[>]")
		offset++;
		Position tagOpen = new Position(offset,1);
		Position tagClose = new Position(offset+ sel.getLength() + 3,1);
		positions.add(tagOpen);
		positions.add(tagClose);
		addPositionsToGroup(offset, positions, document, group);
		if (group.isEmpty()) {
			return;
		}
		try {
			LinkedModeModel model = new LinkedModeModel();
			model.addGroup(group);
			model.forceInstall();
			model.addLinkingListener(new EditorSynchronizer(editor));
			LinkedModeUI ui = new EditorLinkedModeUI(model, viewer);
			ui.setExitPosition(viewer, offset, 0, Integer.MAX_VALUE);
			ui.setExitPolicy(new exitOnSpacePolicy());
			ui.enter();
			viewer.setSelectedRange(sel.getOffset()+1, 0);
			//viewer.setSelectedRange(curRange.x,curRange.y);
		} catch (BadLocationException e) {
			e.printStackTrace();
			// CFMLPlugin.log(e);
		}
	}
	
	public void selectionChanged(IAction action, ISelection selection) {
		if (editor != null && editor instanceof CFMLEditor) {
			action.setEnabled(true);
		} else {
			action.setEnabled(false);
		}
	}
	/*
	 * custom exit policy that exits when a space is detected
	 */
	public static class exitOnSpacePolicy implements org.eclipse.jface.text.link.LinkedModeUI.IExitPolicy {
		public ExitFlags doExit(LinkedModeModel model, VerifyEvent event, int offset, int length) {
			if (length == 0 && (event.character == ' ')) {
				return new ExitFlags(ILinkedModeListener.EXIT_ALL, true);				
			}
			return null; // don't change behavior
		}		
	}

	private void addPositionsToGroup(int offset, List positions, IDocument document, LinkedPositionGroup group) {
		Iterator iter = positions.iterator();
		int i = 0;
		int j = 0;
		int firstPosition = -1;
		try {
			while (iter.hasNext()) {
				Position position = (Position) iter.next();
				if (firstPosition == -1) {
					if (position.overlapsWith(offset, 0)) {
						firstPosition = i;
						group
								.addPosition(new LinkedPosition(document, position.getOffset(), position.getLength(),
										j++));
					}
				} else {
					group.addPosition(new LinkedPosition(document, position.getOffset(), position.getLength(), j++));
				}
				i++;
			}

			for (i = 0; i < firstPosition; i++) {
				Position position = (Position) positions.get(i);
				group.addPosition(new LinkedPosition(document, position.getOffset(), position.getLength(), j++));
			}
		} catch (BadLocationException be) {
			be.printStackTrace();
			// CFMLPlugin.log(be);
		}
	}

}
