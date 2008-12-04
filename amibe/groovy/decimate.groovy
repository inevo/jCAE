/**
 * Decimate a mesh
 */
import org.jcae.mesh.amibe.ds.Mesh
import org.jcae.mesh.amibe.algos3d.*
import org.jcae.mesh.amibe.traits.MeshTraitsBuilder
import org.jcae.mesh.xmldata.*
import java.lang.reflect.Constructor;
import org.apache.commons.cli.*;

cmd=["decimate", "Decimate a mesh"]
usage="<xmlDir>"

void usage(int rc, Options options)
{
	HelpFormatter formatter = new HelpFormatter();
	formatter.printHelp("amibebatch "+cmd[0].trim()+" [OPTIONS] "+usage, cmd[1], options, "");
	System.exit(rc);
}

Options options = new Options();
options.addOption(
	OptionBuilder.hasArg(false)
		.withDescription("usage information")
		.withLongOpt("help")
		.create('h'));
options.addOption(
	OptionBuilder.withArgName("VALUE").hasArg()
		.withDescription("geometry error allowed when decimating")
		.withLongOpt("tolerance")
		.create('t'));
options.addOption(
	OptionBuilder.withArgName("VALUE").hasArg()
		.withDescription("Decimate free edges whose length is smaller than tolerance."+
			" -t value is used if not specified. (for LengthDecimateHalfEdge only)")
		.withLongOpt("freeEdgeTol")
		.create('f'));
options.addOption(
	OptionBuilder.withArgName("NUMBER").hasArg()
		.withDescription("stops iterations when mesh contains this number of triangles")
		.withLongOpt("targetTriangles")
		.create('n'));
options.addOption(
	OptionBuilder.withArgName("VALUE").hasArg()
		.withDescription("no edges longer than this value are created")
		.withLongOpt("maxlength")
		.create('m'));
options.addOption(
	OptionBuilder.hasArg(false)
		.withDescription("removes only free edges (for LengthDecimateHalfEdge only)")
		.withLongOpt("freeEdgeOnly")
		.create('O'));
options.addOption(
	OptionBuilder.withArgName("CLASS").hasArg()
		.withDescription("decimation algorithm (default: LengthDecimateHalfEdge)")
		.withLongOpt("algorithm")
		.create('a'));
options.addOption(
	OptionBuilder.hasArg(false)
		.withDescription("lists all available decimation algorithms")
		.withLongOpt("list-algorithm")
		.create('A'));
options.addOption(
	OptionBuilder.withArgName("DIR").hasArg()
		.withDescription("writes decimated mesh into this directory")
		.withLongOpt("output")
		.create('o'));

CommandLineParser parser = new GnuParser();
CommandLine cmd = parser.parse(options, args, true);
if (cmd.hasOption('h'))
	usage(0, options);
if (cmd.hasOption('A'))
{
	println("Available algorithms for decimation:")
	println("    LengthDecimateHalfEdge")
	println("    QEMDecimateHalfEdge")
	System.exit(0);
}

String [] remaining = cmd.getArgs();
if (remaining.length != 1)
	usage(1, options);
if (cmd.hasOption('t') == cmd.hasOption('n'))
{
	println("One and only one occurrence of -t or -n must be passed on command-line")
	System.exit(1);
}

String xmldir = remaining[0]
String outDir = cmd.getOptionValue('o', xmldir)

HashMap<String, String> algoOptions = new HashMap<String, String>();
if (cmd.hasOption('t'))
	algoOptions.put("size", cmd.getOptionValue('t'));
else if (cmd.hasOption('n'))
	algoOptions.put("maxtriangles", cmd.getOptionValue('n'));
if (cmd.hasOption('O'))
	algoOptions.put("freeEdgeOnly", "true");
if (cmd.hasOption('f'))
	algoOptions.put("freeEdgeTol", cmd.getOptionValue('f'));
if (cmd.hasOption('m'))
	algoOptions.put("maxlength", cmd.getOptionValue('m'));
String algo=cmd.getOptionValue('a', "LengthDecimateHalfEdge");
Constructor cons = Class.forName("org.jcae.mesh.amibe.algos3d."+algo).getConstructor(Mesh.class, Map.class);

Mesh mesh = new Mesh()
MeshReader.readObject3D(mesh, xmldir)

try
{
	cons.newInstance(mesh, algoOptions).compute();
	MeshWriter.writeObject3D(mesh, outDir, null)
}
catch(Exception ex)
{
	ex.printStackTrace();
}


