# JASPERWRAPPER

JasperWrapper is a Java "wrapper" around the JasperReports library, allowing
JasperReports queries and formats to be executed from other environments. Once
assembled, JasperWrapper only requires a working JRE and its compiled JAR
file.

## PREREQUISITES

 * JRE 1.6+
 * Gradle

## BUILDING

``gradle jar`` will generate a jar file in `target/`.

## USAGE

```
usage: JasperWrapper.jar [arguments] [[key] [value] ...]

arguments: ( --{argument}={parameter} )
	dbdriver    javaClass for DBConnect
	dburl       connection String for DB
	dbuser      user for DBConnect
	dbpass      password for DBConnect
	report      name report file
	ipath       input path containing all files
	opath       output path
	oprefix     output prefix, optional
	oimgurl     output image url base
	param       add parameter
	format      output format {PDF,HTML,XML,XLS} (defaults to PDF)
	paramformat add parameter format {int,long,string,date,double}
```

