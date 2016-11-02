/* JIRA Jenkins Integration (c) by Yagnanarayana Dande
*
* JIRA Jenkins Integration is licensed under a
* Creative Commons Attribution 4.0 International License.
*
* You should have received a copy of the license along with this
* work. If not, see <http://creativecommons.org/licenses/by/4.0/>.
*/

package com.carteblanche.jiraJenkinsIntegration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testng.IReporter;
import org.testng.ISuite;
import org.testng.ISuiteResult;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.TestNG;
import org.testng.xml.XmlClass;
import org.testng.xml.XmlInclude;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;

import net.rcarz.jiraclient.BasicCredentials;
import net.rcarz.jiraclient.Field;
import net.rcarz.jiraclient.Issue;
import net.rcarz.jiraclient.JiraClient;
import net.rcarz.jiraclient.JiraException;

public class JiraJenkinsTestNG implements IReporter{

	ArrayList<String> failedTests = new ArrayList<String>();
	Collection<String> passedTests = new ArrayList<String>();
	Collection<String> skippedTests = new ArrayList<String>();
	
	BasicCredentials creds = new BasicCredentials("username", "password");
	JiraClient jira = new JiraClient("https://url", creds);
	String filter = "filter=10010";
	String customField = "customfield_10030";
	
    public static void main(String[] args) {
    	JiraJenkinsTestNG jiraTest = new JiraJenkinsTestNG();
    	jiraTest.runTests();
    
    }
    	
    public void runTests(){
        
        TestNG testNG = new TestNG();
        XmlSuite suite = new XmlSuite();
        suite.setName( "Automated Suite" );
        XmlTest xmlTest = new XmlTest(suite);
        xmlTest.setName( "Autmation Test" );
        xmlTest.setVerbose( 0 );
        List<Class> ListenerClasses = new ArrayList<Class>();
        ListenerClasses.add( com.carteblanche.jiraJenkinsIntegration.JiraJenkinsTestNG.class);
        testNG.setListenerClasses(ListenerClasses);
        try {
        	HashMap<String,ArrayList<String>> map = new HashMap<String,ArrayList<String>>();
            /* Retrieve issue TEST-123 from JIRA. We'll get an exception if this fails. */
            Issue.SearchResult sr = jira.searchIssues(filter);
            System.out.println("Total number of Tests to be run: " + sr.total);
            for (Issue i : sr.issues){
                /* Pretend customfield_1234 is a text field. Get the raw field value... */
                Object cfvalue = i.getField(customField);
                
                /* ... Convert it to a string and then print the value. */
                String[] cfstrings = Field.getString(cfvalue).trim().split("#");
                if(cfstrings.length == 2){
                	ArrayList<String> methods = map.get(cfstrings[0]);
                	if(null == methods){
                		methods = new ArrayList<String>();
                		methods.add(cfstrings[1]);
                	}else{
                		methods.add(cfstrings[1]);
                	}
                	map.put(cfstrings[0], methods);
                }
            }
            
            for (Map.Entry<String,ArrayList<String>> entry : map.entrySet()) {
            	  String key = entry.getKey();
            	  ArrayList<String> methods = entry.getValue();
            	  XmlClass xmlClass = new XmlClass(key);
                  xmlTest.getClasses().add(xmlClass);
                  for(String method: methods){
                	  XmlInclude xmlInclude = new XmlInclude(method);
                	  xmlClass.getIncludedMethods().add(xmlInclude);
                  }
            }
            
           
            
            System.out.println("Running Tests using command ..");
            List<XmlTest> tests = suite.getTests();
            for(XmlTest test: tests){
            	List<XmlClass> classes = test.getClasses();
            	for(XmlClass xmlClass: classes){
            		List<XmlInclude> methods = xmlClass.getIncludedMethods();
            		for(XmlInclude xmlInclude : methods){
            			System.out.println("Class: "+xmlClass.getName()+" Method: "+xmlInclude.getName() );
            		}
            	}
            }
            
            
            testNG.setXmlSuites( Arrays.asList( suite ) );
            testNG.setUseDefaultListeners( false );
            testNG.run();
            
        } catch (JiraException ex) {
            System.err.println(ex.getMessage());

            if (ex.getCause() != null)
                System.err.println(ex.getCause().getMessage());
        }
    }

	@Override
	public void generateReport(List<XmlSuite> xmlSuites, List<ISuite> suites, String outputDirectory) {
		 //Iterating over each suite included in the test
		try{
			 HashMap<String,String> mapOfTCAndMethods = new HashMap<String,String>();
			Issue.SearchResult srlt = jira.searchIssues(filter);
	        System.out.println("Total number of Tests to be run: " + srlt.total);
	        for (Issue i : srlt.issues){
	            /* Pretend customfield_1234 is a text field. Get the raw field value... */
	            Object cfvalue = i.getField(customField);
	            
	            /* ... Convert it to a string and then print the value. */
	            String[] cfstrings = Field.getString(cfvalue).trim().split("#");
	            if(cfstrings.length == 2){
	            	mapOfTCAndMethods.put(Field.getString(cfvalue).trim(),i.getKey());
	            }
	        }
        for (ISuite suite : suites) {
            //Following code gets the suite name
            String suiteName = suite.getName();
	    //Getting the results for the said suite
	    Map<String,ISuiteResult> suiteResults = suite.getResults();
	    for (ISuiteResult sr : suiteResults.values()) {
	        ITestContext tc = sr.getTestContext();
	        System.out.println("Passed tests for suite '" + suiteName +
	             "' is:" + tc.getPassedTests().getAllResults().size());
	        System.out.println("Passed Tests : ");
	        for (ITestResult s :  tc.getPassedTests().getAllResults()) {
	        	System.out.println("Marking "+mapOfTCAndMethods.get(s.getTestClass().getName()+"#"+s.getMethod().getMethodName())+" as Passed ...");
            	Issue issue = jira.getIssue(mapOfTCAndMethods.get(s.getTestClass().getName()+"#"+s.getMethod().getMethodName()));
            	issue.transition().execute("Pass");
            	issue.addComment("Ran Test Case on Jenkins at "+System.currentTimeMillis());
	        }
	        System.out.println("Failed Tests : ");
	        for (ITestResult s :  tc.getFailedTests().getAllResults()) {
	        	System.out.println("Marking "+mapOfTCAndMethods.get(s.getTestClass().getName()+"#"+s.getMethod().getMethodName())+" as Failed ...");
            	Issue issue = jira.getIssue(mapOfTCAndMethods.get(s.getTestClass().getName()+"#"+s.getMethod().getMethodName()));
            	issue.transition().execute("Fail");
            	issue.addComment("Ran Test Case on Jenkins at "+System.currentTimeMillis());
	        }
	        System.out.println("Failed tests for suite '" + suiteName +
	             "' is:" + 
	             tc.getFailedTests().getAllResults().size());
	        System.out.println("Skipped tests for suite '" + suiteName +
	             "' is:" + 
	             tc.getSkippedTests().getAllResults().size());
	      }
        }
		}catch(Exception e){
			e.printStackTrace();
		}
		
	}
}