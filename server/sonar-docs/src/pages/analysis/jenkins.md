---
title: Jenkins
url: /analysis/jenkins/
---

SonarScanners running in Jenkins can automatically detect branches and Merge or Pull Requests in certain jobs. You don't need to explicitly pass the branch or Pull Request details.

## Analysis Prerequisites

To run project analysis with Jenkins, you need to install and configure the following Jenkins plugins _in Jenkins_:
 
- The SonarQube Scanner plugin.
- The Branch Source plugin that corresponds to your ALM (Bitbucket Server, GitHub, or GitLab) if you're analyzing multibranch pipeline jobs in [Developer Edition](https://redirect.sonarsource.com/editions/developer.html) or above. 

See the **Installing and Configuring your Jenkins plugins** section below for more information.

### Configuring Jenkins using the SonarQube tutorial

If you're using Bitbucket Server, GitHub Enterprise, GitHub.com, GitLab Self-Managed, or GitLab.com, you can easily configure and analyze your projects by following the tutorial in SonarQube. You can access the tutorial by going to your project's **Overview** page and selecting **with Jenkins** under "How do you want to analyze your repository?"

[[info]]
|See the **Installing and Configuring your Jenkins plugins** section below to set up your Jenkins plugins before going through the tutorial. 

## Installing and Configuring your Jenkins plugins

### SonarQube Scanner plugin

Click SonarQube Scanner below to expand instructions on installing and configuring the plugin.
 
[[collapse]]
| ## SonarQube Scanner
|
| [SonarQube Scanner plugin](https://plugins.jenkins.io/sonar/) version 2.11 or later is required. 
|
| 1. From the Jenkins Dashboard, navigate to **Manage Jenkins > Manage Plugins** and install the **SonarQube Scanner** plugin.
| 1. Back at the Jenkins Dashboard, navigate to **Credentials > System** from the left navigation. 
| 1. Click the **Global credentials (unrestricted)** link in the **System** table. 
| 1. Click **Add credentials** in the left navigation and add the following information:
| 	- **Kind**: Secret Text
| 	- **Scope**: Global  
| 	- **Secret**: Generate a token at **User > My Account > Security** in SonarQube, and copy and paste it here.
| 1. Click **OK**.
| 1. From the Jenkins Dashboard, navigate to **Manage Jenkins > Configure System**. 
| 1. From the **SonarQube Servers** section, click **Add SonarQube**. Add the following information:
| 	- **Name**: Give a unique name to your SonarQube instance.
| 	- **Server URL**: Your SonarQube instance URL.
| 	- **Credentials**: Select the credentials created during step 4.
| 1. Click **Save**

### Branch Source plugin
_Required to analyze multibranch pipeline jobs in [Developer Edition](https://redirect.sonarsource.com/editions/developer.html) or above_

Click your ALM below to expand the instructions on installing and configuring the Branch Source plugin.

[[collapse]]
| ## BitBucket Server
|
| [Bitbucket Branch Source plugin](https://plugins.jenkins.io/cloudbees-bitbucket-branch-source/) version 2.7 or later is required
| 
| 1. From the Jenkins Dashboard, navigate to **Manage Jenkins > Manage Plugins** and install the **Bitbucket Branch Source** plugin.
| 1. From the Jenkins Dashboard, navigate to **Manage Jenkins > Configure System**. 
| 1. From the **Bitbucket Endpoints** section, Click the **Add** drop-down menu and select **Bitbucket Server**. Add the following information:
| 	- **Name**: Give a unique name to your Bitbucket Server instance.
| 	- **Server URL**: Your Bitbucket Server instance URL.
| 	- Check **Manage hooks**
| 	- **Credentials**: In your credentials, use a Bitbucket Server personal access token with **Read** permissions.
| 	- **Webhook implementation to use**: Native	
| 1. Click **Save**.

[[collapse]]
| ## GitHub
|
| [GitHub Branch Source plugin](https://plugins.jenkins.io/github-branch-source/) version 2.7.1 or later is required
| 
| 1. From the Jenkins Dashboard, navigate to **Manage Jenkins > Manage Plugins** and install the **GitHub Branch Source** plugin.
| 1. From the Jenkins Dashboard, navigate to **Manage Jenkins > Configure System**. 
| 1. From the **GitHub** or **GitHub Enterprise Servers** section, add your GitHub server.
| 1. Click **Save**.

[[collapse]]
| ## GitLab
|
| [GitLab Branch Source plugin](https://plugins.jenkins.io/gitlab-branch-source/) version 1.5.3 or later is required
| 
| 1. From the Jenkins Dashboard, navigate to **Manage Jenkins > Manage Plugins** and install the **GitLab Branch Source** plugin.
| 1. From the Jenkins Dashboard, navigate to **Manage Jenkins > Configure System**. 
| 1. From the **GitLab** section, add your GitLab server. Make sure to check the **Manage Web Hooks** checkbox.
| 1. Click **Save**.

## Configuring Single Branch Pipeline jobs
With Community Edition, you can only analyze a single branch. For more information, see the [SonarScanner for Jenkins](/analysis/scan/sonarscanner-for-jenkins/) documentation.

## Configuring Multibranch Pipeline jobs 
 
Starting in [Developer Edition](https://redirect.sonarsource.com/editions/developer.html), you can analyze multiple branches and Pull Requests. The automatic configuration of branches and Pull Requests relies on environment variables available in Multibranch Pipeline jobs. These are set based on information exported by Jenkins plugins. 

For configuration examples, see the [SonarScanner for Jenkins](/analysis/scan/sonarscanner-for-jenkins/) documentation.

### Configuring Multibranch Pipeline jobs for Pull Request Decoration
You need to configure your Multibranch Pipeline job correctly to avoid issues with Pull Request decoration. From your Multibranch Pipeline job in Jenkins, go to **Configure > Branch Sources > Behaviors**.

For Bitbucket Server and GitHub, under **Discover pull requests from origin**, make sure **The current pull request revision** is selected.

For GitLab, under **Discover merge requests from origin**, make sure **Merging the merge request with the current target branch revision** is selected.

## Detecting changed code in Pull Requests
SonarScanners need access to a Pull Request's target branch to detect code changes in the Pull Request. If you're using a Jenkins Pull Request discovery strategy that only fetches the Pull Request and doesn't merge with the target branch, the target branch is not fetched and is not available in the local git clone for the scanner to read. 

In this case, the code highlighted as “new” in the Pull Request may be inaccurate, and you’ll see the following warning in the scanner’s log:

```
File '[name]' was detected as changed but without having changed lines
```

To fix this, either change the discovery strategy or manually fetch the target branch before running the SonarScanner. For example:

```
git fetch +refs/heads/${CHANGE_TARGET}:refs/remotes/origin/${CHANGE_TARGET}
```


