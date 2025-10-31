package ca.mcgill.story_testing;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.FILTER_TAGS_PROPERTY_NAME;

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("ca/mcgill/story_testing")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "ca.mcgill.story_testing.stepdefs")
@ConfigurationParameter(key = FILTER_TAGS_PROPERTY_NAME, value = "@focus")
public class CucumberTestRunner {
}
