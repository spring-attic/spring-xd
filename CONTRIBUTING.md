Contributor Guidelines
======================

Have something you'd like to contribute to **Spring XD**? We welcome pull requests, seriously we love them! However, we ask that you carefully read this document first to understand how best to submit them; what kind of changes are likely to be accepted; and what to expect from the Spring team when evaluating your submission.

Please refer back to this document as a checklist before issuing any pull request; this will save time for everyone!

== Code of Conduct
This project adheres to the Contributor Covenant link:CODE_OF_CONDUCT.adoc[code of conduct].
By participating, you  are expected to uphold this code. Please report unacceptable behavior to
spring-code-of-conduct@pivotal.io.

## Understand the basics

Not sure what a *pull request* is, or how to submit one?  Take a look at GitHub's excellent [help documentation][] first.

## Search JIRA first; create an issue if necessary

Is there already an issue that addresses your concern?  Do a bit of searching in our [JIRA issue tracker][] to see if you can find something similar. If not, please create a new issue before submitting a pull request unless the change is truly trivial, e.g. typo fixes, removing compiler warnings, etc.

## Sign the contributor license agreement

Very important, before we can accept any *Spring XD contributions*, we will need you to sign the contributor license agreement (CLA). Signing the CLA does not grant anyone commit rights to the main repository, but it does mean that we can accept your contributions, and you will get an author credit if we do. In order to read and sign the CLA, please go to:

* [https://support.springsource.com/spring_committer_signup](https://support.springsource.com/spring_committer_signup)

For **Project**, please select **Spring XD**. The **Project Lead** is **Mark Pollack**.

Once you've completed the web form, simply add the following in a comment on your pull request:

    I have signed and agree to the terms of the SpringSource Individual
    Contributor License Agreement.

## Fork the Repository

1. Go to [https://github.com/SpringSource/spring-xd](https://github.com/SpringSource/spring-xd)
2. Hit the "fork" button and choose your own github account as the target
3. For more detail see [http://help.github.com/fork-a-repo/](http://help.github.com/fork-a-repo/)

## Setup your Local Development Environment

1. `git clone git@github.com:<your-github-username>/spring-xd.git`
2. `cd spring-xd`
3. `git remote show`
_you should see only 'origin' - which is the fork you created for your own github account_
4. `git remote add upstream git@github.com:SpringSource/spring-xd.git`
5. `git remote show`
_you should now see 'upstream' in addition to 'origin' where 'upstream' is the SpringSource repository from which releases are built_
6. `git fetch --all`
7. `git branch -a`
_you should see branches on `origin` as well as `upstream`, including `master` (`remotes/upstream/master`)_

## A Day in the Life of a Contributor

* _Always_ work on topic branches (Typically use the Jira ticket ID as the branch name).
  - For example, to create and switch to a new branch for issue XD-123: `git checkout -b XD-123`
* You might be working on several different topic branches at any given time, but when at a stopping point for one of those branches, commit (a local operation).
* Please follow the "Commit Guidelines" described in this chapter of Pro Git: [http://progit.org/book/ch5-2.html](http://progit.org/book/ch5-2.html)
* Then to begin working on another issue (say XD-101): `git checkout XD-101`. The _-b_ flag is not needed if that branch already exists in your local repository.
* When ready to resolve an issue or to collaborate with others, you can push your branch to origin (your fork), e.g.: `git push origin XD-123`
* If you want to collaborate with another contributor, have them fork your repository (add it as a remote) and `git fetch <your-username>` to grab your branch. Alternatively, they can use `git fetch --all` to sync their local state with all of their remotes.
* If you grant that collaborator push access to your repository, they can even apply their changes to your branch.
* When ready for your contribution to be reviewed for potential inclusion in the master branch of the canonical spring-xd repository (what you know as 'upstream'), issue a pull request to the SpringSource repository (for more detail, see [http://help.github.com/send-pull-requests/](http://help.github.com/send-pull-requests/)).
* The project lead may merge your changes into the upstream master branch as-is, he may keep the pull request open yet add a comment about something that should be modified, or he might reject the pull request by closing it.
* A prerequisite for any pull request is that it will be cleanly merge-able with the upstream master's current state. **This is the responsibility of any contributor.** If your pull request cannot be applied cleanly, the project lead will most likely add a comment requesting that you make it merge-able. For a full explanation, see the Pro Git section on rebasing: [http://progit.org/book/ch3-6.html](http://progit.org/book/ch3-6.html). As stated there: "> Often, you’ll do this to make sure your commits apply cleanly on a remote branch — perhaps in a project to which you’re trying to contribute but that you don’t maintain."

## Keeping your Local Code in Sync

* As mentioned above, you should always work on topic branches (since 'master' is a moving target). However, you do want to always keep your own 'origin' master branch in synch with the 'upstream' master.
* Within your local working directory, you can sync up all remotes' branches with: `git fetch --all`
* While on your own local master branch: `git pull upstream master` (which is the equivalent of fetching upstream/master and merging that into the branch you are in currently)
* Now that you're in synch, switch to the topic branch where you plan to work, e.g.: `git checkout -b XD-123`
* When you get to a stopping point: `git commit`
* If changes have occurred on the upstream/master while you were working you can synch again:
    - Switch back to master: `git checkout master`
    - Then: `git pull upstream master`
    - Switch back to the topic branch: `git checkout XD-123` (no -b needed since the branch already exists)
    - Rebase the topic branch to minimize the distance between it and your recently synched master branch: `git rebase master`
(Again, for more detail see the Pro Git section on rebasing: [http://progit.org/book/ch3-6.html](http://progit.org/book/ch3-6.html))
* **Note** Be careful with rebasing if you have already pushed your branch to your remote because you'd be rewriting history. If you rebase by mistake, you can undo it as discussed [in this stackoverflow discussion](http://stackoverflow.com/questions/134882/undoing-a-git-rebase). Once you have published your branch, you need to merge in the master rather than rebasing.
* Now, if you issue a pull request, it is much more likely to be merged without conflicts. Most likely, any pull request that would produce conflicts will be deferred until the issuer of that pull request makes these adjustments.
* Assuming your pull request is merged into the 'upstream' master, you will actually end up pulling that change into your own master eventually, and at that time, you may decide to delete the topic branch from your local repository and your fork (origin) if you pushed it there.
    - to delete the local branch: `git branch -d INT-123`
    - to delete the branch from your origin: `git push origin :INT-123`

## Maintain a linear commit history

When issuing pull requests, please ensure that your commit history is linear (using rebase). From the command line you can check this using:

````
log --graph --pretty=oneline
````

As this may cause lots of typing, we recommend creating a global alias, e.g. `git logg` for this:

````
git config --global alias.logg 'log --graph --pretty=oneline'
````

Here is an example of commit history that is not perfectly linear:

````
* | f34ac6bf714b85ad8a6cca308465dbfaef871702 XD-439 Add deletion of stream and stream defintions before and after each test method i
* |   148dfb52d781da31de70ce006268667d39ef1972 Merge branch 'xd439' of https://github.com/aclement/spring-xd into aclement-xd439
|\ \
| |/
|/|
| * aa44779bf5251bd62e03349c1bed2ffeb8c0948a XD-439: stream directory hooked up to parser
* | efbe517e74db2e89d5a3c779b40e8053f038e729 XD-585 - upgrade to spring social 1.1.0.M3
|/
* 2815df8ccf3e8f9d6a632741aa70aa543999513d XD-438: Use channel names in stream plugin.
````
If you see intersecting lines, that usually means that you forgot to rebase you branch. As mentioned earlier, **please rebase against master** before issuing a pull request. The result should be a commit history that looks more like this:

````
* 11cda45c8ed3b56ae1a3a4c1e91e5505f518824b XD-576-2 Refactor Target Handling in Shell
* 3358abcda97b6176b259d202eb565d184e132704 XD-591 Updating README for distribution zip
* 178b8204b14f4682f1e9bdd88f06785af9edf60e XD-591 Improve build file distribution tasks
* c507123bf40329f1de488404932e7dadbe6314e1 Fix tests on jdk6
* b5f9a3c6bc1a3852d9f2b2e7eace326afb47bffb Fix stream command tests - Fixed stream list assertion to check if they exist in the stre
* d02b4cfd5e44eff5d9c85a58348f8ccd8a09e6da XD-540 undeploy requests are now broadcast
````

## Mind the whitespace

Please carefully follow the whitespace and formatting conventions already present in the framework.

1. Tabs, not spaces
2. Unix (LF), not DOS (CRLF) line endings
3. Eliminate all trailing whitespace
4. Wrap Javadoc at 90 characters
5. Aim to wrap code at 90 characters, but favor readability over wrapping
6. Preserve existing formatting; i.e. do not reformat code for its own sake
7. Search the codebase using `git grep` and other tools to discover common
   naming conventions, etc.
8. Latin-1 (ISO-8859-1) encoding for Java sources; use `native2ascii` to convert
   if necessary

## Add Apache license header to all new classes

```java
/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ...;
```

## Update license header to modified files as necessary

Always check the date range in the Apache license header. For example, if you've modified a file in 2013 whose header still reads

```java
 * Copyright 2002-2011 the original author or authors.
```

then be sure to update it to 2013 appropriately

```java
 * Copyright 2002-2013 the original author or authors.
```

## Use @since tags

Use **@since** tags for newly-added public API types and methods e.g.

```java
/**
 * ...
 *
 * @author First Last
 * @since 1.0
 * @see ...
 */
```

## Submit JUnit test cases for all behavior changes

Search the codebase to find related unit tests and add additional **@Test** methods within. It is also acceptable to submit test cases on a per JIRA issue basis.

## Squash commits

Use `git rebase --interactive`, `git add --patch` and other tools to "squash" multiple commits into atomic changes. In addition to the man pages for git, there are many resources online to help you understand how these tools work. Here is one: http://book.git-scm.com/4_interactive_rebasing.html.

## Use your real name in git commits

Please configure *git* to use your real first and last name for any commits you intend to submit as pull requests. For example, this is not acceptable:

    Author: Nickname <user@mail.com>

Rather, please include your first and last name, properly capitalized, as submitted against the SpringSource contributor license agreement:

    Author: First Last <user@mail.com>

This helps ensure traceability against the CLA, and also goes a long way to ensuring useful output from tools like `git shortlog` and others.

You can configure this globally via the account admin area GitHub (useful for fork-and-edit cases); globally with

    git config --global user.name "First Last"
    git config --global user.email user@mail.com

or locally for the *spring-xd* repository only by omitting the '--global' flag:

    cd spring-xd
    git config user.name "First Last"
    git config user.email user@mail.com

## Run all tests prior to submission

See the [Building Spring XD][] section of the reference documentation for instructions. Make sure that all tests pass prior to submitting your pull request.

## Mention your pull request on the associated JIRA issue

Add a comment to the associated JIRA issue(s) linking to your new pull request.

[help documentation]: http://help.github.com/send-pull-requests
[JIRA issue tracker]: https://jira.springsource.org/browse/XD
[checking out and building]: https://github.com/SpringSource/spring-xd/wiki/Building-Spring-XD
