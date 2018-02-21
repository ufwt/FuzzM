package jfuzz.solver;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jfuzz.lustre.evaluation.FunctionLookupEV;
import jfuzz.lustre.evaluation.FunctionSignature;
import jfuzz.util.Debug;
import jfuzz.util.ID;
import jfuzz.util.Rat;
import jfuzz.util.RatSignal;
import jfuzz.util.TypedName;
import jkind.lustre.NamedType;
import jkind.lustre.Program;
import jkind.lustre.VarDecl;
import jkind.util.BigFraction;

/**
 * The solver class is our interface to JKind.
 *
 */
public class Solver {

    ProcessBuilder pb;
    Path workingDirectory;
    String modelName;
    List<VarDecl> modelInputs;
    List<SolverName> userSolvers;
    public SolverName slver = SolverName.randomSolver();
    String javaExec;
    String jkindJarPath;
    String pythonExec;
    String xml2vectorPath;

    public Solver(List<SolverName> userSolvers, Path workingDirectory, String modelName, List<VarDecl> inputs) {
        pb = new ProcessBuilder();
        pb.directory(workingDirectory.toFile());
        this.workingDirectory = workingDirectory;
        this.modelName = modelName;
        this.modelInputs = inputs;
        this.userSolvers = userSolvers;
        javaExec = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        try {
            this.jkindJarPath = findFile("jkind.jar").getAbsolutePath();
            this.pythonExec = getExecutable("python");
            this.xml2vectorPath = workingDirectory.resolve("xml2vector.py").toAbsolutePath().toString();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void randomSolver() {
        slver = SolverName.randomSolver(userSolvers);
    }

    private String getExecutable(String execName) throws FileNotFoundException {
        String home = System.getenv(execName.toUpperCase() + "_PATH");
        if (home != null) {
            return home + File.separator + execName;
        }
        File execFile = findFile(execName);
        return execFile.toString();
    }

    private boolean runCommand(List<String> command, String location) {
        pb.command(command);
        int exit = -1;
        boolean done = false;
        try {
            Process proc = pb.start();

            BufferedReader stdOut = null;
            BufferedReader stdErr = null;

            stdOut = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            stdErr = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
            while (!done) {
                try {
                    exit = proc.waitFor();
                    done = true;
                } catch (InterruptedException e) {
                    System.out.println(ID.location() + "Solver : INTERRUPTED");
                }
            }
            String line;
            if (!done) {
                System.err.println(location + "Command(" + command + ") failed to complete.");
            }
            if (exit != 0) {
                System.err.println(location + "Command(" + command + ") exited with code: " + Integer.toString(exit));
            }
            if (Debug.isEnabled()) {
                while ((line = stdOut.readLine()) != null) {
                    System.out.println(location + "STDOUT : " + line);
                }
            }
            while ((line = stdErr.readLine()) != null) {
                System.err.println(location + "STDERR : " + line);
            }
            stdOut.close();
            stdErr.close();
        } catch (Throwable t) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            t.printStackTrace(pw);
            // We print to stderr but don't exit ..
            System.err.println(sw.toString());
            pw.close();
        }
        return (done && (exit == 0));
    }

    private void removeWarningLines(File ifile, File ofile) {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(ofile));
            BufferedReader br = new BufferedReader(new FileReader(ifile));
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.startsWith("Warning")) {
                    bw.write(line);
                    bw.newLine();
                }

            }
            bw.close();
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

    }

    public File findFile(String filename) throws FileNotFoundException {
        String systemPath = System.getenv("PATH");
        String[] paths = systemPath.split(File.pathSeparator);

        for (String path : paths) {
            File testFile = new File(path + File.separator + filename);
            if (testFile.exists()) {
                return testFile;
            }
        }

        throw new FileNotFoundException("Unable to find file: " + filename + " in " + Arrays.toString(paths));
    }

    public boolean runSolver(File ofile) {
        SolverName slver = (this.slver == null) ? SolverName.randomSolver() : this.slver;
        String[] jkindArgs = { "-jkind", "-xml", "-solver", slver.toString(), "--no_slicing", ofile.getAbsolutePath() };
        List<String> jkindCommand = new ArrayList<>();
        jkindCommand.add(javaExec);
        jkindCommand.add("-jar");
        jkindCommand.add(jkindJarPath);
        jkindCommand.addAll(Arrays.asList(jkindArgs));

        List<String> vectorCommand = new ArrayList<>();
        vectorCommand.add(pythonExec);
        vectorCommand.add(xml2vectorPath);
        vectorCommand.add(ofile.getAbsolutePath().substring(0, ofile.getAbsolutePath().length() - 4));

        if (runCommand(jkindCommand, ID.location())) {
            File lusxmlFile = new File(ofile.getAbsolutePath() + ".xml");
            File xmlFile = new File(ofile.getAbsolutePath().substring(0, ofile.getAbsolutePath().length() - 3) + "xml");
            removeWarningLines(lusxmlFile, xmlFile);
            if (runCommand(vectorCommand, ID.location()))
                return true;
        }

        return false;
    }

    public SolverResults invoke(Program program) {
        Map<String, List<String>> res = new HashMap<String, List<String>>();
        FunctionLookupEV fnLookup = new FunctionLookupEV(new FunctionSignature(program.functions));
        Map<Integer, String> loc = null;

        // System.out.println("model : " + model);
        // assert(workingDirectory != null);
        // assert(modelName != null);
        File ofile = workingDirectory.resolve(modelName + ".lus").toFile();
        System.out.println(ID.location() + "Writing file : " + ofile.getAbsolutePath());
        BufferedWriter output;
        try {
            output = new BufferedWriter(new FileWriter(ofile));
            output.write(program.toString());
            output.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        if (runSolver(ofile)) {
            try {
                File ifile = workingDirectory.resolve(modelName + ".inputs").toFile();
                File vfile = workingDirectory.resolve(modelName + ".vector").toFile();
                File ffile = workingDirectory.resolve(modelName + ".funs").toFile();
                BufferedReader ibr = new BufferedReader(new FileReader(ifile));
                String line = null;
                line = ibr.readLine();
                assert (line != null);
                String[] inputNames = line.split(" ");
                loc = new HashMap<Integer, String>();
                int off = 0;
                // The .inputs file should be generated by the fuzzer.
                // The input names it contains should already be consistent with the model
                for (String name : inputNames) {
                    res.put(name, new ArrayList<String>());
                    loc.put(off++, name);
                }
                ibr.close();
                BufferedReader vbr = new BufferedReader(new FileReader(vfile));
                while ((line = vbr.readLine()) != null) {
                    String[] values = line.split(" ");
                    off = 0;
                    for (String value : values) {
                        res.get(loc.get(off++)).add(value);
                    }
                }
                vbr.close();
                BufferedReader fbr = new BufferedReader(new FileReader(ffile));
                while ((line = fbr.readLine()) != null) {
                    fnLookup.addEncodedString(line);
                }
                fbr.close();
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }
        RatSignal sln = new RatSignal();
        // res is a mapping from single variables to their values over time:
        // a: [1,2]
        // b: [0,3]
        // We need a temporal sequence of value vectors:
        // [(a:1,b:0),(a:2,b:3)]
        if (!res.isEmpty()) {
            for (VarDecl vd : modelInputs) {
                String var = vd.id;
                List<String> v = res.get(var);
                for (int time = 0; time < v.size(); time++) {
                    BigFraction decoded = Rat.BigFractionFromString(v.get(time));
                    sln.put(time, new TypedName(var, (NamedType) vd.type), decoded);
                }
            }
        }
        return new SolverResults(sln, fnLookup);
    }

}
