package com.bestos.thebestcrawler.gui;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.text.JTextComponent;

/**
 * Mouse Adapter for the JTextField.
 * @author Wilson Burhan
 * @version 5/2/13
 */
class MyMouseAdapter extends MouseAdapter 
{
	/**
	 * Highlight the Text in the JTextField
	 * @param the_event the selected text field.
	 */
	@Override
	public void mousePressed(final MouseEvent theEvent) 
	{
		JTextComponent comp = (JTextComponent) theEvent.getSource();
		comp.selectAll();
	}
}
