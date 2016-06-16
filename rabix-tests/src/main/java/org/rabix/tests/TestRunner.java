package org.rabix.tests;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.rabix.common.helper.JSONHelper;

public class TestRunner {

  private static String testDirPath;
  private static String cmd_prefix;

  private static String resultPath = "/home/travis/build/markosbg/markoBunny/rabix-backend-local/target/result.yaml";
  private static String workingdir = "/home/travis/build/markosbg/markoBunny/rabix-backend-local/target/";

  public static void main(String[] commandLineArguments) {
    testDirPath = "rabix-tests/testbacklog/";
    cmd_prefix = "./rabix.sh";
    startTestExecution();
  }

  private static void startTestExecution() {
    boolean testPassed = false;
    File dir = new File(testDirPath);
    File[] directoryListing = dir.listFiles();

    if (dir.isDirectory()) {
      if (directoryListing != null) {
        System.out.println("Extracting jar file: ");
        executeCommand("sudo tar -zxvf /home/travis/build/markosbg/markoBunny/rabix-backend-local/target/rabix-backend-local-0.0.1-SNAPSHOT-id3.tar.gz");
        executeCommand("cp -a /home/travis/build/markosbg/markoBunny/rabix-tests/testbacklog .");

        for (File child : directoryListing) {
          if (!child.toString().endsWith(".test.yaml"))
            continue;
          try {
            String currentTest = readFile(child.getAbsolutePath(), Charset.defaultCharset());
            Map<String, Object> inputSuite = JSONHelper.readMap(JSONHelper.transformToJSON(currentTest));
            Iterator entries = inputSuite.entrySet().iterator();
            while (entries.hasNext()) {
              Entry thisEntry = (Entry) entries.next();
              Object test_name = thisEntry.getKey();
              Object test = thisEntry.getValue();
              System.out
                  .println("---------------------------------------------------------------------------------------------------------------------------------------------");
              System.out.println("Running test: " + test_name + "\nWith given parameters:");
              Map<String, Map<String, LinkedHashMap>> mapTest = (Map<String, Map<String, LinkedHashMap>>) test;
              System.out.println("  app: " + mapTest.get("app"));
              System.out.println("  inputs: " + mapTest.get("inputs"));
              System.out.println("  expected: " + mapTest.get("expected"));

              String cmd = cmd_prefix + " " + mapTest.get("app") + " " + mapTest.get("inputs") + " > result.yaml";
              System.out.println("->Running cmd: " + cmd);
              executeCommand(cmd);

              File resultFile = new File(resultPath);

              String resultText = readFile(resultFile.getAbsolutePath(), Charset.defaultCharset());
              Map<String, Object> resultData = JSONHelper.readMap(JSONHelper.transformToJSON(resultText));
              System.out.println("\nGenerated result file:");
              System.out.println(resultText);
              testPassed = validateTestCase(mapTest, resultData);

              System.out.print("\nTest result: ");
              if (testPassed) {

                System.out.println(test_name + " PASSED");
                System.out
                    .println("---------------------------------------------------------------------------------------------------------------------------------------------");
              } else {
                System.out.println(test_name + " FAILED");
                System.out
                    .println("---------------------------------------------------------------------------------------------------------------------------------------------");
              }

            }

          } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
        }
      } else {
        System.out.println("Problem with provided test directory: Test directory is empty.");
      }
    } else {
      System.out.println("Problem with test directory path: Test directory path is not valid directory path.");
    }

  }

  private static boolean validateTestCase(Map<String, Map<String, LinkedHashMap>> mapTest,
      Map<String, Object> resultData) {
    String resultFileName;
    int resultFileSize;
    String resultFileClass;
    Map<String, Object> resultValues = ((Map<String, Object>) resultData.get("outfile"));
    resultFileName = resultValues.get("path").toString();
    resultFileName = resultFileName.split("/")[resultFileName.split("/").length - 1];
    resultFileSize = (int) resultValues.get("size");
    resultFileClass = resultValues.get("class").toString();
    System.out.println("Test validation:");
    System.out.println("result file name: " + resultFileName + ", expected file name: "
        + mapTest.get("expected").get("outfile").get("name"));
    System.out.println("result file size: " + resultFileSize + ", expected file size: "
        + mapTest.get("expected").get("outfile").get("size"));
    System.out.println("result file class: " + resultFileClass + ", expected file class: "
        + mapTest.get("expected").get("outfile").get("class"));

    if (resultFileName.equals(mapTest.get("expected").get("outfile").get("name"))) {
      if (resultFileSize == (int) mapTest.get("expected").get("outfile").get("size")) {
        if (resultFileClass.equals(mapTest.get("expected").get("outfile").get("class"))) {
          return true;
        } else {
          System.out.println("result and expected file class are not equal!");
        }
      } else {
        System.out.println("result and expected file size are not equal!");
      }
    } else {
      System.out.println("result and expected file name are not equal!");
    }

    return false;
  }

  public static ArrayList<String> command(final String cmdline, final String directory) {
    try {
      Process process = new ProcessBuilder(new String[] { "bash", "-c", cmdline }).redirectErrorStream(true)
          .directory(new File(directory)).start();

      ArrayList<String> output = new ArrayList<String>();
      BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
      String line = null;
      while ((line = br.readLine()) != null)
        output.add(line);

      if (0 != process.waitFor()) {
        return null;
      }

      return output;

    } catch (Exception e) {
      return null;
    }
  }

  static void executeCommand(String cmdline) {
    ArrayList<String> output = command(cmdline, workingdir);
    if (null == output)
      System.out.println("COMMAND FAILED: " + cmdline + "\n");
    else
      for (String line : output)
        System.out.println(line);
  }

  /**
   * Reads content from a file
   */
  static String readFile(String path, Charset encoding) throws IOException {
    byte[] encoded = Files.readAllBytes(Paths.get(path));
    return new String(encoded, encoding);
  }

}
