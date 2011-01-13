package at.laborg.briss.gui;

import java.awt.Dialog;
import java.awt.Frame;
import java.io.IOException;
import java.io.InputStream;

import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;

@SuppressWarnings("serial")
public class JHelpDialog extends JDialog {

	private static final String HELP_FILE_PATH = "/help.html";

	public JHelpDialog(Frame owner, String title,
			Dialog.ModalityType modalityType) {
		super(owner, title, modalityType);
		setBounds(232, 232, 500, 800);

		String helpText = "";

		InputStream is = getClass().getResourceAsStream(HELP_FILE_PATH);
		byte[] buf = new byte[1024 * 100];
		try {
			int cnt = is.read(buf);
			helpText = new String(buf, 0, cnt);
		} catch (IOException e) {
			helpText = "Couldn't read the help file... Please contact gerhard.aigner@gmail.com";
		}

		JEditorPane jEditorPane = new JEditorPane("text/html", helpText);
		jEditorPane.setEditable(false);
		jEditorPane.setVisible(true);

		JScrollPane scroller = new JScrollPane(jEditorPane);
		getContentPane().add(scroller);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setVisible(true);
	}

}
