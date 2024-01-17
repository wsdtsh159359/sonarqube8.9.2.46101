/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.scm.git;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.jgit.api.DiffCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.RefDatabase;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.notifications.AnalysisWarnings;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.LogAndArguments;
import org.sonar.api.utils.log.LogTester;

import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.api.utils.log.LoggerLevel.WARN;
import static org.sonar.scm.git.Utils.javaUnzip;

public class GitScmProviderTest {

  // Sample content for unified diffs
  // http://www.gnu.org/software/diffutils/manual/html_node/Example-Unified.html#Example-Unified
  private static final String CONTENT_LAO = "The Way that can be told of is not the eternal Way;\n"
    + "The name that can be named is not the eternal name.\n"
    + "The Nameless is the origin of Heaven and Earth;\n"
    + "The Named is the mother of all things.\n"
    + "Therefore let there always be non-being,\n"
    + "  so we may see their subtlety,\n"
    + "And let there always be being,\n"
    + "  so we may see their outcome.\n"
    + "The two are the same,\n"
    + "But after they are produced,\n"
    + "  they have different names.\n";

  private static final String CONTENT_TZU = "The Nameless is the origin of Heaven and Earth;\n"
    + "The named is the mother of all things.\n"
    + "\n"
    + "Therefore let there always be non-being,\n"
    + "  so we may see their subtlety,\n"
    + "And let there always be being,\n"
    + "  so we may see their outcome.\n"
    + "The two are the same,\n"
    + "But after they are produced,\n"
    + "  they have different names.\n"
    + "They both may be called deep and profound.\n"
    + "Deeper and more profound,\n"
    + "The door of all subtleties!";

  private static final String BRANCH_NAME = "branch";

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public LogTester logs = new LogTester();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private final GitIgnoreCommand gitIgnoreCommand = mock(GitIgnoreCommand.class);
  private static final Random random = new Random();
  private static final System2 system2 = mock(System2.class);

  private Path worktree;
  private Git git;
  private final AnalysisWarnings analysisWarnings = mock(AnalysisWarnings.class);

  @Before
  public void before() throws IOException, GitAPIException {
    worktree = temp.newFolder().toPath();
    Repository repo = FileRepositoryBuilder.create(worktree.resolve(".git").toFile());
    repo.create();

    git = new Git(repo);

    createAndCommitFile("file-in-first-commit.xoo");
  }

  @Test
  public void sanityCheck() {
    assertThat(newGitScmProvider().key()).isEqualTo("git");
  }

  @Test
  public void returnImplem() {
    JGitBlameCommand jblameCommand = new JGitBlameCommand(new PathResolver(), analysisWarnings);
    GitScmProvider gitScmProvider = new GitScmProvider(jblameCommand, analysisWarnings, gitIgnoreCommand, system2);

    assertThat(gitScmProvider.blameCommand()).isEqualTo(jblameCommand);
  }

  /**
   * SONARSCGIT-47
   */
  @Test
  public void branchChangedFiles_should_not_crash_if_branches_have_no_common_ancestors() throws GitAPIException, IOException {
    String fileName = "file-in-first-commit.xoo";
    String renamedName = "file-renamed.xoo";
    git.checkout().setOrphan(true).setName("b1").call();

    Path file = worktree.resolve(fileName);
    Path renamed = file.resolveSibling(renamedName);
    addLineToFile(fileName, 1);

    Files.move(file, renamed);
    git.rm().addFilepattern(fileName).call();
    commit(renamedName);

    Set<Path> files = newScmProvider().branchChangedFiles("master", worktree);

    // no shared history, so no diff
    assertThat(files).isNull();
  }

  @Test
  public void testAutodetection() throws IOException {
    File baseDirEmpty = temp.newFolder();
    assertThat(newGitScmProvider().supports(baseDirEmpty)).isFalse();

    File projectDir = temp.newFolder();
    javaUnzip("dummy-git.zip", projectDir);
    File baseDir = new File(projectDir, "dummy-git");
    assertThat(newScmProvider().supports(baseDir)).isTrue();
  }

  private static JGitBlameCommand mockCommand() {
    return mock(JGitBlameCommand.class);
  }

  @Test
  public void branchChangedFiles_from_diverged() throws IOException, GitAPIException {
    createAndCommitFile("file-m1.xoo");
    createAndCommitFile("file-m2.xoo");
    createAndCommitFile("file-m3.xoo");
    ObjectId forkPoint = git.getRepository().exactRef("HEAD").getObjectId();

    appendToAndCommitFile("file-m3.xoo");
    createAndCommitFile("file-m4.xoo");

    git.branchCreate().setName("b1").setStartPoint(forkPoint.getName()).call();
    git.checkout().setName("b1").call();
    createAndCommitFile("file-b1.xoo");
    appendToAndCommitFile("file-m1.xoo");
    deleteAndCommitFile("file-m2.xoo");

    assertThat(newScmProvider().branchChangedFiles("master", worktree))
      .containsExactlyInAnyOrder(
        worktree.resolve("file-b1.xoo"),
        worktree.resolve("file-m1.xoo"));
  }

  @Test
  public void branchChangedFiles_should_not_fail_with_patience_diff_algo() throws IOException {
    Path gitConfig = worktree.resolve(".git").resolve("config");
    Files.write(gitConfig, "[diff]\nalgorithm = patience\n".getBytes(StandardCharsets.UTF_8));
    Repository repo = FileRepositoryBuilder.create(worktree.resolve(".git").toFile());
    git = new Git(repo);

    assertThat(newScmProvider().branchChangedFiles("master", worktree)).isNull();
  }

  @Test
  public void branchChangedFiles_from_merged_and_diverged() throws IOException, GitAPIException {
    createAndCommitFile("file-m1.xoo");
    createAndCommitFile("file-m2.xoo");
    createAndCommitFile("lao.txt", CONTENT_LAO);
    ObjectId forkPoint = git.getRepository().exactRef("HEAD").getObjectId();

    createAndCommitFile("file-m3.xoo");
    ObjectId mergePoint = git.getRepository().exactRef("HEAD").getObjectId();

    appendToAndCommitFile("file-m3.xoo");
    createAndCommitFile("file-m4.xoo");

    git.branchCreate().setName("b1").setStartPoint(forkPoint.getName()).call();
    git.checkout().setName("b1").call();
    createAndCommitFile("file-b1.xoo");
    appendToAndCommitFile("file-m1.xoo");
    deleteAndCommitFile("file-m2.xoo");

    git.merge().include(mergePoint).call();
    createAndCommitFile("file-b2.xoo");

    createAndCommitFile("file-m5.xoo");
    deleteAndCommitFile("file-m5.xoo");

    Set<Path> changedFiles = newScmProvider().branchChangedFiles("master", worktree);
    assertThat(changedFiles)
      .containsExactlyInAnyOrder(
        worktree.resolve("file-m1.xoo"),
        worktree.resolve("file-b1.xoo"),
        worktree.resolve("file-b2.xoo"));

    // use a subset of changed files for .branchChangedLines to verify only requested files are returned
    assertThat(changedFiles.remove(worktree.resolve("file-b1.xoo"))).isTrue();

    // generate common sample diff
    createAndCommitFile("lao.txt", CONTENT_TZU);
    changedFiles.add(worktree.resolve("lao.txt"));

    // a file that should not yield any results
    changedFiles.add(worktree.resolve("nonexistent"));

    assertThat(newScmProvider().branchChangedLines("master", worktree, changedFiles))
      .containsOnly(
        entry(worktree.resolve("lao.txt"), new HashSet<>(Arrays.asList(2, 3, 11, 12, 13))),
        entry(worktree.resolve("file-m1.xoo"), new HashSet<>(Arrays.asList(4))),
        entry(worktree.resolve("file-b2.xoo"), new HashSet<>(Arrays.asList(1, 2, 3))));

    assertThat(newScmProvider().branchChangedLines("master", worktree, Collections.singleton(worktree.resolve("nonexistent"))))
      .isEmpty();
  }

  @Test
  public void forkDate_from_diverged() throws IOException, GitAPIException {
    createAndCommitFile("file-m1.xoo", Instant.now().minus(8, ChronoUnit.DAYS));
    createAndCommitFile("file-m2.xoo", Instant.now().minus(7, ChronoUnit.DAYS));
    Instant expectedForkDate = Instant.now().minus(6, ChronoUnit.DAYS);
    createAndCommitFile("file-m3.xoo", expectedForkDate);
    ObjectId forkPoint = git.getRepository().exactRef("HEAD").getObjectId();

    appendToAndCommitFile("file-m3.xoo");
    createAndCommitFile("file-m4.xoo");

    git.branchCreate().setName("b1").setStartPoint(forkPoint.getName()).call();
    git.checkout().setName("b1").call();
    createAndCommitFile("file-b1.xoo");
    appendToAndCommitFile("file-m1.xoo");
    deleteAndCommitFile("file-m2.xoo");

    assertThat(newScmProvider().forkDate("master", worktree))
      .isEqualTo(expectedForkDate.truncatedTo(ChronoUnit.SECONDS));
  }

  @Test
  public void forkDate_should_not_fail_if_reference_is_the_same_branch() throws IOException, GitAPIException {
    createAndCommitFile("file-m1.xoo", Instant.now().minus(8, ChronoUnit.DAYS));
    createAndCommitFile("file-m2.xoo", Instant.now().minus(7, ChronoUnit.DAYS));

    ObjectId forkPoint = git.getRepository().exactRef("HEAD").getObjectId();
    git.branchCreate().setName("b1").setStartPoint(forkPoint.getName()).call();
    git.checkout().setName("b1").call();

    Instant expectedForkDate = Instant.now().minus(6, ChronoUnit.DAYS);
    createAndCommitFile("file-m3.xoo", expectedForkDate);

    assertThat(newScmProvider().forkDate("b1", worktree))
      .isEqualTo(expectedForkDate.truncatedTo(ChronoUnit.SECONDS));
  }

  @Test
  public void forkDate_should_not_fail_with_patience_diff_algo() throws IOException {
    Path gitConfig = worktree.resolve(".git").resolve("config");
    Files.write(gitConfig, "[diff]\nalgorithm = patience\n".getBytes(StandardCharsets.UTF_8));
    Repository repo = FileRepositoryBuilder.create(worktree.resolve(".git").toFile());
    git = new Git(repo);

    assertThat(newScmProvider().forkDate("master", worktree)).isNull();
  }

  @Test
  public void forkDate_should_not_fail_with_invalid_basedir() throws IOException {
    assertThat(newScmProvider().forkDate("master", temp.newFolder().toPath())).isNull();
  }

  @Test
  public void forkDate_should_not_fail_when_no_merge_base_is_found() throws IOException, GitAPIException {
    createAndCommitFile("file-m1.xoo", Instant.now().minus(8, ChronoUnit.DAYS));

    git.checkout().setOrphan(true).setName("b1").call();
    createAndCommitFile("file-b1.xoo");

    assertThat(newScmProvider().forkDate("master", worktree)).isNull();
  }

  @Test
  public void forkDate_without_target_branch() throws IOException, GitAPIException {
    createAndCommitFile("file-m1.xoo", Instant.now().minus(8, ChronoUnit.DAYS));
    createAndCommitFile("file-m2.xoo", Instant.now().minus(7, ChronoUnit.DAYS));
    Instant expectedForkDate = Instant.now().minus(6, ChronoUnit.DAYS);
    createAndCommitFile("file-m3.xoo", expectedForkDate);
    ObjectId forkPoint = git.getRepository().exactRef("HEAD").getObjectId();

    appendToAndCommitFile("file-m3.xoo");
    createAndCommitFile("file-m4.xoo");

    git.branchCreate().setName("b1").setStartPoint(forkPoint.getName()).call();
    git.checkout().setName("b1").call();
    createAndCommitFile("file-b1.xoo");
    appendToAndCommitFile("file-m1.xoo");
    deleteAndCommitFile("file-m2.xoo");

    assertThat(newScmProvider().forkDate("unknown", worktree)).isNull();
  }

  @Test
  public void branchChangedLines_should_be_correct_when_change_is_not_committed() throws GitAPIException, IOException {
    String fileName = "file-in-first-commit.xoo";
    git.branchCreate().setName("b1").call();
    git.checkout().setName("b1").call();

    // this line is committed
    addLineToFile(fileName, 3);
    commit(fileName);

    // this line is not committed
    addLineToFile(fileName, 1);

    Path filePath = worktree.resolve(fileName);
    Map<Path, Set<Integer>> changedLines = newScmProvider().branchChangedLines("master", worktree, Collections.singleton(filePath));

    // both lines appear correctly
    assertThat(changedLines).containsExactly(entry(filePath, new HashSet<>(Arrays.asList(1, 4))));
  }

  @Test
  public void branchChangedLines_should_not_fail_if_there_is_no_merge_base() throws GitAPIException, IOException {
    createAndCommitFile("file-m1.xoo");
    git.checkout().setOrphan(true).setName("b1").call();
    createAndCommitFile("file-b1.xoo");

    Map<Path, Set<Integer>> changedLines = newScmProvider().branchChangedLines("master", worktree, Collections.singleton(Paths.get("")));
    assertThat(changedLines).isNull();
  }

  @Test
  public void branchChangedLines_returns_empty_set_for_files_with_lines_removed_only() throws GitAPIException, IOException {
    String fileName = "file-in-first-commit.xoo";
    git.branchCreate().setName("b1").call();
    git.checkout().setName("b1").call();

    removeLineInFile(fileName, 2);
    commit(fileName);

    Path filePath = worktree.resolve(fileName);
    Map<Path, Set<Integer>> changedLines = newScmProvider().branchChangedLines("master", worktree, Collections.singleton(filePath));

    // both lines appear correctly
    assertThat(changedLines).containsExactly(entry(filePath, emptySet()));
  }

  @Test
  public void branchChangedLines_uses_relative_paths_from_project_root() throws GitAPIException, IOException {
    String fileName = "project1/file-in-first-commit.xoo";
    createAndCommitFile(fileName);

    git.branchCreate().setName("b1").call();
    git.checkout().setName("b1").call();

    // this line is committed
    addLineToFile(fileName, 3);
    commit(fileName);

    // this line is not committed
    addLineToFile(fileName, 1);

    Path filePath = worktree.resolve(fileName);
    Map<Path, Set<Integer>> changedLines = newScmProvider().branchChangedLines("master",
      worktree.resolve("project1"), Collections.singleton(filePath));

    // both lines appear correctly
    assertThat(changedLines).containsExactly(entry(filePath, new HashSet<>(Arrays.asList(1, 4))));
  }

  @Test
  public void branchChangedFiles_when_git_work_tree_is_above_project_basedir() throws IOException, GitAPIException {
    git.branchCreate().setName("b1").call();
    git.checkout().setName("b1").call();

    Path projectDir = worktree.resolve("project");
    Files.createDirectory(projectDir);
    createAndCommitFile("project/file-b1");
    assertThat(newScmProvider().branchChangedFiles("master", projectDir))
      .containsOnly(projectDir.resolve("file-b1"));
  }

  @Test
  public void branchChangedLines_should_not_fail_with_patience_diff_algo() throws IOException {
    Path gitConfig = worktree.resolve(".git").resolve("config");
    Files.write(gitConfig, "[diff]\nalgorithm = patience\n".getBytes(StandardCharsets.UTF_8));
    Repository repo = FileRepositoryBuilder.create(worktree.resolve(".git").toFile());
    git = new Git(repo);

    assertThat(newScmProvider().branchChangedLines("master", worktree, Collections.singleton(Paths.get("file")))).isNull();
  }

  /**
   * Unfortunately it looks like JGit doesn't support this setting using .gitattributes.
   */
  @Test
  public void branchChangedLines_should_always_ignore_different_line_endings() throws IOException, GitAPIException {
    Path filePath = worktree.resolve("file-m1.xoo");

    createAndCommitFile("file-m1.xoo");
    ObjectId forkPoint = git.getRepository().exactRef("HEAD").getObjectId();

    git.branchCreate().setName("b1").setStartPoint(forkPoint.getName()).call();
    git.checkout().setName("b1").call();

    String newFileContent = new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8).replaceAll("\n", "\r\n");
    Files.write(filePath, newFileContent.getBytes(StandardCharsets.UTF_8), StandardOpenOption.TRUNCATE_EXISTING);
    commit("file-m1.xoo");

    assertThat(newScmProvider().branchChangedLines("master", worktree, Collections.singleton(filePath)))
      .isEmpty();
  }

  @Test
  public void branchChangedFiles_falls_back_to_origin_when_local_branch_does_not_exist() throws IOException, GitAPIException {
    git.branchCreate().setName("b1").call();
    git.checkout().setName("b1").call();
    createAndCommitFile("file-b1");

    Path worktree2 = temp.newFolder().toPath();
    Git.cloneRepository()
      .setURI(worktree.toString())
      .setDirectory(worktree2.toFile())
      .call();

    assertThat(newScmProvider().branchChangedFiles("master", worktree2))
      .containsOnly(worktree2.resolve("file-b1"));
    verifyNoInteractions(analysisWarnings);
  }

  @Test
  public void branchChangedFiles_use_remote_target_ref_when_running_on_circle_ci() throws IOException, GitAPIException {
    when(system2.envVariable("CIRCLECI")).thenReturn("true");
    git.checkout().setName("b1").setCreateBranch(true).call();
    createAndCommitFile("file-b1");

    Path worktree2 = temp.newFolder().toPath();
    Git local = Git.cloneRepository()
      .setURI(worktree.toString())
      .setDirectory(worktree2.toFile())
      .call();

    // Make local master match analyzed branch, so if local ref is used then change files will be empty
    local.checkout().setCreateBranch(true).setName("master").setStartPoint("origin/b1").call();
    local.checkout().setName("b1").call();

    assertThat(newScmProvider().branchChangedFiles("master", worktree2))
      .containsOnly(worktree2.resolve("file-b1"));
    verifyNoInteractions(analysisWarnings);
  }

  @Test
  public void branchChangedFiles_falls_back_to_local_ref_if_origin_branch_does_not_exist_when_running_on_circle_ci() throws IOException, GitAPIException {
    when(system2.envVariable("CIRCLECI")).thenReturn("true");
    git.checkout().setName("b1").setCreateBranch(true).call();
    createAndCommitFile("file-b1");

    Path worktree2 = temp.newFolder().toPath();
    Git local = Git.cloneRepository()
      .setURI(worktree.toString())
      .setDirectory(worktree2.toFile())
      .call();

    local.checkout().setName("local-only").setCreateBranch(true).setStartPoint("origin/master").call();
    local.checkout().setName("b1").call();

    assertThat(newScmProvider().branchChangedFiles("local-only", worktree2))
      .containsOnly(worktree2.resolve("file-b1"));
    verifyNoInteractions(analysisWarnings);
  }

  @Test
  public void branchChangedFiles_falls_back_to_upstream_ref() throws IOException, GitAPIException {
    git.branchCreate().setName("b1").call();
    git.checkout().setName("b1").call();
    createAndCommitFile("file-b1");

    Path worktree2 = temp.newFolder().toPath();
    Git.cloneRepository()
      .setURI(worktree.toString())
      .setRemote("upstream")
      .setDirectory(worktree2.toFile())
      .call();

    assertThat(newScmProvider().branchChangedFiles("master", worktree2))
      .containsOnly(worktree2.resolve("file-b1"));
    verifyNoInteractions(analysisWarnings);

  }

  @Test
  public void branchChangedFiles_finds_branch_in_specific_origin() throws IOException, GitAPIException {
    git.branchCreate().setName("b1").call();
    git.checkout().setName("b1").call();
    createAndCommitFile("file-b1");

    Path worktree2 = temp.newFolder().toPath();
    Git.cloneRepository()
      .setURI(worktree.toString())
      .setRemote("upstream")
      .setDirectory(worktree2.toFile())
      .call();

    assertThat(newScmProvider().branchChangedFiles("upstream/master", worktree2))
      .containsOnly(worktree2.resolve("file-b1"));
    verifyNoInteractions(analysisWarnings);
  }

  @Test
  public void branchChangedFiles_should_return_null_when_branch_nonexistent() {
    assertThat(newScmProvider().branchChangedFiles("nonexistent", worktree)).isNull();
  }

  @Test
  public void branchChangedFiles_should_throw_when_repo_nonexistent() throws IOException {
    thrown.expect(MessageException.class);
    thrown.expectMessage("Not inside a Git work tree: ");
    newScmProvider().branchChangedFiles("master", temp.newFolder().toPath());
  }

  @Test
  public void branchChangedFiles_should_throw_when_dir_nonexistent() {
    thrown.expect(MessageException.class);
    thrown.expectMessage("Not inside a Git work tree: ");
    newScmProvider().branchChangedFiles("master", temp.getRoot().toPath().resolve("nonexistent"));
  }

  @Test
  public void branchChangedFiles_should_return_null_on_io_errors_of_repo_builder() {
    GitScmProvider provider = new GitScmProvider(mockCommand(), analysisWarnings, gitIgnoreCommand, system2) {
      @Override
      Repository buildRepo(Path basedir) throws IOException {
        throw new IOException();
      }
    };
    assertThat(provider.branchChangedFiles(BRANCH_NAME, worktree)).isNull();
    verifyNoInteractions(analysisWarnings);
  }

  @Test
  public void branchChangedFiles_should_return_null_if_repo_exactref_is_null() throws IOException {
    Repository repository = mock(Repository.class);
    RefDatabase refDatabase = mock(RefDatabase.class);
    when(repository.getRefDatabase()).thenReturn(refDatabase);
    when(refDatabase.findRef(BRANCH_NAME)).thenReturn(null);

    GitScmProvider provider = new GitScmProvider(mockCommand(), analysisWarnings, gitIgnoreCommand, system2) {
      @Override
      Repository buildRepo(Path basedir) {
        return repository;
      }
    };
    assertThat(provider.branchChangedFiles(BRANCH_NAME, worktree)).isNull();

    String refNotFound = "Could not find ref 'branch' in refs/heads, refs/remotes, refs/remotes/upstream or refs/remotes/origin";

    LogAndArguments warnLog = logs.getLogs(WARN).get(0);
    assertThat(warnLog.getRawMsg()).isEqualTo(refNotFound);

    String warning = refNotFound
      + ". You may see unexpected issues and changes. Please make sure to fetch this ref before pull request analysis"
      + " and refer to <a href=\"/documentation/analysis/scm-integration/\" target=\"_blank\">the documentation</a>.";
    verify(analysisWarnings).addUnique(warning);
  }

  @Test
  public void branchChangedFiles_should_return_null_on_errors() throws GitAPIException {
    DiffCommand diffCommand = mock(DiffCommand.class);
    when(diffCommand.setShowNameAndStatusOnly(anyBoolean())).thenReturn(diffCommand);
    when(diffCommand.setOldTree(any())).thenReturn(diffCommand);
    when(diffCommand.setNewTree(any())).thenReturn(diffCommand);
    when(diffCommand.call()).thenThrow(mock(GitAPIException.class));

    Git git = mock(Git.class);
    when(git.diff()).thenReturn(diffCommand);

    GitScmProvider provider = new GitScmProvider(mockCommand(), analysisWarnings, gitIgnoreCommand, system2) {
      @Override
      Git newGit(Repository repo) {
        return git;
      }
    };
    assertThat(provider.branchChangedFiles("master", worktree)).isNull();
    verify(diffCommand).call();
  }

  @Test
  public void branchChangedLines_returns_null_when_branch_doesnt_exist() {
    assertThat(newScmProvider().branchChangedLines("nonexistent", worktree, emptySet())).isNull();
  }

  @Test
  public void branchChangedLines_omits_files_with_git_api_errors() throws IOException, GitAPIException {
    String f1 = "file-in-first-commit.xoo";
    String f2 = "file2-in-first-commit.xoo";

    createAndCommitFile(f2);

    git.branchCreate().setName("b1").call();
    git.checkout().setName("b1").call();

    // both files modified
    addLineToFile(f1, 1);
    addLineToFile(f2, 2);

    commit(f1);
    commit(f2);

    AtomicInteger callCount = new AtomicInteger(0);
    GitScmProvider provider = new GitScmProvider(mockCommand(), analysisWarnings, gitIgnoreCommand, system2) {
      @Override
      AbstractTreeIterator prepareTreeParser(Repository repo, RevCommit commit) throws IOException {
        if (callCount.getAndIncrement() == 1) {
          throw new RuntimeException("error");
        }
        return super.prepareTreeParser(repo, commit);
      }
    };
    Set<Path> changedFiles = new LinkedHashSet<>();
    changedFiles.add(worktree.resolve(f1));
    changedFiles.add(worktree.resolve(f2));

    assertThat(provider.branchChangedLines("master", worktree, changedFiles))
      .isEqualTo(Collections.singletonMap(worktree.resolve(f1), Collections.singleton(1)));
  }

  @Test
  public void branchChangedLines_returns_null_on_io_errors_of_repo_builder() {
    GitScmProvider provider = new GitScmProvider(mockCommand(), analysisWarnings, gitIgnoreCommand, system2) {
      @Override
      Repository buildRepo(Path basedir) throws IOException {
        throw new IOException();
      }
    };
    assertThat(provider.branchChangedLines(BRANCH_NAME, worktree, emptySet())).isNull();
  }

  @Test
  public void relativePathFromScmRoot_should_return_dot_project_root() {
    assertThat(newGitScmProvider().relativePathFromScmRoot(worktree)).isEqualTo(Paths.get(""));
  }

  private GitScmProvider newGitScmProvider() {
    return new GitScmProvider(mock(JGitBlameCommand.class), analysisWarnings, gitIgnoreCommand, system2);
  }

  @Test
  public void relativePathFromScmRoot_should_return_filename_for_file_in_project_root() throws IOException {
    Path filename = Paths.get("somefile.xoo");
    Path path = worktree.resolve(filename);
    Files.createFile(path);
    assertThat(newGitScmProvider().relativePathFromScmRoot(path)).isEqualTo(filename);
  }

  @Test
  public void relativePathFromScmRoot_should_return_relative_path_for_file_in_project_subdir() throws IOException {
    Path relpath = Paths.get("sub/dir/to/somefile.xoo");
    Path path = worktree.resolve(relpath);
    Files.createDirectories(path.getParent());
    Files.createFile(path);
    assertThat(newGitScmProvider().relativePathFromScmRoot(path)).isEqualTo(relpath);
  }

  @Test
  public void revisionId_should_return_different_sha1_after_commit() throws IOException, GitAPIException {
    Path projectDir = worktree.resolve("project");
    Files.createDirectory(projectDir);

    GitScmProvider provider = newGitScmProvider();

    String sha1before = provider.revisionId(projectDir);
    assertThat(sha1before).hasSize(40);

    createAndCommitFile("project/file1");
    String sha1after = provider.revisionId(projectDir);
    assertThat(sha1after)
      .hasSize(40)
      .isNotEqualTo(sha1before);
    assertThat(provider.revisionId(projectDir)).isEqualTo(sha1after);
  }

  @Test
  public void revisionId_should_return_null_in_empty_repo() throws IOException {
    worktree = temp.newFolder().toPath();
    Repository repo = FileRepositoryBuilder.create(worktree.resolve(".git").toFile());
    repo.create();

    git = new Git(repo);

    Path projectDir = worktree.resolve("project");
    Files.createDirectory(projectDir);

    GitScmProvider provider = newGitScmProvider();

    assertThat(provider.revisionId(projectDir)).isNull();
  }

  private String randomizedContent(String prefix, int numLines) {
    StringBuilder sb = new StringBuilder();
    for (int line = 0; line < numLines; line++) {
      sb.append(randomizedLine(prefix));
      sb.append("\n");
    }
    return sb.toString();
  }

  private String randomizedLine(String prefix) {
    StringBuilder sb = new StringBuilder(prefix);
    for (int i = 0; i < 4; i++) {
      sb.append(' ');
      for (int j = 0; j < prefix.length(); j++) {
        sb.append((char) ('a' + random.nextInt(26)));
      }
    }
    return sb.toString();
  }

  private void createAndCommitFile(String relativePath) throws IOException, GitAPIException {
    createAndCommitFile(relativePath, randomizedContent(relativePath, 3));
  }

  private void createAndCommitFile(String relativePath, Instant commitDate) throws IOException, GitAPIException {
    createFile(relativePath, randomizedContent(relativePath, 3));
    commit(relativePath, commitDate);
  }

  private void createAndCommitFile(String relativePath, String content) throws IOException, GitAPIException {
    createFile(relativePath, content);
    commit(relativePath);
  }

  private void createFile(String relativePath, String content) throws IOException {
    Path newFile = worktree.resolve(relativePath);
    Files.createDirectories(newFile.getParent());
    Files.write(newFile, content.getBytes(), StandardOpenOption.CREATE);
  }

  private void addLineToFile(String relativePath, int lineNumber) throws IOException {
    Path filePath = worktree.resolve(relativePath);
    List<String> lines = Files.readAllLines(filePath);
    lines.add(lineNumber - 1, randomizedLine(relativePath));
    Files.write(filePath, lines, StandardOpenOption.TRUNCATE_EXISTING);
  }

  private void removeLineInFile(String relativePath, int lineNumber) throws IOException {
    Path filePath = worktree.resolve(relativePath);
    List<String> lines = Files.readAllLines(filePath);
    lines.remove(lineNumber - 1);
    Files.write(filePath, lines, StandardOpenOption.TRUNCATE_EXISTING);
  }

  private void appendToAndCommitFile(String relativePath) throws IOException, GitAPIException {
    Files.write(worktree.resolve(relativePath), randomizedContent(relativePath, 1).getBytes(), StandardOpenOption.APPEND);
    commit(relativePath);
  }

  private void deleteAndCommitFile(String relativePath) throws GitAPIException {
    git.rm().addFilepattern(relativePath).call();
    commit(relativePath);
  }

  private void commit(String... relativePaths) throws GitAPIException {
    for (String path : relativePaths) {
      git.add().addFilepattern(path).call();
    }
    String msg = String.join(",", relativePaths);
    git.commit().setAuthor("joe", "joe@example.com").setMessage(msg).call();
  }

  private void commit(String relativePath, Instant date) throws GitAPIException {
    PersonIdent person = new PersonIdent("joe", "joe@example.com", Date.from(date), TimeZone.getDefault());
    git.commit().setAuthor(person).setCommitter(person).setMessage(relativePath).call();
  }

  private GitScmProvider newScmProvider() {
    return new GitScmProvider(mockCommand(), analysisWarnings, gitIgnoreCommand, system2);
  }
}
