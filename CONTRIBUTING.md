## Pre-requisites

The project uses [Leiningen 2](http://leiningen.org) and requires RabbitMQ `3.0+` to be running
locally. Prior to running the tests, configure the RabbitMQ permissions
by running `./bin/ci/before_script.sh`. Make
sure you have those two installed and then run tests against all supported Clojure versions using

    lein all test

## Pull Requests

Then create a branch and make your changes on it. Once you are done with your changes and all
tests pass, write a [good, detailed commit message](http://tbaggery.com/2008/04/19/a-note-about-git-commit-messages.html) submit a pull request on GitHub.
