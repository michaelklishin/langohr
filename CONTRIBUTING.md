## Overview

This project **does not** use GitHub issues for questions, investigations, discussions, and so on.
Issues are appropriate for something specific enough for a maintainer or contributor to work on:

 * There should be enough information to reproduce the behavior observed in a reasonable amount of time
 * It should be reasonably clear why the behavior should be changed and why this cannot or should not be addressed
   in application code, a separate library and so on
   
 All issues that do not satisfy the above properties belong to the [Clojure RabbitMQ clients mailing list](http://groups.google.com/forum/#!forum/clojure-rabbitmq) or [RabbitMQ mailing list](http://groups.google.com/forum/#!forum/rabbitmq-users).
 Pull request that do not satisfy them have a high chance of being closed.
 
## Submitting a Pull Request

Please read the sections below to get an idea about how to run test suites first. Successfully
running all tests is an important first step for any contributor.

Once you have a passing test suite, create a branch and make your changes on it.
When you are done with your changes and all
tests pass, write a [good, detailed commit message](http://tbaggery.com/2008/04/19/a-note-about-git-commit-messages.html) submit a pull request on GitHub.

## Pre-requisites

The project uses [Leiningen 2](http://leiningen.org) and requires RabbitMQ `3.6+` to be running
locally with all defaults. Prior to running the tests, configure the RabbitMQ permissions
by running `./bin/ci/before_script.sh`. Make
sure you have those two installed and then run tests against all supported Clojure versions using

    lein all test
