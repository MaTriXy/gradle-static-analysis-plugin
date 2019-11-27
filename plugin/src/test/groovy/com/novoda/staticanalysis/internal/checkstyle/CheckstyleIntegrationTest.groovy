package com.novoda.staticanalysis.internal.checkstyle

import com.novoda.test.Fixtures
import com.novoda.test.TestProject
import com.novoda.test.TestProjectRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

import static com.novoda.test.LogsSubject.assertThat

@RunWith(Parameterized.class)
public class CheckstyleIntegrationTest {

    public static final String DEFAULT_CONFIG = "configFile new File('${Fixtures.Checkstyle.MODULES.path}')"

    @Parameterized.Parameters(name = "{0}")
    public static Iterable<TestProjectRule> rules() {
        return [TestProjectRule.forJavaProject(), TestProjectRule.forAndroidProject()]
    }

    @Rule
    public final TestProjectRule projectRule

    public CheckstyleIntegrationTest(TestProjectRule projectRule) {
        this.projectRule = projectRule
    }

    @Test
    public void shouldFailBuildWhenCheckstyleWarningsOverTheThreshold() {
        TestProject.Result result = projectRule.newProject()
                .withSourceSet('main', Fixtures.Checkstyle.SOURCES_WITH_WARNINGS)
                .withPenalty('''{
                    maxWarnings = 0
                    maxErrors = 0
                }''')
                .withToolsConfig(checkstyle(DEFAULT_CONFIG))
                .buildAndFail('check')

        assertThat(result.logs).containsLimitExceeded(0, 1)
        assertThat(result.logs).containsCheckstyleViolations(0, 1,
                result.buildFileUrl('reports/checkstyle/main.html'))
    }

    @Test
    public void shouldFailBuildAfterSecondRunWhenCheckstyleWarningsStillOverTheThreshold() {
        def project = projectRule.newProject()
                .withSourceSet('main', Fixtures.Checkstyle.SOURCES_WITH_WARNINGS)
                .withPenalty('''{
                    maxWarnings = 0
                    maxErrors = 0
                }''')
                .withToolsConfig(checkstyle(DEFAULT_CONFIG))

        TestProject.Result result = project.buildAndFail('check')

        assertThat(result.logs).containsLimitExceeded(0, 1)
        assertThat(result.logs).containsCheckstyleViolations(0, 1,
                result.buildFileUrl('reports/checkstyle/main.html'))

        result = project.buildAndFail('check')

        assertThat(result.logs).containsLimitExceeded(0, 1)
        assertThat(result.logs).containsCheckstyleViolations(0, 1,
                result.buildFileUrl('reports/checkstyle/main.html'))
    }

    @Test
    public void shouldFailBuildWhenCheckstyleErrorsOverTheThreshold() {
        TestProject.Result result = projectRule.newProject()
                .withSourceSet('main', Fixtures.Checkstyle.SOURCES_WITH_WARNINGS)
                .withSourceSet('test', Fixtures.Checkstyle.SOURCES_WITH_ERRORS)
                .withPenalty('''{
                    maxWarnings = 100
                    maxErrors = 0
                }''')
                .withToolsConfig(checkstyle(DEFAULT_CONFIG))
                .buildAndFail('check')

        assertThat(result.logs).containsLimitExceeded(1, 0)
        assertThat(result.logs).containsCheckstyleViolations(1, 1,
                result.buildFileUrl('reports/checkstyle/main.html'),
                result.buildFileUrl('reports/checkstyle/test.html'))
    }

    @Test
    public void shouldNotFailBuildWhenNoCheckstyleWarningsOrErrorsEncounteredAndNoThresholdTrespassed() {
        TestProject.Result result = projectRule.newProject()
                .withPenalty('''{
                    maxWarnings = 0
                    maxErrors = 0
                }''')
                .withToolsConfig(checkstyle(DEFAULT_CONFIG))
                .build('check')

        assertThat(result.logs).doesNotContainLimitExceeded()
        assertThat(result.logs).doesNotContainCheckstyleViolations()
    }

    @Test
    public void shouldNotFailBuildWhenCheckstyleWarningsAndErrorsEncounteredAndNoThresholdTrespassed() {
        TestProject.Result result = projectRule.newProject()
                .withSourceSet('main', Fixtures.Checkstyle.SOURCES_WITH_WARNINGS)
                .withSourceSet('test', Fixtures.Checkstyle.SOURCES_WITH_ERRORS)
                .withPenalty('''{
                    maxWarnings = 100
                    maxErrors = 100
                }''')
                .withToolsConfig(checkstyle(DEFAULT_CONFIG))
                .build('check')

        assertThat(result.logs).doesNotContainLimitExceeded()
        assertThat(result.logs).containsCheckstyleViolations(1, 1,
                result.buildFileUrl('reports/checkstyle/main.html'),
                result.buildFileUrl('reports/checkstyle/test.html'))
    }

    @Test
    public void shouldNotFailBuildWhenCheckstyleConfiguredToNotIgnoreFailures() {
        projectRule.newProject()
                .withSourceSet('main', Fixtures.Checkstyle.SOURCES_WITH_WARNINGS)
                .withSourceSet('test', Fixtures.Checkstyle.SOURCES_WITH_ERRORS)
                .withFile(Fixtures.Checkstyle.MODULES, 'config/checkstyle/checkstyle.xml')
                .withPenalty('''{
                    maxWarnings = 1
                    maxErrors = 1
                }''')
                .withToolsConfig(checkstyle(DEFAULT_CONFIG, "ignoreFailures = false"))
                .build('check')
    }

    @Test
    public void shouldNotFailBuildWhenCheckstyleConfiguredToExcludePattern() {
        TestProject.Result result = projectRule.newProject()
                .withSourceSet('main', Fixtures.Checkstyle.SOURCES_WITH_WARNINGS)
                .withSourceSet('test', Fixtures.Checkstyle.SOURCES_WITH_ERRORS)
                .withFile(Fixtures.Checkstyle.MODULES, 'config/checkstyle/checkstyle.xml')
                .withPenalty('''{
                    maxWarnings = 1
                    maxErrors = 0
                }''')
                .withToolsConfig(checkstyle(DEFAULT_CONFIG, "exclude 'Greeter.java'"))
                .build('check')

        assertThat(result.logs).doesNotContainLimitExceeded()
        assertThat(result.logs).containsCheckstyleViolations(0, 1,
                result.buildFileUrl('reports/checkstyle/main.html'))
    }

    @Test
    public void shouldNotFailBuildWhenCheckstyleConfiguredToIgnoreFaultySourceFolder() {
        TestProject.Result result = projectRule.newProject()
                .withSourceSet('main', Fixtures.Checkstyle.SOURCES_WITH_WARNINGS)
                .withSourceSet('test', Fixtures.Checkstyle.SOURCES_WITH_ERRORS)
                .withFile(Fixtures.Checkstyle.MODULES, 'config/checkstyle/checkstyle.xml')
                .withPenalty('''{
                    maxWarnings = 1
                    maxErrors = 0
                }''')
                .withToolsConfig(checkstyle(DEFAULT_CONFIG, "exclude project.fileTree('${Fixtures.Checkstyle.SOURCES_WITH_ERRORS}')"))
                .build('check')

        assertThat(result.logs).doesNotContainLimitExceeded()
        assertThat(result.logs).containsCheckstyleViolations(0, 1,
                result.buildFileUrl('reports/checkstyle/main.html'))
    }

    @Test
    public void shouldNotFailBuildWhenCheckstyleConfiguredToIgnoreFaultySourceSet() {
        TestProject.Result result = projectRule.newProject()
                .withSourceSet('main', Fixtures.Checkstyle.SOURCES_WITH_WARNINGS)
                .withSourceSet('test', Fixtures.Checkstyle.SOURCES_WITH_ERRORS)
                .withFile(Fixtures.Checkstyle.MODULES, 'config/checkstyle/checkstyle.xml')
                .withPenalty('''{
                    maxWarnings = 1
                    maxErrors = 0
                }''')
                .withToolsConfig(checkstyle(DEFAULT_CONFIG, "exclude ${projectRule.printSourceSet('test')}.java.srcDirs"))
                .build('check')

        assertThat(result.logs).doesNotContainLimitExceeded()
        assertThat(result.logs).containsCheckstyleViolations(0, 1,
                result.buildFileUrl('reports/checkstyle/main.html'))
    }

    @Test
    public void shouldNotFailWhenCheckstyleNotConfigured() {
        TestProject.Result result = projectRule.newProject()
                .withSourceSet('main', Fixtures.Checkstyle.SOURCES_WITH_WARNINGS)
                .withSourceSet('test', Fixtures.Checkstyle.SOURCES_WITH_ERRORS)
                .withPenalty('''{
                    maxWarnings = 0
                    maxErrors = 0
                }''')
                .build('check')

        assertThat(result.logs).doesNotContainLimitExceeded()
        assertThat(result.logs).doesNotContainCheckstyleViolations()
    }

    @Test
    public void shouldNotFailBuildWhenNoCheckstyleWarningsOrErrorsEncounteredAndNegativeThresholdsProvided() {
        TestProject.Result result = projectRule.newProject()
                .withPenalty('''{
                    maxWarnings = -10
                    maxErrors = -10
                }''')
                .withToolsConfig(checkstyle(DEFAULT_CONFIG))
                .build('check')

        assertThat(result.logs).doesNotContainLimitExceeded()
        assertThat(result.logs).doesNotContainCheckstyleViolations()
    }

    @Test
    public void shouldFailBuildWhenCheckstyleWarningsOrErrorsEncounteredAndNegativeThresholdsProvided() {
        TestProject.Result result = projectRule.newProject()
                .withSourceSet('main', Fixtures.Checkstyle.SOURCES_WITH_WARNINGS)
                .withPenalty('''{
                    maxWarnings = -10
                    maxErrors = -10
                }''')
                .withToolsConfig(checkstyle(DEFAULT_CONFIG))
                .buildAndFail('check')

        assertThat(result.logs).containsLimitExceeded(0, 1)
        assertThat(result.logs).containsCheckstyleViolations(0, 1,
                result.buildFileUrl('reports/checkstyle/main.html'))
    }

    private static String checkstyle(String configFile, String... configs) {
        """checkstyle {
            ${configFile}
            ${configs.join('\n\t\t\t')}
        }"""
    }
}
