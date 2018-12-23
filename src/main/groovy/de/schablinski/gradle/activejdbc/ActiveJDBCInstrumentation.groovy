package de.schablinski.gradle.activejdbc

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction
import org.javalite.instrumentation.Instrumentation

/**
 * Gradle task for performing ActiveJDBC instrumentation on a set of compiled {@code .class} files.
 */
class ActiveJDBCInstrumentation extends DefaultTask {

    /** The directory containing class files to be instrumented. */
    String classesDir

    /** The output directory to write back classes after instrumentation. */
    String outputDir

    ActiveJDBCInstrumentation() {
        description = "Instrument compiled class files extending from 'org.javalite.activejdbc.Model'"

        classesDir = getGradleMajorVersion(project) > 3 ? project.sourceSets.main.java.outputDir.getPath()
                : project.sourceSets.main.output.classesDir.getPath()
        outputDir = classesDir
    }

    @TaskAction
    def instrument() {
        logger.info "ActiveJDBCInstrumentation.instrument"
        Instrumentation instrumentation = new Instrumentation()
        instrumentation.outputDirectory = outputDir ?: classesDir

        def rootLoader = this.class.classLoader.rootLoader
        addUrlIfNotPresent rootLoader, classesDir
        addUrlIfNotPresent Instrumentation.class.classLoader, classesDir

        instrumentation.instrument()
    }

    // from the Griffon ActiveJDBC plugin
    private def addUrlIfNotPresent(to, what) {
        if (!to || !what) {
            return
        }
        def urls = to.URLs.toList()
        switch (what.class) {
            case URL: what = new File(what.toURI()); break
            case String: what = new File(what); break
            case GString: what = new File(what.toString()); break
            case File: break; // ok
            default:
                println "Don't know how to deal with $what as it is not an URL nor a File"
                System.exit(1)
        }

        if (what.directory && !what.exists()) {
            what.mkdirs()
        }
        def url = what.toURI().toURL()
        if (!urls.contains(url) && (what.directory || !urls.find { it.path.endsWith(what.name) })) {
            to.addURL(url)
        }
    }

    private static int getGradleMajorVersion(Project project)
    {
        String gradleVersion = project.getGradle().getGradleVersion()
        Integer.valueOf(gradleVersion.substring(0, gradleVersion.indexOf(".")))
    }
}
