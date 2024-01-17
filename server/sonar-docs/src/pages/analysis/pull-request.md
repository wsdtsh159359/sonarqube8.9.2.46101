---
title: Pull Request Analysis
url: /analysis/pull-request/
---

_Pull Request analysis is available starting in [Developer Edition](https://redirect.sonarsource.com/editions/developer.html)._

You can see your Pull Requests in SonarQube from the Branches and Pull Requests dropdown menu of your project.  

Pull Request analysis shows your Pull Request's Quality Gate and analysis in the SonarQube interface. This analysis shows new issues introduced by the Pull Request before merging with the target branch:

![Pull Request Analysis.](/images/pranalysis.png)

## Prerequisites

Before analyzing your Pull Requests, make sure the Pull Request branch is checked out. Avoid any attempt at previewing the merge or actions involving your main branch.

## Pull request decoration
You can also add pull request decoration that shows the Pull Request analysis and Quality Gate directly in your ALM's interface. To set up pull request decoration, see the ALM integration page that corresponds with your ALM:
- [GitHub Enterprise and GitHub.com](/analysis/github-integration/)
- [GitLab Self-Managed and GitLab.com](/analysis/gitlab-integration/)
- [Bitbucket Server](/analysis/bitbucket-integration/)
- [Azure DevOps](/analysis/azuredevops-integration/)

[[info]]
| To decorate Pull Requests, a SonarQube analysis needs to be run on your code. You can find the additional parameters required for Pull Request analysis below in the **Analysis parameters** section.

## Pull request Quality Gate

A [Quality Gate](/user-guide/quality-gates/) lets you ensure you are meeting your organization's quality policy and that you can merge your pull request. The pull request uses your project Quality Gate as follows:
* **Focuses on new code** – The Pull Request quality gate only uses your project's quality gate conditions that apply to "on New Code" metrics.
* **Assigns a status** – Each Pull Request shows a quality gate status reflecting whether it Passed or Failed.

Pull request analyses on SonarQube are deleted automatically after 30 days with no analysis. This can be updated in **Administration > Configuration > General Settings > Housekeeping > Number of days before purging inactive branches**. 

## Analysis parameters

The following parameters enable PR analysis.

[[info]]
| Scanners running on Jenkins with the Branch Source plugin configured, GitLab CI/CD, Bitbucket Pipelines, Azure Pipelines, and Cirrus CI automatically detect these parameters, and you don't need to pass them manually.

| Parameter Name        | Description |
| --------------------- | ---------------------------------- |
| `sonar.pullrequest.key` | Unique identifier of your Pull Request. Must correspond to the key of the Pull Request in your ALM.<br/> e.g.: `sonar.pullrequest.key=5` |
| `sonar.pullrequest.branch` | The name of the branch that contains the changes to be merged.<br/> e.g.: `sonar.pullrequest.branch=feature/my-new-feature` |
| `sonar.pullrequest.base` | The branch into which the Pull Request will be merged. <br/> Default: master <br/> e.g.: `sonar.pullrequest.base=master` |