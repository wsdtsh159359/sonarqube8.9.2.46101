---
title: SonarScanner for Maven
url: /analysis/scan/sonarscanner-for-maven/
---

<!-- static -->
<update-center updatecenterkey="scannermaven"></update-center>
<!-- /static -->
<!-- embedded -->
[[info]]
| See the [online documentation](https://redirect.sonarsource.com/doc/download-scanner-maven.html) to get more details on the latest version of the scanner and how to download it.
<!-- /embedded -->

The SonarScanner for Maven is recommended as the default scanner for Maven projects.

The ability to execute the SonarQube analysis via a regular Maven goal makes it available anywhere Maven is available (developer build, CI server, etc.), without the need to manually download, setup, and maintain a SonarQube Runner installation. The Maven build already has much of the information needed for SonarQube to successfully analyze a project. By preconfiguring the analysis based on that information, the need for manual configuration is reduced significantly. 

## Prerequisites
* Maven 3.x
* At least the minimal version of Java supported by your SonarQube server is in use 

## Global Settings 

Edit the [settings.xml](http://maven.apache.org/settings.html) file, located in `$MAVEN_HOME/conf` or `~/.m2`, to set the plugin prefix and optionally the SonarQube server URL.

Example:
```
<settings>
    <pluginGroups>
        <pluginGroup>org.sonarsource.scanner.maven</pluginGroup>
    </pluginGroups>
    <profiles>
        <profile>
            <id>sonar</id>
            <activation>
                <activeByDefault>true</activeByDefault>
            </activation>
            <properties>
                <!-- Optional URL to server. Default value is http://localhost:9000 -->
                <sonar.host.url>
                  http://myserver:9000
                </sonar.host.url>
            </properties>
        </profile>
     </profiles>
</settings>
```

## Analyzing
Analyzing a Maven project consists of running a Maven goal: `sonar:sonar` from the directory that holds the main project `pom.xml`. You need to pass an [authentication token](/user-guide/user-token/) using the `sonar.login` property in your command line.

```
mvn clean verify sonar:sonar -Dsonar.login=myAuthenticationToken
```

In some situations you may want to run the `sonar:sonar` goal as a dedicated step. Be sure to use `install` as first step for multi-module projects
```
mvn clean install
mvn sonar:sonar -Dsonar.login=myAuthenticationToken
```

To specify the version of sonar-maven-plugin instead of using the latest:
```
mvn org.sonarsource.scanner.maven:sonar-maven-plugin:3.7.0.1746:sonar
```

To get coverage information, you'll need to generate the coverage report before the analysis. 

## Configuring Analysis
Most analysis properties will be read from your project. If you would like override the default values of specify additional parameters, configure the parameter names found on the [Analysis Parameters](/analysis/analysis-parameters/) page in the `<properties>` section of your pom.xml like this:
```
<properties>
  <sonar.buildString> [...] </sonar.buildString>
</properties>
 ```

## Sample Project
To help you get started, a simple project sample is available here: https://github.com/SonarSource/sonar-scanning-examples/tree/master/sonarqube-scanner-maven

## Excluding a module from analysis
* define property `<sonar.skip>true</sonar.skip>` in the `pom.xml` of the module you want to exclude
* use build profiles to exclude some module (like for integration tests)
* use Advanced Reactor Options (such as "-pl"). For example `mvn sonar:sonar -pl !module2`

## How to Fix Version of Maven Plugin
It is recommended to lock down versions of Maven plugins:
```
<build>
  <pluginManagement>
    <plugins>
      <plugin>
        <groupId>org.sonarsource.scanner.maven</groupId>
        <artifactId>sonar-maven-plugin</artifactId>
        <version>3.7.0.1746</version>
      </plugin>
    </plugins>
  </pluginManagement>
</build>
```

## Troubleshooting
**If you get a java.lang.OutOfMemoryError**  
Set the `MAVEN_OPTS` environment variable, like this in *nix environments:
```
export MAVEN_OPTS="-Xmx512m"
```
In Windows environments, avoid the double-quotes, since they get misinterpreted.
```
set MAVEN_OPTS=-Xmx512m
```
