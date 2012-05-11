/*
 * $Id$
 */

package org.freemedsoftware.util;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
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
import net.sf.jasperreports.engine.export.JRHtmlExporter;
import net.sf.jasperreports.engine.export.JRHtmlExporterParameter;
import net.sf.jasperreports.engine.export.JRXlsExporterParameter;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.engine.xml.JRXmlLoader;

public class JasperWrapper {

	public static String VERSION = "0.6.1";

	private static Hashtable<String, String> arguments = new Hashtable<String, String>();

	private static List<String> reportParameters = new ArrayList<String>();

	private static List<String> reportFormat = new ArrayList<String>();

	private static HashMap<String, Object> hm = new HashMap<String, Object>();

	private static String[] VALID_FORMATS = { "PDF", "HTML", "XML", "XLS" };

	private static SimpleDateFormat dateFormat = new SimpleDateFormat(
			"yyyy-MM-dd");

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
				System.err.println("JasperWrapper Database Exception occured: "
						+ e1.getMessage());
				e1.printStackTrace();
				System.exit(1);
			}
			try {
				if (conn != null) {
					// Grab the report, cached or not
					JasperReport jR = cachedLoadReport(arguments.get("ipath"),
							arguments.get("opath"), arguments.get("report"));

					// Create parameters in hm
					if (reportParameters.size() > 0) {
						for (int iter = 0; iter < reportParameters.size(); iter++) {

							String pName = "param"
									+ new Integer(iter).toString();
							String pFormat = reportFormat.get(iter);
							String type = "";
							if (pFormat == null
									|| pFormat.equalsIgnoreCase("string")) {
								type = "java.lang.String";
								hm.put(pName, reportParameters.get(iter));
							} else if (pFormat.equalsIgnoreCase("int")
									|| pFormat.equalsIgnoreCase("integer")) {
								type = "java.lang.Integer";
								hm.put(pName, Integer.parseInt(reportParameters
										.get(iter)));
							} else if (pFormat.equalsIgnoreCase("long")) {
								type = "java.lang.Long";
								hm.put(pName, Long.parseLong(reportParameters
										.get(iter)));
							} else if (pFormat.equalsIgnoreCase("double")) {
								type = "java.lang.Double";
								hm.put(pName,
										Double.parseDouble(reportParameters
												.get(iter)));
							} else if (pFormat.equalsIgnoreCase("date")) {
								type = "java.util.Date";
								hm.put(pName, dateFormat.parse(reportParameters
										.get(iter)));
							} else {
								System.err
										.println("["
												+ pName
												+ "]"
												+ " type unknown, falling back to string");
								type = "java.lang.String";
								hm.put(pName, reportParameters.get(iter));
							}
							System.err.println("[" + pName + "/" + pFormat
									+ "] " + reportParameters.get(iter));
						}
					}

					// Push input path into report parameters
					hm.put("ipath", arguments.get("ipath"));

					// Fill out report
					System.err.println("Filling report");
					JasperPrint jP = JasperFillManager.fillReport(jR, hm, conn);

					String outputPrefix = (arguments.get("opath"))
							+ arguments.get("report").substring(0,
									arguments.get("report").lastIndexOf("."));
					if (arguments.get("oprefix") != null) {
						outputPrefix = arguments.get("opath")
								+ arguments.get("oprefix");
					}

					String outputFileName = "/dev/stdout";
					if (format.toUpperCase().equals("PDF")) {
						if (arguments.get("opath").length() > 0) {
							outputFileName = outputPrefix + ".pdf";
						} else {
							outputFileName = "/dev/stdout";
						}
					}
					if (format.toUpperCase().equals("XML")) {
						if (arguments.get("opath").length() > 0) {
							outputFileName = outputPrefix + ".xml";
						} else {
							outputFileName = "/dev/stdout";
						}
					}
					if (format.toUpperCase().equals("HTML")) {
						if (arguments.get("opath").length() > 0) {
							outputFileName = outputPrefix + ".html";
						} else {
							outputFileName = "/dev/stdout";
						}
					}
					if (format.toUpperCase().equals("XLS")) {
						if (arguments.get("opath").length() > 0) {
							outputFileName = outputPrefix + ".xls";
						} else {
							outputFileName = "/dev/stdout";
						}
					}

					System.err.println("Output file is " + outputFileName);

					if (format.toUpperCase().equals("PDF")) {
						JasperExportManager.exportReportToPdfFile(jP,
								outputFileName);
					} else if (format.toUpperCase().equals("XML")) {
						JasperExportManager.exportReportToXmlFile(jP,
								outputFileName, true);
					} else if (format.toUpperCase().equals("HTML")) {
						generateHTMLOutput(jP, outputFileName);
					} else if (format.toUpperCase().equals("XLS")) {
						generateXLSOutput(jP, outputFileName);
					} else {
						System.err.println("No valid format given.");
					}
				}
			} catch (Exception e2) {
				System.err.println("JasperWrapper Exception occured: "
						+ e2.getMessage());
				e2.printStackTrace();
			}
		}
	}

	private static JasperReport cachedLoadReport(String inputPath,
			String outputPath, String reportName) throws JRException {
		JasperReport jR = null;
		String reportFile = inputPath + reportName;
		String cachedFile = outputPath + reportName + ".cache";
		if (reportName.toLowerCase().endsWith(".jrxml")) {
			// Check for cached report
			if (new File(cachedFile).exists()
					&& (new File(cachedFile).lastModified() > new File(
							reportFile).lastModified())) {
				// Read and return
				System.err.println("Loading compiled report " + reportName
						+ " from " + cachedFile);
				jR = (JasperReport) JRLoader.loadObject(cachedFile);
			} else {
				System.err.println("Loading report " + reportName);
				JasperDesign jD = JRXmlLoader.load(reportFile);
				System.out.println("Compiling report " + reportName + " to "
						+ cachedFile);
				JasperCompileManager.compileReportToFile(jD, cachedFile);
				System.err.println("Loading compiled report " + reportName
						+ " from " + cachedFile);
				jR = (JasperReport) JRLoader.loadObject(cachedFile);
			}
		} else {
			System.err.println("Loading compiled report " + reportName);
			jR = (JasperReport) JRLoader.loadObject(reportFile);
		}
		return jR;
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
		System.out.println("\toprefix     output prefix, optional");
		System.out.println("\toimgurl     output image url base");
		System.out.println("\tparam       add parameter");
		System.out
				.println("\tformat      output format {PDF,HTML,XML,XLS} (defaults to PDF)");
		System.out
				.println("\tparamformat add parameter format {int,long,string,date,double}");
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
					String k = workpar.substring(0, workpar.indexOf("="))
							.toLowerCase();
					String v = workpar.substring(workpar.indexOf("=") + 1);
					if (k.equals("param")) {
						// Report parameters add to stack
						reportParameters.add(v);
					} else if (k.equals("paramformat")) {
						// Report parameter format add to stack
						reportFormat.add(v);
					} else {
						// All else goes into argument bin
						arguments.put(k, v);
					}
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

	public static void generateHTMLOutput(JasperPrint jasperPrint,
			String outputFileName) throws IOException, JRException {
		JRHtmlExporter exporter = new JRHtmlExporter();

		exporter.setParameter(JRExporterParameter.JASPER_PRINT, jasperPrint);
		exporter.setParameter(JRHtmlExporterParameter.HTML_FOOTER, " ");

		// If "oimgurl" is specified, set image URL base
		if (arguments.containsKey("oimgurl")) {
			exporter.setParameter(JRHtmlExporterParameter.IMAGES_URI, arguments
					.get("oimgurl"));
			exporter.setParameter(
					JRHtmlExporterParameter.IS_USING_IMAGES_TO_ALIGN,
					Boolean.TRUE);
		} else {
			exporter.setParameter(
					JRHtmlExporterParameter.IS_USING_IMAGES_TO_ALIGN,
					Boolean.FALSE);
		}

		// If you want to export the XLS report to physical file.
		exporter.setParameter(JRExporterParameter.OUTPUT_FILE_NAME,
				outputFileName);

		exporter.exportReport();
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
