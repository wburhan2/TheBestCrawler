package com.bestos.thebestcrawler.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

import com.bestos.thebestcrawler.UserPrefs;
import com.bestos.thebestcrawler.buffers.DoubleStringQueue;
import com.bestos.thebestcrawler.buffers.StringQueue;
import com.bestos.thebestcrawler.workers.DataGatherer;
import com.bestos.thebestcrawler.workers.PageParser;
import com.bestos.thebestcrawler.workers.PageRetriever;

/**
 * The GUI for The Best OS Webcrawler.
 * 
 * @author Wilson Burhan
 * @version 5/2/2013
 */
public class BestOsGUI extends JFrame {

	/**
	 * The serial ID.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * The title of the GUI.
	 */
	public static final String DEF_TITLE = "BestOs Webcrawler";

	/**
	 * The start button to start the program.
	 */
	private JButton start = null;

	/**
	 * Storage queue for pending urls and pages to be parsed, with callbacks.
	 */
	private final StringQueue url_buffer;

	/**
	 * Data structure to store the different keywords.
	 */
	private final ArrayList<String> list_key;

	/**
	 * Collection that contains the different panel for the parser threads.
	 */
	private final ArrayList<JPanel> num_of_parser;

	/**
	 * Collection that contains the different panel for the retriever threads.
	 */
	private final ArrayList<JPanel> num_of_retriever;

	/**
	 * The add button to add a keyword.
	 */
	private final JButton add;

	/**
	 * The remove button to remove a keyword.
	 */
	private final JButton remove;

	/**
	 * The keyword field that contains the keyword
	 */
	private final JTextField keyword_field;

	/**
	 * The parser field that contains the number of parsers
	 */
	private final JTextField parser_field;

	/**
	 * The retriever field that contains the number of retrievers
	 */
	private final JTextField retriever_field;

	/**
	 * The panel that displays the different thread indication.
	 */
	private JPanel mid_panel;

	/**
	 * Collection to store the all the created page retrievers.
	 */
	private PageRetriever[] retrievers_array;

	/**
	 * Collection to store the all the created page parsers.
	 */
	private PageParser[] parser_array;

	/**
	 * Storage queue for pending pages to be parsed, with their source url.
	 */
	private final DoubleStringQueue page_buffer;

	/**
	 * The page field that contains the number of pages.
	 */
	private final JTextField page_field;

	/**
	 * The url field that contains the starting URL.
	 */
	private final JTextField URL_field;

	/**
	 * Data gatherer.
	 */
	private final DataGatherer data;

	/**
	 * Data structure to store the different ReporterResponse object.
	 */
	private final static Queue<ReporterResponse> response_queue = new LinkedList<ReporterResponse>();

	/**
	 * Thread for the queue for displaying data purposes.
	 */
	private final QueueThread queue_thread;

	/**
	 * Text area for the error console.
	 */
	private final JTextArea output_area;

	/**
	 * Text area for the working output on the top left panel.
	 */
	private final JTextArea text_area;

	/**
	 * Scroll pane for the text area.
	 */
	private final JScrollPane scrollPane;

	/**
	 * The side panel that contains all the components on the left side.
	 */
	private final JPanel side;

	/**
	 * The bottom panel that contains both final output and error console.
	 */
	private final JPanel bottom;

	/**
	 * The label for the final output panel.
	 */
	private final JLabel final_label;

	/**
	 * To calculate time.
	 */
	private long timeStamp;

	/**
	 * Progress bar indication that replaces the start button to indicate the
	 * work progress.
	 */
	private final JProgressBar bar;

	/**
	 * Construct the GUI.
	 */
	public BestOsGUI() {
		super(DEF_TITLE);
		redirectSystemStreams();
		bar = new JProgressBar();
		bar.setBorderPainted(true);
		bar.setStringPainted(true);
		bottom = new JPanel(new BorderLayout());
		text_area = new JTextArea(30, 32);
		side = new JPanel(new BorderLayout());
		output_area = new JTextArea(15, 50);
		output_area.setEditable(false);
		text_area.setEditable(false);
		final_label = new JLabel();
		final_label.setFont(new Font("name", Font.PLAIN, 12));
		final_label.setHorizontalAlignment(SwingConstants.CENTER);

		queue_thread = new QueueThread();

		scrollPane = new JScrollPane(text_area);
		side.add(scrollPane, BorderLayout.CENTER);
		data = new DataGatherer();
		page_buffer = new DoubleStringQueue();
		url_buffer = new StringQueue();
		list_key = new ArrayList<String>();
		add = new JButton("Add");
		remove = new JButton("Remove");
		keyword_field = new JTextField(15);
		parser_field = new JTextField("1");
		retriever_field = new JTextField("1");
		num_of_retriever = new ArrayList<JPanel>();
		num_of_parser = new ArrayList<JPanel>();
		mid_panel = new JPanel(new GridLayout(0, 4));
		page_field = new JTextField("100", 5);
		URL_field = new JTextField("http://faculty.washington.edu/gmobus/", 30);

		UserPrefs.getUserPrefs().setDebugMode(false);

		setupWindow();
		statusWindow();
		setupListener();
		setVisible(true);
	}

	/**
	 * Set the center panel as well as the start button.
	 */
	private void setupWindow() {
		add(mid_panel, BorderLayout.CENTER);

		start = new JButton("Start");
		start.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				url_buffer.addPending(URL_field.getText());
				int num_parser = 1;
				int num_retriever = 1;
				try {
					num_parser = Integer.parseInt(parser_field.getText());
					// number of retriever
					num_retriever = Integer.parseInt(retriever_field.getText());
					if (num_parser <= 0 || num_retriever <= 0) {
						JOptionPane
								.showMessageDialog(
										null,
										"Incorrect Data Type! Numbers Only!\nNo. of retrievers and No. of parsers are resetted to 1!",
										"Error", JOptionPane.ERROR_MESSAGE);
						parser_field.setText("1");
						retriever_field.setText("1");
					}
				} catch (Exception z) {
					JOptionPane
							.showMessageDialog(
									null,
									"Incorrect Data Type! Numbers Only!\nNo. of retrievers and No. of parsers are resetted to 1!",
									"Error", JOptionPane.ERROR_MESSAGE);
					parser_field.setText("1");
					retriever_field.setText("1");
				}
				parser_array = new PageParser[num_parser];
				for (int i = 1; i <= num_parser; i++) {
					parser_array[i - 1] = new PageParser(page_buffer,
							url_buffer, list_key, data);
					num_of_parser.add(new WorkerStatusComponent(
							parser_array[i - 1]));
				}

				retrievers_array = new PageRetriever[num_retriever];
				for (int j = 1; j <= num_retriever; j++) {
					retrievers_array[j - 1] = new PageRetriever(page_buffer,
							url_buffer);
					num_of_retriever.add(new WorkerStatusComponent(
							retrievers_array[j - 1]));

				}
				timeStamp = System.currentTimeMillis();
				for (JPanel panel : num_of_retriever)
					mid_panel.add(panel);
				for (JPanel panel : num_of_parser)
					mid_panel.add(panel);
				if (num_parser + num_retriever < 4)
					mid_panel.setLayout(new GridLayout(0, num_parser
							+ num_retriever));
				for (int i = 0; i < retrievers_array.length; i++)
					retrievers_array[i].start();
				for (int i = 0; i < parser_array.length; i++)
					parser_array[i].start();

				remove(start);
				add(bar, BorderLayout.NORTH);
				revalidate();
				repaint();
				add.setEnabled(false);
				remove.setEnabled(false);
				URL_field.setEditable(false);
				page_field.setEditable(false);
				keyword_field.setText("");
				keyword_field.setEditable(false);
				parser_field.setEditable(false);
				retriever_field.setEditable(false);
				try {
					UserPrefs.getUserPrefs().setMaxPages(
							Integer.parseInt(page_field.getText()));
				} catch (Exception e) {
					JOptionPane.showMessageDialog(null,
							"Incorrect Data Type! Numbers Only!", "Error",
							JOptionPane.ERROR_MESSAGE);
					page_field.setText("100");
				}
				data.start();
				queue_thread.start();
				start.setEnabled(false);

			}
		});
		add(start, BorderLayout.NORTH);

		this.pack();
		this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		this.setSize(1600, 900);

	}

	@Override
	public void dispose() {
		try {
			FileWriter pw = new FileWriter("spiderRun.txt");
			text_area.write(pw);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		if (queue_thread.isAlive()) {
			try {
				queue_thread.shutdown();
			} catch (Exception e) {

			}
		}
		if (data.isAlive()) {
			try {
				data.shutDown();
			} catch (Exception e) {

			}

		}
		if (parser_array != null && retrievers_array != null) {
			for (int i = 0; i < parser_array.length; i++) {
				parser_array[i].shutdown();
			}
			for (int i = 0; i < retrievers_array.length; i++) {
				retrievers_array[i].shutdown();
			}
		}
		super.dispose();
	}

	/**
	 * Creates the left side panel.
	 */
	public void statusWindow() {

		final JPanel key_panel = new JPanel(new BorderLayout());
		final JPanel page_parse = new JPanel(new BorderLayout());
		final JPanel page_retrieve = new JPanel(new BorderLayout());
		final JPanel temp = new JPanel(new BorderLayout());
		final JPanel temp1 = new JPanel();
		final JPanel utilities = new JPanel(new GridLayout(2, 1));
		final JPanel url = new JPanel(new GridLayout(2, 1));
		final JPanel page = new JPanel(new BorderLayout());
		final JPanel page_info = new JPanel(new GridLayout(2, 1));
		final DefaultListModel<String> listModel = new DefaultListModel<String>();

		final JList<String> keyword_list = new JList<String>(listModel);
		keyword_list
				.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
		keyword_list.setLayoutOrientation(JList.HORIZONTAL_WRAP);
		keyword_list.setPreferredSize(new Dimension(150, 150));
		final JScrollPane list = new JScrollPane(keyword_list);
		TitledBorder title = BorderFactory.createTitledBorder(
				BorderFactory.createLineBorder(Color.black), "Keyword(s)");
		title.setTitleJustification(TitledBorder.CENTER);
		list.setBorder(title);
		remove.setEnabled(false);

		final JLabel parser = new JLabel("Page Parser: ");
		final JLabel retriever = new JLabel("Page Retriever: ");

		final JPanel url_panel = new JPanel();
		final JPanel max_page = new JPanel();
		final JLabel page_label = new JLabel("Max. page: ");
		final JLabel url_label = new JLabel("Enter URL: ");
		final JLabel max = new JLabel("(Up to 10,000 pages.)");
		url_panel.add(url_label);
		url_panel.add(URL_field);
		max_page.add(page_label);
		max_page.add(page_field);
		max_page.add(max);

		add.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				remove.setEnabled(true);
				if (keyword_field.getText().trim().equals("")) {
					JOptionPane.showMessageDialog(null, "Invalid input!",
							"Error", JOptionPane.ERROR_MESSAGE);
					keyword_field.setText("");
					keyword_field.requestFocus();
				} else if (list_key.size() > 9) {
					JOptionPane.showMessageDialog(null,
							"Only up to 10 keywords are allowed!", "Error",
							JOptionPane.ERROR_MESSAGE);
					keyword_field.requestFocus();
					keyword_field.selectAll();
				} else {
					remove.setEnabled(true);
					listModel.addElement(keyword_field.getText());
					list_key.add(keyword_field.getText());
					keyword_list.setSelectedIndex(0);
					invalidate();
					validate();
					repaint();
					keyword_field.setText("");
					keyword_field.requestFocus();
				}
			}
		});
		remove.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int index = keyword_list.getSelectedIndex();
				listModel.remove(index);
				list_key.remove(index);
				if (listModel.getSize() == 0)
					remove.setEnabled(false);
				else { // Select an index.
					if (index == listModel.getSize()) {
						// removed item in last position
						index--;
					}
					keyword_list.setSelectedIndex(index);
					keyword_list.ensureIndexIsVisible(index);
				}
			}
		});
		JPanel button_panel = new JPanel();
		button_panel.add(add);
		button_panel.add(remove);
		page_parse.add(parser, BorderLayout.WEST);
		page_parse.add(parser_field, BorderLayout.CENTER);
		page_retrieve.add(retriever, BorderLayout.WEST);
		page_retrieve.add(retriever_field, BorderLayout.CENTER);
		page_info.add(page_parse);
		page_info.add(page_retrieve);
		page.add(page_info);

		final JLabel keyword = new JLabel("Keyword: ");
		keyword.setVerticalAlignment(SwingConstants.CENTER);
		temp1.add(keyword);
		temp1.add(keyword_field);
		temp1.add(button_panel);
		utilities.add(max_page);
		utilities.add(temp1);
		key_panel.add(utilities, BorderLayout.NORTH);
		key_panel.add(list, BorderLayout.CENTER);

		url.add(url_panel);
		url.add(page);
		temp.add(url, BorderLayout.NORTH);
		temp.add(key_panel, BorderLayout.CENTER);
		bottom.add(mid_panel, BorderLayout.CENTER);
		bottom.add(bottomPanel(), BorderLayout.SOUTH);
		add(bottom, BorderLayout.CENTER);
		side.add(temp, BorderLayout.SOUTH);
		add(side, BorderLayout.WEST);
	}
	
	/**
	 * Helper method to create both final output panel and error console panel
	 * @return bottom panel.
	 */
	private JPanel bottomPanel() {
		final JPanel panel = new JPanel(new GridLayout(1, 2));
		final JPanel top = new JPanel(new BorderLayout());
		final JPanel bottom = new JPanel(new BorderLayout());
		output_area.setForeground(Color.RED);
		final JScrollPane pane = new JScrollPane(output_area);
		TitledBorder title = BorderFactory.createTitledBorder(
				BorderFactory.createLineBorder(Color.black), "Error Console");
		title.setTitleJustification(TitledBorder.CENTER);
		pane.setBorder(title);
		title = BorderFactory.createTitledBorder(
				BorderFactory.createLineBorder(Color.black), "Final output");
		title.setTitleJustification(TitledBorder.CENTER);
		top.setBorder(title);
		top.add(final_label);
		final JScrollPane final_pane = new JScrollPane(top);
		panel.add(final_pane);

		bottom.add(pane);
		panel.add(bottom);
		return panel;
	}

	/**
	 * Helper method for redirecting the console output into the error console panel on the GUI.
	 * @param text the text to be appended into the console output area.
	 */
	private void updateTextArea(final String text) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				output_area.append(text);
			}
		});
	}

	/**
	 * The method that redirects the console output stream into the console output on the GUI error console panel.
	 */
	private void redirectSystemStreams() {
		OutputStream out = new OutputStream() {
			@Override
			public void write(int b) throws IOException {
				updateTextArea(String.valueOf((char) b));
			}

			@Override
			public void write(byte[] b, int off, int len) throws IOException {
				updateTextArea(new String(b, off, len));
			}

			@Override
			public void write(byte[] b) throws IOException {
				write(b, 0, b.length);
			}
		};

		System.setOut(new PrintStream(out, true));
		System.setErr(new PrintStream(out, true));
	}

	/**
	 * Static method to be called by the Reporter. Then put the data into a response_queue. 
	 * @param url the URL
	 * @param keyword the keyword data
	 * @param avg_word the average word
	 * @param pages_retrieved the number of pages retrieved
	 * @param avg_url the average url per page
	 * @param page_limit the page limit
	 * @param avg_parse_time the average parse time
	 * @param running_time the total running time
	 * @param total_hit the total hit for the keyword
	 * @param avg_hit the average hit for the keyword
	 */
	public static void addQueue(String url, Map<String, keyMap> keyword,
			int avg_word, int pages_retrieved, int avg_url, int page_limit,
			double avg_parse_time, long running_time, int total_hit,
			double avg_hit) {
		ReporterResponse response = new ReporterResponse();
		keyMap key = new keyMap();
		response.URL_retrieve = url;
		response.avg_word = avg_word;
		response.pages_retrieved = pages_retrieved;
		response.avg_url = avg_url;
		response.page_limit = page_limit;
		response.avg_parse_time = avg_parse_time;
		response.running_time = running_time;
		key.avg_hit = avg_hit;
		key.total_hit = total_hit;
		response.key_hit = keyword;
		response_queue.add(response);
	}

	/**
	 * The listener for the auto select on the JTextField. 
	 */
	private void setupListener() {
		URL_field.addMouseListener(new MyMouseAdapter());
		;
		page_field.addMouseListener(new MyMouseAdapter());
		;
		keyword_field.addMouseListener(new MyMouseAdapter());
		;
		parser_field.addMouseListener(new MyMouseAdapter());
		;
		retriever_field.addMouseListener(new MyMouseAdapter());
		;
	}

	/**
	 * ReporterResponse wrapper class. 
	 * @author Wilson Burhan
	 * @version 5/2/2013
	 */
	public static class ReporterResponse {
		public String URL_retrieve;
		public int avg_word;
		public int pages_retrieved;
		public int avg_url;
		public int page_limit;
		public double avg_parse_time;
		public long running_time;
		public Map<String, keyMap> key_hit = new TreeMap<String, keyMap>();
	}

	/**
	 * Keymap wrapper class
	 * @author Wilson Burhan
	 * @version 5/2/2013
	 */
	public static class keyMap {
		public int total_hit;
		public double avg_hit;
	}

	/**
	 * The main method.
	 * @param the_args the args
	 */
	public static void main(final String... the_args) {
		new BestOsGUI();
	}

	/**
	 * The queue thread inner class.
	 * @author Wilson Burhan
	 * @version 5/2/2013
	 */
	public class QueueThread extends Thread {

		private boolean stopRunning = false;
		
		/**
		 * The shutdown method.
		 */
		public void shutdown() {
			stopRunning = true;
		}

		/**
		 * The run method.
		 */
		@Override
		public void run() {
			final DecimalFormat df = new DecimalFormat("#.####");
			while (!stopRunning) {
				if (response_queue.peek() != null) {
					ReporterResponse temp = response_queue.remove();
					StringBuilder sb = new StringBuilder();
					text_area.append("Parsed: " + temp.URL_retrieve + "\n");
					text_area.append("Pages Retrieved: " + temp.pages_retrieved
							+ "\n");
					text_area.append("Average words per page: " + temp.avg_word
							+ "\n");
					text_area.append("Average URLs per page: " + temp.avg_url
							+ "\n");
					text_area
							.append("Keyword\tAve. hits per page\tTotalHits \n");
					for (int i = 0; i < list_key.size(); i++) {
						text_area.append(" " + list_key.get(i) + " \t "
								+ temp.key_hit.get(list_key.get(i)).avg_hit
								+ " \t\t "
								+ temp.key_hit.get(list_key.get(i)).total_hit
								+ "\n");
						sb.append("&nbsp;&nbsp;" + list_key.get(i)
								+ "&nbsp;&#09;"
								+ temp.key_hit.get(list_key.get(i)).avg_hit
								+ "&#09;&#09;"
								+ temp.key_hit.get(list_key.get(i)).total_hit
								+ "<br>");
					}
					text_area.append("Page limit: " + page_field.getText()
							+ "\n");
					text_area.append("Average parse time per page "
							+ df.format(temp.avg_parse_time) + " ms\n");
					text_area
							.append("Total running time "
									+ ((System.currentTimeMillis() - timeStamp) / 1000f)
									+ " sec\n\n\n");

					final_label
							.setText("<html><b>Parsed: <font size =\"3\">"
									+ temp.URL_retrieve
									+ "</font><br>Pages Retrieved: "
									+ temp.pages_retrieved
									+ "<br>Average words per page: "
									+ temp.avg_word
									+ "<br>Average URLs per page: "
									+ temp.avg_url
									+ "<br>Keyword&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Ave. hits per page&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;TotalHits<br>"
									+ sb.toString()
									+ "Page limit: "
									+ page_field.getText()
									+ "<br>Average parse time per page "
									+ df.format(temp.avg_parse_time)
									+ " ms<br>"
									+ "Total running time "
									+ ((System.currentTimeMillis() - timeStamp) / 1000f)
									+ " sec</b></html>");

					int x;
					text_area.selectAll();
					x = text_area.getSelectionEnd();
					text_area.select(x, x);
					bar.setValue((int) ((double) (temp.pages_retrieved)
							/ Integer.parseInt(page_field.getText()) * 100));
					repaint();
				} else {
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {

					}
				}
			}

		}
	}
}