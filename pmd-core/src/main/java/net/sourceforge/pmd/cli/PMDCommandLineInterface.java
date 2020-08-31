/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.cli;

import java.util.Properties;
import java.util.stream.Collectors;

import net.sourceforge.pmd.PMD;
import net.sourceforge.pmd.PMDVersion;
import net.sourceforge.pmd.lang.Language;
import net.sourceforge.pmd.lang.LanguageRegistry;
import net.sourceforge.pmd.properties.PropertyDescriptor;
import net.sourceforge.pmd.renderers.Renderer;
import net.sourceforge.pmd.renderers.RendererFactory;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

/**
 * @author Romain Pelisse &lt;belaran@gmail.com&gt;
 *
 */
public final class PMDCommandLineInterface {

    public static final String PROG_NAME = "pmd";

    public static final String NO_EXIT_AFTER_RUN = "net.sourceforge.pmd.cli.noExit";
    public static final String STATUS_CODE_PROPERTY = "net.sourceforge.pmd.cli.status";

    public static final int NO_ERRORS_STATUS = 0;
    public static final int ERROR_STATUS = 1;
    public static final int VIOLATIONS_FOUND = 4;

    private PMDCommandLineInterface() { }

    public static PMDParameters extractParameters(PMDParameters arguments, String[] args, String progName) {
        JCommander jcommander = new JCommander(arguments);
        jcommander.setProgramName(progName);

        try {
            jcommander.parse(args);
            if (arguments.isHelp()) {
                jcommander.usage();
                System.out.println(buildUsageText());
                setStatusCodeOrExit(NO_ERRORS_STATUS);
            }
        } catch (ParameterException e) {
            jcommander.usage();
            System.out.println(buildUsageText());
            System.err.println(e.getMessage());
            setStatusCodeOrExit(ERROR_STATUS);
        }
        return arguments;
    }

    public static String buildUsageText() {
        // TODO: Externalize that to a file available within the classpath ? -
        // with a poor's man templating ?
        String fullText = PMD.EOL + "Mandatory arguments:" + PMD.EOL + "1) A java source code filename or directory"
                + PMD.EOL + "2) A report format " + PMD.EOL
                + "3) A ruleset filename or a comma-delimited string of ruleset filenames" + PMD.EOL + PMD.EOL
                + "For example: " + PMD.EOL + getWindowsLaunchCmd()
                + " -d c:\\my\\source\\code -f html -R java-unusedcode" + PMD.EOL + PMD.EOL;

        fullText += supportedVersions() + PMD.EOL;

        fullText += "Available report formats and their configuration properties are:" + PMD.EOL + getReports()
                + PMD.EOL + getExamples() + PMD.EOL + PMD.EOL + PMD.EOL;

        return fullText;
    }

    @Deprecated
    public static String buildUsageText(JCommander jcommander) {
        return buildUsageText();
    }

    private static String getExamples() {
        return getWindowsExample() + getUnixExample();
    }

    private static String getWindowsLaunchCmd() {
        final String WINDOWS_PROMPT = "C:\\>";
        final String launchCmd = "pmd-bin-" + PMDVersion.VERSION + "\\bin\\pmd.bat";
        return WINDOWS_PROMPT + launchCmd;
    }

    private static String getWindowsExample() {
        final String launchCmd = getWindowsLaunchCmd();
        final String WINDOWS_PATH_TO_CODE = "c:\\my\\source\\code ";

        return "For example on windows: " + PMD.EOL
                + launchCmd + " -dir " + WINDOWS_PATH_TO_CODE + "-format text -R rulesets/java/quickstart.xml -version 1.5 -language java -debug" + PMD.EOL
                + launchCmd + " -dir " + WINDOWS_PATH_TO_CODE + "-f xml -rulesets rulesets/java/quickstart.xml,category/java/codestyle.xml -encoding UTF-8" + PMD.EOL
                + launchCmd + " -d " + WINDOWS_PATH_TO_CODE + "-rulesets rulesets/java/quickstart.xml -auxclasspath lib\\commons-collections.jar;lib\\derby.jar" + PMD.EOL
                + launchCmd + " -d " + WINDOWS_PATH_TO_CODE + "-f html -R rulesets/java/quickstart.xml -auxclasspath file:///C:/my/classpathfile" + PMD.EOL + PMD.EOL;
    }

    private static String getUnixExample() {
        final String launchCmd = "$ pmd-bin-" + PMDVersion.VERSION + "/bin/run.sh pmd";
        return "For example on *nix: " + PMD.EOL
                + launchCmd + " -dir /home/workspace/src/main/java/code -f html -rulesets rulesets/java/quickstart.xml,category/java/codestyle.xml" + PMD.EOL
                + launchCmd + " -d ./src/main/java/code -R rulesets/java/quickstart.xml -f xslt -property xsltFilename=my-own.xsl" + PMD.EOL
                + launchCmd + " -d ./src/main/java/code -f html -R rulesets/java/quickstart.xml -auxclasspath commons-collections.jar:derby.jar" + PMD.EOL;
    }

    private static String supportedVersions() {
        return "Languages and version suported:" + PMD.EOL
                + LanguageRegistry.getLanguages().stream().map(Language::getTerseName).collect(Collectors.joining(", "))
                + PMD.EOL;
    }

    /**
     * For testing purpose only...
     *
     * @param args
     */
    public static void main(String[] args) {
        System.out.println(PMDCommandLineInterface.buildUsageText());
    }

    public static String jarName() {
        return "pmd-" + PMDVersion.VERSION + ".jar";
    }

    private static String getReports() {
        StringBuilder buf = new StringBuilder();
        for (String reportName : RendererFactory.REPORT_FORMAT_TO_RENDERER.keySet()) {
            Renderer renderer = RendererFactory.createRenderer(reportName, new Properties());
            buf.append("   ").append(reportName).append(": ");
            if (!reportName.equals(renderer.getName())) {
                buf.append(" Deprecated alias for '").append(renderer.getName()).append(PMD.EOL);
                continue;
            }
            buf.append(renderer.getDescription()).append(PMD.EOL);

            for (PropertyDescriptor<?> property : renderer.getPropertyDescriptors()) {
                buf.append("        ").append(property.name()).append(" - ");
                buf.append(property.description());
                Object deflt = property.defaultValue();
                if (deflt != null) {
                    buf.append("   default: ").append(deflt);
                }
                buf.append(PMD.EOL);
            }

        }
        return buf.toString();
    }

    public static void run(String[] args) {
        setStatusCodeOrExit(PMD.run(args));
    }

    public static void setStatusCodeOrExit(int status) {
        if (isExitAfterRunSet()) {
            System.exit(status);
        } else {
            setStatusCode(status);
        }
    }

    private static boolean isExitAfterRunSet() {
        String noExit = System.getenv(NO_EXIT_AFTER_RUN);
        if (noExit == null) {
            noExit = System.getProperty(NO_EXIT_AFTER_RUN);
        }
        return noExit == null;
    }

    private static void setStatusCode(int statusCode) {
        System.setProperty(STATUS_CODE_PROPERTY, Integer.toString(statusCode));
    }

}
