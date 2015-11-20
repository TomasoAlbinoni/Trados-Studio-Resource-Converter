package sdlconverter;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.healthmarketscience.jackcess.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.*;


class Termbase {
	Termbase() {
		conceptList = new HashMap<Integer, Concept>();
		languageList = new HashMap<String, Integer>();
		conceptMetaList = new ArrayList<String>();
	}

	void addConcept(int key) {
		Concept c = new Concept();
		conceptList.put(key, c);
	}

	int inLanguageList(String name) {
		if (!languageList.containsKey(name)) {
			return 0;
		} else {
			return languageList.get(name);
		}
	}

	void addLanguage(String name) {
		languageList.put(name, 1);
	}

	void setMaxNumber(String name, int number) {
		languageList.put(name, number);
	}

	int getMaxNumber(String name) {
		return languageList.get(name);
	}

	void inMeta(String s) {
		if (!conceptMetaList.contains(s)) {
			conceptMetaList.add(s);
		}
	}

	public HashMap<Integer, Concept> conceptList;
	public HashMap<String, Integer> languageList;
	public ArrayList<String> conceptMetaList;
}

class Concept {
	Concept() {
		termgroups = new HashMap<String, Termgroup>();
		creator = "";
		creationTime = "";
		modifier = "";
		modificationTime = "";
		metaData = new HashMap<String, String>();
	}

	void setEntryCreator(String c) {
		creator = c;
	}

	String getCreator() {
		return creator;
	}

	void setCreationTime(String t) {
		creationTime = t;
	}

	String getCreationTime() {
		return creationTime;
	}

	void setEntryModifier(String m) {
		modifier = m;
	}

	String getModifier() {
		return modifier;
	}

	void setModificationTime(String t) {
		modificationTime = t;
	}

	String getModificationTime() {
		return modificationTime;
	}

	void addTerm(Term term, String lang) {
		termgroups.get(lang).addTerm(term);
	}

	void addTermgroup(String lang) {
		Termgroup trmgrp = new Termgroup();
		termgroups.put(lang, trmgrp);
	}

	void addDef(String def, String lang) {
		termgroups.get(lang).addDefinition(def);
	}

	void addMeta(String key, String value) {
		metaData.put(key, value);
	}

	String getMeta(String key) {
		if (metaData.get(key) == null) {
			return "";
		} else {
			return metaData.get(key);
		}
	}

	public HashMap<String, Termgroup> termgroups;
	private String creator;
	private String creationTime;
	private String modifier;
	private String modificationTime;
	private HashMap<String, String> metaData;
}

class Termgroup {

	Termgroup() {
		terms = new ArrayList<Term>();
		definition = "";
	}

	void addTerm(Term term) {
		terms.add(term);
	}

	void addDefinition(String def) {
		definition = def;
	}

	String getDefinition() {
		return definition;
	}

	public ArrayList<Term> terms;
	private String definition;
}

class Term {
	Term(String newword) {
		word = newword;
		termInfo = "";
		usage = "";
	}

	void addTermInfo(String i) {
		termInfo += i;
	}

	String getTermInfo() {
		return termInfo;
	}


	void addUsage(String u) {
		usage = u;
	}

	String getUsage() {
		return usage;
	}

	String getWord() {
		return word;
	}

	private String word;
	private String termInfo;
	private String usage;
	// other metadata
}

public class SDLConverter extends JPanel implements ActionListener {

	static JButton buttonSdltm2tmx;
	static JButton buttonSdltb2csv;
	static JFrame window = new JFrame("SDL Trados Studio Resource Converter");
	static String nl = System.getProperty("line.separator");

	public SDLConverter() {

		//super(new BorderLayout());
		buttonSdltm2tmx = new JButton("Convert SDLTM");
		buttonSdltm2tmx.setPreferredSize(new Dimension(200, 40));
		buttonSdltm2tmx.setVerticalTextPosition(AbstractButton.CENTER);
		buttonSdltm2tmx.setHorizontalTextPosition(AbstractButton.LEADING);
		buttonSdltm2tmx.setMnemonic(KeyEvent.VK_S);
		buttonSdltm2tmx.setToolTipText("Click this button to convert a Trados Studio TM to tmx");
		buttonSdltm2tmx.setActionCommand("sdltm2tmx");
		buttonSdltm2tmx.addActionListener(this);
		add(buttonSdltm2tmx);

		//super(new BorderLayout());
		buttonSdltb2csv = new JButton("Convert SDLTB");
		buttonSdltb2csv.setPreferredSize(new Dimension(200, 40));
		buttonSdltb2csv.setVerticalTextPosition(AbstractButton.CENTER);
		buttonSdltb2csv.setHorizontalTextPosition(AbstractButton.LEADING);
		buttonSdltb2csv.setMnemonic(KeyEvent.VK_S);
		buttonSdltb2csv.setToolTipText("Click this button to convert a Trados Studio TB to csv");
		buttonSdltb2csv.setActionCommand("sdltb2csv");
		buttonSdltb2csv.addActionListener(this);
		add(buttonSdltb2csv);

	}

	public void actionPerformed(ActionEvent e) {
		if ("sdltm2tmx".equals(e.getActionCommand())) {
			gettm();
		} else if ("sdltb2csv".equals(e.getActionCommand())) {
			convertTB();
		} else {
			JOptionPane.showMessageDialog(window,"Function not available");
		}
	}

	protected static void showWindow() {


		//Set up the window.
		JFrame window = new JFrame("SDL Trados Studio Resource Converter");
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		window.setSize(250,300);
		window.setLocation(100, 100);
		JPanel newContentPane = new SDLConverter();
		newContentPane.setOpaque(true); //content panes must be opaque
		window.setContentPane(newContentPane);
		Container contentPane = window.getContentPane();
		contentPane.setLayout(new FlowLayout());


		//Display the window.
		//window.pack();
		window.setVisible(true);
	}

	protected void writeCSV(Writer out, String s, String delimiter) {
		try {
			if (delimiter != "\t") {
				out.write('"');
				out.write(s.replaceAll("\"", "\"\"")); // Escape double quotes by doubling them
				out.write('"');
				out.write(delimiter);
			} else { // if delimiter == '\t'
				out.write(s);
				out.write('\t');
			}
		}
		catch (IOException e) {
			JOptionPane.showMessageDialog(window, "IOException2");
		}
	}

	protected void gettm() {
		JFileChooser fc;
		fc = new JFileChooser();
		fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
		fc.setMultiSelectionEnabled(true);
		FileFilter filter = new FileNameExtensionFilter("SDLTM files", "sdltm");
		fc.setAcceptAllFileFilterUsed(false);
		fc.setFileFilter(filter);
		int returnVal = fc.showOpenDialog(getParent());
		if(returnVal == JFileChooser.APPROVE_OPTION) {
			File[] files = fc.getSelectedFiles();
			converttm(files);
		}
	}
	
	protected static void converttm(File[] files) {
		
		/* 
		 * TODO: Parse and build XML properly instead of using regex and string concatenation.
		 */
		
		Writer out = null;
		Path filePath, root;
		String basename;
		root = Paths.get(files[0].getPath()).getParent();
		if (files.length == 1) {
			basename = Paths.get(files[0].getPath()).getFileName().toString();
			basename = basename.substring(0, basename.lastIndexOf('.'));
			filePath = root.resolve(basename + ".tmx");
		} else {
			filePath = root.resolve("converted.tmx");
		}
		Connection c = null;
		Statement stmt = null;
		ResultSet rs = null;

		try {
			// Regular expressions to convert the SDL-specific XML to TMX
			Pattern segPattern = Pattern.compile("<Segment .*?<Elements>(.*?)</Elements><CultureName>(.*?)</CultureName></Segment>", Pattern.DOTALL);
			Matcher segMatcher;
			Pattern dateTimePattern = Pattern.compile("(\\d{4})-(\\d{2})-(\\d{2}) (\\d{2}):(\\d{2}):(\\d{2})");
			Matcher dateTimeMatcher;
			Pattern valueTextPattern = Pattern.compile("</?Value>|</?Text>");
			Matcher valueTextMatcher;
			Pattern startTagPattern = Pattern.compile("<Tag><Type>Start</Type><Anchor>(\\d+)</Anchor><AlignmentAnchor>(\\d+)</AlignmentAnchor><TagID>(.*?)</TagID></Tag>");
			Matcher startTagMatcher;
			Pattern endTagPattern = Pattern.compile("<Tag><Type>End</Type><Anchor>(\\d+)</Anchor><AlignmentAnchor>\\d+</AlignmentAnchor>(<TagID>.*?</TagID>)?</Tag>");
			Matcher endTagMatcher;
			Pattern placeholderPattern = Pattern.compile("<Tag><Type>.*?</Type><Anchor>\\d+</Anchor><AlignmentAnchor>(\\d+)</AlignmentAnchor><TagID>(.*?)</TagID>(<TextEquivalent>.*?</TextEquivalent>|<TextEquivalent />)?</Tag>");
			Matcher placeholderMatcher;
			
			String sourceSegment, targetSegment, tu, srcLang;

			Class.forName("org.sqlite.JDBC");
			
			// Check the first file for the source language. It is assumed that all files have the same language pair.
			c = DriverManager.getConnection("jdbc:sqlite:".concat(files[0].getAbsolutePath()));
			c.setAutoCommit(false);
			stmt = c.createStatement();
			rs = stmt.executeQuery( "SELECT source_segment FROM translation_units LIMIT 1;" );
			sourceSegment = rs.getString("source_segment");
			segMatcher = segPattern.matcher(sourceSegment);
			segMatcher.find();
			srcLang = segMatcher.group(2);
			out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filePath.toString()), "UTF-8"));
			out.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>" + nl + "<tmx version=\"1.4\">" + nl + "  <header creationtool=\"SDLTM Converter\" creationtoolversion=\"1.0\" segtype=\"sentence\" o-tmf=\"SDLTM\" adminlang=\"en\" srclang=\"" + srcLang + "\" datatype=\"unknown\">" + nl + "  </header>" + nl + "  <body>" + nl);
			rs.close();
			stmt.close();
			c.close();
			
			int tus = 0; // Number of translation units converted
			
			for (int i = 0; i < files.length; i++) {
				c = DriverManager.getConnection("jdbc:sqlite:".concat(files[i].getAbsolutePath()));
				c.setAutoCommit(false);
				stmt = c.createStatement();
				rs = stmt.executeQuery( "SELECT source_segment, target_segment, creation_date, creation_user, change_date, change_user, last_used_date, usage_counter FROM translation_units;" );
				while ( rs.next() ) {
					sourceSegment = rs.getString("source_segment");
					targetSegment = rs.getString("target_segment");

					// Process source segment
					segMatcher = segPattern.matcher(sourceSegment);
					sourceSegment = segMatcher.replaceFirst("      <tuv xml:lang=\"$2\">" + nl + "        <seg>$1</seg>" + nl + "      </tuv>");
					valueTextMatcher = valueTextPattern.matcher(sourceSegment);
					sourceSegment = valueTextMatcher.replaceAll("");
					startTagMatcher = startTagPattern.matcher(sourceSegment);
					sourceSegment = startTagMatcher.replaceAll("<bpt i=\"$1\" type=\"$3\" x=\"$2\" />");
					endTagMatcher = endTagPattern.matcher(sourceSegment);
					sourceSegment = endTagMatcher.replaceAll("<ept i=\"$1\" />");
					placeholderMatcher = placeholderPattern.matcher(sourceSegment);
					sourceSegment = placeholderMatcher.replaceAll("<ph x=\"$1\" type=\"$2\" />");

					// Process target segment
					segMatcher = segPattern.matcher(targetSegment);
					targetSegment = segMatcher.replaceFirst("      <tuv xml:lang=\"$2\">" + nl + "        <seg>$1</seg>" + nl + "      </tuv>" + nl + "    </tu>");
					valueTextMatcher = valueTextPattern.matcher(targetSegment);
					targetSegment = valueTextMatcher.replaceAll("");
					startTagMatcher = startTagPattern.matcher(targetSegment);
					targetSegment = startTagMatcher.replaceAll("<bpt i=\"$1\" type=\"$3\" x=\"$2\" />");
					endTagMatcher = endTagPattern.matcher(targetSegment);
					targetSegment = endTagMatcher.replaceAll("<ept i=\"$1\" />");
					placeholderMatcher = placeholderPattern.matcher(targetSegment);
					targetSegment = placeholderMatcher.replaceAll("<ph x=\"$1\" type=\"$2\" />");

					// Assemble TMX translation unit
					tu = "    <tu creationdate=\"" + rs.getString("creation_date") + "\" creationid=\"" + rs.getString("creation_user") + "\" changedate=\"" + rs.getString("change_date") + "\" changeid=\"" + rs.getString("change_user") + "\" lastusagedate=\"" + rs.getString("last_used_date") + "\" usagecount=\"" + rs.getString("usage_counter") + "\">";
					dateTimeMatcher = dateTimePattern.matcher(tu);
					tu = dateTimeMatcher.replaceAll("$1$2$3T$4$5$6Z");
					out.write(tu + nl + sourceSegment + nl + targetSegment + nl);
					tus++;
				}
			}
			out.write("  </body>" + nl + "</tmx>" + nl);
			JOptionPane.showMessageDialog(window, tus + " translation units converted");

		} catch (UnsupportedEncodingException e) {
			JOptionPane.showMessageDialog(window, "UnsupportedEncodingException");
		} catch (FileNotFoundException e) {
			JOptionPane.showMessageDialog(window, "FileNotFoundException");
		} catch (IOException e){
			JOptionPane.showMessageDialog(window, "IOException");
		} catch (SQLException e) {
			JOptionPane.showMessageDialog(window, "SQLException, input file may be empty");
		} catch (ClassNotFoundException e) {
			JOptionPane.showMessageDialog(window, "ClassNotFoundException");
		} finally {
			try {
				if(out!=null)
					out.close();
				rs.close();
				stmt.close();
				c.close();
			} catch (IOException e) {
				JOptionPane.showMessageDialog(window, "IOException");
			} catch (SQLException e) {
				JOptionPane.showMessageDialog(window, "SQLException");
			}
		}
	}

	protected void convertTB() {
		
		/*
		 * TODO: Process language variants (e.g., British, American): /cG/lG/l/tG/dG/d[@type='Variant']
		 */
		
		JFileChooser fc;
		fc = new JFileChooser();
		fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
		fc.setMultiSelectionEnabled(true);
		FileFilter filter = new FileNameExtensionFilter("SDLTB files", "sdltb");
		fc.setAcceptAllFileFilterUsed(false);
		fc.setFileFilter(filter);
		int returnVal = fc.showOpenDialog(getParent());

		if(returnVal == JFileChooser.APPROVE_OPTION) {

			String delimiter = ","; // Default 
			Object[] output_options = {"Comma-separated CSV", "Semicolon-separated CSV", "Tab-separated TXT"};
			String outputType = (String)JOptionPane.showInputDialog(window, "Please select the output type:", "Please select the output type", JOptionPane.PLAIN_MESSAGE, null, output_options, "Comma-separated CSV");
			if (outputType.equals("Comma-separated CSV")) {
				delimiter = ",";
			} else if (outputType.equals("Semicolon-separated CSV")) {
				delimiter = ";";
			} else if (outputType.equals("Tab-separated TXT")) {
				delimiter = "\t";
			} 

			Object[] synonym_options = {"Separate columns", "Separated by a pipe (|)"};
			String synonyms = (String)JOptionPane.showInputDialog(window, "Please select how synonyms should be presented:", "Please select how synonyms should be presented", JOptionPane.PLAIN_MESSAGE, null, synonym_options, "Separate columns");

			if (synonyms != null) {

				BufferedWriter out = null;

				Path filePath, root;
				File[] files = fc.getSelectedFiles();
				root = Paths.get(files[0].getPath()).getParent();
				if (delimiter == "\t") {
					filePath = root.resolve("converted.txt");
				} else {
					filePath = root.resolve("converted.csv");
				}

				Database db = null;

				String xml;

				Termbase termbase = new Termbase();
				Term term = null;
				String lang = null;
				String creationMoment = null;
				String modificationMoment = null;
				int hour;
				String ampm = null;
				String entryCreator = null;
				String entryModifier = null;
				String definition = null;
				String termsWithPipes = "";

				InputSource source;

				Pattern dateTimePattern = Pattern.compile("(\\d{4})-(\\d{2})-(\\d{2})T(\\d{2}):(\\d{2}):(\\d{2})");
				Matcher dateTimeMatcher;

				// Read SDLTB data into termbase object
				try {

					DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
					DocumentBuilder xmldb = dbf.newDocumentBuilder();
					Document document;

					XPathFactory xpathFactory = XPathFactory.newInstance();
					XPath xpath = xpathFactory.newXPath();

					db = DatabaseBuilder.open(files[0]);
					Table table = db.getTable("mtConcepts");
					int entryNumber;

					// Store contents in String xml
					for(Row row : table) {
						// Create new concept
						entryNumber = Integer.parseInt(row.get("conceptid").toString());
						termbase.addConcept(entryNumber);
						xml = row.get("text").toString();

						source = new InputSource(new StringReader(xml));
						document = xmldb.parse(source);

						// ==================== Read entry level data =======================

						entryCreator = xpath.evaluate("/cG/trG/tr[@type='origination']", document);

						creationMoment = xpath.evaluate("/cG/trG/tr[@type='origination']/../dt", document);
						dateTimeMatcher = dateTimePattern.matcher(creationMoment);
						dateTimeMatcher.matches();
						hour = Integer.parseInt(dateTimeMatcher.group(4));
						if (hour > 11) {
							hour -= 12;
							ampm = "PM";
						} else {
							ampm = "AM";
						}
						if (hour == 0) {
							hour = 12;
						}
						creationMoment = dateTimeMatcher.replaceFirst("$2/$3/$1 " + hour + ":$5:$6 " + ampm);

						entryModifier = xpath.evaluate("/cG/trG/tr[@type='modification']", document);

						modificationMoment = xpath.evaluate("/cG/trG/tr[@type='modification']/../dt", document);
						dateTimeMatcher = dateTimePattern.matcher(modificationMoment);
						dateTimeMatcher.matches();
						hour = Integer.parseInt(dateTimeMatcher.group(4));
						if (hour > 11) {
							hour -= 12;
							ampm = "PM";
						} else {
							ampm = "AM";
						}
						if (hour == 0) {
							hour = 12;
						}
						modificationMoment = dateTimeMatcher.replaceFirst("$2/$3/$1 " + hour + ":$5:$6 " + ampm);

						termbase.conceptList.get(entryNumber).setEntryCreator(entryCreator);
						termbase.conceptList.get(entryNumber).setCreationTime(creationMoment);
						termbase.conceptList.get(entryNumber).setEntryModifier(entryModifier);
						termbase.conceptList.get(entryNumber).setModificationTime(modificationMoment);

						// Process other concept-level metadata
						NodeList conceptMetaNodes = (NodeList) xpath.compile("/cG/dG").evaluate(document, XPathConstants.NODESET);
						for (int h = 0; h < conceptMetaNodes.getLength(); h++) {
							Element g = (Element) conceptMetaNodes.item(h).getFirstChild();

							termbase.inMeta(g.getAttribute("type"));
							termbase.conceptList.get(entryNumber).addMeta(g.getAttribute("type"), g.getTextContent());	
						}

						// ================== Read language level data ======================
						NodeList nodeList = (NodeList) xpath.compile("/cG/lG").evaluate(document, XPathConstants.NODESET);
						for (int i = 0; i < nodeList.getLength(); i++) { // For each language group
							// Read language
							lang = xpath.evaluate("l/@type", nodeList.item(i));
							lang = lang.replaceAll(" ", "_");
							lang = lang.replaceAll("\\(|\\)", "");

							termbase.conceptList.get(entryNumber).addTermgroup(lang);
							NodeList elements = nodeList.item(i).getChildNodes(); // l, dG, tG
							for (int j = 0; j < elements.getLength(); j++) {
								if (elements.item(j).getNodeName().equals("dG")) {
									NodeList forbiddenOrDef = elements.item(j).getChildNodes(); // d
									for (int k = 0; k < forbiddenOrDef.getLength(); k++) {
										if (forbiddenOrDef.item(k).getNodeName().equals("d")) {
											Element f = (Element)forbiddenOrDef.item(k);
											if (f.getAttribute("type").equals("Forbidden term")) {
												term = new Term(forbiddenOrDef.item(k).getTextContent());
												termbase.conceptList.get(entryNumber).addTerm(term, lang);
												term.addTermInfo("NonTerm");
											} else if (f.getAttribute("type").equals("Definition")) {
												definition = forbiddenOrDef.item(k).getTextContent();
												termbase.conceptList.get(entryNumber).addDef(definition, lang);
											}
										} 
									}
								} else if (elements.item(j).getNodeName().equals("tG")) {
									term = new Term(xpath.evaluate("t", elements.item(j)));
									termbase.conceptList.get(entryNumber).addTerm(term, lang);

									NodeList terms = elements.item(j).getChildNodes(); // t (term), trG (metadata), dG (Usage example)

									// ================= Read term level data ===================
									for (int l = 0; l < terms.getLength(); l++) {

										if (terms.item(l).getNodeName().equals("dG")) {
											NodeList usage = terms.item(l).getChildNodes(); // d
											for (int m = 0; m < usage.getLength(); m++) {
												if (usage.item(m).getNodeName().equals("d")) {
													Element g = (Element)usage.item(m);
													if (g.getAttribute("type").equals("Usage example")) {
														term.addUsage(usage.item(m).getTextContent());
													}
												} 
											}
										}
									}
								}
							}						
						}
					}

					// Populate languageList
					for (Map.Entry<Integer, Concept> conceptEntry : termbase.conceptList.entrySet()) { // for each concept
						for (Map.Entry<String, Termgroup> termgroupEntry : conceptEntry.getValue().termgroups.entrySet()) { // for each termgroup
							if(termbase.inLanguageList(termgroupEntry.getKey()) < termgroupEntry.getValue().terms.size()) {
								termbase.setMaxNumber(termgroupEntry.getKey(), termgroupEntry.getValue().terms.size());
							}
						}
					}

					// Write csv
					try {
						out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filePath.toString()), "UTF-8"));

						// =================== write first line ====================
						writeCSV(out, "Entry_Created", delimiter);
						writeCSV(out, "Entry_Creator", delimiter);
						writeCSV(out, "Entry_LastModified", delimiter);
						writeCSV(out, "Entry_Modifier", delimiter);

						// Write other concept-level metadata
						for (String meta : termbase.conceptMetaList) {
							writeCSV(out, meta, delimiter);
						}

						for (Map.Entry<String, Integer> languageEntry : termbase.languageList.entrySet()) {
							writeCSV(out, languageEntry.getKey() + "_Def", delimiter);
							if (synonyms.equals("Separate columns")) {
								for(int i=0; i<languageEntry.getValue(); i++) {
									writeCSV(out, languageEntry.getKey(), delimiter);
									writeCSV(out, "Term_Info", delimiter);
									writeCSV(out, "Term_Example", delimiter);
								}
							} else if (synonyms.equals("Separated by a pipe (|)")) {
								writeCSV(out, languageEntry.getKey(), delimiter);
							}
						}
						out.write(nl);

						// ========================== write rows =======================
						for (Map.Entry<Integer, Concept> conceptEntry : termbase.conceptList.entrySet()) { // for each concept

							writeCSV(out, conceptEntry.getValue().getCreationTime(), delimiter);
							writeCSV(out, conceptEntry.getValue().getCreator(), delimiter);
							writeCSV(out, conceptEntry.getValue().getModificationTime(), delimiter);
							writeCSV(out, conceptEntry.getValue().getModifier(), delimiter);

							for (String meta : termbase.conceptMetaList) {
								writeCSV(out, conceptEntry.getValue().getMeta(meta), delimiter);
							}

							for (Map.Entry<String, Integer> languageEntry : termbase.languageList.entrySet()) { // For each language
								if (conceptEntry.getValue().termgroups.get(languageEntry.getKey()) != null) {
									writeCSV(out, conceptEntry.getValue().termgroups.get(languageEntry.getKey()).getDefinition(), delimiter);
									// Write all synonyms in one language
									if (synonyms.equals("Separate columns")) {
										for (Term storedterm : conceptEntry.getValue().termgroups.get(languageEntry.getKey()).terms) {
											writeCSV(out, storedterm.getWord(), delimiter);
											writeCSV(out, storedterm.getTermInfo(), delimiter);
											writeCSV(out, storedterm.getUsage(), delimiter);
										}
										// Fill up with empty cells
										for (int i=0; i<languageEntry.getValue()-conceptEntry.getValue().termgroups.get(languageEntry.getKey()).terms.size(); i++) {
											out.write(delimiter + delimiter + delimiter);
										}
									} else if (synonyms.equals("Separated by a pipe (|)")) {
										for (Term storedterm : conceptEntry.getValue().termgroups.get(languageEntry.getKey()).terms) {
											if (storedterm.getTermInfo().equals("NonTerm")) {
												termsWithPipes += "(NOT: " + storedterm.getWord() + ")|";
											} else {
												termsWithPipes = storedterm.getWord() + '|' + termsWithPipes;
											}
										}
										termsWithPipes = termsWithPipes.substring(0, termsWithPipes.length() - 1); // To remove last pipe
										writeCSV(out, termsWithPipes, delimiter);
										termsWithPipes = "";
									}
								} else { // If no terms in given language, fill up with empty cells
									out.write(delimiter); // For definition
									if (synonyms.equals("Separate columns")) {
										for (int i=0; i<languageEntry.getValue(); i++) {
											out.write(delimiter + delimiter + delimiter);
										}
									} else if (synonyms.equals("Separated by a pipe (|)")) {
										out.write(delimiter);
									}
								}
							}

							out.write(nl);
						}
						out.flush();
						out.close();
						JOptionPane.showMessageDialog(window, "Termbase successfully converted");
					}
					catch(IOException e)
					{
						e.printStackTrace();
						JOptionPane.showMessageDialog(window, "IOException1");
					} 
				}

				catch (IOException e) {
					JOptionPane.showMessageDialog(window, "IOException2");
				}
				catch (ParserConfigurationException e) {
					JOptionPane.showMessageDialog(window, "ParserConfigurationException");
				}
				catch (SAXException e) {
					JOptionPane.showMessageDialog(window, "SAXException");
				}
				catch (XPathExpressionException e) {
					JOptionPane.showMessageDialog(window, "XPathExpressionException");
				}
				finally {
					try {
						db.close();
					}
					catch (IOException e) {
						JOptionPane.showMessageDialog(window, "IOException3");
					}
					catch (NullPointerException e) {
						JOptionPane.showMessageDialog(window, "NullPointerException");
					}
				}
			}
		}
	}

	public static void main(String[] args) {
		
		/* If no arguments are given, start the GUI app.
		 * SDLTM files can be converted from the command line.
		 */
		if (args.length > 0) {
			File[] files = new File[args.length];
			for (int i = 0; i < args.length; ++i) {
				if (args[i].substring(args[i].lastIndexOf('.') + 1).equals("sdltm")) {
					files[i] = new File(args[i]);
				}
			}
			if (files[0] != null) {
				converttm(files);
			} else {
				System.out.println("Wrong file type");
			}
		} else {
			try {
				// Set System L&F
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				javax.swing.SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						showWindow();
					}
				});
			} 
			catch (UnsupportedLookAndFeelException e) {
				System.out.println("Unsupported look and feel");
			}
			catch (ClassNotFoundException e) {
				System.out.println("Class not found");
			}
			catch (InstantiationException e) {
				System.out.println("Instantiation exception");
			}
			catch (IllegalAccessException e) {
				System.out.println("Illegal access exception");
			}
		}
	}
}
