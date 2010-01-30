/*
 * $Id$
 */

package org.freemedsoftware.util;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Properties;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRExporterParameter;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.export.JExcelApiExporter;
import net.sf.jasperreports.engine.export.JRXlsExporterParameter;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.engine.xml.JRXmlLoader;

public class JasperWrapper {

	public static String VERSION = "0.2";

	private static Hashtable<String, String> arguments = new Hashtable<String, String>();

	private static HashMap<String, String> hm = new HashMap<String, String>();

	private static String[] VALID_FORMATS = { "PDF", "HTML", "XML", "XLS" };

	public JasperWrapper() {
		super();
	}

	public static void main(String[] args) {
		readParameters(args);
		if (!testParameters()) {
			showSyntax();
		} else {
			Connection conn = null;
			Properties props = new Properties();
			String format = "PDF";
			String dbDriver = "com.mysql.jdbc.Driver";

			// Parameters ok
			try {
				dbDriver = arguments.get("dbdriver");
			} catch (Exception e1) {
				dbDriver = "com.mysql.jdbc.Driver";
			}
			if (dbDriver == null) {
				dbDriver = "com.mysql.jdbc.Driver";
			}
			try {
				format = arguments.get("format").toUpperCase();
			} catch (Exception e1) {
				format = "PDF";
			}
			try {
				props.put("user", arguments.get("dbuser"));
				props.put("password", arguments.get("dbpass"));
				Class.forName(dbDriver).newInstance();
				conn = DriverManager.getConnection(arguments.get("dburl"),
						props);
			} catch (Exception e1) {
				System.out.println("JasperWrapper Database Exception occured: "
						+ e1.getMessage());
				e1.printStackTrace();
				System.exit(1);
			}
			try {
				if (conn != null) {
					JasperReport jR = null;
					if (arguments.get("report").toLowerCase().endsWith(".xml")) {
						JasperDesign jD = JRXmlLoader.load(arguments
								.get("ipath")
								+ arguments.get("report"));
						jR = JasperCompileManager.compileReport(jD);
					} else {
						jR = (JasperReport) JRLoader.loadObject(arguments
								.get("ipath")
								+ arguments.get("report"));
					}
					// Fill out report
					JasperPrint jP = JasperFillManager.fillReport(jR, hm, conn);

					String outputFileName = "/dev/stdout";
					if (format.toUpperCase().equals("PDF")) {
						if (arguments.get("opath").length() > 0) {
							outputFileName = (arguments.get("opath"))
									+ arguments.get("report").substring(
											0,
											arguments.get("report")
													.lastIndexOf(".")) + ".pdf";
						} else {
							outputFileName = "/dev/stdout";
						}
					}
					if (format.toUpperCase().equals("XML")) {
						if (arguments.get("opath").length() > 0) {
							outputFileName = (arguments.get("opath"))
									+ arguments.get("report").substring(
											0,
											arguments.get("report")
													.lastIndexOf(".")) + ".xml";
						} else {
							outputFileName = "/dev/stdout";
						}
					}
					if (format.toUpperCase().equals("HTML")) {
						if (arguments.get("opath").length() > 0) {
							outputFileName = ((String) arguments.get("opath"))
									+ ((String) arguments.get("report"))
											.substring(0, ((String) arguments
													.get("report"))
													.lastIndexOf("."))
									+ ".html";
						} else {
							outputFileName = "/dev/stdout";
						}
					}
					if (format.toUpperCase().equals("XLS")) {
						if (arguments.get("opath").length() > 0) {
							outputFileName = ((String) arguments.get("opath"))
									+ ((String) arguments.get("report"))
											.substring(0, ((String) arguments
													.get("report"))
													.lastIndexOf(".")) + ".xls";
						} else {
							outputFileName = "/dev/stdout";
						}
					}

					if (format.toUpperCase().equals("PDF")) {
						JasperExportManager.exportReportToPdfFile(jP,
								outputFileName);
					}
					if (format.toUpperCase().equals("XML")) {
						JasperExportManager.exportReportToXmlFile(jP,
								outputFileName, true);
					}
					if (format.toUpperCase().equals("HTML")) {
						JasperExportManager.exportReportToHtmlFile(jP,
								outputFileName);
					}
					if (format.toUpperCase().equals("XLS")) {
						generateXLSOutput(jP, outputFileName);
					}
				}
			} catch (Exception e2) {
				System.out.println("JasperWrapper Exception occured: "
						+ e2.getMessage());
				e2.printStackTrace();
			}
		}
	}

	private static void showSyntax() {
		// Usage screen
		System.out.println("JasperWrapper version " + VERSION);
		System.out.println("command line call for JasperReports");
		System.out.println("Original code (c) 2003 ComSoft GbR Berlin");
		System.out.println("Modified by Jeff Buchbinder under the GPL");
		System.out.println("");
		System.out
				.println("usage: JasperWrapper.jar [arguments] [[key] [value] ...]");
		System.out.println("");
		System.out.println("arguments: ( --{argument}={parameter} )");
		System.out.println("\tdbdriver    javaClass for DBConnect");
		System.out.println("\tdburl       connection String for DB");
		System.out.println("\tdbuser      user for DBConnect");
		System.out.println("\tdbpass      password for DBConnect");
		System.out.println("\treport      name report file");
		System.out.println("\tipath       input path containing all files");
		System.out.println("\topath       output path");
		System.out
				.println("\tformat      output format {PDF,HTML,XML,XLS} (defaults to PDF)");
		System.out.println("");

		System.out.println(arguments.toString());
		System.exit(1);
	}

	private static void readParameters(String[] args) {
		String hold = null;
		arguments.clear();
		for (int i = 0; i < args.length; i++) {
			if (args[i].substring(0, 2).equals("--")) {
				// parameter found
				String workpar = args[i].substring(2);
				if (workpar.indexOf("=") > 0) {
					arguments.put(workpar.substring(0, workpar.indexOf("="))
							.toLowerCase(), workpar.substring(workpar
							.indexOf("=") + 1));
				}
			} else {
				if (hold == null) {
					hold = args[i];
				} else {
					hm.put(hold, args[i]);
					hold = null;
				}
			}
		}
	}

	private static boolean testParameters() {
		boolean booValue = true;

		// if (arguments.get("dbdriver") == null) booValue = false;
		if (arguments.get("dburl") == null)
			booValue = false;
		if (arguments.get("dbuser") == null)
			booValue = false;
		if (arguments.get("dbpass") == null)
			booValue = false;
		if (arguments.get("report") == null)
			booValue = false;
		// if(arguments.get("opath")==null) booValue = false;
		if (arguments.get("ipath") == null)
			booValue = false;
		if ((arguments.get("format") != null)
				&& !Arrays.asList(VALID_FORMATS).contains(
						arguments.get("format"))) {
			booValue = false;
		}

		return booValue;
	}

	public static void generateXLSOutput(JasperPrint jasperPrint,
			String outputFileName) throws IOException, JRException {
		JExcelApiExporter exporterXLS = new JExcelApiExporter();

		exporterXLS.setParameter(JRXlsExporterParameter.JASPER_PRINT,
				jasperPrint);

		exporterXLS.setParameter(JRXlsExporterParameter.IS_DETECT_CELL_TYPE,
				Boolean.TRUE);

		exporterXLS.setParameter(
				JRXlsExporterParameter.IS_WHITE_PAGE_BACKGROUND, Boolean.FALSE);

		exporterXLS.setParameter(
				JRXlsExporterParameter.IS_REMOVE_EMPTY_SPACE_BETWEEN_ROWS,
				Boolean.TRUE);

		// If you want to export the XLS report to physical file.
		exporterXLS.setParameter(JRExporterParameter.OUTPUT_FILE_NAME,
				outputFileName);

		// This’ll allows users to directly download the XLS report without
		// having to save XLS report on server.
		// exporterXLS.setParameter(JRXlsExporterParameter.OUTPUT_STREAM,
		// resp.getOutputStream());

		exporterXLS.exportReport();
	}
}
